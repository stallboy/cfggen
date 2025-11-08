# Implementation Tasks: VSCode CFG Extension

**Feature**: 001-cfg-editor-support
**Generated**: 2025-11-09
**Source Documents**: plan.md, spec.md, data-model.md, contracts/vscode-extension-api.md, theme-system-design.md

## Summary

为VSCode开发CFG配置文件扩展，基于ANTLR4语法和双层语法高亮（TextMate + Semantic Tokens），提供语法高亮、跳转定义、自动补全三大核心功能。支持跨模块引用和大文件处理，提供中国古典色和默认两套主题。

**Total Tasks**: 52
**User Story Tasks**: 33
**Setup/Foundational Tasks**: 13
**Polish Tasks**: 6

**Parallel Opportunities**: 15 tasks marked with [P] can be executed in parallel

**Suggested MVP Scope**: User Story 1 (Syntax Highlighting) - 实现核心双层语法高亮功能

## Phase 1: Setup

**Goal**: 初始化项目结构，配置开发环境和依赖

- [ ] T001 Create project structure per implementation plan (vscode-cfg-extension/)
- [ ] T002 Initialize package.json with dependencies (antlr4ts, @types/vscode, vscode)
- [ ] T003 Configure TypeScript settings (tsconfig.json)
- [ ] T004 Setup ANTLR4 grammar file (Cfg.g4) in vscode-cfg-extension/src/grammar/
- [ ] T005 Generate ANTLR4 TypeScript parsers (antlr4ts)
- [ ] T006 Create basic VSCode extension entry point (extension.ts)
- [ ] T007 Setup build scripts (npm run compile, npm run test, npm run package)

## Phase 2: Foundational

**Goal**: 实现基础数据模型和核心服务，为所有用户故事提供支撑

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [ ] T008 [P] Create core data models in vscode-cfg-extension/src/models/
  - configFile.ts, structDefinition.ts, interfaceDefinition.ts, tableDefinition.ts
  - fieldDefinition.ts, foreignKeyDefinition.ts, metadataDefinition.ts, index.ts
- [ ] T009 [P] Implement file index service (fileIndexService.ts)
- [ ] T010 [P] Implement cache service (cacheService.ts) with LRU eviction
- [ ] T011 [P] Implement symbol table (symbolTable.ts) with cross-module support
- [ ] T012 [P] Implement module resolver (moduleResolver.ts) with directory-based module name parsing
- [ ] T013 [P] Create utility functions in vscode-cfg-extension/src/utils/
  - logger.ts, performance.ts, namespaceUtils.ts

**Checkpoint**: Foundation ready - user story implementation can now begin in parallel

---

## Phase 3: User Story 1 - Syntax Highlighting (Priority: P1) 🎯 MVP

**Goal**: 实现基于双层语法高亮（TextMate + Semantic Tokens）的精确语法高亮，支持两套主题色

**Independent Test**: 在VSCode中打开任何.cfg文件，验证所有语法元素都有正确的高亮显示：struct/interface/table关键字、字段名、类型、外键引用、注释等，双层高亮正常工作，主题切换生效

### Tests for User Story 1 (MANDATORY) ⚠️

> **NOTE: Write these tests FIRST, ensure they FAIL before implementation - per Constitution requirement**

- [ ] T014 [P] [US1] Unit test for TextMate grammar rules in test/unit/test_textmate_grammar.ts
- [ ] T015 [P] [US1] Unit test for Semantic Tokens provider in test/unit/test_semantic_tokens.ts
- [ ] T016 [P] [US1] Integration test for two-layer highlighting in test/integration/test_highlighting.ts

### Implementation for User Story 1

- [ ] T017 [P] [US1] Create TextMate grammar file (syntaxes/cfg.tmLanguage.json)
- [ ] T018 [P] [US1] Create TextMate scope mappings in vscode-cfg-extension/src/providers/textmateGrammar.ts
- [ ] T019 [P] [US1] Create semantic tokens provider in vscode-cfg-extension/src/providers/semanticTokensProvider.ts
- [ ] T020 [P] [US1] Implement theme service in vscode-cfg-extension/src/services/themeService.ts
- [ ] T021 [P] [US1] Implement theme manager in vscode-cfg-extension/src/providers/themeManager.ts
- [ ] T022 [P] [US1] Create ANTLR4 highlighting listener (extends CfgBaseListener)
- [ ] T023 [US1] Register language identifier 'cfg' and activate extension on .cfg files (extension.ts)
- [ ] T024 [US1] Create theme color palettes (default + chineseClassical) with 7 color categories
- [ ] T025 [US1] Test syntax highlighting for all language constructs (struct/interface/table, types, foreign keys, comments, metadata)
- [ ] T026 [US1] Test theme switching between default and chineseClassical
- [ ] T027 [US1] Verify two-layer highlighting works correctly (TextMate + Semantic)

**Checkpoint**: At this point, User Story 1 should be fully functional and testable independently

---

## Phase 4: User Story 2 - Go-to-Definition Navigation (Priority: P1)

