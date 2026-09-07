package config;

public class CfgPosition {
    private int x;
    private int y;
    private int z;

    private CfgPosition() {
    }

    public CfgPosition(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static CfgPosition _create(configgen.genjava.ConfigInput input) {
        CfgPosition self = new CfgPosition();
        self.x = input.readInt();
        self.y = input.readInt();
        self.z = input.readInt();
        return self;
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
    public boolean equals(Object other) {
        if (!(other instanceof CfgPosition))
            return false;
        CfgPosition o = (CfgPosition) other;
        return x == o.x && y == o.y && z == o.z;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + "," + z + ")";
    }

}
