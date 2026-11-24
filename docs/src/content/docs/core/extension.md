---
title: 运行时扩展
description: 在生成的配置对象上安全挂载手写逻辑、缓存与初始化回调
sidebar:
  order: 12
---

运行时扩展允许你在**不修改生成代码**的前提下，为配置对象挂载自定义的业务方法、派生缓存或二次索引，并在所有配置加载、外键解析完成后自动触发初始化回调。

## 1. Java 用法（`ConfigExtension`）

Java 端通过 Schema 的 `javaBase` 属性，让生成的配置类继承你手写的基类；手写基类继承生成的 `ConfigExtension` 即可接入生命周期。

### 第一步：在 Schema 中指定基类

```cfg
table skill[id] (javaBase='game.config.SkillBase') {
    id:int;
    baseDamage:int;
    buffIds:list<int> ->buff (sep='|');
}

table buff[id] {
    id:int;
    power:int;
}

```

### 第二步：编写手写基类

继承生成包下的 `ConfigExtension`，重写 `onConfigReady(ConfigMgr _mgr)`。

```java
package game.config;

import config.Buff;
import config.ConfigExtension;
import config.ConfigMgr;

import java.util.List;

public abstract class SkillBase extends ConfigExtension {
    // 派生缓存字段
    private int totalBuffPower;

    public abstract List<Buff> refBuffIds();

    @Override
    protected void onConfigReady(ConfigMgr _mgr) {
        int sum = 0;
        for (Buff buff : refBuffIds()) {
            sum += buff.getPower();
        }
        this.totalBuffPower = sum;
    }

    public int getTotalBuffPower() {
        return totalBuffPower;
    }
}

```

加载配置时，`ConfigMgrLoader` 会自动完成两件事：

1. **递归收集**：自动遍历表记录以及嵌套在 `struct`、`interface`、`list`、`map`（键和值）中的所有扩展实例，并按对象身份去重。
2. **初始化回调**：收集完成后，为每个扩展实例调用一次 `onConfigReady(ConfigMgr _mgr)`，传入当前加载的配置集。

## 2. C# 用法（`IConfigExtension`）

C# 端通过实现 `IConfigExtension` 接口接入就绪回调（通常配合 `partial class` 写在手写文件中）：

```csharp
using Config;

public partial class Skill : IConfigExtension
{
    public int TotalBuffPower { get; private set; }

    public void OnConfigReady(ConfigMgr _mgr)
    {
        // 所有表加载与外键解析完成后自动调用
        int sum = 0;
        foreach (int buffId in BuffIds)
        {
            // 通过传入的 _mgr 访问当前配置集中的其他表
        }
        TotalBuffPower = sum;
    }
}

```

## 3. 使用注意事项

* **执行时机**：`onConfigReady` / `OnConfigReady` 仅在所有表读入、索引构建和外键解析**全部完成后**执行，此时读取任意关联表都是安全的。
* **访问当前配置集**：优先读取已解析的外键。初始化回调中需要按 key 查其他表时，使用参数 `_mgr`，确保基础包、Mod 或后台预加载的配置实例各自读取本实例的数据。
* **配置只读**：配置加载完成后保持扩展缓存和业务状态只读。

