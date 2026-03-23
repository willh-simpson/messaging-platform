import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { resolve } from "path";

export default defineConfig({
  plugins: [react()],
  resolve: {
    alias: {
      "@": resolve(__dirname, "./src"),
    },
  },
  server: {
    port: 3000,
    proxy: {
      // Proxy API calls to the Spring Boot API Gateway during development
      // so the frontend never has to deal with CORS in local dev.
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
      // WebSocket proxy — passes the Upgrade header through correctly
      "/ws": {
        target: "http://localhost:8083",
        changeOrigin: true,
        ws: true,
      },
    },
  },
});
