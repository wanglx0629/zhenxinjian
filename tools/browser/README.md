# browser/ — 浏览器 / 小程序自动化测试经验

> 状态：方案选型已记录，实证脚本待首次自动化任务时补充（补充时请回填本文件）。

## 方案选型

| 目标 | 方案 | 说明 |
| ---- | ---- | ---- |
| Web 管理后台（zhenxinjian-front, Vue3） | Playwright（Node） | 本机已有 `D:\App\nodejs` / nvm；`npm i -D playwright` + `npx playwright install chromium` |
| 小程序（zhenxinjian-uniapp） | miniprogram-automator | 微信官方方案：微信开发者工具 → 设置 → 安全设置 → 开启服务端口，SDK 直连自动化 |
| H5 兜底 | Playwright 直接跑 `npm run dev:h5` 产物 | 无需微信开发者工具 |

## 微信开发者工具自动化要点（miniprogram-automator）

1. 开发者工具 → 设置 → 通用设置/安全 → 打开「服务端口」（默认 9420）
2. `npm i -D miniprogram-automator`
3. 连接：`automator.launch({ projectPath: 'apps/zhenxinjian-uniapp/dist/dev/mp-weixin' })`
4. 常用：`miniProgram.currentPage()` / `page.$()` / `page.callMethod()` / `miniProgram.evaluate()`

## 待补充（首次任务后回填）

- [ ] 本机微信开发者工具安装路径与 cli 路径（`cli.bat`，支持命令行打开/预览/上传）
- [ ] 服务端口实证端口号
- [ ] Playwright 安装记录与示例脚本
