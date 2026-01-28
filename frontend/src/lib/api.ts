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
    throw new Error(`API ${res.status}: ${text}`);
  }

  // Some endpoints (DELETE/PATCH) may return empty body (204 No Content) — avoid calling json() on empty responses
  const text = await res.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch (e) {
    // If parsing fails, rethrow a clearer error
    throw new Error(`Failed to parse JSON response: ${e instanceof Error ? e.message : String(e)} (raw: ${text})`);
  }
}
