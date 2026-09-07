package configgen.gen;

/**
 * 命令行使用错误（缺参数取值、参数拼写错误、参数取值非法等）。
 * Main.runWithCatch 捕获后打印简短原因和usage，不打印堆栈。
 * 各生成器对参数取值做 fail-fast 校验时也抛此类型，避免依赖 CLI 入口类 Main。
 */
public class CliException extends RuntimeException {
    public CliException(String message) {
        super(message);
    }
}
