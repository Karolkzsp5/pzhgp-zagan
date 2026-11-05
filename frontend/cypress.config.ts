import { defineConfig } from "cypress";

export default defineConfig({
  allowCypressEnv: false,
  e2e: {
    baseUrl: 'http://localhost:3000',
    viewportWidth: 1440,
    viewportHeight: 900,
  },
});