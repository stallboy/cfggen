---
title: Java 和 C# 扩展与 Mod 实现规格
description: 共享结构的增量数据包、多配置实例、Java 父类扩展、C# 初始化接口及分阶段改造计划
sidebar:
  order: 99
---

本规格记录 cfggen 首版 Java 和 C# 配置扩展及 Mod 支持的行为和实施顺序。核心功能已实现，实际参数、加载入口及兼容迁移见第 10 节。TypeScript、Go、Godot 同步支持新容器的完整单包读取；多包合并和扩展生命周期目前由 Java/C# 提供。

记录日期：2026-10-19。

## 1. 已确认的范围

| 项目 | 首版决定 |
| --- | --- |
| Java 手写逻辑 | schema metadata 声明 `javaBase`，生成类继承手写父类；手写父类统一继承 `ConfigExtension` |
| 扩展用途 | 对象方法、派生数据和缓存；不存放角色血量等业务实例状态 |
| C# 扩展 | 手写 partial 类按需实现 `IConfigExtension`，由加载器驱动 `OnConfigReady` |
| 就绪检查 | 就绪状态归 `ConfigMgr`；只在加载完成、发布默认实例等边界检查，普通方法不逐次检查 |
| 数据组合 | 相同 schema，按表主键整条覆盖；新主键追加；没有提供的记录保留 |
| 加载顺序 | 调用方显式传入全部包及顺序，后者覆盖前者；加载器检查依赖，不自动寻找或重排 |
| 多实例 | 同一进程可同时持有多个组合，每个实例独占最终记录、关联引用和扩展缓存 |
| 静态查询 | 查询当前默认实例；其他实例通过实例 API 查询 |
| enum 和 entry | 符号名、ID 及映射固定；允许覆盖已有符号的详情数据；普通数据 ID 可新增 |
| 包构建 | 提供共享 schema、基础包及声明的依赖；验证有效组合，但只导出当前包自己的记录 |
| 包描述 | `base.bytes` 先写 int32 协议版本，再写 `map<string, string>` metadata 和命名数据块；包名和依赖为约定键，不增加清单文件或内容哈希 |
| 来源 | 内存保留包、表、主键和覆盖链，不做字段级追踪及运行时 Excel 行定位 |
| builder | 保留未绑定语义；不自动执行扩展初始化，首版不提供 `buildIn(cfg)` |
| 只读 | 加载后的索引和数据集合对外只读；Java 和 C# 同时落实 |

首版不提供记录删除、字段级补丁、运行时 schema 扩展、mod 脚本执行、依赖版本范围、内容锁定或隐式房间上下文。切换配置通过构造新实例并切换默认指针完成，不原地修改已发布实例。

## 2. 数据包与构建契约

### 2.1 metadata 与命名数据块

主包命名为 `<pack>.bytes`，默认包为 `base.bytes`。分离语言资源命名为 `<pack>_<language>.bytes`，例如 `base_en.bytes` 和 `desert-map_en.bytes`，可在同一目录中存放多个包。容器采用以下逻辑结构，仅协议版本作为固定前缀，包名和依赖继续是 metadata 约定键。详细编码、边界和迁移规则见 [配置包二进制格式设计](./config-bytes-format-design.md)。

```text
formatVersion: int32
metadata: map<string, string>
blocks: count + repeated { name: string, length: int32, payload: bytes }
```

metadata 使用直接编码的带长度 UTF-8 字符串，不依赖后面的池；包名和依赖是 `pack`、`requires` 约定键，依赖值为逗号分隔的包名。它们通过生成参数声明，最终语法遵循现有 `-gen bytes,...` 参数解析规则。扩展描述增加 metadata 项；二进制内容使用 `schema`、`strings`、`text/<language>`、`.<fullName>` 等数据块。

```text
基础包：pack = "base"；requires = ""
平衡包：pack = "combat-balance"；requires = "base"
地图包：pack = "desert-map"；requires = "base,combat-balance"
```

包名是稳定标识，区别于文件路径。名字按原值精确匹配，重复包名直接报错。一个实例的第一个包作为基础包，后续包作为增量层；不额外引入清单中的包种类字段。

默认仍可使用单个 `base.bytes`。若启用分离语言文件，语言文件使用相同容器，只带所属包 metadata 和语言块；它是资源，不作为额外数据包参与覆盖。不增加另一套清单。块名、块内编码与传输契约在 Java/C# 中一致，块的物理顺序不决定读取语义。

### 2.2 依赖与覆盖顺序

调用方传入 `[base, combat-balance, desert-map]`。加载器检查每个声明依赖都在当前位置之前出现，保持原顺序执行覆盖。

缺依赖、依赖位于后面、重复包名、自依赖或循环依赖均报错，不尝试自动修复顺序。依赖不是文件搜索指令或内容版本锁；同名包内容更新后依赖声明保持不变，但实际组合仍须通过验证。

### 2.3 共享 schema 与稀疏数据

Mod 构建新增固定 schema 路径，与现有根据表格自动调整 schema 的编辑工作流分开：

1. 读取指定的共享 schema，建立固定 enum/entry 符号集。
2. 按该 schema 解析当前包的数据。缺少某张表表示该包未修改此表，不删除 schema 中的表。
3. 检查本包的字段类型、必需数据和重复主键。
4. 按调用方提供的依赖顺序解码基础包与依赖包，再叠加当前包。
5. 对有效组合检查唯一键、引用、`seq` 连续性和固定符号身份。
6. 只序列化当前包自己的记录及所需的包内池，不把依赖包的记录展开写入输出。

