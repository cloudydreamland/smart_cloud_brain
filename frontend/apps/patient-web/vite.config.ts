import vue from "@vitejs/plugin-vue";
import { defineConfig } from "vite";
import { fileURLToPath, URL } from "node:url";

const gatewayTarget = process.env.VITE_GATEWAY_TARGET
  ?? (process.env.VITE_GATEWAY_MODE === "docker" ? "http://localhost:18080" : "http://localhost:8080");
const wsTarget = gatewayTarget.replace(/^http/, "ws");

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      "@smart-cloud-brain/shared-api": fileURLToPath(new URL("../../packages/shared-api/src", import.meta.url)),
      "@smart-cloud-brain/shared-types": fileURLToPath(new URL("../../packages/shared-types/src", import.meta.url)),
      "@smart-cloud-brain/shared-ui": fileURLToPath(new URL("../../packages/shared-ui/src", import.meta.url)),
      "@smart-cloud-brain/shared-utils": fileURLToPath(new URL("../../packages/shared-utils/src", import.meta.url)),
    },
  },
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
