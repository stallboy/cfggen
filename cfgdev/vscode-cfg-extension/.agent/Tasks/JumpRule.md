### 文件和模块

#### 文件名->模块名

对于目录名称，按以下规则提取模块名：
1. 截取第一个"."之前的内容
2. 再截取"_汉字"或汉字之前的部分
3. 得到的名称作为module名


#### 根目录确定规则

1. 从当前.cfg文件如果是`config.cfg`，则当前目录就是`<本配置所属根目录>`
2. 否则向上搜索父目录，直到发现包含`config.cfg`文件的目录，该目录即为`<本配置所属根目录>`

#### 模块->文件
- 在`<pkg1对应目录>`下的.cfg文件，必然是`pkg1.cfg`

`pkg1.pkg2`可能对应的文件路径是`pkg1_中文1/pkg2中文2/pkg2.cfg`


### 外键跳转解析规则

**当前状态**: ✅ 已实现

#### 1. 优先查找本模块内引用 
- 若是`->table1`, 在当前.cfg文件中查找名为`table1`的表定义
- 若是`->pkg1.pkg2.table1`, 则从当前.cfg文件所在目录开始查找`pkg1.pkg2`对应的.cfg文件，在那里找`table1`的表定义
- 如果找到，跳转到该表的定义位置

#### 2. 然后完整名称引用
- 先根据**根目录确定规则**找到根目录
- 若是`->table1`, 在根目录的config.cfg文件中查找名为`table1`的表定义
- 若是`->pkg1.pkg2.table1`, 则从根目录开始查找`pkg1.pkg2`对应的.cfg文件，在那里找`table1`的表定义
- 如果找到，跳转到该表的定义位置

### 3. 其他
- 引用也包括`=>table1[field2]`，点击`field2`跳转到table1对应的field2字段定义处
- pkg可以有多级，这里只用了2级来举例



### 类型定义跳转解析规则
**当前状态**: ✅ 已实现

例子：

```
interface InterfaceB {
  struct IX {
    x:int;
  }
  
  struct IY {
    x:IX;
    y:StructC;
  }
}

struct StructC {
  f1:StructA;
  f2:InterfaceB;
}

table t[id] {
  id:int;
  f3:pkg1.pkg2.StructD;
}
```

#### 1. 若在interface内，优先查找本interface内Struct类型 
- `y:IX`的`IX`: 先在`IY`其所属的`InterfaceB`中查找
- 如果找到，跳转到该表的定义位置

#### 2. 在本模块内查找
- `f2:InterfaceB`的`InterfaceB`：在本模块中找到
- `pkg1.pkg2.StructD`：从当前.cfg文件所在目录开始查找`pkg1.pkg2`对应的.cfg文件，在那里找`StructD`的定义
- 如果找到，跳转到定义位置


#### 3. 按完整名称查找
- 先根据**根目录确定规则**找到根目录
- `StructA`：在根目录的config.cfg文件中查找名为`StructA`的定义
- `pkg1.pkg2.StructD`：从当前.cfg文件所在目录开始查找`pkg1.pkg2`对应的.cfg文件，在那里找`StructD`的定义
- 如果找到，跳转到定义位置