生成 Java/C# 代码依据共享 schema 和基础包的固定符号集。正常 Mod 只生成数据，不重新生成或编译业务程序。构建期与运行期使用同一覆盖规则，运行时对实际组合重新验证；两个分别合法的 Mod 组合后仍可能发生唯一键或引用错误。

### 2.4 兼容性边界

复用已有内嵌 schema 的布局描述，在新包路径上由 Java/C# 加载器执行布局检查。新包应带有这部分信息，不再依赖调用者记得额外检查。现有描述没有完整记录主键、唯一键、外键和 `seq` 规则，因此不能把它宣称为生产者和消费者 schema 完全相等的证明。

首版以生成运行时所对应的共享 schema 为约束权威：构建采用这份 schema，运行时验证最终数据及固定符号。包之间有效字段布局、语言列表及顺序、默认语言等必须满足生成代码的读取契约。不同语言输出若使用不同的字段过滤结果，不能混为一个组合；有效 schema 与语言布局相同时，Java 和 C# 可消费相同 payload。

不新增 schema 哈希或业务版本体系。旧单包格式保留显式兼容读取入口；新格式通过生成模式和加载入口选择，不猜测新旧布局。其他语言消费者迁移之前，旧 bytes 生成默认保持不变。参与新组合时，基础包和 Mod 都重新导出，不猜测旧文件的包名及依赖。

## 3. 合并与实例隔离

### 3.1 记录语义

合并键是 schema 中的表主键，包括复合主键。相同主键整条替换，列表、Map、内嵌结构一并替换；未提供字段继续遵守 schema 原有的必填/默认值规则，不从被覆盖记录逐字段继承。

同一包中重复主键是错误。跨包相同主键属于正常覆盖。唯一键在最终主键合并结果上重新建立与验证；被覆盖掉的旧唯一键不应残留。

`seq` 表允许单个 Mod 只提供稀疏的覆盖记录，先放入临时记录容器。最终组合仍须满足 schema 原有的从零连续等约束，再生成对应数组；不能按单包记录数直接分配最终数组。

### 3.2 构建流程

```text
检查协议版本，读 metadata，扫描命名块范围，检查名字、顺序、依赖及读取布局
→ 各包独立解码为本次构建的新记录，暂不解析外键
→ 按主键覆盖，记录包来源
→ 基于最终记录建立全部主键和唯一索引
→ 验证最终数据，解析全部普通引用、反向引用列表及嵌套引用
→ 将数据和索引以只读集合暴露
→ 执行全部扩展初始化回调
→ 将 ConfigMgr 标记为 Ready 并返回
→ 可选地将其设为默认实例
```

只对最终保留下来的记录及其内嵌值对象执行扩展回调，被覆盖记录不参与。临时可变容器在构建阶段由加载器独占，不向业务暴露。

同一基础包可被多个实例读取，包字节和不可变池等输入资源允许共享；已解析引用、持有所属上下文或扩展缓存的记录不跨实例共享。首版每次构建解码自己的记录，不引入通用无类型对象引擎或已绑定记录复用优化。

### 3.3 默认实例与生命周期状态

就绪状态属于 `ConfigMgr`，概念上为 `Building → Initializing → Ready`。扩展回调期间处于 `Initializing`：全部配置字段、索引和引用已经可读，其他对象的扩展缓存尚不保证可用。

加载器只在全部工作完成后返回实例。设置默认实例时集中检查 `Ready`，完成安全发布；Java 可沿用现有 volatile 默认引用，C# 使用对应的安全发布方式。`requireReady` 如作为内部辅助方法存在，只属于这些边界，不加入扩展基类、普通 getter 或缓存读取方法。

普通查询不检查状态，初始化回调也不因 `ConfigMgr` 尚未到达最终 Ready 而被阻止查询。回调不得把正在构建的实例发布到其他线程或设为默认实例。异常中止构建，不返回半初始化实例，不修改已有实例或当前默认指针；框架不承诺回滚用户回调自行造成的外部副作用。

优先沿用已有 Java `ConfigMgr.getMgr()`、`setMgr(...)` 等名称。设置默认实例只检查并切换引用，不再重新绑定全局 enum/entry 详情。新多包加载入口返回独立实例；另提供加载成功后设置默认实例的便捷入口。

```java
// 示意 API；表查询名称沿用生成器的实际命名。
ConfigMgr a = ConfigMgrLoader.loadPackages(base, modA);
ConfigMgr b = ConfigMgrLoader.loadPackages(base, modB);
ConfigMgr.setMgr(b);

Monster.get(100);       // 当前默认实例 B 的最终记录
a.getMonster(100);     // 实例 A 的记录
```

切换后已持有的 A 中对象仍属于 A。一次业务操作若要求一致读取，先取得一个 `ConfigMgr` 局部变量，再用它完成多次查询；连续的静态查询可能遇到默认指针切换。

### 3.4 enum、entry、文本与诊断

固定符号名与 ID 映射，允许详情数据随实例变化。Java 在每个 `ConfigMgr` 内按固定符号顺序缓存详情引用：enum 使用 `ordinal()`，entry 保存固定的 `index`，这个下标与配置 ID 无关。`symbol.ref(cfg)` 直接读取该实例的引用数组；无参 `ref()` 先读取当前默认实例，再读取数组，无 Map 查询、装箱或额外的符号级 volatile 字段。缓存构建后不再修改，切换默认实例只替换 `ConfigMgr` 指针。C# 的 enum `Info(cfg)`、entry 数据查询采用相同的实例归属原则；现有无参接口/属性可作为默认实例访问入口，不保留与某次加载绑定的全局数据数组或字段。

