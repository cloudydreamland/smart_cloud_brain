import { beforeEach } from "vitest";

// jsdom may not fully implement Storage.prototype methods.
// Ensure localStorage is a working in-memory mock for all tests.
const store = new Map<string, string>();

const mockStorage: Storage = {
  get length() {
    return store.size;
  },
  clear() {
    store.clear();
  },
  getItem(key: string) {
    return store.get(key) ?? null;
  },
  setItem(key: string, value: string) {
    store.set(key, String(value));
  },
  removeItem(key: string) {
    store.delete(key);
  },
  key(index: number) {
    return [...store.keys()][index] ?? null;
  },
};

Object.defineProperty(globalThis, "localStorage", {
  value: mockStorage,
  writable: true,
  configurable: true,
});

beforeEach(() => {
  store.clear();
});

// Suppress unhandled rejections from component mounted hooks that call
// toast/notification APIs unavailable in the test environment.
process.on("unhandledRejection", (reason: unknown) => {
  const msg = String(reason ?? "");
  if (msg.includes("toastRef") || msg.includes("toast") || msg.includes("is not a function")) return;
  console.error("Unhandled rejection in test:", reason);
});
