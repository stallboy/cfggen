# cfgeditor

## features

* view table schema, record
* edit record

### Prerequisites

1. nodejs, pnpm
2. `pnpm install`


## prepare: start backend object data server

To edit Excel files, place the matching `cfggen-excel-write.jar` next to `cfggen.jar`. Download both from the release, or build them with `genjar.bat` and `genexceljar.bat` at the repository root.

```bash
java -jar ../cfggen.jar -datadir ../example/config  -gen server
```

## build

### development phase

```bash
pnpm run dev
```

then visit http://localhost:1420/.


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
