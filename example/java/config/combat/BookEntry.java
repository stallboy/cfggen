package config.combat;

public class BookEntry {
    private String name;
    private BookEntry() {
    }

    public BookEntry(String name) {
        this.name = name;
    }

    public static BookEntry _create(configgen.genjava.ConfigInput input) {
        BookEntry self = new BookEntry();
        self.name = input.readStringInPool();
        return self;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(name);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof BookEntry o)) {
            return false;
        }
        return name.equals(o.name);
    }

    @Override
    public String toString() {
        return "(" + name + ")";
    }

}
