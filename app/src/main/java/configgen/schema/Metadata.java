package configgen.schema;

import javax.xml.stream.events.Comment;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.SequencedMap;

import static configgen.schema.Metadata.MetaTag.TAG;


public record Metadata(SequencedMap<String, MetaValue> data) {
    public static Metadata of() {
        return new Metadata(new LinkedHashMap<>());
    }

    public Metadata copy() {
        return new Metadata(new LinkedHashMap<>(data));
    }

    public Metadata {
        Objects.requireNonNull(data);
    }

    public MetaValue get(String name) {
        return data.get(name);
    }

    public boolean hasTag(String tag) {
        MetaValue value = data.get(tag);
        return value == TAG;
    }

    public void putTag(String tag) {
        data.putLast(tag, TAG);
    }

    public void putInt(String name, int value) {
        data.putLast(name, new MetaInt(value));
    }


    public String getComment() {
        if (data.get("__comment") instanceof MetaStr str) {
            return str.value;
        }
        return "";
    }


    public sealed interface MetaValue {
    }

    public enum MetaTag implements MetaValue {
        TAG
    }

    public record MetaInt(int value) implements MetaValue {
    }

    public record MetaFloat(float value) implements MetaValue {
    }

    public record MetaStr(String value) implements MetaValue {
    }

}
