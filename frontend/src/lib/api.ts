import { auth } from './firebase';
const API_BASE = "http://localhost:8080";

export async function apiFetch(
  path: string,
  token?: string,
  options: RequestInit = {}
) {
  // if caller didn't pass a token, try common localStorage keys (patientToken, doctorToken, idToken)
  if (!token && typeof window !== 'undefined') {
    try {
      token = localStorage.getItem('patientToken') ?? localStorage.getItem('doctorToken') ?? localStorage.getItem('idToken') ?? undefined;
    } catch {
      // ignore storage errors
    }
  }
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });

  // If token expired, try one automatic refresh using Firebase client SDK and retry.
  if (res.status === 401) {
    // try to parse body for 'expired' hint
    const bodyText = await res.text();
    if (/expired/i.test(bodyText)) {
      try {
        if (typeof window !== 'undefined' && auth && auth.currentUser) {
          const newTok = await auth.currentUser.getIdToken(true);
          try { localStorage.setItem('patientToken', newTok); } catch {}
          // retry original request once with refreshed token
          const retryRes = await fetch(`${API_BASE}${path}`, {
            ...options,
            headers: {
              "Content-Type": "application/json",
              Authorization: `Bearer ${newTok}`,
              ...options.headers,
            },
          });
          if (retryRes.ok) {
            const t = await retryRes.text();
            if (!t) return null;
            try { return JSON.parse(t); } catch { return t; }
          }
          // fall through to normal error handling for the retry response
          // set res variable to retryRes for downstream handling
          // (can't reassign const res, so we'll proceed to parse retryRes below)
          const text = await retryRes.text();
          throw new Error(`API ${retryRes.status}: ${text}`);
        }
      } catch (refreshErr) {
        // failed to refresh — continue to normal error handling below
        console.warn('Token refresh failed', refreshErr);
      }
    }
    // if not expired or refresh failed, fall through to error handling
    // restore response body text for downstream parsing
    try { /* noop */ } catch {}
  }

  if (!res.ok) {
    const text = await res.text();
    // Build a friendly message from response body if possible.
    let friendly = text && text.length > 0 ? text : null;
    try {
      // try parse JSON like { error: '...' }
      const parsed = JSON.parse(text || "null");
      if (parsed && typeof parsed === 'object') {
        if (parsed.error) friendly = parsed.error;
        else if (parsed.message) friendly = parsed.message;
      }
    } catch {}

    // Map certain statuses to clearer user-facing messages
    let displayMsg: string | null = null;
    // detect duplicate-like messages even if status is 403 (some flows may return 403 with a duplicate message)
    const friendlyText = String(friendly || '').replace(/^"|"$/g, '');
    const isDuplicateMsg = /duplicate|already exists|already added|conflict/i.test(friendlyText);

    if (isDuplicateMsg) {
      displayMsg = 'Availability already exists for the selected facility, date and time.';
    } else if (res.status === 401) {
      displayMsg = friendlyText || 'Unauthorized. Please log in.';
    } else if (res.status === 403) {
      displayMsg = friendlyText || 'Access denied. You do not have permission to perform this action.';
    } else if (res.status === 409) {
      displayMsg = friendlyText || 'Conflict with existing resource.';
    }

    try {
      if (displayMsg) showInlineToast(displayMsg);
    } catch {}

    throw new Error(`API ${res.status}: ${text}`);
  }

  // Some endpoints (DELETE/PATCH) may return empty body (204 No Content) — avoid calling json() on empty responses
  const text = await res.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    // If response is not JSON, return raw text (some endpoints return plain text)
    return text;
  }
}

export function showInlineToast(message: string) {
  try {
    if (typeof window === 'undefined' || !document) return;
    const id = 'wecureit-api-toast';
    // remove existing toast if present
    const prev = document.getElementById(id);
    if (prev) prev.remove();

    const el = document.createElement('div');
    el.id = id;
    el.textContent = message;
    Object.assign(el.style, {
      position: 'fixed',
      right: '20px',
      top: '20px',
      zIndex: '9999',
      background: 'rgba(0,0,0,0.8)',
      color: '#fff',
      padding: '10px 14px',
      borderRadius: '6px',
      fontSize: '14px',
      boxShadow: '0 4px 12px rgba(0,0,0,0.12)',
      opacity: '0',
      transition: 'opacity 180ms ease-in-out',
    });
    document.body.appendChild(el);
    // trigger fade-in
    requestAnimationFrame(() => { el.style.opacity = '1'; });
    setTimeout(() => {
      el.style.opacity = '0';
      setTimeout(() => el.remove(), 300);
    }, 4000);
  } catch {
    // swallow
  }
}
