package configgen.genjava;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import static configgen.genjava.JsonValue.*;

/**
 * 通用的 {@link JsonValue} 渲染器（字段逐行松弛格式）+ 交互式 REPL。
 *
 * <p>{@link #render(JsonValue)} 是纯函数：对领域无关的 {@link JsonValue} 做穷尽 pattern match，
 * 不碰反射/schema，可独立复用与单测。所有复合结构一律 {@code {k: v}} / {@code [...] }，
 * 不区分 bean/map/interface（那是领域语义，已由 {@link CodeDataInspector} 翻译进 JsonValue）。
 *
 * <p>排版规则（自适应，无截断）：当 宽 &gt; 80 或 字段数 &gt; 3 或 深度 &gt; 2 时展开为缩进多行；
 * list 展开时每项 {@code - ...}。
 */
public class CodeDataPrinter {

    private static final int WIDTH_LIMIT = 80;

    private final CodeDataInspector inspector;

    public CodeDataPrinter(CodeDataInspector inspector) {
        this.inspector = inspector;
    }

    // ==================== 薄糖：直接返回字符串 ====================

    public String schema(String tableMatch) {
        return render(inspector.schemaJson(tableMatch));
    }

    public String get(String table, String key) {
        return render(inspector.getJson(table, key));
    }

    public String query(String recordMatch, String tableMatch) {
        return render(inspector.queryJson(recordMatch, tableMatch));
    }

    // ==================== 纯渲染 ====================

    public static String render(JsonValue v) {
        return String.join("\n", toLines(v, 0, ""));
    }

    private static List<String> toLines(JsonValue v, int depth, String indent) {
        return switch (v) {
            case Null ignored -> List.of(indent + "null");
            case Bool b -> List.of(indent + (b.value()));
            case Num n -> List.of(indent + n.value());
            case Str s -> List.of(indent + s.value());
            case Arr a -> arrLines(a.items(), depth, indent);
            case Obj o -> objLines(o, depth, indent);
        };
    }

    private static List<String> objLines(Obj o, int depth, String indent) {
        String line = indent + inlineObj(o, depth);
        if (!shouldExpand(line)) {
            return List.of(line);
        }
        String type = o.impl();
        List<String> lines = new ArrayList<>();
        String childIndent = indent + "  ";
        lines.add(indent + (type != null ? type + "{" : "{"));
        for (Member m : o.members()) {
            lines.addAll(fieldLines(m.name(), m.value(), depth + 1, childIndent));
        }
        lines.add(indent + "}");
        return lines;
    }

    private static List<String> arrLines(List<JsonValue> items, int depth, String indent) {
        String line = indent + inlineArr(items, depth);
        if (!shouldExpand(line)) {
            return List.of(line);
        }
        return arrBlock(items, depth, indent, "");
    }

    /**
     * 渲染数组的括号内容，紧凑链式：对象项之间 {@code }, { } 相接，形如 {@code [{..},{..}]}。
     * openPrefix 为开括号前的内容（顶层为 ""，字段值时为 {@code "key: "}）。
     * 全是单行项（标量/短对象）时退化为每项一行、逗号分隔，避免挤成超长一行。
     */
    private static List<String> arrBlock(List<JsonValue> items, int depth, String indent, String openPrefix) {
        String itemIndent = indent + "  ";
        List<List<String>> rendered = new ArrayList<>();
        for (JsonValue item : items) {
            rendered.add(toLines(item, depth + 1, itemIndent));
        }
        boolean allSingle = rendered.stream().allMatch(r -> r.size() == 1);
        List<String> out = new ArrayList<>();
        if (allSingle) {
            out.add(indent + openPrefix + "[");
            for (int i = 0; i < rendered.size(); i++) {
                out.add(rendered.get(i).getFirst() + (i < items.size() - 1 ? "," : ""));
            }
            out.add(indent + "]");
            return out;
        }
        for (int i = 0; i < rendered.size(); i++) {
            List<String> il = rendered.get(i);
            if (i == 0) {
                String firstStripped = il.getFirst().substring(itemIndent.length());
                out.add(indent + openPrefix + "[" + firstStripped);
            } else {
                // 前一项末行加逗号，下一项的 { 另起一行
                int last = out.size() - 1;
                out.set(last, out.get(last) + ",");
                out.add(il.getFirst());
            }
            for (int j = 1; j < il.size(); j++) {
                out.add(il.get(j));
            }
        }
        out.set(out.size() - 1, out.getLast() + "]");
        return out;
    }

