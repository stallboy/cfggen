package configgen.genbyai;

import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public record PromptModel(@NotNull String table,
                          @NotNull String structInfo, // typescript 包含了一些 ref table info， 包含id，title
                          @NotNull String rule, // 关于此表的一些补充信息，一些规则，从$mod.md + [table].md中读到
                          @NotNull List<Example> examples) {

    public PromptModel {
        Objects.requireNonNull(table);
        Objects.requireNonNull(structInfo);
        Objects.requireNonNull(rule);
        Objects.requireNonNull(examples);
    }

    public record Example(@NotNull String id,
                          @NotNull String description,
                          @NotNull String json) {

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
