---
title: 能力系统网络同步
sidebar:
  order: 2
---

基于全数据驱动能力系统（GAS 理念）的网络层扩展基准。本文档定义了战斗系统在客户端-服务器（C/S）架构下的同步策略、数据分类以及标准的 Protobuf 通信协议。

## 1. Philosophy (核心哲学)

1. **权威服务器 (Server Authority)**：所有游戏逻辑判定（伤害结算、状态挂载、条件校验）仅在服务器执行，客户端只做表现与意图预测。
2. **状态与事件分离**：网络同步严格分为**状态复制（State Replication）**与**瞬时事件（Transient Events）**。状态保证最终一致性，事件驱动表现层（VFX/SFX）播放。
3. **表现层自治**：客户端收到 Tag 或 Cue 变更指令后，独立查询本地 `cue_registry` 表单播放资源。网络协议绝不传输任何资源路径、特效缩放比例等表现数据。
4. **预测与回滚 (Prediction & Rollback)**：仅对玩家自身操控实体的 Ability 激活、Cost 扣除进行本地预测，失败时由服务器指令强制回滚。

---

## 2. Data Classification (数据分级)

所有需在网络中传输的战斗数据分为两级。

### Tier 1 — 权威复制 (Authoritative Replication)
服务器状态发生变更时，驱动客户端本地状态镜像更新。具备**最终一致性**，支持断线重连时的全量快照恢复。

| 数据 | 粒度 | 推送时机 | 备注 |
|------|------|----------|------|
| `Stat.currentValue` | 批量打包 | Tick 末尾脏标记汇总 | 仅同步配置了 `replicationMode` 的属性（如 HP/MP/移速） |
| `TagContainer` 增减 | 批量打包 | 即时或 Tick 末尾 | 用于客户端 UI 判断、特效常驻及预测预校验 |
| `StatusInstance` 增减 | 单个实例 | 挂载 / 移除时 | 包含 statusId、层数、总时长、实例 UID |
| `Status.RemainingTime`| 单个实例 | 低频校准 | 客户端自行 Tick 倒计时，服务器每 N 秒发送一次权威时间校准 |

### Tier 2 — 瞬时事件 (Transient Events)
服务器向客户端发送的一次性 RPC 消息。无状态，无需最终一致性保障（丢包可能导致漏播一次受击特效，但不影响数据正确性）。

| 数据 | 推送时机 | 载荷核心 | 备注 |
|------|----------|----------|------|
| **CueEvent** | 随 Effect 触发 | `cueId`, `target`, `magnitude` | 客户端收到后查 `cue_registry` 播放瞬发特效 |
| **CombatResult**| Pipeline 结算后 | `target`, `magnitude`, `resultTags`| 触发跳字、受击硬直、格挡动画 |
| **DisplaceActor**| 位移 Effect 触发 | `start`, `end`, `duration` | 客户端接管该实体的平滑移动内插 |
| **Ability响应** | 预测校验完毕 | `predictionKey`, `resultCode` | 客户端据此确认或回滚本地预演的技能 |

---

## 3. Protobuf Protocol (通信协议定义)

采用 `proto3` 规范。协议设计贯彻“配置 ID 用 `int32`，实体实例 ID 用 `uint64`，多态采用 `oneof`”的原则。