enum 的代码序号与固定 ID 映射来自基础符号集，不能根据 Mod 记录顺序重新编号。特别是 C# `(int)e` 当前表示代码中的枚举序号，不等于配置主键 ID；运行时必须校验符号名与配置 ID 的映射，不能用序号替代主键。entry 表可以新增不产生新程序符号的普通记录，不能新增、重命名或更换既有固定符号的 ID。

各包使用自身的字符串/语言池解码。特别是 C# client Text，必须携带所属包的池上下文，或使用等价的实例内索引映射，不能再用一个全局数组解释所有包的整数索引。优先采用包级只读池句柄，避免合并时重写全部文本索引。

诊断不能使用跨线程共享的可变静态列表。Java 的每次加载创建独立的 `LoadValueErrs`，显式传递给加载、索引及引用解析；C# 使用本次读取器的独立集合。错误包含包来源、表、主键和失败约束。来源跟踪保留最终包和覆盖链，不需要保存被覆盖的整条旧对象，也不增加来源旁路文件。

## 4. Java 扩展契约

### 4.1 metadata 与类型

使用现有 metadata 机制声明：

```text
(javaBase='game.config.MonsterBehavior')
```

生成结果为 `Monster extends game.config.MonsterBehavior`；union 实现仍实现自己的 schema 接口，已有 sealed/final 策略不因父类扩展而一律关闭。普通表和 struct 可绑定父类；enum 表的绑定作用于详情结构，不作用于 Java enum 本体。

手写父类统一继承生成运行时提供的 `ConfigExtension`。它提供所属 `ConfigMgr` 的访问和初始化回调，不维护每对象 Ready 状态，不提供 `requireReady()`。生成器内部负责绑定上下文并调度回调，业务不直接重新绑定对象。

```java
public abstract class MonsterBehavior extends ConfigExtension {
    public abstract int getAttack();

    private int power;

    @Override
    protected void onConfigReady() {
        power = getAttack() * 10;
    }

    public final int power() {
        return power;
    }

    public final Skill findSkill(int id) {
        return config().getSkill(id);
    }
}
```

父类构造器必须允许生成类正常无参构造，不读取尚未填入的配置字段。父类声明所用 abstract getter，由生成 getter 满足契约；缺失的方法、错误的返回类型或父类继承关系由 Java 编译诊断。

手写源码放在生成输出目录之外，避免现有输出清理器删除它。`javaBase` 只影响 Java 生成，不影响 C# 类声明、数据布局或包依赖。

### 4.2 调度规则

所有 Java 扩展对象在回调前绑定到本次 `ConfigMgr`。回调读取所属实例，不使用当前默认实例替代。只有数据字段和已解析引用在回调阶段完整可用；不能依赖其他对象尚未计算的派生缓存，也不建立跨对象缓存初始化排序系统。

初始化遍历独立于 `HasRef`：没有外键的结构也可能有扩展；父结构需要遍历包含扩展的内嵌结构。遍历表记录及其拥有的值对象，不沿外键重复遍历其他表，避免引用环导致重复回调。每个保留的扩展对象一次初始化。

## 5. C# 扩展契约

使用 C# 命名约定提供接口，参数传递所属配置：

```csharp
public interface IConfigExtension
{
    void OnConfigReady(ConfigMgr config);
}
```

手写 partial 类按需实现，不通过 `javaBase` 启用，也不要求每个生成类型都实现接口。接口与生成代码处于兼容的 namespace/程序集边界。

```csharp
public partial class DMonster : IConfigExtension
{
    private ConfigMgr _config = null!;
    private int _power;

    void IConfigExtension.OnConfigReady(ConfigMgr config)
    {
        _config = config;
        _power = Attack * 10;
    }

    public int Power => _power;

    public DSkill? FindSkill(int id) => DSkill.Get(_config, id);
}
```

显式接口实现避免把初始化方法混入常用业务方法；加载器持有接口后仍可正常调用。只需计算一次缓存的扩展可不保存 `config`，只有后续方法需要任意跨表查询时才保存。

`_resolve` 只为有外键引用的类型生成，不再承担扩展登记。全部引用解析及校验通过后，`Processor` 从最终记录开始调用 `Ready`，直接执行 `OnConfigReady`，不保存扩展列表。普通叶子对象共用一个泛型 `Ready<T>`；只有持有嵌套结构的类型，才在 `Processor` 中生成遍历重载，union 也只分派包含嵌套结构的具体实现。遍历只走序列化持有的 struct、list/map 中的结构，不沿外键 `Ref`；每个嵌套对象都有唯一的持有位置，因此只回调一次。内容相同的不同对象仍分别回调。被覆盖的记录不在最终记录中，因此自身和嵌套对象均不回调。无需 reader 登记、扩展列表、反射、身份集合或每个类型的 `_collectExtensions` 方法。普通 getter 不判断扩展接口。

由接口承担生命周期入口，不同时增加另一套同名 partial 回调并重复调用。C# 不为所有配置对象增加每对象 Ready 标记或无条件的扩展上下文字段。

C# 扩展上下文通过回调参数获取；不要在自己的回调运行前访问其手写上下文字段。跨对象初始化只依赖配置字段、已解析引用及传入的 `ConfigMgr`，不调用依赖其他对象未完成初始化的扩展逻辑。实例返回后，所有参与回调的扩展已完成初始化。

