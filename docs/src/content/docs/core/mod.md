---
title: 增量 Mod
description: 在不修改基础配置包的前提下，通过增量 Mod 包覆盖或新增配置数据
sidebar:
  order: 11
---

增量 Mod 允许你在**不改动基础包**的前提下，用独立的配置目录新增或覆盖部分数据。导出时 Mod 包只保存增量记录，运行时与基础包叠加加载即可生效。

## 1. 准备 Mod 数据

Mod 目录与基础配置共享同一套 Schema，只需放入**要新增或修改的表**，未改动的表无需复制。

假设基础配置在 `config/`，Mod 配置在 `config_mod/`：

```text
config/                  # 基础配置（含 .cfg Schema 与全部表数据）
config_mod/              # Mod 目录（只放要改动的表，如 monster.xlsx、task.json）

```

在 `config_mod/` 中：

* 填入**已存在的主键**：运行时覆盖基础包的对应记录。
* 填入**新的主键**：运行时新增一条记录。
* 跨包外键直接正常填写（例如 Mod 中的新任务引用基础包里的物品 ID），导出和加载时都会基于合并后的完整数据做校验。

## 2. 导出基础包与 Mod 包

先正常导出基础包，再通过 `pack`、`requires`、`deps` 导出增量 Mod 包：

```bash
# 1. 导出基础包（默认 pack=base，生成 base.bytes）
java -jar cfggen.jar -datadir config \
     -gen bytes,dir=out

# 2. 导出增量 Mod 包（生成 demo-mod.bytes）
java -jar cfggen.jar -datadir config_mod -schema config \
     -gen bytes,dir=out,pack=demo-mod,requires=base,deps=out/base.bytes
     
```

| 参数 | 说明 |
| --- | --- |
| `pack` | 当前包名（默认 `base`），决定输出文件名 `<pack>.bytes` |
| `requires` | 声明依赖的包名，多个用 `,` 或 `;` 分隔（会写入包的元数据，供运行时校验顺序） |
| `deps` | 构建期参与合并校验的依赖 `.bytes` 文件路径，多个用 `;` 分隔 |

> 导出成功后，`demo-mod.bytes` 只包含 `config_mod/` 中的增量数据，但已经完成了全量外键与 Schema 兼容性检查。

## 3. 运行时加载与使用

### Java 示例

```java
// 1. 读取包文件
ConfigPackage base = ConfigPackage.read(Path.of("out/base.bytes"));
ConfigPackage mod1 = ConfigPackage.read(Path.of("out/mod1.bytes"));

// 若开启了 langSeparated，加载前先挂载对应语言包：
// base = base.attach(ConfigPackage.read(Path.of("out/base_en.bytes")));

// 2. 叠加加载（基础实例与 Mod 实例互相独立，可同时并存）
ConfigMgr baseMgr = ConfigMgrLoader.loadPackages(base);
ConfigMgr mod1Mgr = ConfigMgrLoader.loadPackages(base, mod1);

// 3. 设为全局默认配置（也可直接调用 ConfigMgrLoader.loadPackagesAndSetMgr(base, mod1)）
ConfigMgr.setMgr(mod1Mgr);

// 4. 查询某条记录被哪些 Mod 修改过（按生效顺序返回包名；未改动返回空列表）
List<String> origins = mod1Mgr.explain("monster", 1); // 例如 ["mod1"]

```

### C# 对应接口

C# 侧的包读取、语言挂载、默认切换与来源查询与 Java 一一对应：

```csharp
var basePack = ConfigPackage.ReadFile("out/base.bytes");
var mod1Pack = ConfigPackage.ReadFile("out/mod1.bytes");

// 挂载分离语言包：basePack = basePack.Attach(ConfigPackage.ReadFile("out/base_en.bytes"));
// 加载得到 mod1Mgr 后切换默认配置：ConfigMgr.SetMgr(mod1Mgr);
// 查询覆盖来源：var origins = mod1Mgr.Explain("monster", 1);

```

## 4. 从 Mod1 切换到 Mod2

`ConfigMgr` 加载完成后是**只读快照**，不支持在原实例上直接“卸载 Mod1 再打补丁”。要从 `mod1` 切换到 `mod2`，只需**复用已读入内存的 `base` 包**，重新与 `mod2` 组合加载并切换默认配置即可：

```java
// base 已在内存中且不可变，无需重新读盘，直接与 mod2 组合加载
ConfigPackage mod2 = ConfigPackage.read(Path.of("out/mod2.bytes"));
ConfigMgr mod2Mgr  = ConfigMgrLoader.loadPackages(base, mod2);

// 原子切换全局默认配置（C# 使用 ConfigMgr.SetMgr(mod2Mgr)）
ConfigMgr.setMgr(mod2Mgr);

```

* **无需重读基础包**：`ConfigPackage` 内部以独立游标视图读取数据，可安全反复复用。
* **平滑过渡**：切换后，后续通过 `ConfigMgr.getMgr()` 读取的都是 `mod2` 的配置；此前已持有旧 `mod1Mgr` 或旧记录引用的逻辑不会受影响，待不再引用后由 GC 自动回收。
* **回退到纯基础包**：同理，调用 `ConfigMgr.setMgr(baseMgr)`（或仅用 `base` 重新加载）即可切回无 Mod 状态。

## 5. 合并规则与限制

使用 Mod 时需遵守以下约束（违反会在导出或加载阶段直接报错）：

* **加载顺序**：被依赖的包必须排在前面，不能重复加载同名包，也不能循环依赖。
* **主键覆盖**：跨包同主键由后加载的包覆盖；**同一个包内同一张表**不允许主键重复。
* **Schema 一致**：Mod 包与基础包必须基于兼容的 Schema 导出。
* **固定枚举保护**：Schema 中定义的 `enum` 表允许 Mod 覆盖详情字段（如描述），但**不能增删枚举项**，带数值赋值的 `enum` **不能修改 `id**`。
* **安全热切换**：加载完成后 `ConfigMgr` 即冻结为只读快照；全局默认配置支持线程安全切换，不会读到半成品数据。
