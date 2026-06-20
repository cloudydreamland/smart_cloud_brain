import vue from "@vitejs/plugin-vue";
import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: "jsdom",
    globals: true,
    include: [
      "packages/**/*.{test,spec}.{ts,js}",
      "apps/**/*.{test,spec}.{ts,js}",
    ],
    exclude: [
      "**/node_modules/**",
      "**/dist/**",
      "**/coverage/**",
      "apps/patient-mobile/**",
    ],
    coverage: {
      provider: "v8",
      reporter: ["text", "html", "lcov"],
      reportsDirectory: "coverage",
      include: [
        "packages/shared-api/src/index.ts",
        "packages/shared-ui/src/components/ConfirmDialog.vue",
        "packages/shared-ui/src/components/DataTable.vue",
        "packages/shared-ui/src/components/Drawer.vue",
        "packages/shared-ui/src/components/EmptyState.vue",
        "packages/shared-ui/src/components/ErrorState.vue",
        "packages/shared-ui/src/components/LoadingState.vue",
        "packages/shared-ui/src/components/Modal.vue",
        "packages/shared-ui/src/components/StatusTag.vue",
      ],
      exclude: [
        "**/*.d.ts",
        "**/*.test.*",
        "**/*.spec.*",
        "**/main.ts",
        "**/vite.config.ts",
        "**/router/**",
      ],
      thresholds: {
        lines: 70,
        functions: 70,
        branches: 60,
        statements: 70,
      },
    },
  },
  resolve: {
    alias: {
      "@smart-cloud-brain/shared-api": fileURLToPath(new URL("./packages/shared-api/src/index.ts", import.meta.url)),
      "@smart-cloud-brain/shared-ui": fileURLToPath(new URL("./packages/shared-ui/src/index.ts", import.meta.url)),
    },
  },
});