## 6. builder、手工构造与只读集合

### 6.1 未绑定对象

Java `builder.build()`、struct 构造器、C# 对象初始化器构造的都是未绑定对象：原始字段和纯字段计算可使用，没有自动所属实例、外键解析或扩展缓存初始化，不加入任何表或索引。

首版不增加 `buildIn(cfg)`、自动注册或隐式绑定。业务遵守未绑定对象的使用约定；不生成逐次 Ready 检查，也不承诺误用时一定抛出某个特定异常。误读缓存可能得到默认值，误用上下文可能导致空引用，文档和示例须明确这一点。

已解除原有 builder 选项与引用生成的耦合：是否提供 builder 仅决定构造入口，是否需要生成引用解析由 schema 决定。加载器创建的该类型记录照常具备完整引用和扩展缓存。

### 6.2 集合边界

加载完成的实例对外暴露只读主键/唯一索引、表遍历集合、字段 List/Map、内嵌集合、引用列表。Java 使用不可修改视图或拷贝并封闭可变底层；C# 使用只读接口和不可被消费者改写的实际容器。仅把可变 `List` 强转为只读接口不构成保护。

内部 `seq` 数组保持私有；外部不能通过公开数组、`Arrays.asList` 的可写 `set`、字典视图或其他别名绕过只读约束。现有公开的 `ConfigMgr` 容器字段需要迁移为封装访问，这是有意的源码兼容变化。

构建阶段允许加载器填充内部容器，回调阶段已经只能读取配置集合。运行时自己创建这些容器，因此能够封闭底层所有权；手工构造的未绑定对象不自动获得整个已加载实例的不可变保证。

用户手写缓存自行遵守初始化后只读约定。框架不尝试冻结任意业务对象，不强制每次方法调用检查，也不把生成集合的只读保证扩大为所有用户代码都不可变。

## 7. 现有实现的改造位置

以下记录原有约束及本次改造位置。

| 位置 | 当前约束 | 改造目标 |
| --- | --- | --- |
| `app/src/main/java/configgen/ctx/Context.java`、`data/CfgSchemaAlignToData.java` | 数据读入会 align，可能删除缺数据的表并写回 schema | 增加固定 schema 的包构建路径，保留现有编辑路径 |
| `value/VTableParser.java`、`VTableCreator.java`、`CfgValueParser.java` | 解析记录后立即建立索引和整体验证 | 分开本包解析、主键合并、最终组合验证 |
| `genbytes/BytesGenerator.java`、`TableSerializer.java`、`CfgValueSerializer.java` | 固定段顺序、完整表记录及包内池 | metadata 与命名块、有界读取、稀疏表记录，复用表内及池编码 |
| `genjava/SchemaParser.java`、输入支持代码 | 现有 schema 描述侧重布局 | 新包读取时执行布局校验，最终约束由运行时验证 |
| `jte/java/ConfigMgr.jte`、`ConfigMgrLoader.jte` | 默认指针、浅拷贝 partial reload、全局重绑 | 独立实例构建、就绪边界、只切换默认指针 |
| `jte/java/GenStructuralClass.jte`、`GenStructuralClassTablePart.jte` | 具体类创建、整表容器初始化、builder 抑制引用 | 父类扩展、暂存记录、独立建索引、解除 builder 耦合 |
| `jte/java/GenEntryOrEnumClass.jte`、`GenMapGetBy.jte` | 符号保存可变 ref，静态查询依赖默认 mgr | 固定符号与实例详情分离，静态入口只代理默认实例 |
| `gencs/StructModel.java`、`jte/cs/GenStruct.jte`、`GenMapGetBy.jte` | 静态表容器、可变集合属性、全局 enum/entry 数据 | 实例级存储、只读集合、显式实例详情访问 |
| `jte/cs/GenModuleLoader.jte`、`Processor.jte` | `_resolve` 经静态表查询 | 传递当前实例，统一合并、解析和接口回调 |
| `support/cs/Loader.cs`、`jte/cs/ClientText.jte` | 客户端文本使用全局池和局部索引 | 保留每包文本池归属 |
| `genjava/LoadValueErrs.java` | sealed record 诊断与实例收集器 | 每次构建独立收集错误 |

移除旧 `loadPartialAndSetMgr`、`copyFrom` 入口。其浅拷贝再重新解析引用会修改旧实例共享对象；热更新统一重新构建独立实例后发布。不在 `ConfigMgr` 保存原始字节以支持局部重载。

## 8. 分阶段改造计划

每阶段保持可编译，并完成与本阶段行为有关的验证后再进入下一阶段。阶段成果是同一首版的实施步骤，不能把只完成 Java 当作整项功能交付。

### 阶段 1 配置实例与只读边界

**目标：** Java/C# 在单包加载下先支持两个彼此独立的配置实例。

- Java 保留实例容器，封装公开 map/数组；C# 将表存储与查询移入实例级 `ConfigMgr`。
- 两端引用解析都显式使用本次实例，所有静态查询代理默认实例。
- enum/entry 不持有跨实例可变详情；C# enum 序号采用固定符号集。
- 默认发布统一在 `ConfigMgr` 检查就绪；错误收集、文本池归属独立。
- 解除 Java builder 与引用生成的耦合，迁移数据集合、索引和遍历 API 为只读。
- 审计并处理旧局部加载入口，避免旧调用继续修改已返回实例。

