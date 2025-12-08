package configgen.geni18n;

import org.jetbrains.annotations.NotNull;

public record TermUpdateWithRefModel(@NotNull String termsInCsv,
                                     @NotNull String relatedTranslatedPairs) {
}