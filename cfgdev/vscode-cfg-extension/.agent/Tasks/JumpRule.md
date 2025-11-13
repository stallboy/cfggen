# 跳转和引用规则

## 文件和模块

### 目录名->模块名

对于目录名称，按以下规则提取模块名：
1. 截取第一个"."之前的内容
2. 再截取"_汉字"或汉字之前的部分
3. 得到的名称作为module名

### 根目录确定规则

1. 从当前.cfg文件如果是`config.cfg`，则当前目录就是`<本配置所属根目录>`
2. 否则向上搜索父目录，直到发现包含`config.cfg`文件的目录，该目录即为`<本配置所属根目录>`

### 模块->文件
- 在`<pkg1对应目录>`下的.cfg文件，必然是`pkg1.cfg`
- `pkg1.pkg2`可能对应的文件路径是`pkg1_中文1/pkg2中文2/pkg2.cfg`


### 外键跳转解析规则


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
- 引用也包括`=>table1[field2]`
- pkg可以有多级，这里只用了2级来举例



### 类型定义跳转解析规则

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



### implementation

```typescript

interface Ref {
  refType?:string;
  refTypeStart:int;
  refTypeEnd:int;

  refTable?:string;
  refTableStart:int;
  refTableEnd:int;

  inInterfaceName?:string;
}


interface TRange {
  type:  'struct' | 'interface' | 'table';
  range: Range;
}

class FileDefinitionAndRef {
  definitions: Map<string， TRange>； // name -> range;
  definitionsInInterface: Map<string， Map<string， Range>>; // interfaceName -> structName -> range

  lineToRefs: Map<int, Ref> ;  //line ->  ref, 一行只能配置一个类型+一个外键，所以以line为key

 // 用于判断cache是否失效
  lastModified:long; 
  fileSize:long; 
}


class FileCache {
  cache:Map<string, FileDefinitionAndRef>； // filepath -> file definition and ref
}
```

- 参照以上数据结构，名称可以修改，但核心逻辑保留
- 一次CfgVisitor就填上FileDefinitionAndRef
- cache用lastModified和fileSize判断是否失效
- 根据位置在lineToRefs查找要寻找的ref信息，然后在相应的definitions中寻找到定义的位置



### 实现outline

用DocumentSymbolProvider 来实现

使用FileDefinitionAndRef里的definitions和definitionsInInterface来做


### 实现GoToReference

用ReferenceProvider  来实现

```typescript
interface TName {
  type:  'struct' | 'interface' | 'table';
  name: string;
}
interface ImplName{
  name: string;
  inInterfaceName: string;
}

class FileDefinitionAndRef {
  lineToDefinitions?: Map<int, string>;  // line ->  name,
  lineToDefinitionInInterfaces?: Map<int, ImplName>;  // line -> implName
  moduleName: string; // 根目录下则为空，`pkg1_中文1/pkg2中文2/pkg2.cfg` 则为pkg1.pkg2
}
```

FileDefinitionAndRef增加以上3字段。
lineToDefinitions，lineToDefinitionInInterfaces，延迟初始化，get时如果undefined则从definitions和definitionsInInterface中构造出来


provideReferences的逻辑：
1. getOrParseDefinitionAndRef得到curDef

2. 从lineToDefinitions查position.line得到tname，如果ok，否则跳到3

    1. 找到<本配置所属根目录>，同时找到文件所在的模块全称，
       比如`pkg1_中文1/pkg2中文2/pkg2.cfg`的模块全称为pkg1.pkg2，
       如此这个definition的全称就是pkg1.pkg2.[tname.name]
       
    2. 遍历<本配置所属根目录>，depth最多2层，目录名要符合能提取模块名的规则，目录下也有[模块名].cfg文件，
       根目录下读config.cfg。对这些.cfg文件做getOrParseDefinitionAndRef得到def:FileDefinitionAndRef
       对每个def，遍历lineToRefs，
        - 如果 refType == tname.name 放到refs里
        - 如果 refTable == tname.name也放到refs
        - 如果 curDef.module.length > 0 且 curDef.module.startWith(def.module + ".") 则找到relativeModuleName
          - 如果 refType == tname.name 放到refs里
          - 如果 refTable == tname.name也放到refs
    

3. 从lineToDefinitionInInterfaces得到implName，如果ok，
  遍历lineToRefs，如果inInterfaceName == implName.inInterfaceName，且refType == implName.name则加到refs里