**验收：** 同 ID 不同值的 A/B 同时存在；默认切换不改变 A 的已解析引用；enum/entry 和 C# 客户端文本不串实例；外部无法经集合或数组修改加载结果；builder 类型的 loader 产物仍有正确引用。

### 阶段 2 固定 schema 构建与命名块格式

**目标：** 生成可声明依赖的具名基础包和稀疏 Mod 包。

- 分离固定 schema 读取和自动 align，缺表不删 schema，不写回共享定义。
- 分离本包字段/重复主键验证和合并后的约束验证。
- 增加固定协议版本、metadata 生成参数和命名块容器；不支持的协议在读取 metadata 前报错，包名、依赖为约定键，解析器按块范围读取。
- 构建时读取基础包和声明依赖，验证组合，仅导出本包记录。
- Java/C# 都能读取相同 metadata 和块 payload，并使用布局检查；新旧入口显式区分，旧单包读取继续可用。

**验收：** 只含一条高 ID 覆盖的 Mod 可输出；跨包引用在构建时可验证；输出不夹带基础数据；metadata 可扩展，未知可选块可跳过，重复块和块越界报错；缺依赖、重复包名和布局不兼容有明确错误；分语言资源保持所属包。

### 阶段 3 多包运行时合并

**目标：** 同一套包顺序在 Java/C# 得到相同最终配置。

- 各包解码为本次构建独占的未解析记录，按主键整条覆盖。
- 统一最终唯一性、外键、反向引用、固定符号、`seq` 等验证。
- 合并后建立全部索引，再解析全部引用；不依赖 Map 插入时的偶然覆盖或异常。
- 保留包来源和覆盖链，提供查询入口。
- 加载成功后才返回，便捷入口在成功后才切换默认实例。

**验收：** 基础 100 条加三条同 ID 覆盖及两条新增，结果 102 条；基础记录引用指向 Mod 覆盖后的目标；不同包组合各自独立；中途任何失败不影响默认实例；同包重复主键与最终唯一键冲突正确区分。

### 阶段 4 Java 父类和 C# 接口扩展

**目标：** 两端在全部配置引用完成后执行一次扩展初始化。

- 解析并校验 `javaBase` 元数据，覆盖普通结构、表、union 实现及 enum 详情的生成路径。
- 提供 Java `ConfigExtension`，生成内部上下文绑定及回调入口。
- 提供 C# `IConfigExtension.OnConfigReady(ConfigMgr)`，支持用户 partial 类按需实现。
- 生成独立于外键的拥有关系遍历，覆盖无外键的内嵌扩展结构。
- 确认 builder/手工构造保持未绑定、不自动回调；移除逐方法 Ready 检查的设计。

**验收：** Java 对象可直接调用父类方法；C# 接口由加载器自动发现并调用；被覆盖记录不回调，每个最终扩展对象只回调一次；嵌套扩展、循环外键均不漏调或重复；缓存来自所属实例；回调失败不发布；builder 只保留原始数据。

### 阶段 5 兼容迁移与完整交付

**目标：** 用相同样例完成 Java/C# 端到端验收，并说明真实的兼容边界。

- 更新 Java/C# 生成示例，加入基础包、两个增量包、两个并存组合和默认切换。
- 补充静态默认入口、显式实例入口、enum/entry 详情访问、父类/接口扩展及未绑定对象示例。
- 验证现有 supported Java/C# 生成模式，包括 Unity/普通 C#、客户端/服务端文本和分离语言资源。
- 给出 C# 只读集合属性、Java 公开容器收敛、旧 partial reload 的迁移说明。
- 运行现有生成器测试、示例生成与编译，并执行本规格的端到端行为验证。
- 补充包参数与错误信息文档；Java、C#（普通/Unity）、TypeScript、Go、Godot 统一读取版本化容器，移除旧格式。

**验收：** Java 与 C# 对同一组合返回一致的主键集合、字段值、引用关系和覆盖来源；旧单包场景可按兼容路径使用；新增 Mod 不要求重新生成和编译业务代码。

## 9. 首版验收清单

| 场景 | 必须成立的结果 |
| --- | --- |
| 同主键覆盖 | 后包整条替换，旧唯一索引不残留 |
| 新主键追加 | 可通过默认入口与所属实例入口查询 |
| 缺依赖、错误顺序、重复包名 | 拒绝加载，不自动排序 |
| 多实例并存与切换默认 | 旧对象的关联配置和缓存保持原实例归属 |
| 同包重复主键、最终唯一键冲突 | 两端均明确报错，不能由底层容器行为决定 |
| 跨包引用、反向列表 | 基于最终组合解析 |
| 固定 enum/entry | 名称和 ID 不变，详情可覆盖；C# 序号不随 Mod 行顺序变化 |
| 稀疏 seq 覆盖 | 本包允许稀疏，最终集合满足连续性要求 |
| 不同包相同文本池索引 | 读取各自正确文本，多实例与分离语言文件不串用 |
| 无外键的内嵌扩展 | 仍调用初始化，且每个对象一次 |
| Java 与 C# 扩展 | 全部引用可读后调用；普通字段和缓存访问没有 Ready 分支 |
| builder/对象初始化器 | 无绑定、无回调；误用按约定负责，不承诺特定异常 |
| 修改集合、索引或数组别名 | 已加载数据无法被改写 |
| 回调或校验失败 | 不返回半成品，不切换默认实例，不修改旧实例 |
| 同时构建两个组合 | 数据、文本和诊断互不污染 |
| 重跑生成器 | 不删除独立源码目录中的 Java 父类和 C# partial 文件 |

