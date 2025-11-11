
### 语法高亮规则

以下.cfg文件元素需要语法高亮显示：

- **结构定义**：`struct xx`, `interface xx`, `table xx`，只高亮xx
- **复杂结构类型声明**：对于非基本类型，高亮
- **主键字段名称**：`PK:int`里的PK
- **唯一键字段名称**：key_decl里的`[UK];`里的UK，以及具体的field定义比如`UK:int`里的UK
- **外键引用**：`-> tt`, `-> tt[kk]`, `=> tt[kk]`,整体统一对待。
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

对以上例子高亮
- **结构定义**：`Range`, `RewardItem`, `task`
- **复杂结构类型声明**：RewardItem 中的`list<int>`, `Range`,  task中的`RewardItem`
- **主键字段名称**：`id:int`中的`id`
- **外键引用**：`->item.item`


注意以上`list<int>`, `->item.item`视为一个整体来高亮