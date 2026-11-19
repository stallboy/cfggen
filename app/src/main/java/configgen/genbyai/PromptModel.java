package configgen.genbyai;


import java.util.List;
import java.util.Objects;

public record PromptModel(String table,
                          String structInfo, // typescript 包含了一些 ref table info， 包含id，title
                          String rule, // 关于此表的一些补充信息，一些规则，从$mod.md + [table].md中读到
                          List<Example> examples) {

    public PromptModel {
        Objects.requireNonNull(table);
        Objects.requireNonNull(structInfo);
        Objects.requireNonNull(rule);
        Objects.requireNonNull(examples);
    }

    public record Example(String id,
                          String description,
                          String json) {

        public Example {
            Objects.requireNonNull(id);
            Objects.requireNonNull(description);
            Objects.requireNonNull(json);
        }

        public String toPrompt() {
            return "ID: " + id + "\n" +
                    "Description: " + description + "\n" +
                    "Data:\n" +
                    "```json\n" +
                    json + "\n" +
                    "```\n";
        }

    }
}
