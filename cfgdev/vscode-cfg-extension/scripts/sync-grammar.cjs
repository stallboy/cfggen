const fs = require('node:fs');
const path = require('node:path');
const target = path.resolve(__dirname, '../src/grammar/Cfg.g4');
const source = path.resolve(__dirname, '../../../app/src/main/java/configgen/schema/cfg/Cfg.g4');
if (process.argv.includes('--normalize')) {
    for (const name of ['CfgLexer.ts', 'CfgParser.ts', 'CfgVisitor.ts']) {
        const file = path.join(path.dirname(target), name);
        const text = fs.readFileSync(file, 'utf8').replace(/[ \t]+$/gm, '')
            .replace(/^[ \t]+/gm, indent => indent.replace(/\t/g, '    '));
        fs.writeFileSync(file, text);
    }
    process.exit(0);
}
if (fs.existsSync(source)) {
    const canonical = fs.readFileSync(source, 'utf8').replace(/\r\n/g, '\n');
    if (process.argv.includes('--check')) {
        if (fs.readFileSync(target, 'utf8').replace(/\r\n/g, '\n') !== canonical) {
            throw new Error('CFG grammar differs from app; run npm run generate-parser');
        }
    } else if (fs.readFileSync(target, 'utf8').replace(/\r\n/g, '\n') !== canonical) {
        fs.writeFileSync(target, canonical);
    }
}
