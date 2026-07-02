import vue from "@vitejs/plugin-vue";
import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "vitest/config";

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: "jsdom",
    globals: true,
    setupFiles: ["./src/test-setup.ts"],
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
      reporter: ["text", "html", "json-summary", "lcov"],
      reportsDirectory: "coverage",
      include: [
        "packages/shared-api/src/**/*.ts",
        "packages/shared-ui/src/components/*.vue",
        "apps/admin-web/src/composables/*.ts",
        "apps/admin-web/src/patientSitePresentation.ts",
        "apps/patient-web/src/site-config/*.ts",
        "apps/patient-web/src/composables/*.ts",
        "apps/patient-web/src/components/cms/*.ts",
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
        functions: 65,
        branches: 70,
        statements: 70,
        "packages/shared-api/src/stores/*.ts": {
          lines: 100,
          functions: 100,
          branches: 100,
          statements: 100,
        },
        "packages/shared-api/src/**/*.ts": {
          lines: 80,
          functions: 80,
          branches: 80,
          statements: 80,
        },
        "packages/shared-ui/src/components/*.vue": {
          lines: 80,
          functions: 60,
          branches: 60,
          statements: 80,
        },
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
