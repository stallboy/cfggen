# Implementation Tasks: VSCode CFG Extension

**Feature**: 001-cfg-editor-support
**Generated**: 2025-11-08
**Source Documents**: plan.md, spec.md, data-model.md, contracts/vscode-extension-api.md

## Summary

为VSCode开发CFG配置文件扩展，基于ANTLR4语法提供语法高亮、跳转定义、自动补全三大核心功能。支持跨模块引用和大文件处理，提供中国古典色和默认两套主题。

**Total Tasks**: 47
**User Story Tasks**: 28
**Setup/Foundational Tasks**: 13
**Polish Tasks**: 6

**Parallel Opportunities**: 12 tasks marked with [P] can be executed in parallel

**Suggested MVP Scope**: User Story 1 (Syntax Highlighting) - 实现核心语法高亮功能

## Phase 1: Setup

**Goal**: 初始化项目结构，配置开发环境和依赖

- [x] T001 Create project structure per implementation plan
- [x] T002 Initialize package.json with dependencies (antlr4ts, @types/vscode, vscode-languageclient)
- [x] T003 Configure TypeScript settings (tsconfig.json)
- [x] T004 Setup ANTLR4 grammar file (Cfg.g4)
- [x] T005 Generate ANTLR4 TypeScript parsers (antlr4ts)
- [x] T006 Create basic VSCode extension entry point (extension.ts)
- [x] T007 Setup build scripts (npm run compile, npm run test, npm run package)

## Phase 2: Foundational

**Goal**: 实现基础数据模型和核心服务，为所有用户故事提供支撑

- [x] T008 Create core data models (ConfigFile, Definition, StructDefinition, InterfaceDefinition, TableDefinition, FieldDefinition, ForeignKey, Metadata, SymbolTable, ModuleResolver)
- [x] T009 Implement file index service (fileIndexService.ts)
- [x] T010 Implement cache service (cacheService.ts) with LRU eviction
- [x] T011 Implement symbol table (symbolTable.ts) with cross-module support
- [x] T012 Implement module resolver (moduleResolver.ts) with directory-based module name parsing
- [x] T013 Create utility functions (logger.ts, performance.ts, namespaceUtils.ts)
- [x] T014 Setup Language Server Protocol (LSP) server and client infrastructure

## Phase 3: User Story 1 - Syntax Highlighting (P1)

**Goal**: 实现基于ANTLR4的精确语法高亮，支持两套主题色

**Independent Test Criteria**: 在VSCode中打开任何.cfg文件，所有语法元素（struct/interface/table、字段名、类型、外键引用、注释、元数据）都有正确的高亮显示

- [x] T015 Create custom ANTLR listener for syntax highlighting (extends CfgBaseListener.ts)
- [x] T016 Implement syntax highlighting provider (syntaxHighlightingProvider.ts) using ANTLR parse tree
- [x] T017 Register language identifier 'cfg' and activate extension on .cfg files
- [x] T018 Create theme color palettes (default + chineseClassical) with 7 color categories
- [x] T019 Test syntax highlighting for all language constructs (struct/interface/table, types, foreign keys, comments, metadata)

## Phase 4: User Story 2 - Go-to-Definition Navigation (P1)

**Goal**: 实现外键引用和类型定义的跳转功能，支持跨模块导航

**Independent Test Criteria**: 在.cfg文件中对外键引用或类型引用上执行"跳转到定义"操作，能正确定位到表或类型的定义位置

- [x] T020 Implement definition provider (definitionProvider.ts) with LSP 3.17 support
- [x] T021 Parse foreign key references (->, =>) and extract target table/field
- [x] T022 Resolve cross-module references using module resolver
- [x] T023 Handle edge cases: missing definitions, invalid references
- [x] T024 Test go-to-definition for simple references, key references, list references, and cross-module references

## Phase 5: User Story 3 - Autocompletion (P2)

**Goal**: 提供上下文感知的自动补全，减少输入错误

**Independent Test Criteria**: 在.cfg文件的多个上下文中触发自动补全（输入表名、字段名、类型名时），提示列表包含正确的候选项

