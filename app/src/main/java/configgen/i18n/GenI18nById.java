package configgen.i18n;

import configgen.ctx.Context;
import configgen.gen.Generator;
import configgen.gen.Parameter;
import configgen.util.CachedFileOutputStream;
import configgen.util.Logger;
import configgen.value.CfgValue;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;


/**
 * by Pk and FieldChain
 */
public final class GenI18nById extends Generator {
    private final String outputDir;
    private final String backupDir;
    private final I18nStat stat = new I18nStat();

    public GenI18nById(Parameter parameter) {
        super(parameter);
        outputDir = parameter.get("dir", "../i18n/en");
        backupDir = parameter.get("backup", "../backup");
    }

    @Override
    public void generate(Context ctx) throws IOException {
        CfgValue cfgValue = ctx.makeValue();
        LangTextInfo extracted = LangTextInfoExtractor.extract(cfgValue);

        // 我们用的这个fastexcel lib同样内容写入xlsx，文件会不同，
        // xlsx里面的core.xml里有dcterms:created，是每次创建时时间，所以每次生成文件内容都不同
        // 而我们又希望内容不同时才提交svn，所以需要如下逻辑，内容比较
        LangTextInfo needReplace = null;
        String readI18nFilename = ctx.getContextCfg().i18nFilename();
        if (readI18nFilename != null && readI18nFilename.equals(outputDir)) {
            needReplace = LangTextInfo.of(ctx.nullableLangTextFinder().getMap());
        }

        // 确保无temp目录，然后创建
        Path outputDirPath = Path.of(outputDir);
        String lang = outputDirPath.getFileName().toString();
        Path outputDirTempPath = Path.of(backupDir, lang + "_temp");
        String outputDirTemp = outputDirTempPath.normalize().toString();
        if (Files.isDirectory(outputDirTempPath)) {
            throw new RuntimeException("temp directory = %s exist, delete it then retry".formatted(outputDirTemp));
        }
        if (needReplace != null) {
            if (!new File(outputDirTemp).mkdirs()) {
                throw new RuntimeException("make temp directory = %s failed".formatted(outputDirTemp));
            }
        }

        // 如果不是覆盖，就直接输出到outputDir，如果是覆盖，就输出到temp
        Path wrotePath = needReplace != null ? outputDirTempPath : outputDirPath;
        for (var e : extracted.entrySet()) {
            String topModuleFn = e.getKey() + ".xlsx";
            File dst = wrotePath.resolve(topModuleFn).toFile();
            TopModuleTextInfo topModule = e.getValue();
            try (OutputStream os = needReplace != null ? new FileOutputStream(dst) : new CachedFileOutputStream(dst)) {
                topModule.save(os, stat);
            }
        }

        { // 测试：fastexcel的xlsx文件写入是否正确（用再读取一次，然后比较的方式）
            LangTextFinder wrote = TextFinderById.loadOneLang(wrotePath);
            LangTextInfo wroteInfo = LangTextInfo.of(wrote.getMap());
            if (!wroteInfo.equals(extracted)) {
                throw new RuntimeException("wrote files not match extracted files, SHOULD NOT HAPPEN");
            }
        }

        // 是覆盖，此时需要先备份原有的，然后temp->outputDir（通过3实现内容相同，就用原有的）
        if (needReplace != null) {
            // 1.先把 <outputDir> -> <outputDirBackup>
            String outputDirBackup = Path.of(backupDir, lang).normalize().toString();
            moveDirFilesToAnotherDir(outputDir, outputDirBackup);

            // 2.然后把<outputDirTemp> -> <outputDir>
            moveDirFilesToAnotherDir(outputDirTemp, outputDir);
            if (!new File(outputDirTemp).delete()) {
                throw new RuntimeException("delete temp directory = %s failed".formatted(outputDirTemp));
            }

            // 3.最后把内容相同的file 从 <outputDirBackup> ---拷贝到---> <outputDir>
            for (var e : extracted.entrySet()) {
                String topModule = e.getKey();
                String fn = topModule + ".xlsx";
                TopModuleTextInfo cur = e.getValue();
                TopModuleTextInfo oldInBackup = needReplace.get(topModule);

                Path dstPath = Path.of(outputDir, fn);
                if (cur.equals(oldInBackup)) {
                    Files.copy(Path.of(outputDirBackup, fn), dstPath,
                            StandardCopyOption.REPLACE_EXISTING,
                            StandardCopyOption.COPY_ATTRIBUTES);
                } else {
                    Logger.log((oldInBackup != null ? "modify " : "create ") + dstPath.toAbsolutePath().normalize());
                }
            }
        }

        stat.dump();

        // 额外生成一份zzz.csv汇总文件，方便查询
        extracted.save(new File(outputDir, "zzz.csv"));
    }


    private static void moveDirFilesToAnotherDir(String from, String to) {
        File toDir = new File(to);
        if (toDir.isDirectory()) {
            for (File file : Objects.requireNonNull(toDir.listFiles())) {
                if (!file.delete()) {
                    throw new RuntimeException("delete " + file + " failed");
                }
            }
        } else {
            if (!toDir.mkdirs()) {
                throw new RuntimeException("mkdir " + toDir + " failed");
            }
        }
        for (File file : Objects.requireNonNull(new File(from).listFiles())) {
            if (!file.renameTo(new File(to, file.getName()))) {
                throw new RuntimeException("rename %s to %s failed".formatted(from, to));
            }
        }
    }


    static class GenOneTable {
        private final List<String> fields = new ArrayList<>();


    }

}
