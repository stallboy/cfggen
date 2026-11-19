package config;

import org.jspecify.annotations.Nullable;

public class CfgPosition {
    private final int x;
    private final int y;
    private final int z;

    public CfgPosition(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    private CfgPosition(configgen.genjava.ConfigInput input) {
        this.x = input.readInt();
        this.y = input.readInt();
        this.z = input.readInt();
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(x, y, z);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (!(other instanceof CfgPosition o)) {
            return false;
        }
        return x == o.x && y == o.y && z == o.z;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + "," + z + ")";
    }

    public static CfgPosition _create(configgen.genjava.ConfigInput input) {
        return new CfgPosition(input);
    }

}