## 10. 当前实现与使用

### 构建和导出

在仓库根执行 `app/gradlew.bat -p app fatJar`。以下使用新构建的 JAR，不覆盖根目录现有的发布 JAR：

```shell
java -jar app/build/libs/cfggen.jar -datadir config -gen java,dir:generated/java -gen cs,dir:generated/cs -gen bytes,pack:base,dir:out/base
java -jar app/build/libs/cfggen.jar -datadir mods/desert -schema config/config.cfg -gen bytes,pack:desert,requires:base,deps:out/base/base.bytes,dir:out/desert
```

现有参数用冒号赋值、逗号分隔。多个依赖分别写为 `requires:base;balance,deps:out/base/base.bytes;out/balance/balance.bytes`，整个 `bytes,...` 参数加 shell 引号。`deps` 按显式顺序包含全部直接/传递依赖，`requires` 仅声明直接依赖；包名同时作为文件名，禁止路径分隔符、保留字符、逗号、分号和首尾空白。`-schema` 接受共享根文件或目录，一起读取模块定义，不 align、不写回；未知数据表报错。

包名默认为 `base`，始终包含 schema，直接 `-gen bytes` 即可输出版本化容器。只支持当前格式，旧数据须重新导出；不提供 `format` 或 `schema` 导出选项。多个独立包应显式指定各自的 `pack`。Mod 只重新导出数据，不重新生成/编译业务代码。Java 父类和 C# partial 源码都应放在生成目录之外。

构建参数 `deps` 读取已经解码的包文件，不自动解密。`cipher` 仍是容器之外的传输编码，业务读取器应先解码再传入包 API；可保留未加密的构建产物作为依赖输入。

### Java 和 C#

```java
// ConfigPackage 位于 configgen.genjava；其余类型位于生成目标包。
ConfigPackage base = ConfigPackage.read(Path.of("out/base/base.bytes"));
ConfigPackage mod = ConfigPackage.read(Path.of("out/desert/desert.bytes"));
ConfigMgr original = ConfigMgrLoader.loadPackages(base);
ConfigMgr game = ConfigMgrLoader.loadPackages(base, mod);
ConfigMgr.setMgr(game);
Monster added = Monster.get(1002);
Monster old = original.getMonster(1);
var chain = game.explain("monster", 1); // [desert]
```

```csharp
var original = Processor.LoadPackages(ConfigPackage.ReadFile("out/base/base.bytes"));
var game = Processor.LoadPackages(ConfigPackage.ReadFile("out/base/base.bytes"), ConfigPackage.ReadFile("out/desert/desert.bytes"));
ConfigMgr.SetMgr(game);
var added = DMonster.Get(1002);
var old = DMonster.Get(original, 1);
var details = DRank.Green.Info(original);
var chain = game.Explain("monster", 1);
```

Java `loadPackagesAndSetMgr(...)`、C# `LoadPackagesAndSetMgr(...)` 在构建成功后设置默认实例。普通加载不切换默认实例，校验/回调失败不发布半成品。C# 默认前缀 `D`；显式查询是 `DMonster.Get(config, id)` / `All(config)`，entry 是 `NameIn(config)`，enum 是 `Info(config)`。

热更新重新调用 `loadPackages(...)`，成功后 `ConfigMgr.setMgr(...)`。`ConfigMgr` 只保存最终索引、来源信息和就绪状态，不保留原始输入。业务使用生成实例的 get/all 方法；下划线 `_get_...` / `_set_...` / `_index` / `_resolve` 属于生成器内部协议。

Java 的 `configgen.genjava.ConfigLoader<K, T>` 是普通 Java 支持类，随运行时源码分发，不再使用模板，也不生成子类。构造函数接收表名、解码器、主键函数、索引函数和引用解析器，不依赖生成的 `ConfigMgr` 类型。生成的总加载器把索引、引用解析函数绑定到当前实例；普通表使用 `mgr::_set_表名_All`，只有 `seq`、唯一键需要生成表的静态 `_index` 函数。

加载器合并主键 Map，在索引交接前封闭可变 Map 并冻结来源列表，随后把只读容器交给 `ConfigMgr`。内部 `_set_` 接管生成器独占的容器并包装只读视图，不再复制整张 Map/List；调用方交接后不得继续修改底层容器。无外键的表不遍历解析；错误上下文延迟到实际发现错误时才格式化，成功记录不拼接诊断文字。稀疏包只扫描自身实际存在的表块，不遍历 schema 全部表。

第一包视为基础包，不为其记录建立来源项；`origins` 只保存后续 Mod 写入的主键与包名链，未改动的基础数据查询来源时返回空列表。基础包重复主键用合并索引识别，Mod 包内重复主键通过来源链的最后一个包名识别，无需 `localKeys`。总加载器用 `ConfigLoader<?, ?>` 分发表数据，无 `Object` 记录容器或行强转；返回配置后不再持有临时加载器。完成阶段依次：建索引、建立符号引用缓存并校验符号、解析引用、绑定全部扩展、执行回调、标记就绪。缓存由生成的 `setAllRefs` 一次建立，解析阶段即可使用；只为有详情数据的 enum 和 entry 分配缓存。

