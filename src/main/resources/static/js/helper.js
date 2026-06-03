"use strict";
const TIMEOUT = 5000;

const request = async function (url, options = {}) {
  const fetchPromise = fetch(url, {
    method: options.method || "POST",
    headers: {
      "Content-Type": "application/json",
      ...options.headers
    },
    credentials: "same-origin",
    ...options,
  });

  const timeoutPromise = new Promise((_, reject) => {
    setTimeout(() => reject(new Error("请求超时")), TIMEOUT);
  });

  const response = await Promise.race([fetchPromise, timeoutPromise]);

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}: ${response.statusText}`);
  }
  return response.json();
};

export default request;
