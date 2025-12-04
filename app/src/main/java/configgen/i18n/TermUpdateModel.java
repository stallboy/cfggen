package configgen.i18n;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record TermUpdateModel(@NotNull String termsInCsv,
                              @NotNull List<Translated> tableTranslatedList) {

    public record Translated(@NotNull String table,
                             @NotNull String translatedInCsv) {
    }
}
