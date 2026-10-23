/// <reference types="vitest/config" />
import { readdirSync, statSync } from 'node:fs';
import { defineConfig } from 'vite';
import { configDefaults } from 'vitest/config';
import react from '@vitejs/plugin-react';

// Nhánh main chứa các tệp test rỗng (chỗ của từng thành viên) — vitest báo lỗi với tệp rỗng nên bỏ qua chúng.
const emptyTests = readdirSync('src/test')
  .filter((f) => /\.test\.tsx?$/.test(f) && statSync(`src/test/${f}`).size === 0)
  .map((f) => `src/test/${f}`);

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    strictPort: true,
  },
  build: {
    outDir: 'dist',
    sourcemap: false,
    chunkSizeWarningLimit: 1200,
  },
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/test/setup.ts'],
    css: false,
    exclude: [...configDefaults.exclude, ...emptyTests],
  },
});
