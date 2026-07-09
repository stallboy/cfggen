package configgen.genbyai;

import org.simdjson.SimdJsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public record AICfg(String baseUrl,
                    String apiKey,
                    String model) {
    public static AICfg readFromFile(String cfgFn) {
        Path path = Path.of(cfgFn);
        if (!Files.exists(path)) {
            throw new RuntimeException(cfgFn + " not exist!");
        }
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (bytes.length == 0) {
            throw new RuntimeException(cfgFn + " is empty!");
        }
        // simdjson 严格 UTF-8 不跳 BOM，剥离可能的 UTF-8 BOM
        int len = bytes.length;
        if (len >= 3
                && (bytes[0] & 0xFF) == 0xEF
                && (bytes[1] & 0xFF) == 0xBB
                && (bytes[2] & 0xFF) == 0xBF) {
            bytes = java.util.Arrays.copyOfRange(bytes, 3, len);
            len = bytes.length;
        }
        return new SimdJsonParser().parse(bytes, len, AICfg.class);
    }
}
