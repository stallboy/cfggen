package configgen.i18n;

import configgen.ctx.Context;
import configgen.gen.Generator;
import configgen.gen.Parameter;
import configgen.util.Logger;
import configgen.value.CfgValue;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;


/**
 * by Pk and FieldChain
 * # 翻译工作流:
 * - 生成翻译文件.bat时会创建TODO文件，文件名称为_todo_[lang].xlsx，[lang]是语言代码（就是目录名称）
 * - TODO文件包含2个分页，一个是todo，含还没翻译的文本。一个是done(参考用)是已经翻译的文本。
 * 《1》每次生成完后，把发给翻译人员，翻译人员填todo分页的就行了。（它可以参考done里已经翻译的文本）
 * 《2》翻译人员返回TODO文件后，我们把它放到对应目录下，点生成客户端.bat等 TODO文件会直接起效。
 *     点生成翻译文件.bat则会把现在TODO文件的todo分页内容填回到具体的比如item.xlsx文件，
 *     同时TODO文件会再次更新，内含新的todo和done(参考用)分页。
 * 然后《1》《2》循环
 *  note: 也可以不用TODO文件，直接在具体的比如item.xlsx里填充翻译
 *  当一个翻译项在item.xlsx和TODO文件都存在时，优先用TODO里的，在TODO的翻译为空时，会用item.xlsx里的
 */
public final class GenI18nById extends Generator {
    private final String outputDir;
    private final String backupDir;
    private final boolean checkWrite;
    private final I18nStat stat = new I18nStat();

    public GenI18nById(Parameter parameter) {
        super(parameter);
        outputDir = parameter.get("dir", "../i18n/en");
        backupDir = parameter.get("backup", "../backup");
        checkWrite = parameter.has("checkWrite");
    }

    @Override
    public void generate(Context ctx) throws IOException {
        CfgValue cfgValue = ctx.makeValue();
        LangText extracted = LangText.extract(cfgValue);

        // 我们用的这个fastexcel lib同样内容写入xlsx，文件会不同，
        // xlsx里面的core.xml里有dcterms:created，是每次创建时时间，所以每次生成文件内容都不同
        // 而我们又希望内容不同时才提交svn，所以需要如下逻辑，内容比较
        String readI18nFilename = ctx.getContextCfg().i18nFilename();
        boolean needReplace = readI18nFilename != null && readI18nFilename.equals(outputDir);

        // 确保无temp目录，然后创建
        Path outputDirPath = Path.of(outputDir);
        String lang = outputDirPath.getFileName().toString();
        Path outputDirTempPath = Path.of(backupDir, lang + "_temp");
        String outputDirTemp = outputDirTempPath.normalize().toString();
        if (Files.isDirectory(outputDirTempPath)) {
            throw new RuntimeException("temp directory = %s exist, delete it then retry".formatted(outputDirTemp));
        }
        if (needReplace) {
            if (!new File(outputDirTemp).mkdirs()) {
                throw new RuntimeException("make temp directory = %s failed".formatted(outputDirTemp));
            }
        }

        // 如果不是覆盖，就直接输出到outputDir，如果是覆盖，就输出到temp
        Path wrotePath = needReplace ? outputDirTempPath : outputDirPath;
        extracted.save(wrotePath, stat);
        stat.dump();

        if (needReplace && checkWrite) {
            // 测试：fastexcel的xlsx文件写入是否正确（用再读取一次，然后比较的方式）, 可以不做
            LangText wrote = LangText.ofFinder(TextByIdFinder.loadOneLang(wrotePath));
            if (!wrote.equalsWithLog(extracted)) {
                throw new RuntimeException("wrote files not match extracted files, SHOULD NOT HAPPEN");
            }
        }

        String outputDirBackup = Path.of(backupDir, lang).normalize().toString();
        // 是覆盖，此时需要先备份原有的，然后temp->outputDir（通过3实现内容相同，就用原有的）
        if (needReplace) {
            // 1.先把 <outputDir> -> <outputDirBackup>
            Utils.moveDirFilesToAnotherDir(outputDir, outputDirBackup);

            // 2.然后把<outputDirTemp> -> <outputDir>
            Utils.moveDirFilesToAnotherDir(outputDirTemp, outputDir);
            if (!new File(outputDirTemp).delete()) {
                throw new RuntimeException("delete temp directory = %s failed".formatted(outputDirTemp));
            }

            // 3.最后把内容相同的file 从 <outputDirBackup> ---拷贝到---> <outputDir>
            for (var e : extracted.entrySet()) {
                String topModule = e.getKey();
                String fn = topModule + ".xlsx";
                Map<String, TextByIdFinder> cur = e.getValue();
                Path dstPath = Path.of(outputDir, fn);
                Path backupFile = Path.of(outputDirBackup, fn);
                if (Files.exists(backupFile)) {
                    Map<String, TextByIdFinder> oldInBackup = TextByIdFinder.loadOneFile(backupFile);
                    if (oldInBackup.equals(cur)) {
                        Files.copy(backupFile, dstPath, StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.COPY_ATTRIBUTES);
                    } else {
                        Logger.log(("modify ") + dstPath.toAbsolutePath().normalize());
                    }
                } else {
                    Logger.log(("create ") + dstPath.toAbsolutePath().normalize());
                }
            }
        }

        // 额外生成一份_todo_[lang].xlsx汇总文件，包含todo和done两个分页，
        {
            String todoFileName = Todo.getTodoFileName(lang);
            Path todoFilePath = outputDirPath.resolve(todoFileName);
            Todo todo = Todo.ofLangText(extracted);

            boolean isKeepSame = false;
            boolean exist = false;
            if (needReplace) {
                Path backupFile = Path.of(outputDirBackup, todoFileName);
                if (Files.exists(backupFile)) {
                    Todo oldInBackup = Todo.read(backupFile.toFile());
                    if (oldInBackup.equals(todo)) {
                        Files.copy(backupFile, todoFilePath, StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.COPY_ATTRIBUTES);
                        isKeepSame = true;
                    }
                    exist = true;
                }
            }
            if (!isKeepSame) {
                try (OutputStream os = new BufferedOutputStream(new FileOutputStream(todoFilePath.toFile()))) {
                    todo.save(os);
                }
                Logger.log("%s %s", exist ? "modify" : "create", todoFilePath.toAbsolutePath().normalize());
            }
        }
    }

    private static void generateForValue(CfgValue value, String lang, String outputDir, String backupDir) throws IOException {

    }

}