Java 生成代码及支持源码以 JDK 25 为目标。`LoadValueErrs.Error` 为 sealed interface，permits 具体的 record：`MissingReference` 携带来源类型、字段、目标表、引用键，`InvalidSeq` 携带值和有效大小，`ChangedSymbolId` 携带预期和实际 ID，包依赖及二进制格式错误也各有参数明确的 record。错误所需参数直接放在各自 record 中，无 `Row` 包装或 `at` 可变上下文。包内重复主键用 `DuplicateRecord(table, pack, key)`，最终唯一键冲突用 `DuplicateUniqueKey(table, field, rowKey, key, packs)`；索引和符号错误保留自己的来源链。引用错误只记录结构、字段、目标和引用键，复合键保留为值列表。公共收集方法按类别提供，只记录调用方已判断的错误；不接收待校验对象、不读取包、不比较 schema，通用的 `add/fail` 均为 private。

错误收集器是普通对象，无静态状态、`ThreadLocal`、Supplier 上下文或 checkpoint。每次加载自动收集可恢复的错误，建立索引后检查一次，引用和符号校验后再检查；失败不调用扩展、不发布实例。`ConfigLoader` 不再逐行设置诊断上下文或查询来源链，引用解析直接遍历最终记录。未知 union、截断数据和非法池索引记录后立即终止读取。`ConfigErr.getErrors()` 返回本次失败的不可变错误快照，可使用模式匹配直接处理具体 record。后续加载无需清空或重置错误，多个加载可并行使用同一包，各自拥有收集器。固定符号在 `setAllRefs` 中统一检查。

`ConfigPackage` 在读取时解析并冻结包名、依赖列表，保留原始 metadata 扩展项。`attach(resources...)` 校验资源归属、块类型和重名后创建新包，复用已解析的身份信息。统一入口 `block(".monster", "en", "zh_cn")` 返回可解码的表块，池由包内部按语言顺序延迟解析并缓存；省略语言参数则使用 metadata 的默认语言。`block("schema")` 等内部块不初始化池。每次读取使用独立游标，支持同包并发构建；无需独立的 table 接口、语言视图或公开池载体。

生成的 Java 类型只在方法签名需要错误收集器时 import `configgen.genjava.LoadValueErrs`；同名生成类使用全限定名。创建对象时通过 `input.errors()` 收集错误，无需为了 Map 或 union 错误单独 import。生成的引用解析只有 `_resolve`，不再生成 `_resolveDirect` 转发；引用查询、null 判断和错误输出由生成代码依次完成；`missingReference(source, field, target, key)` 只收集错误，不检查对象或返回值。固定符号同样由生成代码判断 `ref(mgr) == null`，再调用 `missingSymbol(table, symbol, key)`。复合引用键仅在失败分支中构造；nullable 引用不生成缺失检查。`ConfigPackage.validateSchema(expected, errors)` 负责读取包内 schema 并双向比较，失败时调用纯记录的 `schemaMismatch(pack, reason)`；现有 `compatible` 对 interface 和部分枚举是单向的，单次调用不能排除额外结构或符号。

Java 的 `setAllRefs` 先取得名字对应的固定符号，再在公共分支比较 ID；switch 的每个 case 只返回符号，不重复生成校验和错误输出。字符串主键 enum 直接复用已有的 `get(symbol)`。命名 enum 的显式 ID 保存在符号的 `fixedId` 字段中。缺失检查遍历现有 `map.values()`，不再调用会复制数组的 `values()`。

C# 的 `Processor.ReadTable<K, T>` 直接解码并合并每条记录，主键及记录保留具体类型，不为每包每表额外创建 `ReadRows` 临时列表。`ConfigMgr` 生成每张表具体类型的索引和遍历集合；enum 详情缓存为按代码序号访问的数组，entry 为每个固定符号生成直接引用字段。`Info(config)`、`NameIn(config)` 和默认实例入口均不查符号字典或表索引，缓存于索引阶段建立，在解析引用及扩展回调前可用。

C# 的名字到 enum 转换按符号数量生成：零个直接使用无效序号，一个使用条件表达式，多个使用 switch 表达式。不使用 `Enum.Parse`，也不在初始化时再次匹配 enum 字符串。未知名字暂记为无效序号，在最终记录索引阶段统一报告。固定主键或显式 ID 按符号序号查生成的常量数组；空符号集不生成这些数组，主键本身就是符号名字时不重复比较相同值。entry 不生成临时引用变量、符号字典或重复符号判断：合并后主键唯一，固定名字与主键映射校验已保证符号唯一。enum/entry 缓存按固定键从最终索引一次建立，缺失检查继续保留。缓存字段标注为可空以反映构建阶段的状态，公开访问入口依赖加载成功后的完整性保证。

C# 错误使用 `LoadError` record 层级，具体 record 只保存各自需要的参数；每次加载创建一个 `LoadErrors`，分类方法只收集错误，通用 `Add/Fail` 为 private。无 `SimpleIssueHandler`、`IIssueHandler`、`SetRowContext`、每行诊断字符串或包装异常的 `try/catch`。缺失引用直接记录 source、field、target、实际 key，复合引用键为值元组；重复主键记录 table、pack、key，索引和固定符号错误只在失败分支取得来源链。包结构/版本、schema、字符串池和 union 错误也有独立 record。可恢复错误在加载阶段统一检查；无法继续解码的错误立即抛出。`ConfigException.Errors` 是不可变快照；异常消息只在抛出时格式化，并包含实际来源包名。

