package configgen.genjson;

import java.util.List;
import java.util.Objects;

public record PromptModel(String table,
                          String structInfo,
                          String extra,
                          List<Example> examples) {

    public PromptModel{
        Objects.requireNonNull(table);;
        Objects.requireNonNull(structInfo);;
        Objects.requireNonNull(extra);;
        Objects.requireNonNull(examples);;
    }

    public record Example(String id,
                          String description,
                          String json) {

        public Example{
            Objects.requireNonNull(id);;
            Objects.requireNonNull(description);;
            Objects.requireNonNull(json);
        }

    }
}
