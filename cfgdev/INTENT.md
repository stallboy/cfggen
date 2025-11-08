- 使用antlr4定义语法结构：../app/src/main/java/configgen/schema/cfg/Cfg.g4
- 例子配置：../example/config/task/task.cfg
- 如果需要了解语义，参考../docs/docs/cfggen/03.schema.md

### 语法高亮规则

以下.cfg文件元素需要语法高亮显示：

- **结构定义**：`struct xx`, `interface xx`, `table xx`，只高亮xx
- **复杂结构类型声明**：对于非基本类型，高亮
- **主键字段名称**：`table xx[PK]` 里的PK，以及具体的field定义比如`PK:int`里的PK
- **唯一键字段名称**：key_decl里的`[UK];`里的UK，以及具体的field定义比如`UK:int`里的UK
- **外键引用**：`-> tt`, `-> tt[kk]`, `=> tt[kk]`,整体统一对待。
- **注释**：单行注释//，使用绿色，不是纯绿
- **特定元数据**：nullable, mustFill, enumRef, enum, entry, sep, pack, fix, block


```
struct Range {
	rmin:int; // 最小
	rmax:int; // 最大
}

struct RewardItem {
	chance:int; // 掉落概率
	itemids:list<int> ->item.item (fix=2); // 掉落物品
	range:Range; // 数量范围
}

table task[id] {
    id:int;
    reward:RewardItem;
}
```

以上例子有且只有以下内容要高亮

- **结构定义**：`Range`, `RewardItem`, `task`
- **复杂结构类型声明**：RewardItem 中的`list<int>`, `Range`,  task中的`RewardItem`
- **主键字段名称**：task中的`id`
- **外键引用**：`->item.item`
- **注释**：// 和后面的注释


注意以上`list<int>`, `->item.item`视为一个整体来高亮


- 预设两套主题色，其中一套是中国古典色，默认选择。
- 颜色选择放在vs code插件的配置里


### 外键跳转解析规则

系统支持以下两种外键引用格式的跳转：

#### 1. 优先查找本模块内引用 (`->table1`)
- 在当前.cfg文件中查找名为`table1`的表定义
- 如果找到，跳转到该表的定义位置
- 如果未找到，显示"未找到定义"提示

#### 2. 然后完整名称引用 (`->pkg1.table2`)
- **根目录确定**：从当前.cfg文件向上搜索父目录，直到发现包含`config.cfg`文件的目录，该目录即为`<本配置所属根目录>`
- **模块名提取**：对于目录名称，按以下规则提取模块名：
  1. 截取第一个"."之前的内容
  2. 再截取"_汉字"或汉字之前的部分
  3. 得到的名称作为module名
- **目录搜索**：如果提取的module名为`pkg1`，则该目录即为`<pkg1对应目录>`
- **文件查找**：在`<pkg1对应目录>`下查找`pkg1.cfg`文件
- **目标查找**：在`pkg1.cfg`文件中查找名为`table2`的表定义
- 如果找到，跳转到该表的定义位置
- 如果未找到，显示"未找到定义"提示
- pkg可以有0级或多级，这里只用了一级来举例

### 3. 其他
- 引用可以包括对应table1的field2（`=>table1[field2]`）
- 引用也包括`=>table1[field2]`


### 类型定义跳转解析规则
类似外键跳转解析规则，包含本模块类型和完整类型名称




还是不对，要只含这七类，中文名是：- **结构定义**
- **复杂结构类型声明**
- **主键字段名称**
- **唯一键字段名称**
- **外键引用**
- **注释**
- **特定元数据**


 1，不要高亮 struct，interface，table等关键词，只高亮名称。2，不要判断isUniqueKey，而改为判断isPrimaryKey，在enterTableDecl里可以记录primaryKey，
从而可以判断。3，少了comment。4，检查Syntax Node Types and Colors，修复。


完整review ### 5. ANTLR-Based Syntax Highlighting 这一段。1，Example Implementation
  逻辑我看基本正确，但最好不要用re来获取信息，而是用Cfg.g4里的结构来获得。2，Syntax Node Types and Colors 这一段不知道有什么用，如果无用请删除。


读specs，quickstart.md里### 2. 语法高亮 一节不对。参考...


 更新research.md里的Theme Color System Design，参照specs\001-cfg-editor-support\contracts\vscode-extension-api.md