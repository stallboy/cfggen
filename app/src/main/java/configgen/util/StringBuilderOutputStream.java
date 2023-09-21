package configgen.util;

import java.io.OutputStream;

public class StringBuilderOutputStream extends OutputStream {

    private final StringBuilder sb;

    public StringBuilderOutputStream(StringBuilder sb) {
        this.sb = sb;
    }

    @Override
    public void write(int b) {
        this.sb.append((char) b);
    }

    @Override
    public String toString() {
        return this.sb.toString();
    }
}
