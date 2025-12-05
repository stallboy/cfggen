package configgen.i18n;

import org.dhatim.fastexcel.reader.Cell;
import org.dhatim.fastexcel.reader.Row;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

public class I18nUtils {
    private static final Pattern pattern = Pattern.compile("\r\n");

    public static String normalize(String text) {
        return pattern.matcher(text).replaceAll("\n");
    }


    /**
     * 让number类型也返回string，因为翻译返回的excel有些格子是数字默认用了number
     */
    public static Optional<String> getCellAsString(Row row, int c) {
        Optional<Cell> cell = row.getOptionalCell(c);
        if (cell.isPresent()) {
            switch (cell.get().getType()) {
                case NUMBER -> {
                    return Optional.of(cell.get().asNumber().toPlainString());
                }
                case STRING -> {
                    return Optional.of(cell.get().asString());
                }
                case EMPTY -> {
                    return Optional.of("");
                }
                default -> {
                    throw new IllegalArgumentException("不支持的单元格类型: " + cell.get().getType());
                }
            }
        } else {
            return Optional.empty();
        }
    }

    public static String fieldChainStr(List<String> fieldChain) {
        return fieldChain.size() == 1 ? fieldChain.getFirst() : String.join("-", fieldChain);
    }
}
