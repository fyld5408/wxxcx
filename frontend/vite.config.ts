import { defineConfig } from "vite";
import uni from "@dcloudio/vite-plugin-uni";

// H5 调试时浏览器没有 openid，由代理补一个假身份。
// 默认 dev-openid；在浏览器控制台执行 document.cookie = 'dev_openid=tom; path=/'
// 再刷新即可切成另一个用户，用来验证"未登记会跳注册页"。
const DEV_OPENID = process.env.DEV_OPENID || "dev-openid";

export default defineConfig({
  plugins: [uni()],
  server: {
    port: 5173,
    proxy: {
      "/api": {
        target: process.env.API_TARGET || "http://127.0.0.1:8080",
        changeOrigin: true,
        headers: { "X-WX-OPENID": DEV_OPENID },
        configure: (proxy) => {
          proxy.on("proxyReq", (proxyReq, req) => {
            const match = /(?:^|;\s*)dev_openid=([^;]+)/.exec(req.headers.cookie || "");
            if (match) {
              proxyReq.setHeader("X-WX-OPENID", decodeURIComponent(match[1]));
            }
          });
        },
      },
    },
  },
});
