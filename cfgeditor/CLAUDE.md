# CLAUDE.md

配置编辑器 (cfgeditor)，可视化浏览与编辑表结构和记录。React 19 + TypeScript + Vite + Tauri 桌面应用，UI 用 Ant Design，图形用 React Flow (XYFlow)，状态用 Resso + React Query。

> 架构、分层、数据模型、状态管理、API、undo/redo 等细节都在 `docs/`，本文件只放命令、文档索引和易踩坑约定。

## 开发命令

```bash
pnpm install                 # 安装依赖
pnpm run dev                 # 开发服务器 http://localhost:1420/
pnpm tauri build             # 构建 Tauri 桌面应用（exe 在 src-tauri/target/release/，需 Rust）
pnpm run lint                # 代码检查（oxlint）
pnpm test                    # 单元测试 watch
pnpm test:run                # 单元测试单次跑（CI 用）
```

## 后端依赖

需启动 Java 后端提供 API（`localhost:3456`）：

```bash
java -jar ../cfggen.jar -datadir ../example/config -gen server
```

## 文档索引（详情见 docs/）

> 👉 想理解 cfgeditor 源码与设计，先读 [`docs/README.md`](docs/README.md)——分层地图 + 一条记录的旅程主线 + 六条贯穿红线。

| # | 主题 | 文档 |
|---|---|---|
| 00 | 总览（地图 / 旅程 / 红线） | `docs/README.md` |
| 01 | 数据流：URL → API → React Query | `docs/01-data-flow.md` |
| 02 | 状态管理：五种方案分工 | `docs/02-state-management.md` |
| 03 | 编辑会话 + Undo/Redo | `docs/03-editing-session-undo.md` |
| 04 | 布局引擎 + 视口 | `docs/04-layout-viewport.md` |
| 05 | Flow 图层（XYFlow 集成） | `docs/05-flow-graph.md` |
| 06 | 编辑表单 | `docs/06-edit-form.md` |
| 07 | 字段内嵌 embedding（`$fold` + `$embed_<fieldName>`） | `docs/07-embedding.md` |
| 08 | AI Chat | `docs/08-ai-chat.md` |
| 09 | 横切关注点 | `docs/09-cross-cutting.md` |

## 必守约定（易踩坑）

- **依赖只能向下**：`app/features → flow/res → store/services → domain → api`，反向 import 被 oxlint 立即拦下。分层地图见 `docs/README.md`，规则以 `.oxlintrc.json` 为准。
- **`src/domain/storageJson.ts` 是自动生成的**（quicktype 产出），不要手改——改 `src/domain/json.ts` 定义类型后跑 `genJsonParser.bat` 重新生成，再注释掉未用函数。
- **`*.test.ts` 被 `.ignore` 排除**：Grep 工具搜不到测试内容，需用 `Glob src/**/*.test.ts` 定位再 Read（LSP `findReferences` 不受影响）。
- **测试只覆盖纯逻辑**：vitest，jsdom 环境，不 mock、不碰 UI/网络/Tauri IPC；喂 fixture 断言输出。
- **国际化**：翻译内联在 `src/app/i18n.ts`（en/zh 两段，无独立 locales 目录），用 i18next。
