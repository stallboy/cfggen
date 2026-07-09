package configgen.genbyai;

import com.alibaba.fastjson2.JSON;

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
        String jsonStr;
        try {
            jsonStr = Files.readString(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        if (jsonStr.isEmpty()) {
            throw new RuntimeException(cfgFn + " is empty!");
        }
        return JSON.parseObject(jsonStr, AICfg.class);
    }
}
