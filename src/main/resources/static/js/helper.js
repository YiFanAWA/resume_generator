"use strict";

const TIMEOUT = 8000;

const request = async function (url, options = {}) {
  const fetchPromise = fetch(url, {
    method: options.method || "POST",
    headers: {
      "Content-Type": "application/json",
      ...options.headers,
    },
    credentials: "same-origin",
    ...options,
  });

  const timeoutPromise = new Promise((_, reject) => {
    setTimeout(() => reject(new Error("Request timeout")), TIMEOUT);
  });

  const response = await Promise.race([fetchPromise, timeoutPromise]);
  const contentType = response.headers.get("content-type") || "";
  const isJson = contentType.includes("application/json");
  const body = isJson ? await response.json() : await response.text();

  if (!response.ok) {
    const message = isJson && body.error ? body.error : `HTTP ${response.status}: ${response.statusText}`;
    throw new Error(message);
  }

  return body;
};

export default request;