普通 C# 和 Unity 共用 C# 9 兼容的 `Loader.cs`。`ConfigReader` 直接读取有边界的字节视图，`ConfigPackage.Block(".monster", "en", "zh_cn")` 内部绑定字符串和文本池；不再使用 `BinaryReader`、`MemoryStream`、重复的 Cursor 或公开的 Data 包装。各块游标和错误收集器独立，语言池按顺序延迟解析并缓存，支持同包并行加载。包名和依赖在读取时解析，`Attach(resources...)` 复用身份信息；代码 schema 的规范签名只计算一次。复合键生成 sealed record；空集合复用空实例，非空 List 包装只读视图，Map 使用维护序列化顺序的 `ConfigMap`，交接时不再复制。服务端文本直接读取池中的各语言值，无每文本临时数组；客户端 Text 始终绑定所属包的池。

不引入 `ConfigNode` / `IConfigNode`。Java 仅为自身或内嵌类型具有 `javaBase` 的类型生成 `_collectExtensions`，按对象身份去重，不跟随外键；无扩展的 schema 不生成收集阶段。Java 先绑定全部扩展，再直接调用受保护的 `onConfigReady()`，不再添加 `_initialize()` 转发。C# 全量引用校验通过后，由 `Processor` 的泛型入口和必要的嵌套遍历重载直接调用 `OnConfigReady(config)`；`_resolve` 只处理引用，不生成扩展登记状态。手工构造且未参与加载的对象不会自动回调。

### 分离语言与其他语言

使用 `langSeparated` 时，先把 sidecar 挂到所属包，再读取：Java/Godot `attach`，C#/TS/Go `Attach`。它们返回新包，不修改原包；打开语言池时检查 textCount 和索引。主文件和资源仍须成套分发。

| 语言 | 容器入口 | 范围 |
| --- | --- | --- |
| Java | `ConfigMgrLoader.loadPackages(...)` | 多包、多实例、扩展 |
| C# / Unity | `Processor.LoadPackages(...)` | 多包、多实例、扩展 |
| TS | `Config.Processor.ProcessPackage(ConfigPackage.Read(buffer), errors)` | 完整单包；`Loader.LoadBytes` 同样读取新格式 |
| Go | `InitPackage(ReadPackage(data))` | 完整单包；`Init(io.Reader)` 同样读取新格式 |
| Godot | `ConfigProcessor.new().load_from_package(ConfigPackage.read_file(path), errors)` | 完整单包 |

TS 的 `ConfigPackage` 从 `ConfigUtil.ts` 导出。后三者保持既有静态/全局对象模型，拒绝带 `requires` 或缺少所需表的独立包，避免误用旧数据完成增量覆盖；它们不提供 Java/C# 的 Mod 组合 API。Lua 本身输出 Lua 数据，不消费 `base.bytes`。

C# client 使用所属包的文本池，以 `TextPoolManager.Language` 选择语言。TS/Go/Godot client 保持原有 TextPoolManager 用法，从返回 stream 的语言池选择当前语言。serverText 生成各语言字段。

Java、C#、TS、Go、Godot 的普通模式均由包的 `defaultLanguage` 选择默认文本池，不在生成的加载调用中写死 `"default"`。Java/C# 多语言模式将与生成字段顺序一致的语言列表保存为一份私有静态数组，只在打开块时使用；C# 的 `ReadTable` 不接收语言参数。Java/C# 的默认语言列表和文本池均在包内缓存，避免逐表重复分配。Mod 构建器在普通模式下也使用依赖包自己的默认语言；多语言模式使用当前构建的默认语言读取依赖数据。

TS 推荐使用生成的 `Processor.ProcessPackage` 加载，它会选择生成代码需要的语言。直接使用通用 `Loader.LoadBytes(data, Processor.Process, errors, languages?)` 时，多语言调用方显式传入语言列表；省略或传入空列表表示包的默认语言。此入口不从回调推断语言。

TS/Go/Godot 在替换静态表之前预检必要表是否齐全，缺表时保留原来的配置。TS/Godot 不再在预检后维护和删除同一份缺表集合；Go 直接检查包的块目录，不再额外构造表名 Map。所有二进制读取器均拒绝负数集合长度，零字节结构的集合使用 `ReadCount(0)` 或等价入口。

Java、C#、TS、Go 的多语言 Text 均在读取文本索引后直接取得对应语言值，不为每个字段创建临时文本数组。语言池长度在加载时统一校验，文本索引只检查第一个池的边界；TS/Go 的单语言和兼容数组入口也复用该检查。Go 的有界读取器直接定位以跳过数据，不复制整块字节；TS 复用 UTF-8 解码器。Godot 的 union 工厂遇到未知标签时必须将输入标记为失败，不能仅打印错误后返回 null；单一实现的工厂在各语言中使用直接判断，多个实现保留分派。

### 升级与验证

支持文件沿用“已存在不覆盖”的规则。升级已有输出目录时，应一并更新 Java `configgen.genjava` 源码，以及 C# `Loader.cs`、TS `ConfigUtil.ts`、Go `stream.go` / `LoadErrors.go` / `package.go`、Godot 支持文件。先保存手改差异，移走旧支持文件再重新生成；保留旧 loader 而只生成新类型会导致 API 不匹配。

```shell
app/gradlew.bat -p app test dotnetTest otherRuntimeTest godotTest fatJar
```

普通测试覆盖 Java 和生成器；显式外部任务分别实际运行 .NET 普通/Unity C# 9 的 client/serverText、Node+TypeScript、Go、Godot 4.6 headless。Godot 路径可通过 `-Dcfggen.godot=<绝对路径>` 指定；TS 编译器默认来自 `cfgeditor/node_modules/typescript/bin/tsc`。工具缺失时测试任务直接失败，不静默跳过。
