# Configuration Table Generation System

![intro](docs/assets/intro.png)

An object database browser, editor, and program access code generator

1. Define object structure
2. Use Excel to edit, or use node-based interface to edit and browse all objects
3. Generate access code

## Main Features

* Support for polymorphic and nested structures
* Configure foreign keys, and detect data consistency
* Generate typed data access code, foreign key references, entries, and enums (eliminating magic numbers in programs)
* Support for Java, C#, Lua, Go, TypeScript
* Structure data can be configured in Excel or JSON, providing node-based interface for editing and browsing
* Java generation focuses on hot update safety, Lua generation focuses on memory size

## Documentation

Please read the [detailed documentation](https://stallboy.github.io/cfggen)

## Quick Start

### Configuration System cfggen

Please refer to [Configuration System Documentation](app/README.md).

### Editor cfgeditor.exe

Please refer to [Editor cfgeditor Documentation](cfgeditor/README.md)

### VSCode Extension: cfg-support

We provide a specialized VSCode extension for `.cfg` configuration files with the following features:

- **Syntax Highlighting**: Structure definitions, type identifiers, foreign key references, etc.
- **Go to Definition**: Ctrl+click on type names or foreign key references to jump to definition locations

For detailed features and usage instructions, please refer to [VSCode CFG Extension Documentation](cfgdev/vscode-cfg-extension/README.md).
