async function request(path, options = {}) {
  const { body, responseType = "json", ...fetchOptions } = options;
  const response = await fetch(path, {
    credentials: "include",
    ...fetchOptions,
    headers: {
      ...(body ? { "Content-Type": "application/json" } : {}),
      ...(fetchOptions.headers || {})
    },
    body: body ? JSON.stringify(body) : undefined
  });

  if (!response.ok) {
    throw new Error(await readError(response));
  }

  if (responseType === "blob") {
    return response.blob();
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

async function readError(response) {
  try {
    const data = await response.json();
    return data.error || data.message || response.statusText;
  } catch {
    return response.statusText || "Request failed";
  }
}

export function login(credentials) {
  return request("/api/auth/login", {
    method: "POST",
    body: credentials
  });
}

export function register(payload) {
  return request("/api/auth/register", {
    method: "POST",
    body: payload
  });
}

export function logout() {
  return request("/api/auth/logout", {
    method: "POST"
  });
}

export function me() {
  return request("/api/auth/me");
}

export function getProfile() {
  return request("/api/profile");
}

export function saveProfile(profile) {
  return request("/api/profile", {
    method: "PUT",
    body: profile
  });
}

export function generateShareLink() {
  return request("/api/profile/share/generate", {
    method: "POST"
  });
}

export function revokeShareLink() {
  return request("/api/profile/share/revoke", {
    method: "POST"
  });
}

export function getPublicProfile(token) {
  return request(`/api/public/${encodeURIComponent(token)}`);
}
