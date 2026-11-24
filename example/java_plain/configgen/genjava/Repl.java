package configgen.genjava;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用交互式 REPL，命令注册式：{@code new Repl(prompt).command(...).run()}。
 *
 * <p>每条命令自带 usage，启动时 Repl 自动把已注册命令的 usage 拼成命令清单打印，无需外部传 intro。
 * 命令按输入首词分发，handler 默认收到首词之后的 token 数组。
 *
 * <p>Repl 负责全部机械流程：启动打印命令清单、stdin 读行、空行跳过、{@code q}/{@code quit} 退出、
 * 分发、未命中给「未知命令」提示（命令名取自已注册集合）、handler 抛异常打印错误后继续、
 * 返回值非 {@code null} 则打印。
 */
public class Repl {

    /** 命令处理器：{@code args} 为命令名之后的参数。 */
    @FunctionalInterface
    public interface Command {
        String run(String[] args);
    }

    private record Registration(String usage, Command handler, boolean literalTail) {
    }

    private final String prompt;
    private final Map<String, Registration> commands = new LinkedHashMap<>();

    public Repl(String prompt) {
        this.prompt = prompt;
    }

    public Repl command(String name, String usage, Command handler) {
        commands.put(name, new Registration(usage, handler, false));
        return this;
    }

    /** 第一个参数是单词。其后的第一个分隔字符之后，剩余输入原样作为第二个参数。 */
    public Repl commandWithLiteralTail(String name, String usage, Command handler) {
        commands.put(name, new Registration(usage, handler, true));
        return this;
    }

    public void run() throws IOException {
        System.out.println(intro());
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print(prompt);
            String line = br.readLine();
            if (line == null) {
                break;
            }
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.equals("q") || trimmed.equals("quit")) {
                break;
            }
            try {
                String out = dispatch(line);
                System.out.println(out);
            } catch (Exception ex) {
                System.out.println("错误: " + ex.getMessage());
            }
        }
    }

    private String dispatch(String line) {
        String[] parts = line.stripLeading().split("\\s+", 2);
        Registration cmd = commands.get(parts[0]);
        if (cmd != null) {
            if (parts.length < 2 || parts[1].isEmpty()) {
                return cmd.handler().run(new String[0]);
            }
            String[] args = cmd.literalTail() ? parts[1].split("\\s", 2) : parts[1].trim().split("\\s+");
            return cmd.handler().run(args);
        }
        return "未知命令: " + parts[0] + "（" + String.join("/", commands.keySet()) + "/q）";
    }

    private String intro() {
        List<String> all = new ArrayList<>();
        for (Registration cmd : commands.values()) {
            all.add(cmd.usage());
        }
        all.add("q 退出");
        return "命令：" + String.join(" | ", all);
    }
}
