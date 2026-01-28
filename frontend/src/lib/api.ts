const API_BASE = "http://localhost:8080";

export async function apiFetch(
  path: string,
  token?: string,
  options: RequestInit = {}
) {
  const res = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });

  if (!res.ok) {
    const text = await res.text();
    // If the response is an authentication/authorization error, redirect to the
    // appropriate client login page so protected client routes do not remain open.
    try {
      if (res.status === 401 || res.status === 403) {
        // choose login path based on API path prefix
        const p = String(path || "").toLowerCase();
        if (p.includes("/admin")) {
          try { window.location.href = "/public/admin/login"; } catch {}
        } else if (p.includes("/doctor")) {
          try { window.location.href = "/public/doctor/login"; } catch {}
        } else {
          // generic fallback
          try { window.location.href = "/"; } catch {}
        }
      }
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
