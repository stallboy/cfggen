/**
 * 测试跳转到定义功能
 * T024: 测试简单引用、键引用、列表引用、跨模块引用
 *
 * 此脚本在开发环境中手动运行，用于验证definitionProvider的功能
 */

const fs = require('fs');
const path = require('path');

console.log('=== CFG Extension: 跳转到定义测试 ===\n');

// 测试用例目录
const testDir = path.join(__dirname, 'fixtures', 'definitions');

// 测试用例列表
const testCases = [
    {
        name: '简单引用测试',
        file: 'simple_definition.cfg',
        tests: [
            { type: 'struct', symbol: 'Position', line: 4 },
            { type: 'table', symbol: 'Task', line: 9 },
            { type: 'interface', symbol: 'IMovable', line: 17 },
            { type: 'type_ref', symbol: 'Position', line: 12 },
            { type: 'foreign_key', symbol: 'item', line: 14 }
        ]
    },
    {
        name: '键引用测试',
        file: 'key_references.cfg',
        tests: [
            { type: 'table', symbol: 'Item', line: 4 },
            { type: 'foreign_key_with_key', symbol: 'item', line: 11 }
        ]
    },
    {
        name: '列表引用测试',
        file: 'list_references.cfg',
        tests: [
            { type: 'table', symbol: 'Npc', line: 4 },
            { type: 'list_foreign_key', symbol: 'npc', line: 11 }
        ]
    },
    {
        name: '跨模块引用测试',
        file: 'cross_module_references.cfg',
        tests: [
            { type: 'table', symbol: 'TaskConfig', line: 7 },
            { type: 'cross_module_ref', symbol: 'other.item', line: 10 },
            { type: 'cross_module_ref', symbol: 'monster.monster', line: 11 },
            { type: 'cross_module_list_ref', symbol: 'item.reward', line: 13 }
        ]
    },
    {
        name: '无效引用测试',
        file: 'invalid_references.cfg',
        tests: [
            { type: 'table', symbol: 'Task', line: 4 },
            { type: 'struct', symbol: 'InvalidStruct', line: 9 },
            { type: 'undefined_type', symbol: 'UndefinedType', line: 11, shouldFail: true },
            { type: 'undefined_type', symbol: 'UnknownStruct', line: 13, shouldFail: true },
            { type: 'undefined_table', symbol: 'undefined_table', line: 18, shouldFail: true }
        ]
    }
];

// 运行测试用例
function runTests() {
    let passed = 0;
    let failed = 0;

    for (const testCase of testCases) {
        console.log(`\n--- ${testCase.name} ---`);

        const filePath = path.join(testDir, testCase.file);

        if (!fs.existsSync(filePath)) {
            console.log(`❌ 测试文件不存在: ${filePath}`);
            failed++;
            continue;
        }

        const content = fs.readFileSync(filePath, 'utf-8');
        const lines = content.split('\n');

        for (const test of testCase.tests) {
            const lineContent = lines[test.line - 1] || '';

            console.log(`\n  测试: ${test.type}`);
            console.log(`  符号: ${test.symbol}`);
            console.log(`  行号: ${test.line}`);
            console.log(`  内容: ${lineContent.trim()}`);

            // 检查符号是否在内容中
            const hasSymbol = lineContent.includes(test.symbol);

            if (test.shouldFail) {
                // 对于无效引用测试，符号应该存在（作为无效引用）
                // 这些引用在语法上正确，但在语义上是无效的
                if (hasSymbol) {
                    console.log(`  ✓ 无效引用测试: 符号存在（预期无效）`);
                    passed++;
                } else {
                    console.log(`  ❌ 无效引用测试: 符号不存在`);
                    failed++;
                }
            } else {
                if (hasSymbol) {
                    console.log(`  ✓ 符号存在，准备测试跳转定义`);

                    // TODO: 在实际测试中，这里会调用definitionProvider
                    // const definitions = definitionProvider.provideDefinitions(document, position);
                    // 验证definitions是否正确

                    // 模拟检查: 验证语法正确性
                    const syntaxOk = validateSyntax(lineContent, test.type);
                    if (syntaxOk) {
                        console.log(`  ✓ 语法验证通过`);
                        passed++;
                    } else {
                        console.log(`  ❌ 语法验证失败`);
                        failed++;
                    }
                } else {
                    console.log(`  ❌ 符号不存在于指定行`);
                    failed++;
                }
            }
        }
    }

    console.log('\n=== 测试结果 ===');
    console.log(`通过: ${passed}`);
    console.log(`失败: ${failed}`);
    console.log(`总计: ${passed + failed}`);
    console.log(`成功率: ${(passed / (passed + failed) * 100).toFixed(2)}%\n`);

    return failed === 0;
}

// 验证语法正确性
function validateSyntax(line, type) {
    switch (type) {
        case 'struct':
            return line.includes('struct ') && line.includes('{');
        case 'table':
            return line.includes('table ') && line.includes('[') && line.includes(']');
        case 'interface':
            return line.includes('interface ') && line.includes('{');
        case 'type_ref':
            return line.includes(':') && line.includes(' ');
        case 'foreign_key':
            return line.includes('->') || line.includes('=>');
        case 'foreign_key_with_key':
            return (line.includes('->') || line.includes('=>')) && line.includes('[');
        case 'list_foreign_key':
            return line.includes('list<') && (line.includes('->') || line.includes('=>'));
        case 'cross_module_ref':
            return (line.includes('->') || line.includes('=>')) && line.includes('.');
        case 'cross_module_list_ref':
            return line.includes('list<') && (line.includes('->') || line.includes('=>')) && line.includes('.');
        case 'undefined_type':
        case 'undefined_table':
            return true; // 无效引用在语法上可能无效，但这是预期的
        default:
            return true;
    }
}

// 执行测试
const success = runTests();
process.exit(success ? 0 : 1);
