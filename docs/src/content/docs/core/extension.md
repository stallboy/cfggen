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
    buffIds:list<int> ->buff;
}

```

### 第二步：编写手写基类

继承生成包下的 `ConfigExtension`，重写 `onConfigReady()`：

```java
package game.config;

import config.ConfigExtension;
import config.ConfigMgr;

public abstract class SkillBase extends ConfigExtension {
    // 派生缓存字段
    private int totalBuffPower;

    // 声明需要访问的生成字段 getter（或由子类实现）
    public abstract java.util.List<Integer> getBuffIds();

    @Override
    protected void onConfigReady() {
        // 此时所有表、索引和外键都已就绪，可通过 config() 安全访问当前配置集
        ConfigMgr mgr = config();
        int sum = 0;
        for (int buffId : getBuffIds()) {
            var buff = mgr.getBuff(buffId);
            if (buff != null) {
                sum += buff.getPower();
            }
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
2. **两阶段初始化**：先对全部扩展实例绑定所属的 `ConfigMgr`（`_bind`），再统一调用各自的 `onConfigReady()`。

## 2. C# 用法（`IConfigExtension`）

C# 端通过实现 `IConfigExtension` 接口接入就绪回调（通常配合 `partial class` 写在手写文件中）：

```csharp
using Config;

public partial class Skill : IConfigExtension
{
    public int TotalBuffPower { get; private set; }

    public void OnConfigReady(ConfigMgr config)
    {
        // 所有表加载与外键解析完成后自动调用
        int sum = 0;
        foreach (int buffId in BuffIds)
        {
            // 通过传入的 config 访问当前配置集中的其他表
        }
        TotalBuffPower = sum;
    }
}

```

## 3. 使用注意事项

* **执行时机**：`onConfigReady` / `OnConfigReady` 仅在所有表读入、索引构建和外键解析**全部完成后**执行，此时读取任意关联表都是安全的。
* **禁止依赖全局默认配置**：在初始化回调或自定义方法中访问其他表时，务必使用 `config()`（Java）或回调参数 `config`（C#），**不要**调用静态的 `ConfigMgr.getMgr()` / `ConfigMgr.GetMgr()`。这样可以确保在基础包与多个 Mod 实例并存，或后台预加载新配置时，不会串读到其他实例的数据。
* **不可重复绑定**：每个扩展实例只绑定一次所属的 `ConfigMgr`，配置加载完成进入只读态后不应再修改其中的业务状态。