```protobuf
syntax = "proto3";
package combat.net;

// ==========================================
// 通用基础数据结构
// ==========================================

message Vec3 {
  // 工业界优化：可使用 int32 存储 (float_val * 1000) 以节省带宽
  float x = 1;
  float y = 2;
  float z = 3;
}

// 目标选择数据的多态表达
message TargetingData {
  oneof target_type {
    uint64 target_actor_id = 1;  // 锁定了具体实体
    Vec3 aim_position = 2;       // 鼠标/准星指向的地形坐标
    Vec3 aim_direction = 3;      // 纯方向向量（如摇杆朝向）
  }
}

// ==========================================
// 客户端 -> 服务器 (上行指令)
// ==========================================

// 客户端请求释放技能
message C2S_AbilityRequest {
  uint64 instigator_id = 1;      // 施法者 ID (防作弊前置校验)
  int32 ability_id = 2;          // 对应 ability 表的 id
  int32 prediction_key = 3;      // 客户端生成的自增序号，用于回滚对齐
  TargetingData targeting = 4;   // 客户端瞄准意图
  int64 client_timestamp = 5;    // 客户端发包毫秒级时间戳，用于服务器延迟补偿(Lag Compensation)
}


// ==========================================
// 服务器 -> 客户端 (下行事件 - Tier 2)
// ==========================================

// 技能激活结果（响应客户端的预测）
message S2C_AbilityResponse {
  int32 prediction_key = 1;
  int32 ability_id = 2;
  ActivationResult result = 3;
  int64 server_timestamp = 4;

  enum ActivationResult {
    CONFIRMED = 0;               // 预测成功，继续执行
    REJECTED_COOLDOWN = 1;       // 失败：CD 中
    REJECTED_COST = 2;           // 失败：资源不足
    REJECTED_TAG_BLOCKED = 3;    // 失败：处于沉默/眩晕等限制状态
    REJECTED_CONDITION = 4;      // 失败：前置条件不满足
    REJECTED_DEAD = 5;           // 失败：施法者已死亡
  }
}

// 战斗管线结算结果（伤害/治疗跳字、受击表现）
message S2C_CombatResult {
  uint64 target_id = 1;
  uint64 instigator_id = 2;
  int32 pipeline_id_ = 3;
  float final_magnitude = 4;     // 最终结算数值（用于跳字大小计算）
  
  repeated int32 result_tags = 5; // 如 Dodged, Blocked, Critical (tagId)
  repeated int32 cue_ids = 6;     // 随同结算下发的表现 Cue
}

// 瞬发表现触发
message S2C_CueEvent {
  int32 cue_id = 1;
  uint64 target_id = 2;
  uint64 instigator_id = 3;
  float magnitude = 4;           // 可能影响特效大小或音量
}

// 战术位移指令（冲刺/击退/拉扯）
message S2C_DisplaceActor {
  uint64 actor_id = 1;
  int32 motion_type_id = 2;      // 客户端据此选择动画 (如被击飞、主动翻滚)
  Vec3 start_pos = 3;
  Vec3 end_pos = 4;
  float duration = 5;
  int64 server_timestamp = 6;    // 客户端据此计算已经过去的时间，进行平滑插值
}


// ==========================================
// 服务器 -> 客户端 (下行状态 - Tier 1)
// ==========================================

// 属性批量更新 (Tick 脏标记收集后下发)
message S2C_StatUpdateBatch {
  uint64 actor_id = 1;
  repeated StatEntry updates = 2;

  message StatEntry {
    int32 stat_id = 1;
    float current_value = 2;
  }
}

// Tag 增删批量更新
message S2C_TagUpdateBatch {
  uint64 actor_id = 1;
  repeated int32 added_tags = 2;
  repeated int32 removed_tags = 3;
}

// Status 实例完整生命周期同步
message S2C_StatusSync {
  uint64 actor_id = 1;
  uint64 instance_uid = 2;       // 服务器分配的全局唯一状态 ID
  SyncAction action = 3;

  // 当 action = APPLIED 或 UPDATED 时有效
  int32 status_id = 4;
  uint64 instigator_id = 5;
  int32 current_stacks = 6;
  float remaining_duration = 7;  // 剩余秒数 (-1表示永久)
  int64 server_timestamp = 8;    // 时间校准基准线

  // 当 action = REMOVED 时有效
  RemovalReason reason = 9;

  enum SyncAction {
    APPLIED = 0;                 // 新增挂载
    UPDATED = 1;                 // 层数或时间刷新 / 周期校准
    REMOVED = 2;                 // 移除销毁
  }

  enum RemovalReason {
    EXPIRED = 0;                 // 时间到期自然脱落
    INTERRUPTED = 1;             // 被外力打断
    DISPELLED = 2;               // 被净化/驱散
    OVERFLOW = 3;                // 叠层溢出被替换
    MANUAL = 4;                  // 逻辑主动移除
  }
}

// 断线重连/初次见面的全量快照包
message S2C_ActorFullSnapshot {
  uint64 actor_id = 1;
  int64 server_timestamp = 2;
  
  repeated S2C_StatUpdateBatch.StatEntry stats = 3;
  repeated int32 active_tags = 4;              // 当前持有的所有叶子节点 Tag
  repeated S2C_StatusSync active_statuses = 5; // 当前挂载的所有 Status 快照
}
```

