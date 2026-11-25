package configgen.tool;

import com.ibm.icu.text.CharsetDetector;
import configgen.gen.CliException;
import configgen.gen.DataReadOptions;
import configgen.gen.Parameter;
import configgen.gen.Tool;
import configgen.util.FileUtil;
import configgen.util.Logger;
import configgen.util.UnicodeReader;
import org.jspecify.annotations.Nullable;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public final class CsvEncodingTool extends Tool {
    private static final Charset GBK = Charset.forName("GBK");

    private final Path dataDir;
    private final @Nullable Charset targetOverride;
    private final @Nullable Charset source;
    private final boolean fix;

    public CsvEncodingTool(Parameter parameter) {
        super(parameter);
        parameter.title("check CSV encodings or convert files without a BOM");
        dataDir = Path.of(parameter.get("datadir", "."));
        targetOverride = parseCharset(parameter.get("to", null), "to");
        source = parseCharset(parameter.get("source", null), "source");
        fix = parameter.has("fix");
    }

    @Override
    public void call() {
        Locale locale = Locale.getDefault();
        Charset localeEncoding = localeEncoding(locale);
        Charset target = targetOverride != null ? targetOverride : DataReadOptions.configuredCsvEncoding(dataDir);
        if (target == null) {
            target = localeEncoding;
        }
        if (!target.canEncode()) {
            throw new CliException("CSV target encoding cannot encode text: " + target.name());
        }

        Set<Charset> sources = source != null ? Set.of(source)
                : new LinkedHashSet<>(List.of(target, StandardCharsets.UTF_8, GBK, localeEncoding));
        Set<String> icuNames = icuNames(sources);
        Logger.log("CSV encoding %s: %s, target %s, locale %s",
                fix ? "fix" : "check", dataDir, target.name(), locale.toLanguageTag());

        if (!Files.isDirectory(dataDir)) {
            throw new CliException("CSV data directory does not exist or is not a directory: " + dataDir);
        }
        List<Conversion> conversions = new ArrayList<>();
        boolean canFix = true;
        try (var paths = Files.walk(dataDir)) {
            for (Path file : paths.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".csv"))
                    .sorted().toList()) {
                if (!check(file, target, sources, icuNames, conversions)) {
                    canFix = false;
                }
            }
            if (fix && canFix) {
                applyConversions(conversions, target);
            } else if (fix) {
                Logger.log("No changes written: resolve the skipped CSVs before updating .cfggen-data.");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot process CSVs under " + dataDir, e);
        }
    }

    private record Conversion(Path file, byte[] original, byte[] converted) {}

    private boolean check(Path file, Charset target, Set<Charset> sources, Set<String> icuNames,
                          List<Conversion> conversions) throws IOException {
        Path name = dataDir.relativize(file);
        byte[] bytes;
        try (var in = new BufferedInputStream(Files.newInputStream(file))) {
            in.mark(4);
            Charset bom = UnicodeReader.bomCharset(in.readNBytes(4));
            if (bom != null) {
                Logger.log("BOM %s, skipped: %s", bom.name(), name);
                return true;
            }
            in.reset();
            bytes = in.readAllBytes();
        }

        String candidates = Arrays.stream(new CharsetDetector().setText(bytes).detectAll())
                .filter(match -> icuNames.contains(match.getName()))
                .map(match -> match.getName() + " " + match.getConfidence())
                .collect(Collectors.joining(", "));
        if (candidates.isEmpty()) {
            candidates = "no match among source encodings";
        }
        var decoded = new LinkedHashMap<String, Charset>();
        for (Charset charset : sources) {
            try {
                String text = decode(bytes, charset);
                decoded.putIfAbsent(text, charset);
            } catch (CharacterCodingException ignored) {
                // Invalid input for this source charset.
            }
        }
        if (decoded.size() != 1) {
            String reason = decoded.isEmpty() ? "no supported source decoded"
                    : "ambiguous source " + decoded.values();
            Logger.log("Skipped: %s (%s). ICU4J: %s", name, reason, candidates);
            return false;
        }

        var entry = decoded.firstEntry();
        String text = entry.getKey();
        byte[] converted;
        try {
            ByteBuffer encoded = target.newEncoder().encode(CharBuffer.wrap(text));
            converted = new byte[encoded.remaining()];
            encoded.get(converted);
            if (!decode(converted, target).equals(text)) {
                Logger.log("Skipped: %s (text changes after %s round trip). ICU4J: %s",
                        name, target.name(), candidates);
                return false;
            }
        } catch (CharacterCodingException e) {
            Logger.log("Skipped: %s (text cannot be converted losslessly to %s). ICU4J: %s",
                    name, target.name(), candidates);
            return false;
        }
        if (Arrays.equals(bytes, converted)) {
            Logger.log("OK: %s. ICU4J: %s", name, candidates);
            return true;
        }
        if (fix) {
            conversions.add(new Conversion(file, bytes, converted));
        }
        Logger.log("%s: %s (%s -> %s). ICU4J: %s",
                fix ? "Ready to fix" : "Would fix", name, entry.getValue().name(), target.name(), candidates);
        return true;
    }

    private void applyConversions(List<Conversion> conversions, Charset target) throws IOException {
        int applied = 0;
        try {
            for (Conversion conversion : conversions) {
                replace(conversion.file(), conversion.original(), conversion.converted());
                applied++;
            }
            DataReadOptions.updateCsvEncoding(dataDir, target);
        } catch (IOException | RuntimeException e) {
            for (int index = applied - 1; index >= 0; index--) {
                Conversion conversion = conversions.get(index);
                try {
                    replace(conversion.file(), conversion.converted(), conversion.original());
                } catch (IOException | RuntimeException rollbackError) {
                    e.addSuppressed(rollbackError);
                }
            }
            throw e;
        }
        Logger.log("Fixed %d CSV file(s). Directory csvEncoding: %s", conversions.size(), target.name());
    }

    private static void replace(Path file, byte[] expected, byte[] replacement) throws IOException {
        FileUtil.writeReplacement(file, staged -> {
            Files.write(staged, replacement);
            if (!Arrays.equals(expected, Files.readAllBytes(file))) {
                throw new IOException("CSV changed during conversion: " + file);
            }
        });
    }

    private static String decode(byte[] bytes, Charset charset) throws CharacterCodingException {
        return charset.newDecoder().decode(ByteBuffer.wrap(bytes)).toString();
    }

    private static Set<String> icuNames(Set<Charset> sources) {
        Set<String> names = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Charset charset : sources) {
            names.add(charset.name());
            names.addAll(charset.aliases());
            switch (charset.name()) {
                case "GBK" -> names.add("GB18030");
                case "windows-31j" -> names.add("Shift_JIS");
                case "x-windows-949" -> names.add("EUC-KR");
                default -> { }
            }
        }
        return names;
    }

    private static @Nullable Charset parseCharset(@Nullable String name, String parameter) {
        if (name == null) {
            return null;
        }
        try {
            return Charset.forName(name.trim());
        } catch (IllegalArgumentException e) {
            throw new CliException("Invalid csvencoding parameter '" + parameter + "': " + name);
        }
    }

    private static Charset localeEncoding(Locale locale) {
        return switch (locale.getLanguage()) {
            case "zh" -> switch (locale.getCountry()) {
                case "TW", "HK", "MO" -> Charset.forName("Big5");
                default -> locale.getScript().equals("Hant") ? Charset.forName("Big5") : GBK;
            };
            case "ja" -> Charset.forName("windows-31j");
            case "ko" -> Charset.forName("x-windows-949");
            default -> StandardCharsets.UTF_8;
        };
    }
}
