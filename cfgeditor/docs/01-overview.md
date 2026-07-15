# cfgeditor 总览：它是什么、核心概念、怎么用

> 本文是理解 cfgeditor 的**起点**。只回答三件事:这应用干嘛的、它嘴里的那些名词(entity / record / table / schema / res / node)指什么、一个用户从头到尾怎么操作。**不讲分层、不讲任何机制的实现**——那些有专门的文档,文末给链接。

---

## 一、它是什么

cfgeditor 是 [cfggen](../) 配表体系的**可视化前端**:策划在一个图形界面里浏览和编辑"配表"(游戏配置数据),编辑完的数据由 cfggen 生成多语言程序代码。

它是一个 **React + TypeScript + Tauri** 桌面应用,图形用 React Flow(XYFlow),但**自己不存配表数据**——所有数据来自一个 Java 后端:

```
cfgeditor (React 前端,本仓库)
      ↕  HTTP (默认 localhost:3456)
cfggen.jar -gen server (Java 后端,../app)
      ↕  读写
Excel / CSV / JSON 配表文件 (磁盘)
```

> 后端怎么起、端点清单、缓存策略——见 [`05-url-api-reactquery.md`](./05-url-api-reactquery.md)。**本文只点一句:cfgeditor 是瘦前端,数据在后端。**

---

## 二、核心概念词汇表

新人最容易卡住的是:同一个东西,在**类型系统(cfggen 领域)**和**画布视图(cfgeditor 前端)**里叫不同的名字。先把这层分清,后面所有文档都好读。

### 领域层(cfggen 的类型系统,从后端 `/schemas` 拉来)

| 概念 | 一句话 | 锚点 |
|---|---|---|
| **schema** | 整个类型系统:有哪些结构体(struct)、表(table)、接口(interface),各自有什么字段。编辑是否允许也由它(`isEditable`)决定。 | `src/domain/schema.ts`(`Schema` 类),类型定义在 `src/api/schemaModel.ts` |
| **table** | 一张**配表**的定义:字段 + 主键(pk) + 已有记录 id 列表 + 外键。是 schema 里 `type=='table'` 的条目。 | `src/api/schemaModel.ts`(`STable`) |
| **record** | 一条**真实数据**:由 `(table, id)` 定位,内容是一个 JSON 对象(带 `$type` 指明它是哪个结构体)。 | `src/api/recordModel.ts`(`RecordResult`) |

> struct / interface 是 schema 里的另外两类条目(复合类型 / 可多态的接口),和 table 一起构成类型系统;field、foreignKey 是 table/struct 内部的细节。先记住 **schema(类型) / table(表定义) / record(数据)** 三个就够建心智模型了。

### 视图层(cfgeditor 把数据画到画布上用的模型)

| 概念 | 一句话 | 锚点 |
|---|---|---|
| **entity** | 画布上**一个节点的视图模型**(只读 / 可编辑 / 卡片三种形态)。由 record 的数据 + schema 的类型信息**变换**而来。 | `src/domain/entityModel.ts`(`Entity` 联合类型) |
| **node** | React Flow 里的**节点**,把 entity 包了一层(再加呈现信息)。画布上的点就是它。 | `src/flow/FlowGraph.tsx`(`EntityNode`) |
| **res** | 挂在 entity 上的**资源元数据**(视频 / 音频 / 图片 / 字幕路径等),供预览用。 | `src/domain/resInfo.ts`(`ResInfo`) |

### 一条变换主线(最该记住的图)

数据怎么从后端变成屏幕上的图:

```
record (数据)  ┐
               ├─→  entity (视图模型)  ─→  node + edge  ─→  React Flow 画布
schema (类型)  ┘    (recordEditEntityCreator /        (FlowGraph)
                      entityToNodeAndEdges)
```

一条 record 不是直接渲染的:它先和 schema 一起被**变换成若干 entity**(一条记录往往展开成多个节点——主节点 + 它引用的、被引用的节点),entity 再包成 node + 连线,最终上画布。**编辑就是反着走**:在 node 的表单里改值 → 写回 record 的数据对象 → 提交回后端。这条正反双向流的全过程,见 [`03-data-lifecycle.md`](./03-data-lifecycle.md)。

---

## 三、用户操作主线

一个典型使用流程(对应代码里的特性目录 `src/features/`):

1. **选表** —— 左侧表列表(`headerbar/TableList`)选一张配表。
2. **看图** —— 表的记录按引用关系展开成一张流程图(节点 = entity,连线 = 引用/外键)。视口怎么稳定不乱跳,见 [`07-fitview.md`](./07-fitview.md)。
3. **编辑** —— 双击节点进入编辑态,在表单里改字段值、增删数组项、换接口实现。字段怎么内嵌显示,见 [`08-embedding.md`](./08-embedding.md)。
4. **撤销/重做** —— Ctrl+Z / Ctrl+Y。见 [`06-undo-redo.md`](./06-undo-redo.md)。
5. **保存** —— alt+s 提交,经后端落盘。失败会保留你的编辑态,不丢。

> 这五步背后是一条完整的"编辑→缓冲→撤销→提交→刷新"数据流。想看它怎么转一圈,直接读 [`03-data-lifecycle.md`](./03-data-lifecycle.md)。

---

## 四、想深入,去这几篇

本文是地图,以下每篇是一个机制的深挖:

| 想懂 | 读 |
|---|---|
| 数据从输入到落盘怎么转一圈 | [`03-data-lifecycle.md`](./03-data-lifecycle.md) |
| 目录怎么分、依赖只能向下、oxlint 护栏 | [`02-directory-structure.md`](./02-directory-structure.md) |
| 状态怎么管理(Resso / EditingSession / useSyncExternalStore) | [`04-state-management.md`](./04-state-management.md) |
| URL / API / React Query 数据流 | [`05-url-api-reactquery.md`](./05-url-api-reactquery.md) |
| undo/redo | [`06-undo-redo.md`](./06-undo-redo.md) |
| 视口适配 | [`07-fitview.md`](./07-fitview.md) |
| 字段内嵌 | [`08-embedding.md`](./08-embedding.md) |
| 单元测试 | [`09-unit-testing-guide.md`](./09-unit-testing-guide.md) |
| 性能 | [`10-perf-optimization.md`](./10-perf-optimization.md) |

不确定先读哪篇?看 [`README.md`](./README.md) 的阅读路径。
