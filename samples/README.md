# Samples - cfggen 配置示例

cfggen 配置系统的实际应用示例，展示各种游戏系统的配置设计模式。

## 快速开始

### 0. 前置条件

- 确保 `../cfggen.jar` 存在。若不存在，在 `..` 目录下执行 `genjar.bat`

- 确保 `cfgeditor.exe` 存在。若不存在，在 `../cfgeditor` 目录下执行 `genexe.bat`，然后拷贝 `cfgeditor.exe` 到当前目录

### 1. 运行 `cfgeditor_server.bat`
### 2. 运行 `cfgeditor.exe` 查看、编辑

--- 

## 示例列表

### ⚔️ [buff/](./buff/) - 技能系统

**参考星际争霸 ABE 架构**（Ability-Behavior-Effect）

核心概念：
- **skill**（技能）- actor 的技能列表
- **buff**（状态）- actor 身上的状态效果（buff/debuff）
- **effect**（效果）- 直接效果（伤害、治疗、加 buff 等）

**特色：**
- 🎯 正交组合：触发时刻 × 触发条件 × 作用目标
- ⏰ TimelineBuff：按时间线触发的 Buff
- 🎪 TriggerBuff：事件驱动的 Buff
- 🎯 目标选择：Self/Sender/Caster/LockedTarget + 区域目标
- 🚀 创建子物体和运行轨迹（Static/Bind/Line/Chase）

**详细文档：** [buff/README.md](./buff/README.md)



### 🎬 [video/](./video/) - 剧情对话系统

视频/剧情配置示例，展示：
- 📺 视频节点配置
- 💬 对话选项系统
- ✅ 条件判断接口（检查物品、信息、解锁、好感度等）
- 🌳 逻辑组合（and/or）

**主要接口：**
- `condition` - 检查物品、信息、解锁视频、场景节点、好感度等
- `choice` - 对话选项配置，包含条件、超时、坐标等


### 🎮 [trigger/](./trigger/) - 触发器系统

副本和事件触发系统示例，展示：
- 🏠 副本实例事件（InstanceOpen、PhaseBegin）
- 👹 NPC 事件（NpcKilled、NpcEnterZone）
- 👥 玩家事件（APlayerEnter、APlayerEnterZone、APlayerLeave）
- 💀 战斗事件（AllPlayerDie、AActorEnterCombat）
- 🔧 自定义事件（Custom）

**典型应用：**
- 副本进度控制
- NPC AI 触发器
- 任务系统触发条件


### 🧪 [test/](./test/) - 语法测试

综合测试示例，展示各种 CFG 语法特性：
- 📋 基础类型：`list<bool>`、`list<int>`、`list<float>`、`list<str>`
- 🔗 枚举引用：`int -> buff.buffclass`、`str -> buff.triggerevt`
- 👥 列表外键：`list<int> -> buff.skill`
- 📦 JSON 输出标记：`(json)` 元数据

---

## 相关文档

- **[主项目文档](../)** - cfggen 完整介绍
- **[结构定义规范](../docs/docs/cfggen/03.schema.md)** - CFG 语法说明
- **[example 生成示例](../example/README.md)** - 生成代码示例
