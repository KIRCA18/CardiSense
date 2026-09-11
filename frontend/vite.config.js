import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd(), '');

  return {
    plugins: [react(), tailwindcss()],
    server: {
       port: 5173,
      proxy: {
        '/api': {
          // Use the variable loaded from your .env file
          target: env.VITE_API_TARGET || 'http://localhost:8000',
          changeOrigin: true
        },
      },
    },
  };
});

// export default defineConfig({
  
//   server: {
   
//     proxy: {
//       '^/(?!src|node_modules|@vite|@fs|favicon.ico|index.html)': {
//         target: 'http://localhost:5000',
//         changeOrigin: true,
//         secure: false,
//       },
//     },
//   },
// });