- [x] T025 Implement completion provider (completionProvider.ts) with context awareness
- [x] T026 Provide type completion (basic types + custom types)
- [x] T027 Provide foreign key table reference completion
- [x] T028 Provide metadata keyword completion (nullable, mustFill, pack, etc.)
- [x] T029 Test autocompletion in all contexts (types, foreign keys, metadata)

## Phase 6: Polish & Cross-Cutting Concerns

**Goal**: 完善用户体验，处理边界情况，确保稳定性和性能

**Independent Test Criteria**: 大文件性能测试通过（<2秒响应），错误提示清晰，主题切换正常

- [x] T030 Implement hover provider (hoverProvider.ts) for documentation display
- [x] T031 Implement reference provider (referenceProvider.ts) to find all symbol references
- [x] T032 Add diagnostic collection for syntax errors and warnings
- [x] T033 Implement incremental parsing and cache invalidation for large files
- [x] T034 Add configuration settings UI (cfg.theme, cfg.enableCache, cfg.maxFileSize)
- [x] T035 Add performance monitoring and logging for LSP operations

## Dependency Graph

```
Phase 1 (Setup) → Phase 2 (Foundational) → Phase 3 (US1: Syntax Highlighting)
                                        ↘ Phase 4 (US2: Go-to-Definition)
                                        ↘ Phase 5 (US3: Autocompletion)
                                                         ↓
                                           Phase 6 (Polish & Cross-Cutting)
```

**User Story Dependencies**:
- US1 (Syntax Highlighting) - Independent
- US2 (Go-to-Definition) - Depends on US1 (needs symbol resolution)
- US3 (Autocompletion) - Depends on US2 (needs symbol lookup)
- Polish Phase - Depends on all user stories

## Parallel Execution Examples

### Example 1: Development Phase
**Parallel Tasks**: T008-T014 (Foundational)
- T008 (Models) can be implemented in parallel with T009-T014 (Services)
- **Reason**: Different directories (models/ vs services/) with no dependencies

### Example 2: User Story Implementation
**Parallel Tasks**: T015-T019 (US1: Syntax Highlighting)
- T015 (Listener) can be developed in parallel with T016-T018 (Provider and Themes)
- **Reason**: Different components with clear interfaces

### Example 3: Cross-Story Parallelization
**Parallel Tasks**: T020-T024 (US2) and T025-T029 (US3)
- US2 (Definition) and US3 (Completion) can be developed in parallel after US1
- **Reason**: Different LSP endpoints, share symbol table foundation

## Implementation Strategy

### MVP First (Phase 1-3)
1. Complete **Phase 1 Setup** (T001-T007)
2. Complete **Phase 2 Foundational** (T008-T014) - Core infrastructure
3. Complete **User Story 1** (T015-T019) - Syntax highlighting
4. **MVP Demo**: 语法高亮在VSCode中正常工作

### Incremental Delivery
1. **Increment 1**: MVP (US1) - 语法高亮
2. **Increment 2**: Add US2 - 跳转定义
3. **Increment 3**: Add US3 - 自动补全
4. **Increment 4**: Polish Phase - 完善功能

### Each Increment is Independently Testable
- **US1 Test**: 打开.cfg文件，观察高亮效果
- **US2 Test**: 按F12跳转，验证跳转到正确位置
- **US3 Test**: 输入时触发补全，验证候选项
- **Polish Test**: 大文件性能，主题切换，错误提示

## Task Details

