import nextVitals from "eslint-config-next/core-web-vitals";
import nextTs from "eslint-config-next/typescript";
import { defineConfig, globalIgnores } from "eslint/config";

const eslintConfig = defineConfig([
  ...nextVitals,
  ...nextTs,
  {
    rules: {
      "react-hooks/set-state-in-effect": "off",
    },
  },
  {
    files: ["cypress/**/*.ts", "cypress.config.ts"],
    rules: {
      "@typescript-eslint/no-namespace": "off",
      // Asercje Chai (np. expect(value).to.be.true) są wyrażeniami, a nie wywołaniami.
      "@typescript-eslint/no-unused-expressions": "off",
    },
  },
  globalIgnores([
    ".next/**",
    "out/**",
    "build/**",
    "next-env.d.ts",
  ]),
]);

export default eslintConfig;