    private static List<String> fieldLines(String key, JsonValue value, int depth, String indent) {
        // 按整行实际宽度（含缩进 + key + ": "）决定是否换行
        String line = indent + key + ": " + inline(value, depth);
        if (isScalar(value) || !shouldExpand(line)) {
            return List.of(line);
        }
        // 展开的 Obj：左大括号跟在 key 后；若为接口（impl 非空），类型名作前缀：key: TypeName{ .. }
        if (value instanceof Obj(String type, List<Member> members)) {
            List<String> lines = new ArrayList<>();
            String childIndent = indent + "  ";
            lines.add(indent + key + ": " + (type != null ? type + "{" : "{"));
            for (Member m : members) {
                lines.addAll(fieldLines(m.name(), m.value(), depth + 1, childIndent));
            }
            lines.add(indent + "}");
            return lines;
        }
        // Arr：紧凑链式 key: [{..},{..}]
        if (value instanceof Arr(List<JsonValue> items)) {
            return arrBlock(items, depth, indent, key + ": ");
        }
        // 其它（理论不会到这里）：key 单独一行，子节点缩进
        List<String> lines = new ArrayList<>();
        lines.add(indent + key + ":");
        lines.addAll(toLines(value, depth + 1, indent + "  "));
        return lines;
    }

    // ---------- 内联渲染（紧凑单行） ----------

    private static String inline(JsonValue v, int depth) {
        return switch (v) {
            case Null ignored -> "null";
            case Bool b -> Boolean.toString(b.value());
            case Num n -> n.value().toString();
            case Str s -> s.value();
            case Arr a -> inlineArr(a.items(), depth);
            case Obj o -> inlineObj(o, depth);
        };
    }

    private static String inlineObj(Obj o, int depth) {
        String type = o.impl();
        StringBuilder sb = new StringBuilder();
        if (type != null) {
            sb.append(type);
        }
        sb.append("{");
        List<Member> members = o.members();
        for (int i = 0; i < members.size(); i++) {
            if (i > 0) sb.append(", ");
            Member m = members.get(i);
            sb.append(m.name()).append(": ").append(inline(m.value(), depth + 1));
        }
        return sb.append("}").toString();
    }

    private static String inlineArr(List<JsonValue> items, int depth) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(inline(items.get(i), depth + 1));
        }
        return sb.append("]").toString();
    }

    /** 仅按宽度决定是否展开：内联形式超宽才展开，否则尽量一行（短结构不论嵌套多深都保持单行）。 */
    private static boolean shouldExpand(String inlineStr) {
        return inlineStr.length() > WIDTH_LIMIT;
    }

    private static boolean isScalar(JsonValue v) {
        return v instanceof Null || v instanceof Bool || v instanceof Num || v instanceof Str;
    }

    // ==================== 交互式 REPL ====================

    public void loop() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("CodeDataInspector REPL。命令：schema [表名子串] | get <表> <键> | query <记录子串> [表名子串] | q");
        while (true) {
            System.out.print("> ");
            String line = br.readLine();
            if (line == null) break;
            line = line.trim();
            if (line.isEmpty()) continue;
            if (line.equals("q") || line.equals("quit")) break;
            try {
                System.out.println(runCommand(line));
            } catch (Exception ex) {
                System.out.println("错误: " + ex.getMessage());
            }
        }
    }

    private String runCommand(String line) {
        String[] parts = line.split("\\s+");
        String cmd = parts[0];
        switch (cmd) {
            case "schema" -> {
                return schema(parts.length > 1 ? parts[1] : "");
            }
            case "get" -> {
                if (parts.length < 3) return "用法：get <表> <键>";
                return get(parts[1], parts[2]);
            }
            case "query" -> {
                if (parts.length < 2) return "用法：query <记录子串> [表名子串]";
                return query(parts[1], parts.length > 2 ? parts[2] : "");
            }
            default -> {
                return "未知命令: " + cmd + "（schema/get/query/q）";
            }
        }
    }
}
