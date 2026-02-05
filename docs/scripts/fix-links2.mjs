import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const DOCS_DIR = path.resolve(__dirname, '../src/content/docs')

// 文件名映射表：旧文件名 -> 新文件名（不带扩展名）
const fileMappings = {
  '00.quickstart': 'quickstart',
  '01.usage': 'usage',
  '02.directoryStructure': 'directoryStructure',
  '02.batch': 'batch',
  '03.schema': 'schema',
  '03.mcpserver': 'mcpserver',
  '04.key': 'key',
  '04.translate': 'translate',
  '05.tabularMapping': 'tabularMapping',
  '05.fold': 'fold',
  '06.tag': 'tag',
  '07.otherMetadatas': 'otherMetadatas',
  '10.i18n': 'i18n',
  '11.i18n2': 'i18n2',
  '12.verify': 'verify',
  '20.bestPractices': 'bestPractices',
  '21.inside': 'inside',
  '01.interactive': 'interactive'
}

function processFile(filePath) {
  let content = fs.readFileSync(filePath, 'utf-8')
  let originalContent = content
  let modified = false

  // 替换所有文件链接，包括 .md 和 .mdx 扩展名
  for (const [oldName, newName] of Object.entries(fileMappings)) {
    // 匹配各种格式的链接：
    // [text](./00.quickstart.md)
    // [text](../cfggen/03.schema.md)
    // [text](./01.usage.md#anchor)
    // (./00.quickstart.md)
    const regex = new RegExp(`([\\(\\[])\\.?\\/??\\.?\\.?\\/??(${oldName.replace('.', '\\.')})(\\.md|\\.mdx|)(#[^\\)]*)?([\\)\\]])`, 'g')

    content = content.replace(regex, (match, prefix, oldFile, ext, anchor, suffix) => {
      modified = true
      const newAnchor = anchor || ''
      return `${prefix}${newName}${newAnchor}${suffix}`
    })
  }

  if (modified && content !== originalContent) {
    fs.writeFileSync(filePath, content, 'utf-8')
    console.log(`  ✓ ${path.relative(DOCS_DIR, filePath)}`)
    return true
  } else if (modified) {
    console.log(`  ~ ${path.relative(DOCS_DIR, filePath)} (无变化)`)
    return false
  }
  return false
}

function walkDirectory(dir) {
  const files = fs.readdirSync(dir)
  let count = 0

  for (const file of files) {
    const filePath = path.join(dir, file)
    const stat = fs.statSync(filePath)

    if (stat.isDirectory()) {
      count += walkDirectory(filePath)
    } else if (file.endsWith('.mdx') || file.endsWith('.md')) {
      if (processFile(filePath)) {
        count++
      }
    }
  }

  return count
}

console.log('修复所有内部链接...\n')
const count = walkDirectory(DOCS_DIR)
console.log(`\n✓ 修复完成！共修改 ${count} 个文件`)
