package configgen.gen;

import configgen.ctx.Context;
import configgen.util.CachedFileOutputStream;
import configgen.util.CachedFiles;
import configgen.util.CachedIndentPrinter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class Generator {
    protected final Parameter parameter;


    /**
     * @param parameter 此接口有2个实现类，一个用于收集usage，一个用于实际参数解析
     *                  从而实现在各Generator的参数需求，只在构造函数里写一次就ok
     */
    public Generator(Parameter parameter) {
        this.parameter = parameter;
    }

    public abstract void generate(Context ctx) throws IOException;


    protected static CachedIndentPrinter createCode(File file, String encoding) {
        return new CachedIndentPrinter(file, encoding);
    }

    protected static CachedIndentPrinter createCode(File file, String encoding, StringBuilder dst, StringBuilder cache, StringBuilder tmp) {
        return new CachedIndentPrinter(file.toPath(), encoding, dst, cache, tmp);
    }

    public static OutputStreamWriter createUtf8Writer(File file) {
        return new OutputStreamWriter(new CachedFileOutputStream(file), StandardCharsets.UTF_8);
    }

    protected static void copySupportFileIfNotExist(String file, Path dstDir, String dstEncoding) throws IOException {
        Path dst = dstDir.resolve(file);
        if (Files.exists(dst)) {
            CachedFiles.keepFile(dst);
            return;
        }
        try (InputStream is = Generator.class.getResourceAsStream("/support/" + file);
             BufferedReader br = new BufferedReader(new InputStreamReader(is != null ? is : new FileInputStream("src/support/" + file), StandardCharsets.UTF_8));
             CachedIndentPrinter ps = new CachedIndentPrinter(dst, dstEncoding)) {
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                ps.println(line);
            }
        }
    }

    public static String upper1(String value) {
        return value.substring(0, 1).toUpperCase() + value.substring(1);
    }

    public static String upper1Only(String value) {
        return value.substring(0, 1).toUpperCase() + value.substring(1).toLowerCase();
    }

    public static String lower1(String value) {
        return value.substring(0, 1).toLowerCase() + value.substring(1);
    }


}
