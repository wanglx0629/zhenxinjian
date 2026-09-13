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

## 发布态配置（B-T34）

- **微信 appid**：不入库，`manifest.json` 保持空串占位。构建/开发时经环境变量 `VITE_MP_WEIXIN_APPID` 注入（`vite.config.ts` 内存改写，不落盘）；未设置时微信开发者工具可用测试号联调。
- **隐私协议**：`manifest.json` 已开 `__usePrivacyCheck__`，启动时未授权自动弹微信官方隐私授权弹窗（协议名取自小程序后台「用户隐私保护指引」配置，提审前须在 mp 后台完成配置）。
- **版本升级**：启动时 `uni.getUpdateManager` 检查新版本，下载就绪后弹窗提示重启生效。

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
