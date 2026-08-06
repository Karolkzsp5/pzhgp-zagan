import { defineConfig } from "cypress";

export default defineConfig({
  allowCypressEnv: false,

  e2e: {
    baseUrl: 'http://localhost:3000',
    setupNodeEvents(on, config) {},
    viewportWidth: 1920,
    viewportHeight: 1080,
  },
});