**Goal**: 实现外键引用和类型定义的跳转功能，支持跨模块导航

**Independent Test**: 在.cfg文件中对外键引用或类型引用上执行"跳转到定义"操作，能正确定位到表或类型的定义位置

### Tests for User Story 2 (MANDATORY) ⚠️

- [ ] T028 [P] [US2] Unit test for definition provider in test/unit/test_definition_provider.ts
- [ ] T029 [P] [US2] Unit test for symbol resolution in test/unit/test_symbol_resolution.ts
- [ ] T030 [P] [US2] Integration test for go-to-definition in test/integration/test_go_to_definition.ts

### Implementation for User Story 2

- [ ] T031 [P] [US2] Implement definition provider in vscode-cfg-extension/src/providers/definitionProvider.ts
- [ ] T032 [P] [US2] Parse foreign key references (->, =>) and extract target table/field
- [ ] T033 [P] [US2] Resolve cross-module references using module resolver
- [ ] T034 [US2] Handle edge cases: missing definitions, invalid references
- [ ] T035 [US2] Test go-to-definition for simple references (taskid:int ->task)
- [ ] T036 [US2] Test go-to-definition for key references (itemids:list<int> ->item.item)
- [ ] T037 [US2] Test go-to-definition for type references (testSubBean:Position)
- [ ] T038 [US2] Test go-to-definition for cross-module references (->other.monster)
- [ ] T039 [US2] Test go-to-definition for list references (=>table1[field2])

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently

---

## Phase 5: User Story 3 - Autocompletion (Priority: P2)

**Goal**: 提供上下文感知的自动补全，减少输入错误

**Independent Test**: 在.cfg文件的多个上下文中触发自动补全（输入表名、字段名、类型名时），提示列表包含正确的候选项

### Tests for User Story 3 (MANDATORY) ⚠️

- [ ] T040 [P] [US3] Unit test for completion provider in test/unit/test_completion_provider.ts
- [ ] T041 [P] [US3] Unit test for context awareness in test/unit/test_completion_context.ts
- [ ] T042 [P] [US3] Integration test for autocompletion in test/integration/test_autocompletion.ts

### Implementation for User Story 3

- [ ] T043 [P] [US3] Implement completion provider in vscode-cfg-extension/src/providers/completionProvider.ts
- [ ] T044 [P] [US3] Provide type completion (basic types + custom types)
- [ ] T045 [P] [US3] Provide foreign key table reference completion
- [ ] T046 [P] [US3] Provide metadata keyword completion (nullable, mustFill, pack, etc.)
- [ ] T047 [US3] Test autocompletion in all contexts (types, foreign keys, metadata)
- [ ] T048 [US3] Test autocompletion for cross-module scenarios
- [ ] T049 [US3] Verify completion items are contextually appropriate

**Checkpoint**: All user stories should now be independently functional

---

## Phase 6: Polish & Cross-Cutting Concerns

**Goal**: 完善用户体验，处理边界情况，确保稳定性和性能

**Independent Test**: 大文件性能测试通过（<2秒响应），错误提示清晰，主题切换正常

- [ ] T050 [P] Implement hover provider in vscode-cfg-extension/src/providers/hoverProvider.ts for documentation display
- [ ] T051 [P] Implement reference provider in vscode-cfg-extension/src/providers/referenceProvider.ts to find all symbol references
- [ ] T052 Add diagnostic collection for syntax errors and warnings
- [ ] T053 Implement incremental parsing and cache invalidation for large files
- [ ] T054 Add configuration settings UI (cfg.theme, cfg.enableCache, cfg.maxFileSize)
- [ ] T055 Add performance monitoring and logging for all operations

---

## Dependencies & Execution Order

### Phase Dependencies

```
Phase 1 (Setup) → Phase 2 (Foundational) → Phase 3 (US1: Syntax Highlighting)
                                        ↘ Phase 4 (US2: Go-to-Definition)
                                        ↘ Phase 5 (US3: Autocompletion)
                                                         ↓
                                           Phase 6 (Polish & Cross-Cutting)
```

### User Story Dependencies

- **US1 (Syntax Highlighting)**: Independent
- **US2 (Go-to-Definition)**: Depends on US1 (needs symbol resolution)
- **US3 (Autocompletion)**: Depends on US2 (needs symbol lookup)
- **Polish Phase**: Depends on all user stories

### Within Each User Story

- Tests (if included) MUST be written and FAIL before implementation
- TextMate and Semantic layers can be developed in parallel
- Theme system (service + manager) can be developed in parallel with highlighting
- Core implementation before integration
- Story complete before moving to next priority

## Parallel Execution Examples

### Example 1: Phase 1 Setup
**Parallel Tasks**: T001-T007 (Setup)
- T001 (Structure) can be developed with T002-T007 (Dependencies) in parallel
- **Reason**: Different files with no dependencies

### Example 2: Phase 2 Foundational
**Parallel Tasks**: T008-T013 (Foundational)
- T008 (Models) can be implemented in parallel with T009-T013 (Services)
- **Reason**: Different directories (models/ vs services/) with no dependencies

