# cfggen

* Make structural data in Excel file.
* View & make structural data using node based editor
* Generate code for accessing structural data in programs.

## Features

* Allow designer to check data consistency at any time through configuration foreign keys.

* Generate code to facilitate program access to typed data, create foreign key references, generate entries and enums (eliminating magic numbers in the program), and support Java, C#, and Lua.

* Support polymorphic structures and nested structures. Complex structured data can be written in a single cell, providing the flexibility of XML to CSV.

* Focuse security in generating Java code.

* Optimize memory size in generating Lua code.


## build & test

### Prerequisites

* jdk21
* gradle

### build, generate cfggen.jar.

```bash
genjar.bat
```

### test

first `cd example` 

* show usage 

```bash
usage.bat  
```

* generate java code

```bash
cd example
genjavasealed.bat 
```

* test java code

```bash
gradle build 
java -jar build/libs/example.jar 
```

* generate lua code, test lua

```bash
genlua.bat 
cd lua
chcp 65001
lua.exe test.lua
```

* generate c# code, test c#

```bash
gencshape.bat 
cd cs
dotnet run
```

