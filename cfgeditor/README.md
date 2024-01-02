# cfgeditor

## features

* view table schema, view record in table
* edit record

## build

1. run cfgeditor backend server

```bash
java -jar ../cfggen.jar -datadir ../example/config  -gen server
```

2. use cfgeditor front server & web browser to test

    ```bash
    pnpm run dev
    ```

   http://localhost:5173/

   or use cfgeditor.exe to test
    ```bash
    pnpm tauri dev
    ```

## generate front server

```bash
pnpm run build  # generate dist/*
```

then you can use apache or nginx to serve the dist directory. or use jwebserver to test

```bash
cd dist
jwebserver # use
```

## generate cfgeditor.exe

```bash
pnpm tauri build  # generate src-tauri\target\release\cfgeditor.exe
```
