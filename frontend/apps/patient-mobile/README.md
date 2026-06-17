# Patient Mobile

`patient-mobile` is a uni-app patient entry. It is not wired into the current Vite web build pipeline and is not included in the root `corepack pnpm test` or web build commands.

Runtime API base is configurable in this order:

1. User-entered value stored under `patient-mobile-api-base`.
2. `globalThis.SMART_CLOUD_BRAIN_API_BASE`.
3. `UNI_APP_API_BASE` or `VUE_APP_API_BASE`.
4. Relative `/api`.

For local gateway access use `http://localhost:8080/api`. For Docker gateway access use `http://localhost:18080/api`.
