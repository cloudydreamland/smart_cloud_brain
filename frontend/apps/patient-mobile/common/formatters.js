export function text(value, fallback = "-") {
  if (value === null || value === undefined || value === "") return fallback;
  return String(value);
}

export function num(value, fallback = 0) {
  const next = Number(value);
  return Number.isFinite(next) ? next : fallback;
}

export function list(value) {
  return Array.isArray(value) ? value : [];
}

export function statusText(value) {
  const key = String(value || "").toUpperCase();
  const map = {
    PENDING: "待处理",
    CONFIRMED: "已确认",
    WAITING: "待就诊",
    COMPLETED: "已完成",
    CANCELLED: "已取消",
    READ: "已读",
    UNREAD: "未读",
    HANDLED: "已处理",
    IGNORED: "已忽略",
    LOW: "低风险",
    MEDIUM: "中风险",
    HIGH: "高风险"
  };
  return map[key] || text(value, "待同步");
}

export function genderText(value) {
  const key = String(value || "").toUpperCase();
  if (key === "MALE") return "男";
  if (key === "FEMALE") return "女";
  return text(value, "未填写");
}

export function riskText(value) {
  return statusText(value);
}

export function dateText(value) {
  return text(value, "时间待同步").replace("T", " ").slice(0, 16);
}

export function latestById(rows, field) {
  return [...list(rows)].sort((left, right) => num(right[field]) - num(left[field]))[0] || null;
}

export function canCancelRegistration(item) {
  const status = String(item && item.status || "").toUpperCase();
  return status !== "CANCELLED" && status !== "COMPLETED";
}
