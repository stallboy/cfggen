---
layout: page
title: tag
parent: 配表系统
nav_order: 6
---


# tag

```
为了抽取部分信息，而不是全部
比如写编辑器，客户端不希望读全部的配表数据，而只取部分。

让-gen生成时的参数own，跟事先在.cfg文件里配置的tag 配合来提取部分
```

## 只读部分目录

参见 [`文件和目录结构`-`兼容老的表格`](./directoryStructure.html#兼容老的表格) 一节

通过-exceldirs -jsondirs 限定读取的目录。



## 白名单

生成lua代码命令`-gen lua,own:client`，用own来表名，只提取加了`client` tag的数据

在table的meta里可配置任意tag，比如我们一般会包含client（用更精简的c也行）。用于提取特定的字段，减少客户端使用内存。

- 在field上标注tag就行，不用标注foreign key。foreign key是否提取，只由是否可行决定，能包含就包含。

- 如果在struct或table上配置了tag，分3种情况
  1. 所有field都没tag，-tag, 则包含所有field
     ```
     table task[id] (client){
         id:int;
         desc:text;
     }
     ```

  2. 有部分field设了tag，则取这设置了tag的field
     ```
     table task[id] (client){
         id:int (client);
         desc:text (client);
         reward:Reward;
     }
     ```
    
  3. 没有设置tag的，但有部分设置了-tag，则提取没设-tag的field

     ```
     table task[id] (client){
         id:int;
         desc:text;
         reward:Reward(-client);
     }
     ```

- 一般情况下，impl不需要设置tag，* 如果impl上设置tag，则是为了能filter出空结构，
    相当于只用此impl类名字做标志，普通的struct不支持filter出空结构。



## 黑名单

生成服务器代码命令`-gen java,own:-noserver`，用own来表名，注意noserver前的-，配合no实现了双重否定，
忽略设置了noserver的table

- table、struct、interface上设置noserver，则全部不要
    ```
    table avatar[id] (noserver){
        id:int;
        path:str;
    }
    ```

- field里设置nosserver，则只此field不要
    ```
    table task[id]{
        id:int;
        desc:text (noserver);
        reward:Reward;
    }
    ```

