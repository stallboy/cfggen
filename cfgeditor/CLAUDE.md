# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个配置编辑器应用 (cfgeditor)，用于可视化浏览和编辑表结构和记录。项目使用 React + TypeScript + Tauri 技术栈构建。

## 开发命令

### 开发环境
```bash
# 安装依赖
pnpm install

# 启动开发服务器
pnpm run dev
# 访问 http://localhost:5173/
```

### 构建发布
```bash
# 构建 Web 版本
pnpm run build
# 生成的静态文件在 dist 目录

# 启动测试服务器
cd dist
jwebserver
# 访问 http://localhost:8000/
```

### 桌面应用构建
```bash
# 构建 Tauri 桌面应用 (需要 Rust 环境)
pnpm tauri build
# 生成的 exe 文件在 src-tauri\target\release\ 目录
```

### 代码质量
```bash
# 代码检查
pnpm run lint
```

## 架构概览

### 技术栈
- **前端**: React 18 + TypeScript + Vite
- **桌面框架**: Tauri (Rust 后端)
- **UI 库**: Ant Design + Ant Design Pro Chat
- **状态管理**: Resso (轻量级) + React Query
- **图形可视化**: React Flow (XYFlow)
- **路由**: React Router DOM
- **国际化**: i18next

### 核心组件结构

#### 应用入口
- `src/main.tsx` - React 应用主入口，配置路由和查询客户端
- `src/CfgEditorApp.tsx` - 主应用组件，使用分割布局
- `src/AppLoader.tsx` - 应用加载器，初始化设置

#### 主要页面组件
- `src/routes/table/Table.tsx` - 表结构可视化
- `src/routes/record/Record.tsx` - 记录显示和编辑
- `src/routes/tableRef/TableRef.tsx` - 表引用可视化
- `src/routes/recordRef/RecordRef.tsx` - 记录引用可视化

#### 搜索和查询
- `src/routes/query/Query.tsx` - 搜索界面
- `src/routes/finder/Finder.tsx` - 搜索功能
- `src/routes/adder/Adder.tsx` - 添加记录
- `src/routes/chat/Chat.tsx` - AI 聊天界面
- `src/routes/addJson/AddJson.tsx` - JSON 数据导入

#### 设置管理
- `src/routes/setting/Setting.tsx` - 应用设置面板
- `src/routes/setting/store.ts` - 状态管理 (Resso)
- `src/routes/setting/storage.ts` - 存储管理 (localStorage/YAML)

#### 图形可视化
- `src/flow/FlowGraph.tsx` - 图形可视化包装器
- `src/flow/FlowNode.tsx` - 节点渲染
- `src/flow/EntityCard.tsx` - 实体显示卡片
- `src/flow/EntityForm.tsx` - 实体编辑表单
- `src/flow/FlowStyleManager.tsx` - 样式管理
- `src/flow/FlowVisualizationSetting.tsx` - 可视化配置

### 数据模型

#### 模式模型 (`src/routes/table/schemaModel.ts`)
- `STable` - 表结构定义，包含字段、外键和记录ID
- `SStruct` - 结构定义
- `SInterface` - 接口定义
- `SField` - 字段定义
- `RawSchema` - 完整模式数据结构

#### 实体模型 (`src/flow/entityModel.ts`)
- `Entity` - 图形可视化核心实体
- `EntityEdit` - 编辑状态和功能
- `EntityGraph` - 完整图形结构

#### 记录模型 (`src/routes/record/recordModel.ts`)
- `RecordResult` - 记录数据与引用
- `JSONObject` - 通用 JSON 数据结构

### API 集成

#### HTTP API 客户端 (`src/routes/api.ts`)
- 使用 Axios 连接后端服务器 (localhost:3456)
- 主要端点:
  - `/schemas` - 获取模式数据
  - `/record` - 获取单个记录
  - `/recordRefIds` - 获取引用ID
  - `/recordAddOrUpdate` - 创建/更新记录

#### React Query 集成
- 在 main.tsx 中配置查询客户端
- 支持乐观更新和缓存
- 自动重试和错误处理

### 状态管理

#### 存储架构 (`src/routes/setting/store.ts`)
- 使用 Resso 进行轻量级状态管理
- `StoreState` 接口定义所有应用状态:
  - 服务器配置
  - AI 设置
  - 引用深度和节点限制
  - UI 偏好设置
  - 导航历史
  - 编辑状态

#### 存储管理 (`src/routes/setting/storage.ts`)
- 双重存储: localStorage (Web) 和 YAML 文件 (Tauri)
- 自动持久化到 `cfgeditor.yml` 和 `cfgeditorSelf.yml`
- 类型安全的偏好设置管理

### 关键架构模式

#### 1. 基于图形的可视化
- 使用 React Flow 进行交互式图形可视化
- 实体作为节点，关系作为边
- 上下文菜单用于节点交互

#### 2. 分割布局系统
- 拖拽面板系统实现并排视图
- 固定页面配置用于持久化布局
- 灵活的面板排列

#### 3. 热键导航
- 常用操作的键盘快捷键
- 历史导航 (alt+c/alt+v)
- 页面类型切换 (alt+1 到 alt+4)

#### 4. 多模式界面
- 表视图 (模式可视化)
- 记录视图 (单个数据显示)
- 引用视图 (关系探索)
- 编辑模式 (数据修改)

#### 5. 可视化配置系统
- 节点尺寸配置 (编辑/非编辑模式宽度)
- 边样式配置 (颜色、粗细)
- 布局间距配置 (树状/分层布局)
- 实时配置更新
- 国际化支持

## 开发注意事项

### 后端依赖
- 需要启动 Java 后端服务器:
  ```bash
  java -jar ../cfggen.jar -datadir ../example/config -gen server
  ```

### Tauri 配置
- 桌面应用配置在 `src-tauri/tauri.conf.json`
- 构建时自动运行前端构建命令
- 开发时使用 `http://localhost:5173`

### 样式和主题
- 使用 Ant Design 组件库
- 支持暗色/亮色主题
- 自定义样式通过 antd-style 管理

### 国际化
- 支持中英文界面
- 翻译文件在 `src/locales/` 目录
- 使用 i18next 进行文本管理

### 错误处理
- React Query 提供统一的错误处理
- 组件级别的错误边界
- 用户友好的错误提示