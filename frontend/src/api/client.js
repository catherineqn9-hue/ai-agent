const JSON_HEADERS = { "Content-Type": "application/json" };
const AUTH_TOKEN_KEY = "sherry_auth_token";

async function request(path, options = {}) {
  const headers = withAuthHeaders(options.headers || {});
  const response = await fetch(path, { ...options, headers });
  const contentType = response.headers.get("content-type") || "";
  const payload = contentType.includes("application/json") ? await response.json() : await response.text();

  if (isStandardResponse(payload)) {
    if (!response.ok || !payload.success) {
      const traceText = payload.trace_id ? `，TraceId：${payload.trace_id}` : "";
      throw new Error(`${payload.message || "接口调用失败"}（${payload.code}）${traceText}`);
    }
    return payload.data;
  }

  if (!response.ok) {
    const message = typeof payload === "object" ? payload.detail || payload.message : payload;
    throw new Error(message || "接口调用失败");
  }
  return payload;
}

function isStandardResponse(payload) {
  return payload
    && typeof payload === "object"
    && Object.prototype.hasOwnProperty.call(payload, "success")
    && Object.prototype.hasOwnProperty.call(payload, "code")
    && Object.prototype.hasOwnProperty.call(payload, "data");
}

function withAuthHeaders(headers) {
  const nextHeaders = { ...headers };
  const token = getAuthToken();
  if (token && !nextHeaders.Authorization) {
    nextHeaders.Authorization = `Bearer ${token}`;
  }
  return nextHeaders;
}

export function getAuthToken() {
  return window.localStorage.getItem(AUTH_TOKEN_KEY);
}

export function setAuthToken(token) {
  if (token) {
    window.localStorage.setItem(AUTH_TOKEN_KEY, token);
  } else {
    window.localStorage.removeItem(AUTH_TOKEN_KEY);
  }
}

export function checkHealth() {
  return request("/health");
}

export function registerUser(payload) {
  return request("/api/v1/auth/register", {
    method: "POST",
    headers: JSON_HEADERS,
    body: JSON.stringify(payload)
  });
}

export function loginUser(payload) {
  return request("/api/v1/auth/login", {
    method: "POST",
    headers: JSON_HEADERS,
    body: JSON.stringify(payload)
  });
}

export function getCurrentUser() {
  return request("/api/v1/auth/me");
}

export function logoutUser() {
  return request("/api/v1/auth/logout", { method: "POST" });
}

export function getAdminMenus() {
  return request("/api/v1/admin-menus");
}

export function runChat(payload) {
  return request("/api/v1/chat/run", {
    method: "POST",
    headers: JSON_HEADERS,
    body: JSON.stringify(payload)
  });
}

export function resumeChat(payload) {
  return request("/api/v1/chat/resume", {
    method: "POST",
    headers: JSON_HEADERS,
    body: JSON.stringify(payload)
  });
}

export function listBasicConfigs(resource) {
  return request(`/api/v1/basic-configs/${resource}`);
}

export function saveBasicConfig(resource, id, payload) {
  return request(id ? `/api/v1/basic-configs/${resource}/${id}` : `/api/v1/basic-configs/${resource}`, {
    method: id ? "PUT" : "POST",
    headers: JSON_HEADERS,
    body: JSON.stringify(payload)
  });
}

export function deleteBasicConfig(resource, id) {
  return request(`/api/v1/basic-configs/${resource}/${id}`, { method: "DELETE" });
}

export function listUsers() {
  return request("/api/v1/users");
}

export function updateUser(id, payload) {
  return request(`/api/v1/users/${id}`, {
    method: "PUT",
    headers: JSON_HEADERS,
    body: JSON.stringify(payload)
  });
}

export function listSupervisionItems() {
  return request("/api/v1/supervision-items");
}

export function listMySupervisionItems() {
  return request("/api/v1/my/supervision-items");
}

export function getMySupervisionItemDetail(id) {
  return request(`/api/v1/my/supervision-items/${id}`);
}

export function getSupervisionItemDetail(id) {
  return request(`/api/v1/supervision-items/${id}`);
}

export function saveSupervisionItem(id, payload) {
  return request(id ? `/api/v1/supervision-items/${id}` : "/api/v1/supervision-items", {
    method: id ? "PUT" : "POST",
    headers: JSON_HEADERS,
    body: JSON.stringify(payload)
  });
}

export function deleteSupervisionItem(id) {
  return request(`/api/v1/supervision-items/${id}`, { method: "DELETE" });
}

export function createProgressFeedback(payload) {
  return request("/api/v1/progress-feedbacks", {
    method: "POST",
    headers: JSON_HEADERS,
    body: JSON.stringify(payload)
  });
}

export function assignSupervisionItem(id, payload) {
  return request(`/api/v1/supervision-items/${id}/assignees`, {
    method: "POST",
    headers: JSON_HEADERS,
    body: JSON.stringify(payload)
  });
}

export function listAssignmentRecommendations(id, params = {}) {
  const search = new URLSearchParams();
  if (params.role_type) search.set("role_type", params.role_type);
  if (params.department_id) search.set("department_id", params.department_id);
  const suffix = search.toString() ? `?${search}` : "";
  return request(`/api/v1/supervision-items/${id}/assignment-recommendations${suffix}`);
}

export function confirmSupervisionReceive(id) {
  return request(`/api/v1/supervision-items/${id}/confirm-receive`, { method: "POST" });
}

export function rejectSupervisionAssignment(id, payload) {
  return request(`/api/v1/supervision-items/${id}/reject-assignment`, {
    method: "POST",
    headers: JSON_HEADERS,
    body: JSON.stringify(payload)
  });
}

export function listImportTemplates() {
  return request("/api/v1/supervision-imports/templates");
}

export function listImportBatches() {
  return request("/api/v1/supervision-imports/batches");
}

export function getImportBatchDetail(batchId) {
  return request(`/api/v1/supervision-imports/batches/${batchId}`);
}

export function importSupervisionExcel(formData) {
  return request("/api/v1/supervision-imports/excel", {
    method: "POST",
    body: formData
  });
}
