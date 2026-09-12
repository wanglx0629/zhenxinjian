# zhenxinjian UniApp

作者: wanglx

对接同一套 Spring Boot 后端的移动端板子（Vue3 + TypeScript + Pinia），风格与 Web 端一致：淡蓝 + 灰 + 白。

## 启动

```bash
npm install
npm run dev:h5
# 或微信小程序
npm run dev:mp-weixin
```

默认 API：`http://localhost:8080/api`（见 `.env.development`）。

H5 开发端口默认 `5174`，请确保后端 CORS 包含该 Origin。

## 目录

```
src/
  api/         请求与类型
  store/       Pinia
  utils/       storage / ws / 节流
  components/  TeLogo / CaptchaInput / WsBoard
  pages/       login / register / home / websocket / mine
```

## 默认账号

后端 `data.sql` 不含初始账号：首个管理员由部署者自行生成 BCrypt 哈希手工 INSERT（见 `tools/db/README`「管理员初始化」）。
