import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const DOCS_DIR = path.resolve(__dirname, '../src/content/docs')

function processFile(filePath) {
  let content = fs.readFileSync(filePath, 'utf-8')
  let originalContent = content

  // 移除 Jekyll TOC 块（多种格式）
  content = content.replace(/## Table of contents\n.*?\n- TOC\n{:toc}\n---\n?/gs, '\n')
  content = content.replace(/## Table of contents\n\n\n- TOC\n{:toc}\n---\n?/g, '\n')
  content = content.replace(/## Table of contents\n.*?\n- TOC\n{:toc}\n/gs, '\n')

  // 移除 Jekyll 特有的 CSS 类语法
  content = content.replace(/\n?{: [^}]+}\n?/g, '\n')

  // 移除单独的 {:toc} 行
  content = content.replace(/\n{:toc}\n/g, '\n')

  // 移除 - TOC 行
  content = content.replace(/\n- TOC\n/g, '\n')

  // 清理多余的空行（超过2个连续空行）
  content = content.replace(/\n{3,}/g, '\n\n')

  if (content !== originalContent) {
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

console.log('清理所有 Jekyll 特有语法...\n')
const count = walkDirectory(DOCS_DIR)
console.log(`\n✓ 处理完成！共修改 ${count} 个文件`)
