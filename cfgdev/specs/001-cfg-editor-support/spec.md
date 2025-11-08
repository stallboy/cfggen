# Feature Specification: VSCode CFG Extension

**Feature Branch**: `001-cfg-editor-support`
**Created**: 2025-11-08
**Status**: Draft
**Input**: Develop a VSCode extension for .cfg file syntax highlighting, code navigation, and autocompletion using ANTLR4 grammar

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Syntax Highlighting (Priority: P1)

游戏策划人员和开发者使用VSCode打开.cfg配置文件时，需要直观的语法高亮显示来快速识别文件结构和关键元素。通过颜色区分不同类型的token，提升代码可读性，减少阅读和理解配置文件的时间。

**Why this priority**: 这是扩展的核心功能，为用户提供即时的视觉反馈，让复杂的配置文件变得易于理解和导航。没有语法高亮，配置文件难以阅读，严重影响开发效率。

**Independent Test**: 可以通过在VSCode中打开任何.cfg文件，验证所有语法元素都有正确的高亮显示：struct/interface/table关键字、字段名、类型、外键引用、注释等。

**Acceptance Scenarios**:

1. **Given** VSCode中打开.cfg文件，**When** 文件包含struct/interface/table定义，**Then** 其名称以高亮显示
2. **Given** 字段类型为非基本类型（如自定义struct），**When** 用户查看该字段，**Then** 类型名称高亮显示
3. **Given** table定义包含主键或唯一键，**When** 用户查看键声明，**Then** 键标识符（如PK、UK）高亮显示
4. **Given** 字段包含外键引用（->, =>），**When** 用户查看引用部分，**Then** 整个引用链路高亮显示
5. **Given** 文件包含单行注释（//），**When** 用户查看注释，**Then** //和注释以绿色（浅绿，非纯绿）显示
6. **Given** 字段包含元数据（如nullable, mustFill, enumRef等），**When** 用户查看元数据，**Then** 元数据关键字高亮显示

---

### User Story 2 - Go-to-Definition Navigation (Priority: P1)

当用户在.cfg文件中查看外键引用或类型引用时，需要能够快速跳转到定义位置。开发者编辑配置时，经常需要追踪一个值从哪里来，或者检查外键引用的表结构，通过"跳转到定义"功能可以快速导航到相关代码。

**Why this priority**: 代码导航是提高开发效率的关键功能。配置文件中充满各种引用关系，手动搜索定义位置非常繁琐。跳转功能让开发者可以快速理解数据结构和依赖关系。

**Independent Test**: 可以通过在.cfg文件中对外键引用（如`->tableName`）或类型引用上执行"跳转到定义"操作，验证是否能正确定位到表或类型的定义位置。

**Acceptance Scenarios**:

1. **Given** 光标在外键引用上（如`taskid:int ->task`），**When** 用户执行"跳转到定义"命令，**Then** 跳转到task表的定义位置
2. **Given** 光标在外键引用带键的字段上（如`itemids:list<int> ->item.item`），**When** 用户执行"跳转到定义"，**Then** 跳转到item表的item字段定义
3. **Given** 光标在类型引用上（如`testSubBean:Position`），**When** 用户执行"跳转到定义"，**Then** 跳转到Position类型的定义
4. **Given** 光标在跨模块引用上（如`->other.monster`），**When** 用户执行"跳转到定义"，**Then** 跳转到other模块中monster表的定义
5. **Given** 引用的表或类型不存在，**When** 用户执行"跳转到定义"，**Then** 显示"未找到定义"提示
6. **Given** 在复杂引用链中（如`=>table1[field2]`），**When** 用户执行"跳转到定义"，**Then** 正确解析并跳转到table1的field2字段定义

---

### User Story 3 - Autocompletion (Priority: P2)

用户在.cfg文件中编辑时，需要智能的自动提示来减少输入错误和提效。编写配置时，用户经常需要输入表名、字段名、类型名等，通过上下文感知的自动提示，可以快速补全这些标识符，避免拼写错误。

