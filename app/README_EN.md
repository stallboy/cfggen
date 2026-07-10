# 🗂️ Configuration System (cfggen)

## ✨ Core Features

- 🔗 **Data Consistency Checking** - Automatically detect data reference integrity through foreign key relationships
- 💻 **Multi-language Code Generation** - Generate typed data access code for multiple programming languages
- 🏗️ **Flexible Data Structures** - Support polymorphic structures, nested structures, write complex configuration data in Excel cells
- 🔧 **Editor Service** - Provide RESTful API to support configuration editor (cfgeditor.exe)
- 🤖 **AI Integration** - MCP service to support AI-generated configurations

### 🎯 Supported Languages and Formats

| Language/Format | Description | Main Use Cases |
|-----------------|-------------|----------------|
| **Java** | Type-safe configuration access, supports sealed classes | Backend services, Android applications |
| **C#** | .NET platform configuration access | Unity games, .NET applications |
| **TypeScript** | Typed configuration for frontend and Node.js | Web applications, frontend projects |
| **Go** | Configuration structs for Go language | Go backend services |
| **Lua** | Lua table configuration data | Game scripting, embedded systems |
| **GDScript** | Configuration code for the Godot engine | Godot games |
| **JSON** | Universal configuration data format | Data exchange, API configuration |
| **Bytes** | Binary config file, supports runtime dynamic loading and multiple languages | Release artifacts, hot loading |

## 📋 Environment Requirements

* **JDK 25** - Java development environment
* **Gradle** - Build tool
* **Git** - Version control tool (ensure git/bin path is added to PATH environment variable)

## 🚀 Quick Start

### 📦 Build Project

Execute in the project root directory:

```bash
# Generate executable JAR file
genjar.bat

# Generate Windows executable (contained in cfggen.zip)
mkexe.bat
```

### 🎯 Basic Usage

```bash
# Launch the GUI to visually assemble the command line (prints help when no args given)
java -jar cfggen.jar -gui

# Generate code with the JAR file (multiple -gen flags can be combined)
java -jar cfggen.jar -datadir [config_directory] -gen [language]

# Example: Generate Java code
java -jar cfggen.jar -datadir example -gen java

# Example: Generate TypeScript code + binary data
java -jar cfggen.jar -datadir example -gen ts -gen bytes

# Example: Validate config foreign key reference integrity
java -jar cfggen.jar -datadir example -gen verify
```

> Run `java -jar cfggen.jar -h` for the full parameter list (i18n, encoding, head row, performance profiler, etc.).

## 🧪 Testing Examples

### 📖 View Usage Instructions

```bash
cd example
usage.bat
```

### Multi-language Code Generation Testing

#### ☕ Java Testing
```bash
cd example
genjava.bat    # Generate Java code and data
./gradlew.bat build   # Build project
java -jar build/libs/example.jar
# Enter command line interactive mode, type 'q' to exit, type table name prefix (like "ai") to view related data
```

> **Note**: Java 17+ supports sealed classes, remove sealed keyword if compatibility with older versions is needed

#### 📜 Lua Testing
```bash
cd example
genlua.bat
cd lua
chcp 65001     # Set UTF-8 encoding (Windows)
lua.exe test.lua
```

#### 🔷 C# Testing
```bash
cd example
gencshape.bat
cd cs
run.bat
```

#### 🐹 Go Testing
```bash
cd example
gengo.bat
cd go
go run .
```

#### 🔷 TypeScript Testing
```bash
cd example
gents.bat
cd ts
pnpm i -D tsx  # Install TypeScript runtime environment
npx tsx main.ts
```

## 📚 Additional Resources

* 📖 [Main Project Documentation](../README.md) - Complete project introduction and architecture description
* 📚 [Detailed Documentation](https://stallboy.github.io/cfggen) - Online documentation and API reference
* 🔌 [VSCode Extension](../cfgdev/vscode-cfg-extension/README.md) - Configuration editing and syntax highlighting
* 🎨 [Configuration Editor](../cfgeditor/README.md) - Graphical configuration editing tool

## 💡 Use Cases

- **Game Development** - Game configuration data management and code generation
- **Application Configuration** - Structured management and validation of complex business configurations
- **Multi-language Projects** - Provide type-safe configuration access for different technology stacks
- **Data-driven Development** - Drive business logic and behavior through configurations