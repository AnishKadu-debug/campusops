import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    react(),
    tailwindcss(),
  ],
  server: {
    port: 3000,
    proxy: {
      '/api/v1/incidents': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
      '/api/v1/assets': {
        target: 'http://localhost:8083',
        changeOrigin: true,
      },
      '/api/v1/notifications': {
        target: 'http://localhost:8084',
        changeOrigin: true,
      },
      '/api/actuator/incident': {
        target: 'http://localhost:8082',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/actuator\/incident/, '/actuator'),
      },
      '/api/actuator/asset': {
        target: 'http://localhost:8083',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/actuator\/asset/, '/actuator'),
      },
      '/api/actuator/notification': {
        target: 'http://localhost:8084',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/actuator\/notification/, '/actuator'),
      },
    },
  },
})
