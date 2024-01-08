# cfgeditor

## features

* view table schema, record
* edit record

### Prerequisites

1. nodejs, pnpm
2. `pnpm install`


## prepare: start backend object data server

```bash
java -jar ../cfggen.jar -datadir ../example/config  -gen server
```

## build

### development phase

```bash
pnpm run dev
```

then visit http://localhost:5173/.


### publish
```bash
pnpm run build
```

the target files is in `dist` directory. run frontend server:
```bash
cd dist
jwebserver
```
then visit http://localhost:8000/.


## build exe

### Prerequisites
1. rust

### generate cfgeditor.exe

```bash
pnpm tauri build
```

the generated cfgeditor.exe is in `src-tauri\target\release\` directory.
