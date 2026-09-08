# Tasks — 环境基线

## 1. 端口统一

- [ ] 1.1 修改 `apps/zhenxinjian-backend/src/main/resources/application.yml`：`server.port` 1215 → 8080
- [ ] 1.2 核对 front Vite 代理（`apps/zhenxinjian-front/vite.config.*`）与 uniapp `src/api/request.ts` 的 BASE_URL 均为 8080，确认无需改动
- [ ] 1.3 验证：`mvn spring-boot:run` 启动后端，浏览器访问 `http://localhost:8080/api/swagger-ui.html` 可打开

## 2. 口径资产入库

- [ ] 2.1 抽查 `data/foods.ts` 与 `MRD-PRD/foods_200.json（开发导入用）.json` 10 条记录的 kcal/三宏字段一致性
- [ ] 2.2 提交 commit A：暂存区现有的 V1.1 文档更新 + `common/ai/` 删除 + pom/application.yml 既有改动
- [ ] 2.3 提交 commit B：`apps/zhenxinjian-uniapp/src/{utils,config,data}/` 5 个算法/数据文件，message 注明「48 项对拍通过，后端计算服务对拍基准」
- [ ] 2.4 验证：`git status` 干净、`git log` 两个 commit 划分清晰

## 3. 食物库真源声明

- [ ] 3.1 在 `docs/prd/README.md` 需求真源表中为「foods_200.json（开发导入用）」追加「后端导入唯一真源」标注（一行改动）