**Why this priority**: 自动补全功能提升编写配置的准确性和速度。在大型配置文件中，手动输入完整的表名或字段名既慢又容易出错。智能提示可以显示可用的选项，让用户快速选择正确的标识符。

**Independent Test**: 可以在.cfg文件的多个上下文中触发自动补全（如输入表名、字段名、类型名时），验证提示列表是否包含正确的候选项。

**Acceptance Scenarios**:

1. **Given** 在字段类型位置输入，**When** 触发自动补全，**Then** 显示可用的类型列表（基本类型和自定义类型）
2. **Given** 在外键引用位置输入，**When** 触发自动补全，**Then** 显示可引用的表名列表
3. **Given** 在元数据位置输入，**When** 触发自动补全，**Then** 显示支持的元数据关键字（如nullable, mustFill, enumRef等）
4. **Given** 跨模块场景中，**When** 触发自动补全，**Then** 正确识别模块名并显示对应模块的定义列表
5. **Given** 上下文无关的位置，**When** 触发自动补全，**Then** 不显示任何候选项或显示适当提示

---

### Edge Cases

- **大文件处理**: 当.cfg文件包含大量表和结构定义时，所有功能应保持流畅，不出现性能问题
- **跨模块引用**: 当引用位于不同模块/目录的表或类型时，正确解析模块名和文件路径
- **不完整文件**: 当文件存在语法错误或不完整的定义时，提供有意义的错误提示而非崩溃
- **重复定义**: 当存在重复的表名或类型名时，明确冲突并提供警告
- **非标准目录结构**: 当.cfg文件不遵循标准目录命名规则时，给出明确的错误提示

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: 系统必须基于ANTLR4定义的Cfg.g4语法进行解析，确保所有功能严格遵循语法规则
- **FR-002**: 系统必须对所有.cfg文件元素提供语法高亮：struct/interface/table关键字及名称、非基本类型、主键/唯一键标识符、外键引用、注释、元数据关键字
- **FR-003**: 系统必须支持外键引用的"跳转到定义"功能，包括简单引用（如->tt）、带键引用（如->tt[kk]）、列表引用（如=>tt[kk]）
- **FR-004**: 系统必须支持类型定义的"跳转到定义"功能，包括本模块类型和跨模块完整类型名称
- **FR-005**: 系统必须提供上下文感知的自动补全，涵盖表名、类型名、字段名和元数据关键字
- **FR-006**: 系统必须处理跨模块引用，通过目录名解析模块名（截取第一个"."之前，再截取"_汉字"或汉字之前的部分）
- **FR-007**: 系统必须在引用目标不存在时显示明确的"未找到定义"提示
- **FR-008**: 系统必须支持大文件处理，采用增量解析和缓存策略确保性能
- **FR-009**: 系统必须对所有语法错误和不完整结构提供清晰的错误提示

### Key Entities

- **CFG文件**: 包含结构定义（struct、interface、table）的配置文件
- **语法标记**: 从Cfg.g4语法中识别的token类型（关键字、标识符、运算符、注释等）
- **定义位置**: 表、类型、字段的源码位置信息，用于跳转和补全
- **模块上下文**: 包含目录名、模块名、文件路径的解析上下文
- **外键引用**: 字段间的引用关系，包括引用链和键信息
- **符号表**: 存储所有已解析符号（表、类型、字段）的映射表，用于快速查找

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: "跳转到定义"功能在95%的情况下能正确跳转到目标定义位置
- **SC-002**: 80%的用户在首次使用时能够成功使用语法高亮功能，无需额外学习成本
- **SC-003**: 90%的外键引用能被正确识别和跳转，剩余10%应显示明确的错误提示而非静默失败
- **SC-004**: 自动补全功能在用户输入时显示正确的候选项列表，候选项准确率不低于95%
- **SC-005**: 跨模块引用场景下，模块名解析准确率达到95%
- **SC-006**: 错误提示信息必须对95%的语法错误提供可理解的说明，帮助用户快速定位和修复问题
