import { createApp } from "vue";
import { createPinia } from "pinia";
import App from "./App.vue";
import router from "./router";
import "./style.css";

const app = createApp(App);

/* 全局错误兜底：未被捕获的异常不会导致白屏 */
app.config.errorHandler = (err, instance, info) => {
  console.error("[Global Error]", err, info);
};

app.use(createPinia()).use(router).mount("#app");
