import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const DOCS_DIR = path.resolve(__dirname, '../src/content/docs')

function processFile(filePath) {
  let content = fs.readFileSync(filePath, 'utf-8')
  let modified = false

  // 移除 Jekyll 特有的 CSS 类语法
  // 例如: {: .no_toc } 或 {: .no_toc .text-delta }
  content = content.replace(/\n?{: [^}]+}\n?/g, () => {
    modified = true
    return '\n'
  })

  // 移除 Jekyll TOC 语法
  // - TOC
  // {:toc}
  content = content.replace(/\n- TOC\n{:toc}\n/g, () => {
    modified = true
    return '\n'
  })

  // 移除单独的 {:toc} 行
  content = content.replace(/\n{:toc}\n/g, () => {
    modified = true
    return '\n'
  })

  // 移除 "## Table of contents" 标题（Starlight 有自己的 TOC）
  content = content.replace(/\n## Table of contents\n.*\n- TOC\n{:toc}\n/g, () => {
    modified = true
    return '\n'
  })

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

console.log('清理 Jekyll 特有语法...\n')
const count = walkDirectory(DOCS_DIR)
console.log(`\n✓ 处理完成！共修改 ${count} 个文件`)
