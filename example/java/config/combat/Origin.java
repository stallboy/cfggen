package config.combat;

public class Origin {
    private int x;
    private int y;
    private Origin() {
    }

    public Origin(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public static Origin _create(configgen.genjava.ConfigInput input) {
        Origin self = new Origin();
        self.x = input.readInt();
        self.y = input.readInt();
        return self;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(x, y);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Origin o)) {
            return false;
        }
        return x == o.x && y == o.y;
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }

}