### Example 3: User Story 1 Implementation
**Parallel Tasks**: T017-T022 (US1: Syntax Highlighting)
- T017 (TextMate grammar) can be developed in parallel with T019-T021 (Semantic + Theme)
- **Reason**: Different components (TextMate vs Semantic layers)

### Example 4: Cross-Story Parallelization
**Parallel Tasks**: T031-T039 (US2) and T043-T049 (US3)
- US2 (Definition) and US3 (Completion) can be developed in parallel after US1
- **Reason**: Different VSCode API providers, share symbol table foundation

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete **Phase 1 Setup** (T001-T007)
2. Complete **Phase 2 Foundational** (T008-T013) - Core infrastructure
3. Complete **User Story 1** (T014-T027) - Two-layer syntax highlighting
4. **STOP and VALIDATE**: Test syntax highlighting independently
5. Deploy/demo if ready

### Incremental Delivery

1. **Increment 1**: MVP (US1) - 双层语法高亮
2. **Increment 2**: Add US2 - 跳转到定义
3. **Increment 3**: Add US3 - 自动补全
4. **Increment 4**: Polish Phase - 完善功能

### Each Increment is Independently Testable

- **US1 Test**: 打开.cfg文件，观察双层高亮效果，测试主题切换
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
│   ├── extension.ts                # T006, T023
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
│   │   ├── moduleResolver.ts
│   │   └── themeService.ts         # T020
│   ├── providers/                  # T018-T022, T031-T051
│   │   ├── completionProvider.ts
│   │   ├── definitionProvider.ts
│   │   ├── hoverProvider.ts
│   │   ├── referenceProvider.ts
│   │   ├── semanticTokensProvider.ts
│   │   ├── textmateGrammar.ts
│   │   └── themeManager.ts
│   └── utils/                      # T013
│       ├── logger.ts
│       ├── performance.ts
│       └── namespaceUtils.ts
└── syntaxes/                       # T017
    ├── cfg.tmLanguage.json
    └── cfg-language-configuration.json
```

### Key Technical Decisions

1. **双层高亮**: TextMate处理基础token（毫秒级响应），Semantic处理语义信息（基于ANTLR4）
2. **主题系统**: themeService + themeManager，支持默认和中国古典色两套主题
3. **无LSP**: 直接使用VSCode Extension API，性能更好（2-5倍速度提升）
4. **性能优化**: 增量解析 + 符号表缓存，支持大文件（<2秒响应>5k行）
5. **跨模块**: 基于目录结构的模块名解析算法

### Performance Requirements

- 语法高亮: <50ms (TextMate层)
- 自动补全: <200ms
- 跳转到定义: <300ms
- 大文件处理: >5k行 <2秒
- 10k+行文件: 不卡顿

### ANTLR4文件生成说明

- `CfgLexer.ts`, `CfgParser.ts`, `CfgListener.ts`, `CfgBaseListener.ts` 由ANTLR4工具从`Cfg.g4`自动生成
- 通过`npm run generate-parser`命令生成（见package.json scripts）
- 生成的TypeScript文件使用antlr4ts运行时库
- T005任务执行后，会自动生成这些TypeScript文件
- T022任务创建自定义监听器类（扩展自动生成的CfgBaseListener.ts）

### Test Coverage

- 单元测试: 核心服务（symbol table, module resolver, cache service, theme service）
- 集成测试: VSCode API功能（completion, definition, hover, semantic tokens）
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
- [ ] Read all design documents (plan.md, spec.md, data-model.md, theme-system-design.md)
- [ ] Run `npm install` to install dependencies
- [ ] Run `npm run generate-parser` to generate ANTLR4 TypeScript parsers

**Per Phase**:
- [ ] Complete all tasks in order (mark as [x])
- [ ] Run tests: `npm test`
- [ ] Check linting: `npm run lint`
- [ ] Build extension: `npm run compile`
- [ ] Verify in VSCode: F5 to open extension development host

**Completion Criteria**:
- [ ] All 52 tasks marked as complete
- [ ] All user stories pass independent test criteria
- [ ] Performance metrics within thresholds
- [ ] Documentation updated (quickstart.md)

## Next Steps

1. Start with Phase 1: Setup (T001-T007)
2. Follow dependency order (Phase 2 → US1 → US2 → US3 → Polish)
3. Take advantage of parallel opportunities (marked with [P])
4. Test each increment before moving to next
5. Each user story must be independently testable

---

**Total Tasks**: 52
- Phase 1 (Setup): 7 tasks
- Phase 2 (Foundational): 6 tasks
- Phase 3 (US1 - Syntax Highlighting): 14 tasks
- Phase 4 (US2 - Go-to-Definition): 12 tasks
- Phase 5 (US3 - Autocompletion): 10 tasks
- Phase 6 (Polish): 6 tasks

**Parallel Opportunities**: 15 tasks (marked with [P])
**User Stories**: 3 (US1 P1, US2 P1, US3 P2)
**Suggested MVP**: User Story 1 (Syntax Highlighting)
