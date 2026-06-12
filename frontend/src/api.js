export class ApiError extends Error {
  constructor(message, status, data) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.data = data;
  }
}

const UNSAFE_METHODS = new Set(["POST", "PUT", "PATCH", "DELETE"]);

async function request(path, options = {}) {
  const { body, responseType = "json", ...fetchOptions } = options;
  const csrfHeaders = await csrfHeadersFor(fetchOptions.method);
  const response = await fetch(path, {
    credentials: "include",
    ...fetchOptions,
    headers: {
      ...(body ? { "Content-Type": "application/json" } : {}),
      ...csrfHeaders,
      ...(fetchOptions.headers || {})
    },
    body: body ? JSON.stringify(body) : undefined
  });

  if (!response.ok) {
    const errorData = await readErrorData(response);
    throw new ApiError(errorData.message, response.status, errorData.data);
  }

  if (responseType === "blob") {
    return response.blob();
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

async function readErrorData(response) {
  try {
    const data = await response.json();
    return {
      data,
      message: data.error || data.message || response.statusText
    };
  } catch {
    return {
      data: null,
      message: response.statusText || "Request failed"
    };
  }
}

async function csrfHeadersFor(method = "GET") {
  if (!UNSAFE_METHODS.has(method.toUpperCase())) {
    return {};
  }

  const response = await fetch("/api/csrf", {
    credentials: "include"
  });
  if (!response.ok) {
    throw new ApiError("Failed to obtain CSRF token", response.status, null);
  }

  const csrf = await response.json();
  return { [csrf.headerName || "X-XSRF-TOKEN"]: csrf.token };
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

export function generateShareLink(settings = {}) {
  return request("/api/profile/share/generate", {
    method: "POST",
    body: settings
  });
}

export function updateShareSettings(settings = {}) {
  return request("/api/profile/share/settings", {
    method: "PUT",
    body: settings
  });
}

export function revokeShareLink() {
  return request("/api/profile/share/revoke", {
    method: "POST"
  });
}

export function getPublicProfile(token, password) {
  if (password) {
    return request(`/api/public/${encodeURIComponent(token)}/access`, {
      method: "POST",
      body: { password }
    });
  }

  return request(`/api/public/${encodeURIComponent(token)}`);
}
