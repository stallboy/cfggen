import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const DOCS_DIR = path.resolve(__dirname, '../src/content/docs')

// 文件名映射表：旧文件名 -> 新文件名
const fileMappings = {
  '00.quickstart.md': 'quickstart.mdx',
  '01.usage.md': 'usage.mdx',
  '02.directoryStructure.md': 'directoryStructure.mdx',
  '03.schema.md': 'schema.mdx',
  '04.key.md': 'key.mdx',
  '05.tabularMapping.md': 'tabularMapping.mdx',
  '06.tag.md': 'tag.mdx',
  '07.otherMetadatas.md': 'otherMetadatas.mdx',
  '10.i18n.md': 'i18n.mdx',
  '11.i18n2.md': 'i18n2.mdx',
  '12.verify.md': 'verify.mdx',
  '20.bestPractices.md': 'bestPractices.mdx',
  '21.inside.md': 'inside.mdx',
  // cfgeditor
  '01.quickstart.md': 'quickstart.mdx',
  '02.build.md': 'build.mdx',
  '03.postrun.md': 'postrun.mdx',
  '04.res.md': 'res.mdx',
  '05.fold.md': 'fold.mdx',
  // aigen
  '01.interactive.md': 'interactive.mdx',
  '02.batch.md': 'batch.mdx',
  '03.mcpserver.md': 'mcpserver.mdx',
  '04.translate.md': 'translate.mdx'
}

function processFile(filePath) {
  let content = fs.readFileSync(filePath, 'utf-8')
  let modified = false

  // 替换所有文件链接
  for (const [oldName, newName] of Object.entries(fileMappings)) {
    // 匹配各种格式的链接：[text](./00.quickstart.md), [text](../cfggen/00.quickstart.md), 等
    const regex = new RegExp(`(\\(|\\])(${oldName.replace('.', '\\.')})(\\)|)`, 'g')
    const newContent = content.replace(regex, (match, prefix, oldFile, suffix) => {
      modified = true
      // 去掉扩展名，Starlight 会自动处理
      const newFile = newName.replace('.mdx', '')
      return `${prefix}${newFile}${suffix}`
    })

    if (newContent !== content) {
      content = newContent
    }
  }

  // 同时修复 .html 链接（02.batch.html -> batch）
  for (const [oldName, newName] of Object.entries(fileMappings)) {
    const baseName = oldName.replace('.md', '')
    const newBaseName = newName.replace('.mdx', '')
    const regex = new RegExp(`(\\(|\\])(${baseName.replace('.', '\\.')})\\.html(\\)|)`, 'g')
    const newContent = content.replace(regex, (match, prefix, oldFile, suffix) => {
      modified = true
      return `${prefix}${newBaseName}${suffix}`
    })

    if (newContent !== content) {
      content = newContent
    }
  }

  if (modified) {
    fs.writeFileSync(filePath, content, 'utf-8')
    console.log(`  ✓ ${path.relative(DOCS_DIR, filePath)}`)
    return true
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

console.log('修复内部链接...\n')
const count = walkDirectory(DOCS_DIR)
console.log(`\n✓ 修复完成！共修改 ${count} 个文件`)
