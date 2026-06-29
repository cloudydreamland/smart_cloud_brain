import { clearSession, getApiBase, getSession, normalizeApiBase } from "./session.js";

function parseError(response, body) {
  if (body && body.message) return body.message;
  if (response && response.statusCode === 401) return "登录已过期，请重新登录";
  return "请求失败，请稍后重试";
}

export function request(path, method = "GET", data = null, options = {}) {
  const session = getSession();
  const apiBase = normalizeApiBase(options.apiBase || getApiBase());
  const token = options.token === undefined ? session && session.token : options.token;
  return new Promise((resolve, reject) => {
    uni.request({
      url: apiBase + path,
      method,
      data,
      header: {
        "Content-Type": "application/json",
        ...(token ? { Authorization: "Bearer " + token } : {})
      },
      success(response) {
        const body = response.data || {};
        const ok = response.statusCode >= 200 && response.statusCode < 300 && (body.code === 0 || body.code === undefined);
        if (ok) {
          resolve(body.data === undefined ? body : body.data);
          return;
        }
        if (response.statusCode === 401 || body.code === 401) {
          clearSession();
        }
        reject(new Error(parseError(response, body)));
      },
      fail(error) {
        reject(new Error(error && error.errMsg ? error.errMsg : "网络连接失败"));
      }
    });
  });
}

export const api = {
  get: (path) => request(path),
  post: (path, body) => request(path, "POST", body),
  registerPatient: (body) => request("/patient/register", "POST", body, { token: "" }),
  loginPatient: (account, password, apiBase) => request("/patient/login", "POST", { account, password }, { token: "", apiBase }),
  patientInfo: () => request("/patient/info"),
  saveProfile: (body) => request("/patient/profile/save", "POST", body),
  departments: () => request("/doctor/department/list"),
  doctors: (departmentId) => request("/doctor/list" + (departmentId ? "?departmentId=" + encodeURIComponent(departmentId) : "")),
  triage: (chiefComplaint) => request("/triage/consult", "POST", { chiefComplaint }),
  triageList: () => request("/triage/list"),
  slots: () => request("/registration/slots"),
  registrations: () => request("/registration/list"),
  createRegistration: (body) => request("/registration/create", "POST", body),
  cancelRegistration: (registrationId) => request("/registration/cancel", "POST", { registrationId }),
  records: () => request("/medical-record/list"),
  recordDetail: (id) => request("/medical-record/detail?id=" + encodeURIComponent(id)),
  prescriptions: () => request("/prescription/list"),
  prescriptionDetail: (id) => request("/prescription/detail?id=" + encodeURIComponent(id)),
  visitors: () => request("/patient/visitor/list"),
  saveVisitor: (body) => request("/patient/visitor/save", "POST", body),
  deleteVisitor: (id) => request("/patient/visitor/delete", "POST", { id }),
  notifications: (readStatus = "") => request("/notification/list" + (readStatus ? "?readStatus=" + encodeURIComponent(readStatus) : "")),
  markNotificationRead: (notificationId) => request("/notification/read", "POST", { notificationId })
};
