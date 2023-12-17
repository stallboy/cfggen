# cfgeditor

## features

* view table schema, view record in table
* edit record

## build

1. run cfgeditor backend server

```bash
java -jar ../cfggen.jar -datadir ../example/config  -gen server
```

2. run cfgeditor front server

```bash
pnpm run dev
```

3. browser 
http://localhost:5173/


## generate cfgeditor.exe

```bash
pnpm tauri dev
```

```bash
pnpm tauri build
```
