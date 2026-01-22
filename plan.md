# 开发计划（plan.md）

> 基于 `readme.md` 需求文档与默认假设（v0）。目标是在最小可用范围内交付可复习、可记录、可落盘、可 Git 同步的 Web 应用。

## 1. 范围与目标（v0.1）

- 面向单用户、本地使用（localhost）
- 数据落盘为普通 Git 仓库（不接管 GitHub 认证）
- MVP 功能：
  - 题库管理（Deck/Item CRUD、归档）
  - 复习会话（作答、自评、错因记录）
  - SRS 调度（简化 SM-2，0/1/2 -> Again/Hard/Good）
  - 今日任务编排（到期题 > 错题复测 > 新题）
  - 导入/导出（JSON）
  - 复盘结果（得分、错题列表、下次 5 问）

## 2. 技术选型与架构方案

### 2.1 运行形态
- 开发期：前端 Vite Dev Server + 后端 API，`/api` 通过代理转发到后端（避免 CORS）。
- 生产期：前端构建产物由 Spring Boot 静态资源托管，同源访问。
- 单机 localhost 模式；数据目录由用户配置，必须是已有 Git 仓库；应用仅做文件读写，不接管认证。

### 2.2 技术栈（推荐）
- 后端：Java 21 + Spring Boot 3.x（Spring Web、Validation）。
- JSON/JSONL：Jackson（支持 streaming 追加写）。
- 前端：Vue 3 + Vite + TypeScript + Pinia + Vue Router。
- 构建：Maven + Node.js（LTS）/npm 或 pnpm；前端产物打包到 `backend/src/main/resources/static/`。
- 测试：JUnit 5 + AssertJ（前端可选 Vitest）。

### 2.3 架构分层
- Controller：REST API（Deck/Item/Review/Settings）。
- Service：业务编排（SRS、任务生成、会话流程）。
- Repository：文件读写（JSON/JSONL + 原子写入）。
- Domain：Deck/Item/Review/Settings 等模型与规则。

### 2.4 文件存储策略
- 结构：`decks/<id>.json`、`items/<id>.json`、`reviews/<date>/<sessionId>.jsonl`、`config/*.json`。
- 写入策略：JSON 采用“临时文件 → 原子替换”；Review 采用 JSONL 追加写。
- 并发保护：对单文件写入加文件锁，避免并发写破坏。

### 2.5 同步与状态检测
- 仅检测 `.git` 目录与工作区状态提示（不执行 git 命令）。
- 快照记录到 `config/last-sync.json`，用于回滚提示。

### 2.6 接口文档（v0.1）
- 接口清单、请求/响应示例与错误码独立维护：`docs/api.md`。
- 计划层面仅关注资源域：Deck / Item / Review / Settings / Import & Export。

### 2.7 目录结构与模块划分（建议）
- 后端：
  - `backend/src/main/java/com/recallstudio/api`：Controller
  - `backend/src/main/java/com/recallstudio/service`：业务逻辑（SRS/会话/任务生成）
  - `backend/src/main/java/com/recallstudio/repo`：文件读写（JSON/JSONL）
  - `backend/src/main/java/com/recallstudio/domain`：模型与规则
  - `backend/src/main/resources/application.yml`：配置
  - `backend/src/main/resources/static/`：前端产物
- 前端（独立目录）：
  - `frontend/`：Vite 项目源码
  - `frontend/dist/`：构建产物（部署时复制到 `backend/src/main/resources/static/`）
- 数据目录（运行时）：
  - `<dataDir>/decks/`
  - `<dataDir>/items/`
  - `<dataDir>/reviews/`
  - `<dataDir>/config/`

### 2.8 开发与构建流程（建议）
- 开发：
  - 后端：`cd backend` 后执行 `mvn spring-boot:run`（默认 8080）。
  - 前端：`cd frontend` 后执行 `npm run dev` 或 `pnpm dev`（Vite 5173）。
  - 代理：前端将 `/api` 代理到 `http://localhost:8080`。
