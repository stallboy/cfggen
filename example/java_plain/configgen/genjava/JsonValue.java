package configgen.genjava;

import java.util.List;

/**
 * 通用、领域无关的 JSON 值模型 —— {@link CodeDataInspector}（领域→JsonValue）与
 * {@link CodeDataPrinter}（JsonValue→字符串）之间的中间契约。
 *
 * <p>刻意不携带 cfggen 的 schema 语义（没有 bean/interface/enum 之分）：一切复合结构要么是
 * {@link Obj}（有序键值），要么是 {@link Arr}（有序列表）。这样打印机是纯粹的 JSON 风格渲染器，
 * 可独立复用与单测。
 */
public sealed interface JsonValue permits JsonValue.Obj, JsonValue.Arr, JsonValue.Str, JsonValue.Num, JsonValue.Bool, JsonValue.Null {

    /**
     * 有序键值对象（对应 bean / map / 接口多态值等一切具名复合结构）。
     * {@code impl} 为可空类型标记：非空时该 Obj 是接口多态值，渲染为 {@code ImplName{...}}；
     * 为 null 时渲染为普通 {@code {...}}。
     */
    record Obj(String impl, List<Member> members) implements JsonValue {
    }

    /** 有序列表。 */
    record Arr(List<JsonValue> items) implements JsonValue {
    }

    record Str(String value) implements JsonValue {
    }

    record Num(Number value) implements JsonValue {
    }

    record Bool(boolean value) implements JsonValue {
    }

    record Null() implements JsonValue {
    }

    record Member(String name, JsonValue value) {
    }

    // ---------- 构造便捷方法 ----------

    static Obj obj(Member... ms) {
        return new Obj(null, List.of(ms));
    }

    /** 接口多态值：带实现类名作类型标记。 */
    static Obj obj(String impl, Member... ms) {
        return new Obj(impl, List.of(ms));
    }

    static Arr arr(JsonValue... vs) {
        return new Arr(List.of(vs));
    }

    static Member member(String name, JsonValue v) {
        return new Member(name, v);
    }

    /** 按运行时值自动选合适标量：Number→Num、Boolean→Bool、null→Null、其余→Str(toString)。 */
    static JsonValue of(Object v) {
        return switch (v) {
            case null -> new Null();
            case Number n -> new Num(n);
            case Boolean b -> new Bool(b);
            default -> new Str(v.toString());
        };
    }
}
