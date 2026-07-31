package configgen.genjava;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用交互式 REPL，命令注册式：{@code new Repl(prompt).command(...).run()}。
 *
 * <p>每条命令自带 usage，启动时 Repl 自动把已注册命令的 usage 拼成命令清单打印，无需外部传 intro。
 * 命令按输入首词分发，handler 收到首词之后的 token 数组。
 *
 * <p>Repl 负责全部机械流程：启动打印命令清单、stdin 读行、trim、空行跳过、{@code q}/{@code quit} 退出、
 * 分发、未命中给「未知命令」提示（命令名取自已注册集合）、handler 抛异常打印错误后继续、
 * 返回值非 {@code null} 则打印。
 */
public class Repl {

    /** 命令处理器：{@code args} 为命令名之后的 token。 */
    @FunctionalInterface
    public interface Command {
        String run(String[] args);
    }

    private final String prompt;
    private final Map<String, Command> commands = new LinkedHashMap<>();
    private final Map<String, String> usages = new LinkedHashMap<>();

    public Repl(String prompt) {
        this.prompt = prompt;
    }

    public Repl command(String name, String usage, Command handler) {
        commands.put(name, handler);
        usages.put(name, usage);
        return this;
    }

    public void run() throws IOException {
        System.out.println(intro());
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print(prompt);
            String line = br.readLine();
            if (line == null) break;
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.equals("q") || line.equals("quit")) break;
            try {
                String out = dispatch(line);
                if (out != null) {
                    System.out.println(out);
                }
            } catch (Exception ex) {
                System.out.println("错误: " + ex.getMessage());
            }
        }
    }

    private String dispatch(String line) {
        String[] parts = line.split("\\s+");
        Command cmd = commands.get(parts[0]);
        if (cmd != null) {
            return cmd.run(Arrays.copyOfRange(parts, 1, parts.length));
        }
        return "未知命令: " + parts[0] + "（" + String.join("/", commands.keySet()) + "/q）";
    }

    private String intro() {
        List<String> all = new ArrayList<>(usages.values());
        all.add("q 退出");
        return "命令：" + String.join(" | ", all);
    }
}
