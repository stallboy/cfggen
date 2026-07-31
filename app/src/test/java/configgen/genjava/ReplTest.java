package configgen.genjava;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link Repl} 的注册式分发、intro 自动生成、未知命令、错误恢复行为，
 * 靠重定向 stdin/stdout 喂入命令序列来验证。
 */
class ReplTest {

    @FunctionalInterface
    private interface ThrowingIo {
        void run() throws IOException;
    }

    private static String capture(String input, ThrowingIo task) throws IOException {
        InputStream oldIn = System.in;
        PrintStream oldOut = System.out;
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(out, true, StandardCharsets.UTF_8));
            task.run();
        } finally {
            System.setIn(oldIn);
            System.setOut(oldOut);
        }
        return out.toString(StandardCharsets.UTF_8);
    }

    @Test
    void registry_dispatchesByFirstToken_andHandlesUnknown() throws IOException {
        String out = capture("schema user\nget\nbadcmd\nq\n", () ->
                new Repl("> ")
                        .command("schema", "schema [表名子串]", args -> "S:" + (args.length > 0 ? args[0] : ""))
                        .command("get", "get <表> <键>", args -> args.length < 2 ? "用法：get <表> <键>" : "G:" + args[0] + "," + args[1])
                        .run());
        assertTrue(out.contains("S:user"), "具名命令应按首词分发，后续 token 作为参数");
        assertTrue(out.contains("用法：get <表> <键>"), "get 缺参时应返回 handler 里的用法串");
        assertTrue(out.contains("未知命令: badcmd"), "未注册命令应给出未知命令提示");
        assertTrue(out.contains("schema/get/q"), "未知命令提示应列出已注册命令名 + q");
    }

    @Test
    void intro_builtFromCommandUsages() throws IOException {
        String out = capture("q\n", () ->
                new Repl("> ")
                        .command("schema", "schema [表名子串]", args -> "")
                        .command("get", "get <表> <键>", args -> "")
                        .run());
        assertTrue(out.contains("命令："), "启动应打印命令清单");
        assertTrue(out.contains("schema [表名子串]"), "intro 应含各命令自己声明的 usage");
        assertTrue(out.contains("get <表> <键>"));
        assertTrue(out.contains("q 退出"), "intro 末尾应附 q 退出");
    }

    @Test
    void handlerException_printsErrorAndContinues() throws IOException {
        String out = capture("echo boom\necho ok\nq\n", () ->
                new Repl(">").command("echo", "echo <x>", args -> {
                    if (args.length > 0 && args[0].equals("boom")) throw new RuntimeException("explode");
                    return "done:" + (args.length > 0 ? args[0] : "");
                }).run());
        assertTrue(out.contains("错误: explode"), "handler 抛异常应打印错误而非崩溃");
        assertTrue(out.contains("done:ok"), "出错后应继续处理后续输入");
    }

    @Test
    void quitToken_stopsLoopBeforeHandler() throws IOException {
        String out = capture("quit\necho x\n", () ->
                new Repl(">").command("echo", "echo <x>", args -> "ran:" + (args.length > 0 ? args[0] : "")).run());
        assertFalse(out.contains("ran:"), "q/quit 应由 Repl 截获退出，不交给 handler");
    }
}
