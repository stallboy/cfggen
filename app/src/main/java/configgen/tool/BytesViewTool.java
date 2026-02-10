package configgen.tool;

import configgen.gen.Parameter;
import configgen.gen.Tool;
import configgen.genjava.BytesInspector;

import java.io.IOException;

public class BytesViewTool extends Tool {
    private final String bytesFilename;
    private final String match;

    public BytesViewTool(Parameter parameter) {
        super(parameter);
        bytesFilename = parameter.get("bytes", "config.bytes");
        match = parameter.get("match", null, "loop when match not set");
    }

    @Override
    public void call() {
        BytesInspector jd = new BytesInspector(bytesFilename);
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