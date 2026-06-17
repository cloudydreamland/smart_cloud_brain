import vue from "@vitejs/plugin-vue";
import { defineConfig } from "vite";

const gatewayTarget = process.env.VITE_GATEWAY_TARGET
  ?? (process.env.VITE_GATEWAY_MODE === "docker" ? "http://localhost:18080" : "http://localhost:8080");
const wsTarget = gatewayTarget.replace(/^http/, "ws");

export default defineConfig({
  plugins: [vue()],
  define: {
    "import.meta.env.VITE_API_BASE": JSON.stringify(process.env.VITE_API_BASE ?? "/api"),
  },
  server: {
    port: 5173,
    proxy: {
      "/api": gatewayTarget,
      "/ws": {
        target: wsTarget,
        ws: true,
      },
    },
  },
});
