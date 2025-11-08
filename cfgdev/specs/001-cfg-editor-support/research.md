# Research Report: VSCode CFG Extension

**Date**: 2025-11-08
**Feature**: VSCode CFG Extension with ANTLR4-based syntax support

## Research Tasks

### 1. VSCode Extension API and LSP Integration

**Decision**: 使用VSCode Extension API + Language Server Protocol实现
**Rationale**:
- VSCode Extension API提供成熟的语言支持框架
- LSP协议确保标准化实现（completion, definition, hover）
- 生态成熟，社区支持良好

**Key Findings**:
- VSCode Extension API 1.85+ 支持最新LSP 3.17版本
- 需使用@types/vscode提供TypeScript类型定义
- LanguageClient模式推荐用于复杂语言特性

**Alternatives Considered**:
- 直接Extension API实现：复杂但性能更好
- 纯TextMate语法：仅能高亮，无法实现跳转和补全

### 2. ANTLR4 TypeScript Runtime Integration

**Decision**: 使用antlr4ts库作为TypeScript运行时
**Rationale**:
- 官方支持的TypeScript运行时库
- 完整的TypeScript泛型支持
- 良好的错误处理机制

**Key Findings**:
- antlr4ts 0.5.0+版本性能优化，适合大文件
- 需要预先生成TypeScript语法分析器代码
- 支持监听器模式遍历语法树

**Alternatives Considered**:
- 使用antlr4原版 + 自定义包装：工作量更大
- 手写解析器：无法保证语法准确性和维护性

### 3. Theme Color System Design

**Decision**: 实现两套主题色系统（默认 + 中国古典色）
**Rationale**:
- 默认主题：VSCode标准配色，确保兼容性
- 中国古典色：符合本地化需求，提升用户体验

**Color Palette**:
```json
{
  "default": {
    "keyword": "#0000FF",      // 蓝色关键字
    "type": "#267F99",         // 青色类型
    "comment": "#008000",      // 绿色注释
    "string": "#A31515",       // 红色字符串
    "number": "#098658",       // 绿色数字
    "operator": "#000000"      // 黑色操作符
  },
  "chineseClassical": {
    "keyword": "#1E3A8A",      // 黛青（深蓝）
    "type": "#0F766E",         // 苍青（青绿）
    "comment": "#166534",      // 竹青（深绿）
    "string": "#991B1B",       // 胭脂（深红）
    "number": "#854D0E",       // 琥珀（深黄）
    "operator": "#3F3F46"      // 玄灰（深灰）
  }
}
```

**Alternatives Considered**:
- 动态主题：根据时间/季节自动切换：不实用
- 用户自定义颜色：增加复杂度，默认两套足够

### 4. Performance Optimization for Large Files

**Decision**: 增量解析 + 符号表缓存 + 虚拟文件系统
**Rationale**:
- 大型配置可能有数千行，实时解析性能关键
- 增量解析避免重复工作
- 缓存提升响应速度

**Key Strategies**:
1. **增量解析**: 仅解析变化的文件和依赖
2. **符号表缓存**: 内存中维护已解析符号，支持快速查找
3. **后台解析**: 使用VSCode worker thread避免阻塞UI
4. **分块处理**: 大文件分块解析，平衡内存和速度

**Alternatives Considered**:
- 全量缓存：内存占用过高，不适合大项目
- 纯实时解析：性能差，用户体验差

### 5. Cross-Module Reference Resolution

**Decision**: 基于目录结构的模块名解析算法
**Rationale**:
- 游戏配置通常按模块组织（task, item, npc等）
- 跨模块引用需要正确解析模块路径

**Algorithm**:
```typescript
function parseModuleName(filePath: string): string {
  // 1. 获取目录名
  const dirName = path.basename(path.dirname(filePath));

  // 2. 截取第一个"."之前的部分
  const firstDot = dirName.indexOf('.');
  if (firstDot > 0) {
    return dirName.substring(0, firstDot);
  }

  // 3. 截取"_汉字"或纯汉字之前的部分
  const chineseMatch = dirName.match(/(.+?)(_[\u4e00-\u9fa5]+|[\u4e00-\u9fa5]+|$)/);
  return chineseMatch ? chineseMatch[1] : dirName;
}
```

**Alternatives Considered**:
- 配置文件显式声明模块：增加用户负担
- 符号表全局搜索：性能差，无法处理大型项目

### 6. Error Handling and Robustness

**Decision**: 分层错误处理 + 渐进式降级
**Rationale**:
- 配置文件可能有语法错误，不能因此阻塞整个系统
- 用户需要清晰错误信息快速定位问题

**Strategy**:
1. **语法错误**: 标记错误位置，继续解析其他部分
2. **引用错误**: 标记未找到的定义，提供候选建议
3. **模块错误**: 明确提示无法找到的模块
4. **降级处理**: 核心功能（高亮）优先，高级功能（跳转）可选

## Summary

所有关键技术和设计决策已确定：

1. ✅ **技术栈**: TypeScript + VSCode Extension API + ANTLR4
2. ✅ **核心功能**: 语法高亮、跳转定义、自动补全
3. ✅ **主题系统**: 默认+中国古典色两套配色
4. ✅ **性能**: 增量解析+符号表缓存
5. ✅ **跨模块**: 基于目录结构的解析算法
6. ✅ **容错**: 分层错误处理和渐进式降级

**No unresolved clarifications** - Ready for Phase 1 design.
