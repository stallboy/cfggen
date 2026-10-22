package configgen.schema;

import java.util.List;
import java.util.Objects;

/** A schema path; fields are navigation steps, never a runtime expression. */
public record ReferencePath(Start start, String owner, List<String> fields) {
    public enum Start { CURRENT, ROOT, ANCESTOR, TABLE }

    public ReferencePath {
        Objects.requireNonNull(start);
        Objects.requireNonNull(owner);
        fields = List.copyOf(fields);
        if ((start == Start.TABLE || start == Start.ANCESTOR) != !owner.isEmpty()) {
            throw new IllegalArgumentException("reference path owner does not match its start");
        }
    }

    public boolean isLocalField() {
        return start == Start.CURRENT && fields.size() == 1;
    }

    public String expression() {
        String prefix = switch (start) {
            case CURRENT -> "";
            case ROOT -> "::";
            case ANCESTOR -> "@" + owner + "::";
            case TABLE -> owner + (fields.isEmpty() ? "" : "::");
        };
        return prefix + String.join(".", fields);
    }
}
