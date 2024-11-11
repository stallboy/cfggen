package configgen.tool;

import java.util.List;

public record PromptModel(String table,
                          String structInfo,
                          String extra,
                          List<Example> examples) {

    public record Example(String id,
                          String description,
                          String json) {
    }
}
