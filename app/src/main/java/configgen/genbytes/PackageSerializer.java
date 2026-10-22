package configgen.genbytes;

import configgen.genjava.ConfigOutput;

import java.util.Map;
import java.util.TreeMap;

/** The envelope only; all block payloads retain their existing serializers. */
public final class PackageSerializer {
    private PackageSerializer() {}

    public static void write(ConfigOutput output, Map<String, String> metadata, Map<String, byte[]> blocks) {
        output.writeInt(1);
        output.writeInt(metadata.size());
        new TreeMap<>(metadata).forEach((key, value) -> {
            output.writeString(key);
            output.writeString(value);
        });
        output.writeInt(blocks.size());
        blocks.forEach((name, bytes) -> {
            output.writeString(name);
            output.writeInt(bytes.length);
            output.writeRawBytes(bytes);
        });
    }
}
