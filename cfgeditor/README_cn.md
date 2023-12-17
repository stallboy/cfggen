# cfgeditor

## 功能

* 可视化表结构，可视化记录
* 编辑记录

## build

1. 启动app服务器

```bash
../cfgeditor.exe -datadir ../example/config  -gen server
```

2. 启动cfgeditor调试server，使用浏览器查看

```bash
pnpm run dev
```

http://localhost:5173/

3. 生成cfgeditor.exe

```bash
pnpm tauri dev
```

```bash
pnpm tauri build
```
