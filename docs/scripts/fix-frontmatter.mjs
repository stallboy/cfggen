import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const DOCS_DIR = path.resolve(__dirname, '../src/content/docs')

function processFile(filePath) {
  let content = fs.readFileSync(filePath, 'utf-8')

  // 检查是否已经处理过（如果已经有 sidebar 字段，跳过）
  if (content.includes('sidebar:')) {
    console.log(`  ⊘ ${path.relative(DOCS_DIR, filePath)} (已处理)`)
    return
  }

  // 提取 title
  const titleMatch = content.match(/^title:\s*(.+)$/m)
  const title = titleMatch ? titleMatch[1].trim() : ''

  // 提取 nav_order
  const navOrderMatch = content.match(/^nav_order:\s*(.+)$/m)

  // 构建新的 frontmatter
  let newFrontmatter = `---\ntitle: ${title}\n`

  // 添加 description
  newFrontmatter += `description: ${title}\n`

  // 添加 sidebar 配置
  if (navOrderMatch) {
    const order = navOrderMatch[1].trim()
    newFrontmatter += `sidebar:\n  order: ${order}\n`
  }

  newFrontmatter += '---'

  // 替换原 frontmatter（从开头到第一个 ---）
  content = content.replace(/^---[\s\S]*?---/m, newFrontmatter)

  fs.writeFileSync(filePath, content, 'utf-8')
  console.log(`  ✓ ${path.relative(DOCS_DIR, filePath)}`)
}

function walkDirectory(dir) {
  const files = fs.readdirSync(dir)

  for (const file of files) {
    const filePath = path.join(dir, file)
    const stat = fs.statSync(filePath)

    if (stat.isDirectory()) {
      walkDirectory(filePath)
    } else if (file.endsWith('.mdx') || file.endsWith('.md')) {
      processFile(filePath)
    }
  }
}

console.log('开始处理 frontmatter...\n')
walkDirectory(DOCS_DIR)
console.log('\n✓ 处理完成！')
