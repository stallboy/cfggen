package configgen.schema;

import java.util.Objects;

public sealed interface RefKey {

    record RefPrimary(boolean nullable) implements RefKey {
    }

    record RefUniq(KeySchema key, boolean nullable) implements RefKey {
        public RefUniq {
            Objects.requireNonNull(key);
        }
    }

    record RefList(KeySchema key) implements RefKey {
        public RefList {
            Objects.requireNonNull(key);
        }
    }
}
