import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const OLD_DOCS = path.resolve(__dirname, '../../olddocs/docs')
const NEW_DOCS = path.resolve(__dirname, '../src/content/docs')

// 迁移配置：文档和图片都会被复制到同一目录
const migrations = [
  {
    source: path.join(OLD_DOCS, 'cfggen'),
    target: path.join(NEW_DOCS, 'cfggen'),
    rename: (file) => {
      // 去掉数字前缀，将 .md 转为 .mdx
      return file.replace(/^\d+\./, '').replace('.md', '.mdx')
    },
    // 复制图片和 PlantUML 文件到同一目录
    assets: ['.png', '.jpg', '.jpeg', '.gif', '.plantuml']
  },
  {
    source: path.join(OLD_DOCS, 'cfgeditor'),
    target: path.join(NEW_DOCS, 'cfgeditor'),
    rename: (file) => file.replace(/^\d+\./, '').replace('.md', '.mdx'),
    assets: ['.png', '.jpg', '.jpeg', '.gif']
  },
  {
    source: path.join(OLD_DOCS, 'aigen'),
    target: path.join(NEW_DOCS, 'aigen'),
    rename: (file) => file.replace(/^\d+\./, '').replace('.md', '.mdx'),
    assets: ['.png', '.jpg', '.jpeg', '.gif', '.plantuml']
  }
]

// 创建目标目录
function ensureDir(dir) {
  if (!fs.existsSync(dir)) {
    fs.mkdirSync(dir, { recursive: true })
  }
}

// 转换 Jekyll frontmatter 到 Starlight 格式
function convertFrontmatter(content) {
  return content.replace(
    /^---\n([\s\S]*?)\n---/m,
    (match, frontmatter) => {
      // 提取 title
      const titleMatch = frontmatter.match(/^title:\s*(.+)$/m)
      const title = titleMatch ? titleMatch[1].trim() : ''

      // 提取 nav_order
      const navOrderMatch = frontmatter.match(/^nav_order:\s*(.+)$/m)
      let sidebarConfig = ''
      if (navOrderMatch) {
        const order = navOrderMatch[1].trim()
        sidebarConfig = `\nsidebar:\n  order: ${order}`
      }

      // 只保留必要字段，移除其他 Jekyll 特有字段
      const lines = frontmatter.split('\n')
      const cleanLines = []
      for (const line of lines) {
        const trimmed = line.trim()
        // 保留 title 和 description（如果存在）
        if (trimmed.startsWith('title:') || trimmed.startsWith('description:')) {
          cleanLines.push(line)
        }
        // 移除 Jekyll 特有字段
        else if (
          trimmed.startsWith('layout:') ||
          trimmed.startsWith('parent:') ||
          trimmed.startsWith('grand_parent:') ||
          trimmed.startsWith('nav_order:') ||
          trimmed.startsWith('has_children:') ||
          trimmed.startsWith('has_toc:')
        ) {
          // 跳过这些行
        }
        // 保留其他可能的字段
        else if (trimmed) {
          cleanLines.push(line)
        }
      }

      let newFrontmatter = cleanLines.join('\n')

      // 如果没有 description，添加一个默认的
      if (!newFrontmatter.includes('description:')) {
        newFrontmatter += `\ndescription: ${title}`
      }

      // 添加 sidebar 配置
      newFrontmatter += sidebarConfig

      return `---\n${newFrontmatter}\n---`
    }
  )
}

// 迁移逻辑
async function migrate() {
  for (const config of migrations) {
    console.log(`正在迁移: ${config.source} -> ${config.target}`)
    ensureDir(config.target)

    const files = fs.readdirSync(config.source)

    for (const file of files) {
      const ext = path.extname(file)
      const srcPath = path.join(config.source, file)

      // 处理 Markdown 文件
      if (ext === '.md' && file !== 'index.md') {
        const newName = config.rename(file)
        const destPath = path.join(config.target, newName)

        // 读取文件内容
        let content = fs.readFileSync(srcPath, 'utf-8')

        // 更新图片路径为相对路径
        content = content.replace(/!\[([^\]]*)\]\(assets\//g, '![\$1](./')
        content = content.replace(/!\[([^\]]*)\]\(\.\.\/assets\//g, '![\$1](../')

        // 转换 frontmatter
        content = convertFrontmatter(content)

        // 写入文件
        fs.writeFileSync(destPath, content, 'utf-8')
        console.log(`  ✓ ${file} -> ${newName}`)
      }
      // 复制 index.md
      else if (file === 'index.md') {
        const destPath = path.join(config.target, file)
        let content = fs.readFileSync(srcPath, 'utf-8')
        content = convertFrontmatter(content)
        fs.writeFileSync(destPath, content, 'utf-8')
        console.log(`  ✓ ${file}`)
      }
      // 复制资源文件（与文档共置）
      else if (config.assets.includes(ext)) {
        const destPath = path.join(config.target, file)
        fs.copyFileSync(srcPath, destPath)
        console.log(`  ✓ [资源] ${file}`)
      }
    }
  }

  // 处理根目录文件
  const rootFiles = ['index.md', 'vscodeExtension.md']
  for (const file of rootFiles) {
    const srcPath = path.join(OLD_DOCS, file)
    if (fs.existsSync(srcPath)) {
      const destPath = path.join(NEW_DOCS, file)
      let content = fs.readFileSync(srcPath, 'utf-8')
      if (file === 'vscodeExtension.md') {
        content = convertFrontmatter(content)
        const newPath = destPath.replace('.md', '.mdx')
        fs.writeFileSync(newPath, content, 'utf-8')
        console.log(`✓ [根目录] ${file} -> ${path.basename(newPath)}`)
      } else {
        content = convertFrontmatter(content)
        fs.writeFileSync(destPath, content, 'utf-8')
        console.log(`✓ [根目录] ${file}`)
      }
    }
  }

  // 复制 olddocs/assets/intro.png 到根目录
  const introSrc = path.join(OLD_DOCS, '../assets/intro.png')
  const introDest = path.join(NEW_DOCS, 'intro.png')
  if (fs.existsSync(introSrc)) {
    fs.copyFileSync(introSrc, introDest)
    console.log('✓ [根目录] intro.png')
  }

  console.log('\n迁移完成！')
}

// 运行迁移
migrate().catch(console.error)
