# 🗂️ Configuration System

## ✨ Main Features

- 🔗 Detect data consistency through foreign key configuration
- 💻 Generate typed data access code, foreign key references, entries, and enums
- 🏗️ Support polymorphic structures and nested structures, allowing writing arbitrarily complex structured data in a single cell, giving Excel the flexibility of XML
- ⚡ Java generation focuses on hot-reload safety, Lua generation focuses on memory size

## 📋 Prerequisites

* JDK 21
* Gradle
* Set git/bin path to PATH environment variable

## 🔨 Build & Test

In the root directory

### 📦 Generate cfggen.jar, cfggen.exe

```bash
genjar.bat  # Generate cfggen.jar
```

```bash
mkexe.bat  # Generate cfggen.zip containing exe
```

### 🧪 Testing

#### 📖 View Usage Instructions

```bash
cd example
usage.bat  # Print usage instructions
```

#### ☕ Test Java: Generate Java Code and Data

```bash
cd example
genjava.bat # sealed requires Java 17 or above, can also remove sealed
```

#### ✅ Test Java: Verify Java Generation

```bash
gradle build
java -jar build/libs/example.jar
# Enter command line, type 'q' to quit, type other inputs like "ai" to print table names starting with ai structure definitions and data
```

#### 📜 Test Lua

```bash
genlua.bat
cd lua
chcp 65001
lua.exe test.lua
```

#### 🔷 Test C#

```bash
gencshape.bat
cd cs
run.bat
```

#### 🐹 Test Go

```bash
gengo.bat
cd go
go run .
```

#### 🔷 Test TypeScript

```bash
gents.bat
cd ts
pnpm i -D tsx
npx tsx main.ts
```

## 🔗 Related Links

* 📖 [Main Project README](../README.md)
* 📚 [Detailed Documentation](https://stallboy.github.io/cfggen)
* 🔌 [VSCode CFG Extension](../cfgdev/vscode-cfg-extension/README.md)
* 🎨 [Editor cfgeditor Documentation](../cfgeditor/README.md)