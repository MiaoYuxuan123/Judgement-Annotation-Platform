import { defineConfig } from 'vitest/config'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  test: {
    environment: 'jsdom',
    globals: true,
    coverage: {
      provider: 'v8',
      reporter: ['text', 'html'],
      include: [
        'src/utils/argumentGraphElkLayout.js',
        'src/utils/taskRows.js',
        'src/utils/reviewHelpers.js'
      ],
      thresholds: {
        lines: 60,
        functions: 60,
        statements: 60,
        branches: 40
      }
    }
  }
})
