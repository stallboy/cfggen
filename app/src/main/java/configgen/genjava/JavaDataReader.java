package configgen.genjava;

import configgen.gen.Parameter;
import configgen.gen.Tool;

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
            jd.loop();
        } else {
            jd.match(match);
        }
    }
}