---
layout: page
title: 命令行
parent: 配表系统
nav_order: 1
---

# 命令行

```
Usage: cfggen.exe(java -jar cfggen.jar) [options] -datadir [dir] [options] [gens]

-----schema & data
    -datadir          配表根目录，目录下有文件config.cfg
    -headrow          csv/Excel文件里数据头行数, 默认为2
    -encoding         csv编码，默认是GBK，如果文件中含有bom则用bom标记的编码
    -asroot           兼容之前的目录结构，有ClientTables、PublicTables、ServerTables目录，目录下是.txt后缀的tsv文件。这里可以配置为'ClientTables:noserver,PublicTables,ServerTables:noclient',配合gen的own:-noclient来提取
    -exceldirs        excel目录，以,分隔
    -jsondirs         json目录，以,分隔，-asroot、-exceldirs、-jsondirs一旦有一个配置，说明要明确只用-datadir下的部分目录，而不是全部。

-----i18n support
    -i18nfile         国际化需要的文件，如果不用国际化，就不要配置
    -langswitchdir    国际化并且可随时切换语言
    -defaultlang      langswitchdir设置时有效，表示默认的语言，默认为zh_cn

-----tools
    -verify           检查配表约束
    -searchto         保存搜索结果到文件, 默认是stdout
    -searchtag        搜索部分配置.默认是全部
    -search           后接命令，找到匹配的数据
        int <int> <int> ...: search integers
        str <str>: search string
        ref <refTable<[uniqKeys]>?> <IgnoredTables>: search ref
        sl  <name>?: list name of schemas
        sll <name>?: list schemas
        slljson <name>?: list schemas. use json
        h: help
        q: quit
    -binarytotext     后可接1或2个参数（java data的file，table名称-用startsWith匹配），打印table的定义和数据
    -binarytotextloop 后可接1个参数（java data的file），打印table的定义和数据
    -xmltocfg         .xml变成.cfg文件
    -compareterm      检查翻译名词表
-----options
    -v                verbose，级别1，输出统计和warning信息
    -vv               verbose，级别2，输出额外信息
    -p                profiler，内存和时间监测
    -pp               profiler，内存监测前加gc
    -nowarn           不打印警告信息，默认打印
    -weakwarn         打印弱警告，默认不打印

-----以下gen参数之间由,分割,参数名和参数取值之间由=或:分割
    -gen i18n
        file=../i18n/en.csv  生成文件
    -gen i18nbyid
        dir=../i18n/en       目录
        backup=../backup     备份目录
        checkWrite           测试fastexcel的xlsx文件写入是否正确（用再读取一次，然后比较的方式）,默认为false
    -gen i18nbyidtest
        dir=../i18n/en
        backup=../backup
        checkWrite           ,默认为false
    -gen java
        own=null             只提取含tag的数据
        dir=config           目录
        pkg=config           包名
        encoding=UTF-8       生成代码文件的编码
        sealed               生成sealed interface，需要java17,默认为false
        builders=null        指向txt文件，每行是一个table，对这些table生成对应的builder
        schemanumperfile=100 当配表数量过多时生成的ConfigCodeSchema会超过java编译器限制，用此参数来分文
    -gen javadata
        own=null             只提取含tag的数据
        file=config.data     文件名
    -gen cs
        own=null             只提取含tag的数据
        dir=Config           目录
        pkg=Config           包名
        encoding=GBK         生成文件的编码
        prefix=Data          生成类的前缀
    -gen bytes
        own=null             只提取含tag的数据
        file=config.bytes    文件名
        cipher=              xor加密
        stringpool           ,默认为false
    -gen lua
        own=null             只提取含tag的数据
        dir=.                生成代码所在目录
        pkg=cfg              模块名称
        encoding=UTF-8       编码
        emmylua              是否生成EmmyLua相关的注解,默认为false
        preload              是否一开始就全部加载配置，默认用到的时候再加载,默认为false
        sharedemptytable     是否提取空table {},默认为false
        shared               是否提取非空的公共table,默认为false
        packbool             是否要把同一个结构里的多个bool压缩成一个int,默认为false
        rforoldshared        以前R用于修饰shared table，现在默认行为改为R修饰list，map,默认为false
        nostr                只用来测试字符串占用内存大小,默认为false
    -gen ts
        own=null             只提取含tag的数据
        file=Config.ts
        pkg=Config
        encoding=UTF-8       ts文件编码
    -gen go
        own=null             只提取含tag的数据
        dir=config
        pkg=config
        encoding=GBK
        mod=null
    -gen server
        own=null             只提取含tag的数据
        port=3456            为cfgeditor.exe提供服务的端口
        note=_note.csv       非json记录的标注存储位置
        aicfg=null           配置llm参数和单个table的提示词模板文件等信息，请参考doc
        watch=0              x>0表示数据文件修改x秒后自动重载配置
        postrun=null         xx.bat或xx.sh，用于重载配置后的额外动作， .bat最开始多行的注释可有':: -gen '用当前上下文生成，.sh则是'# -gen '
    -gen tsschema
        own=null             只提取含tag的数据
        tables=              要生成的表名称列表，以;分隔
        dst=.                目标目录
        encoding=UTF-8       生成的ts文件编码
    -gen json
        own=null             只提取含tag的数据
        tables=              表名，;分割
        dst=.                json文件输出目录
    -gen jsonbyai
        cfg=ai.json          同-gen server里的aiCfg
        ask=ask.txt          问题，每行生成一个json
        table=skill.buff     表名称
        promptfn=null        默认为在<cfg>文件目录下的<table>.jte，格式参考https://jte.gg/
        raw                  false表示是把结构信息转为typescript类型信息提供给llm,默认为false
        retry=1              重试llm次数，默认1代表不重试
```


