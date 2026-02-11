pnpm i -D tsx       # 安装 TypeScript 运行环境
rm ConfigUtil.ts
java -jar ../../cfggen.jar  -datadir ../config  -gen ts -gen bytes
pause
