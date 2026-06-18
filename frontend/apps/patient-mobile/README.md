# 患者移动端

`patient-mobile` 是 uni-app 患者端入口。它没有接入当前 Vite Web 构建流程，也不会包含在根目录 `corepack pnpm test` 或 Web 构建命令中。

运行时接口地址按以下顺序读取：

1. 用户输入并存储在 `patient-mobile-api-base` 下的值。
2. `globalThis.SMART_CLOUD_BRAIN_API_BASE`.
3. `UNI_APP_API_BASE` 或 `VUE_APP_API_BASE`。
4. 相对路径 `/api`。

本地网关使用 `http://localhost:8080/api`。Docker 网关使用 `http://localhost:18080/api`。