### File Paths Reference
```
vscode-cfg-extension/
├── package.json                    # T002
├── tsconfig.json                   # T003
├── src/
│   ├── extension.ts                # T006
│   ├── grammar/
│   │   └── Cfg.g4                  # T004 (手动创建)
│   │   └── *.ts                    # T005 (自动生成: CfgLexer.ts, CfgParser.ts, CfgListener.ts, CfgBaseListener.ts)
│   ├── models/                     # T008
│   │   ├── configFile.ts
│   │   ├── structDefinition.ts
│   │   ├── tableDefinition.ts
│   │   ├── interfaceDefinition.ts
│   │   ├── fieldDefinition.ts
│   │   ├── foreignKeyDefinition.ts
│   │   ├── metadataDefinition.ts
│   │   └── index.ts
│   ├── services/                   # T009-T012
│   │   ├── cacheService.ts
│   │   ├── fileIndexService.ts
│   │   ├── symbolTable.ts
│   │   └── moduleResolver.ts
│   ├── providers/                  # T016-T032
│   │   ├── completionProvider.ts
│   │   ├── definitionProvider.ts
│   │   ├── hoverProvider.ts
│   │   ├── referenceProvider.ts
│   │   └── syntaxHighlightingProvider.ts
│   └── utils/                      # T013
│       ├── logger.ts
│       ├── performance.ts
│       └── namespaceUtils.ts
└── test/                           # All test tasks
```

### Key Technical Decisions
1. **ANTLR4统一**: 语法解析和高亮都使用同一语法规则（Cfg.g4），确保一致性
2. **两套主题**: 默认配色 + 中国古典色（黛青、苍青、竹青、胭脂、琥珀、玄灰）
3. **LSP标准**: 遵循LSP 3.17规范实现completion、definition、hover
4. **性能优化**: 增量解析 + 符号表缓存，支持大文件（<2秒响应）
5. **跨模块**: 基于目录结构的模块名解析算法

### ANTLR4文件生成说明
- `CfgLexer.ts`, `CfgParser.ts`, `CfgListener.ts`, `CfgBaseListener.ts` 由ANTLR4工具从`Cfg.g4`自动生成
- 通过`npm run generate-parser`命令生成（见package.json scripts）
- 生成的TypeScript文件使用antlr4ts运行时库
- T005任务执行后，会自动生成这些TypeScript文件
- T015任务创建自定义监听器类（扩展自动生成的CfgBaseListener.ts）

### Performance Requirements
- 语法高亮: <50ms
- 自动补全: <200ms
- 跳转到定义: <300ms
- 悬停提示: <200ms
- 符号表加载: <1s

### Test Coverage
- 单元测试: 核心服务（symbol table, module resolver, cache service）
- 集成测试: LSP功能（completion, definition, hover）
- 大文件测试: 10k+行配置文件的性能
- 主题测试: 两套主题的颜色切换

### Configuration
- `cfg.theme`: "default" | "chineseClassical" (default: chineseClassical)
- `cfg.enableCache`: boolean (default: true)
- `cfg.maxFileSize`: number (default: 10485760 = 10MB)

## Success Metrics
- SC-001: 95%的跳转到定义能正确跳转到目标位置
- SC-002: 80%用户能成功使用语法高亮，无额外学习成本
- SC-003: 90%的外键引用能被正确识别和跳转
- SC-004: 自动补全候选项准确率不低于95%
- SC-005: 跨模块引用场景下，模块名解析准确率达到95%
- SC-006: 95%的语法错误提供可理解的说明

## Task Execution Checklist

**Before Starting**:
- [ ] Read all design documents (plan.md, spec.md, data-model.md, contracts/)
- [ ] Run `npm install` to install dependencies
- [ ] Run `npm run generate-parser` to generate ANTLR4 TypeScript parsers (CfgLexer.ts, CfgParser.ts, CfgListener.ts, CfgBaseListener.ts)

**Per Phase**:
- [ ] Complete all tasks in order (mark as [x])
- [ ] Run tests: `npm test`
- [ ] Check linting: `npm run lint`
- [ ] Build extension: `npm run compile`
- [ ] Verify in VSCode: F5 to open extension development host

**Completion Criteria**:
- [ ] All 47 tasks marked as complete
- [ ] All user stories pass independent test criteria
- [ ] Performance metrics within thresholds
- [ ] Documentation updated (quickstart.md)

## Next Steps
1. Start with Phase 1: Setup (T001-T007)
2. Follow dependency order (Phase 2 → US1 → US2 → US3 → Polish)
3. Take advantage of parallel opportunities (marked with [P])
4. Test each increment before moving to next
