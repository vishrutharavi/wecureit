import { auth } from './firebase';
const API_BASE = "http://localhost:8080";

// single refresh-in-progress promise to avoid multiple parallel token refreshes
let tokenRefreshPromise: Promise<string | undefined> | null = null;

async function ensureFreshToken(): Promise<string | undefined> {
  if (typeof window === 'undefined' || !auth || !auth.currentUser) return undefined;
  if (!tokenRefreshPromise) {
    tokenRefreshPromise = auth.currentUser.getIdToken(true)
      .then((t) => {
        try { localStorage.setItem('patientToken', t); } catch {}
        tokenRefreshPromise = null;
        return t;
      })
      .catch((e) => {
        tokenRefreshPromise = null;
        throw e;
      });
  }
  return tokenRefreshPromise;
}

export async function apiFetch(
  path: string,
  token?: string,
  options: RequestInit = {}
) {
  // Defensive guard: avoid sending requests that embed a null/undefined doctorId
  // (many components build URLs from localStorage doctorProfile.id which may be null).
  // Sending e.g. /api/doctors/null/availability causes Spring to try to parse "null" as UUID
  // and results in MethodArgumentTypeMismatchException; catch that early and provide
  // a clearer client-side error so the UI can handle it gracefully.
  try {
    if (typeof path === 'string') {
      const lower = path.toLowerCase();
      if (lower.includes('/doctors/null') || lower.includes('/doctors/undefined') || /doctorid=(null|undefined)/i.test(lower)) {
        const msg = 'Missing doctor id in request URL. Please re-open the doctor dashboard or re-login.';
        try { if (typeof window !== 'undefined' && process.env.NODE_ENV !== 'production') console.warn('apiFetch blocked request with null doctorId:', path); } catch {}
        try { showInlineToast(msg); } catch {}
        throw new Error(msg);
      }
    }
  } catch {
    // if our guard check itself fails for some reason, continue normally and let fetch handle errors
  }
  // if caller didn't pass a token, try common localStorage keys (patientToken, doctorToken, idToken)
  // If caller didn't pass a token, prefer obtaining a fresh ID token from Firebase if a user is signed in.
  if (!token && typeof window !== 'undefined' && auth && auth.currentUser) {
    try {
      // Ensure only one parallel refresh runs and have callers await it to avoid races
      const fresh = await ensureFreshToken();
      if (fresh) {
        token = fresh;
        try { localStorage.setItem('patientToken', fresh); } catch {}
        console.debug('apiFetch: using fresh id token from firebase.currentUser');
      }
    } catch (e) {
      // If refresh fails for some reason, fall back to any token saved in localStorage.
      console.warn('apiFetch: failed to refresh id token, falling back to stored tokens', e);
      try {
        token = localStorage.getItem('patientToken') ?? localStorage.getItem('doctorToken') ?? localStorage.getItem('idToken') ?? undefined;
      } catch {
        // ignore storage errors
      }
    }
  } else if (!token && typeof window !== 'undefined') {
    try {
      token = localStorage.getItem('patientToken') ?? localStorage.getItem('doctorToken') ?? localStorage.getItem('idToken') ?? undefined;
    } catch {
      // ignore storage errors
    }
  }
  // Dev debug: show whether we have a token and a short preview (first 8 chars) to help diagnose 403/401 issues.
  try {
    if (typeof window !== 'undefined') {
      // Always print a short debug line locally so developers can confirm whether the
      // Authorization header will be included. Do not expose full token.
      try { console.debug('apiFetch ->', path, 'hasToken=', !!token, token ? token.slice(0,8) + '...' : null); } catch {}
    }
  } catch {}

  // Final check before sending request: log outgoing Authorization header presence
  try {
    if (typeof window !== 'undefined') {
      const authPreview = token ? (token.slice ? token.slice(0,8) + '...' : 'present') : null;
      console.debug('apiFetch - will send Authorization header:', !!token, authPreview, 'for', path);
    }
  } catch {}

  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });

  // If 401 received, attempt one refresh using Firebase client SDK and retry (covers expired tokens or missing header scenarios)
  if (res.status === 401) {
    try {
      if (typeof window !== 'undefined' && auth && auth.currentUser) {
        try {
          // use the centralized refresh so concurrent callers wait instead of firing multiple refreshes
          try { if (typeof window !== 'undefined' && process.env.NODE_ENV !== 'production') console.debug('apiFetch: 401 -> refreshing token'); } catch {}
          const newTok = await ensureFreshToken();
          if (newTok) {
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
            const text = await retryRes.text();
            try { if (typeof window !== 'undefined' && process.env.NODE_ENV !== 'production') console.warn('apiFetch: retry after 401 ->', retryRes.status, text); } catch {}
            throw new Error(`API ${retryRes.status}: ${text}`);
          }
        } catch (refreshErr) {
          console.warn('Token refresh failed', refreshErr);
          // continue to normal error handling below
        }
      }
    } catch {
      // ignore and fall through to error handling
    }
  }

  // If 403 received, it may be caused by a missing/old custom claim on the token (role not present).
  // Try one forced token refresh and retry the request once when a Firebase user is present.
  if (res.status === 403) {
    try {
      if (typeof window !== 'undefined' && auth && auth.currentUser) {
        try {
          try { if (typeof window !== 'undefined' && process.env.NODE_ENV !== 'production') console.debug('apiFetch: 403 -> refreshing token and retrying'); } catch {}
          const newTok = await ensureFreshToken();
          if (newTok) {
            try { localStorage.setItem('patientToken', newTok); } catch {}
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
            const text = await retryRes.text();
            try { if (typeof window !== 'undefined' && process.env.NODE_ENV !== 'production') console.warn('apiFetch: retry after 403 ->', retryRes.status, text); } catch {}
            throw new Error(`API ${retryRes.status}: ${text}`);
          }
        } catch (e) {
          console.warn('apiFetch: 403 retry attempt failed', e);
        }
      }
    } catch {
      // ignore and proceed to normal error handling
    }
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
