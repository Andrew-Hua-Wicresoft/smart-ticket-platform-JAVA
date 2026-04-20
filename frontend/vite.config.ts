import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        // Phase 1: all browser traffic now enters through the Java gateway.
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