- 构建：
  - `cd frontend` 后执行 `npm run build` 或 `pnpm build` 生成 `frontend/dist/`。
  - 将 `frontend/dist/` 复制到 `backend/src/main/resources/static/`。
  - `cd backend` 后执行 `mvn package` 打包后端与前端静态资源。

## 3. 里程碑与交付物

### v0.1（能用）
- 数据模型与落盘格式确定（JSON/JSONL）
- Deck/Item CRUD + 归档
- 复习会话 + Review 记录
- SRS 调度 + 今日任务生成
- Git 目录选择 + 同步提示
- JSON 导入/导出
- 基础页面流转（首页/题库/复习/复盘/设置）

### v0.2（好用）
- 搜索与过滤
- 统计面板
- 冲突提示与引导更清晰（会话文件拆分策略完善）

### v0.3（通用化）
- 完整导入/导出格式
- OAuth 多用户（可选）

## 4. 详细任务拆解（v0.1）

### 4.1 数据模型与存储
- 明确目录结构：`decks/` `items/` `reviews/` `config/`
- 明确 JSON Schema（Deck/Item/Review/Settings）
- Review 采用 JSONL 追加写（按日期/会话分文件）
- 快照策略：记录最近一次成功同步 commit hash 到 `config/last-sync.json`

### 4.2 题库管理
- Deck：新建/编辑/删除/归档
- Item：新建/编辑/归档/恢复
- 标签、题型、难度字段的输入与展示

### 4.3 复习会话
- 今日任务汇总卡片（到期/错题/新题数量）
- 会话流程：题干 → 作答 → 评分 → 下一题
- 评分映射：0/1/2 -> Again/Hard/Good
- 错因标签记录（可多选）
- 复盘页生成：得分、错题列表、下次 5 问（可编辑）

### 4.4 SRS 调度（简化 SM-2）
- 初始参数：`easiness=2.5`，`interval` 初始为 1
- 评分驱动下次到期时间计算
- 支持“只复测错题”策略
- 可配置参数：每日上限、新题比例、错题复测间隔

### 4.5 Git 同步（MVP）
- 选择本地 Git 仓库目录（仅校验，不自动 init）
- 同步提示：开始复习前 pull、结束复习后 commit/push
- 冲突提示：检测工作区脏状态或 pull 失败提示用户处理

### 4.6 导入/导出（JSON）
- 导入：Deck + Item（JSON）
- 导出：decks/items/config/reviews（支持勾选是否包含 reviews）
- 导入冲突策略：同 ID 覆盖或跳过（默认覆盖并记录日志）

### 4.7 页面与交互
- 首页：任务统计 + 同步状态
- Deck 列表：新增/编辑/导入/导出
- Deck 详情：题目列表 + 过滤 + 批量操作
- 复习页：题干/作答/评分
- 复盘页：错题 + 错因 + 下次 5 问
- 设置页：SRS 参数/会话策略/数据目录

### 4.8 测试与验收
- 数据落盘一致性（CRUD 后 JSON 正确）
- SRS 调度正确性（同分数给出预期 due）
- 复习会话流程无阻塞
- 导入/导出完整性（可往返）
- Git 目录非仓库时的提示与阻断

## 5. 关键默认值（v0）

- 评分体系：仅 `0/1/2`（Again/Hard/Good）
- 今日任务优先级：到期题 > 错题复测 > 新题
- 错题判定：评分为 0
- 错题复测间隔：D+1 / D+3 / D+7
- 导入/导出：默认 JSON
- Git 同步：只选择已有仓库，不代管认证

## 6. 风险与对策

- 冲突风险：Review 采用 JSONL 追加写 + 会话拆分
- 数据损坏风险：写入前备份/快照
- 误操作风险：删除/归档需二次确认
- 同步流程理解成本：首页固定提示推荐工作流

## 7. 验收清单（v0.1）

- 能建 Deck/题目并持久化到 Git 目录
- 能开始复习并记录评分/错因
- 能生成今日任务
- 复习记录按会话 JSONL 保存
- JSON 导入/导出可往返
- 手动 `pull/commit/push` 不破坏数据结构