---

## 4. Relevancy & Interest Management (可见性与关注域)

为了节约服务器带宽开销，状态数据采取分级广播策略（类似 Unreal 的 NetRelevancy）。

| 数据项 | 广播范围 | 说明 |
|--------|----------|------|
| **自身属性** (HP, MP, Stamina) | `OwnerOnly` (仅自身) | 自己必须全知。 |
| **敌方核心属性** (仅 HP, Shield) | `NearbyClients` (AOI 范围内可见玩家) | 只需要用于渲染敌人的头顶血条，其他属性（如攻击力、冷却）**绝对不要下发**，防止外挂窥探。 |
| **状态/Buff** (`StatusSync`) | `NearbyClients` | 渲染模型外观特效、UI Buff 栏。 |
| **表现事件** (`CueEvent`) | `NearbyClients` | 视野外发生的特效和音效无需发送，由服务器网络层在发送前做距离剔除 (Distance Culling)。 |

---

## 5. Client Prediction Framework (客户端预测架构)

由于网络延迟存在，玩家按下技能键到服务器确认往往有 50~100ms 延迟。纯服务端权威会导致极差的“黏手感”。客户端必须进行**局部预测**。

### 预测管线流程：

1. **输入阶段 (Client)**：玩家按下技能按键。
2. **预校验 (Client)**：客户端调用底层的 `ActorConditions.test` 和 `TagContainer.hasTag`，检查本地镜像数据中的 CD、消耗和控制标签。
3. **预测执行 (Client)**：
   * 校验通过，生成 `prediction_key`。
   * **预扣** Cost 资源属性（本地血条/蓝条先扣）。
   * **预转** Cooldown 计时器。
   * 播放武器起手动画与本地 SFX。
   * 发送 `C2S_AbilityRequest`。
4. **权威裁决 (Server)**：
   * 服务器收到 Request，进行严格的权威校验。
   * 如果校验通过，执行后续 `Effect` 链，下发 `S2C_AbilityResponse (CONFIRMED)` 及产生的伤害/状态同步包。
   * 如果校验失败（由于延迟，服务器上该玩家刚好吃到了眩晕），下发 `S2C_AbilityResponse (REJECTED_TAG_BLOCKED)`。
5. **对账阶段 (Client)**：
   * 收到 `CONFIRMED`：清除本地预测标记，维持现状。
   * 收到 `REJECTED`：触发**回滚 (Rollback)**。将刚才预扣的 Cost 加回来，重置 CD，强行中断正在播放的攻击动画。

---

## 6. Anti-Cheat & Security (反作弊与安全边界)

系统设计遵循**“客户端数据不可信”**的绝对原则。

### 信任黑名单（绝不允许客户端上传的数据）：
* ❌ 造成的伤害量 / 治疗量
* ❌ 命中了哪些目标（目标扫描 `TargetScan` 必须在服务器用最新的空间索引重新计算）
* ❌ Buff 的剩余时间（以服务器下发的 `server_timestamp` 为准计算）
* ❌ 自身的移动速度（通过属性推算，而非客户端上报位移步长）

### 服务器审计清单 (Server Audit List)：
1. **技能 CD 审计**：记录玩家每个 Ability 的上次施放时间。收到请求时，即使差 1 毫秒冷却没转完，也直接返回 `REJECTED_COOLDOWN`。
2. **位置合法性校验 (Rubberbanding)**：针对附带位移的 Ability（如冲锋），如果客户端的施法起手位置与服务器记录的位置距离超过 `position_error_tolerance`（如 200 units），拉回客户端位置并拒绝技能。
3. **频率墙 (Rate Limiting)**：拦截封包发送器（Packet Injectors）。同一玩家一秒内的 `C2S_AbilityRequest` 超过阈值（如 10 次），直接丢弃并在服务器后台拉响警报。