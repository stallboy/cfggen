package configgen.genjava;

import configgen.gen.Parameter;
import configgen.gen.Tool;

import java.io.IOException;

public class JavaDataReader extends Tool {
    private final String javaDataFile;
    private final String match;

    public JavaDataReader(Parameter parameter) {
        super(parameter);
        javaDataFile = parameter.get("javadata", "config.data");
        match = parameter.get("match", null, "loop when match not set");
    }

    @Override
    public void call() {
        JavaData jd = new JavaData(javaDataFile);
        if (match == null) {
            try {
                jd.loop();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            jd.match(match);
        }
    }
}