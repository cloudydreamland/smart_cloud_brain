import { createApp } from "vue";
import { createPinia } from "pinia";
import { setTokenProvider, useAuthStore } from "@smart-cloud-brain/shared-api";
import App from "./App.vue";
import router from "./router";
import "./style.css";

const app = createApp(App);

/* 全局错误兜底：未被捕获的异常不会导致白屏 */
app.config.errorHandler = (err, instance, info) => {
  console.error("[Global Error]", err, info);
};

app.use(createPinia()).use(router);

/* 将 auth store 的 token 注入为 API 层默认 token 来源，各 API 函数无需再手动传 token */
setTokenProvider(() => useAuthStore().token());

app.mount("#app");
