package configgen.genjava;

import org.jspecify.annotations.Nullable;

public enum SchemaPrimitive implements Schema {
    SBool, SInt, SLong, SFloat, SStr, SText;

    @Override
    public boolean compatible(@Nullable Schema other) {
        return this == other;
    }
}
