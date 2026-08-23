package configgen.genjava.code;

/**
 * 一次 java 代码生成的固定配置。
 * 原先散落在 Name.codeTopPkg / Name.beautifulName / NameableName.isSealedInterface /
 * TypeStr.isLangSwitch 四个 static 字段里，generate() 每次运行时写入：
 * 并发生成（多个 generator 实例、watch 触发的 postRun 线程）会互相踩踏。
 * 现改为 generate() 入口构造一份不可变实例，沿 model 链显式传递。
 */
public record GenCfg(String codeTopPkg, boolean isSealedInterface, boolean beautifulName, boolean isLangSwitch) {
}
