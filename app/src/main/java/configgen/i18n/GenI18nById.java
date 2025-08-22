package configgen.i18n;

import configgen.ctx.Context;
import configgen.gen.Generator;
import configgen.gen.Parameter;
import configgen.util.Logger;
import configgen.value.CfgValue;
import configgen.value.TextValue;


import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;


/**
 * 翻译工作流说明
 *  byId的准确含义是： ByPkAndFieldChain
 * 翻译文件生成流程：
 * - 执行"生成翻译文件.bat"时会创建TODO文件，命名格式为：_todo_[lang].xlsx（[lang]为语言代码，即目录名称）
 * - TODO文件包含两个工作表：
 *     1. todo：包含待翻译文本
 *     2. done：包含已翻译文本（供参考）
 * 工作流程：
 * 《1》生成TODO文件后，发送给翻译人员。翻译人员只需填写todo工作表中的内容（可参考done工作表中的已有翻译）
 * 《2》翻译完成后，将TODO文件放回对应目录。执行"生成客户端.bat"等操作时，系统会自动识别并使用TODO文件中的翻译内容
 * 执行"生成翻译文件.bat"时：
 * - 将TODO文件中todo工作表的内容回填到具体的翻译文件（如item.xlsx）
 * - 同时更新TODO文件，包含新的待翻译内容和已完成内容（供参考）
 * 随后重复《1》《2》循环流程
 * 注意：
 * - 也可跳过TODO文件，直接在具体翻译文件（如item.xlsx）中填写翻译内容
 * - 当同一翻译项在item.xlsx和TODO文件中同时存在时，优先采用TODO文件中的翻译内容
 * - 若TODO文件中翻译内容为空，则回退使用item.xlsx中的翻译内容
 */
public final class GenI18nById extends Generator {
    private final Path outputDir;
    private final Path backupDir;
    private final boolean checkWrite;

    public GenI18nById(Parameter parameter) {
        super(parameter);
        outputDir = Path.of(parameter.get("dir", "../i18n/en"));
        backupDir = Path.of(parameter.get("backup", "../backup"));
        checkWrite = parameter.has("checkWrite");
    }

    @Override
    public void generate(Context ctx) throws IOException {
        CfgValue cfgValue = ctx.makeValue();


        if (ctx.nullableLangSwitch() != null) {
            // -langSwitchDir 对应的outputDir是单一语言的上层目录
            for (var e : ctx.nullableLangSwitch().langMap().entrySet()) {
                String lang = e.getKey();
                LangTextFinder langFinder = e.getValue();
                TextValue.setTranslated(cfgValue, langFinder);
                generateForValueWithLog(cfgValue, lang, checkWrite, outputDir, backupDir);
            }

        } else {
            // -i18nfile 对应的outputDir是单一语言的目录
            String lang = outputDir.getFileName().toString();
            Path langsDir = outputDir.getParent();
            generateForValueWithLog(cfgValue, lang, checkWrite, langsDir, backupDir);
        }
    }

    private static void generateForValueWithLog(CfgValue cfgValue,
                                                String lang,
                                                boolean checkWrite,
                                                Path langsDir,
                                                Path backupDir) throws IOException {
        try (PrintStream verboseStream = new PrintStream("log_" + lang + ".txt")) {
            PrintStream old = Logger.setVerboseStream(verboseStream);
            generateForValue(cfgValue, lang, checkWrite, langsDir, backupDir);
            Logger.setVerboseStream(old);
        }
    }

    private static void generateForValue(CfgValue cfgValue,
                                         String lang,
                                         boolean checkWrite,
                                         Path langsDir,
                                         Path backupDir) throws IOException {
        LangText extracted = LangText.extract(cfgValue);

        // 我们用的这个fastexcel lib同样内容写入xlsx，文件会不同，
        // xlsx里面的core.xml里有dcterms:created，是每次创建时时间，所以每次生成文件内容都不同
        // 而我们又希望内容不同时才提交svn，所以需要如下逻辑，内容比较
        Path outputDir = langsDir.resolve(lang);
        Path outputTempDir = backupDir.resolve(lang + "_temp");
        Path outputBackupDir = backupDir.resolve(lang);

        boolean needReplace = Utils.hasFiles(outputDir);
        // 确保无temp目录，然后创建
        if (Files.isDirectory(outputTempDir)) {
            throw new RuntimeException("temp directory = %s exist, delete it then retry".formatted(outputTempDir));
        }
        if (needReplace) {
            if (!outputTempDir.toFile().mkdirs()) {
                throw new RuntimeException("make temp directory = %s failed".formatted(outputTempDir));
            }
        }

        // 如果不是覆盖，就直接输出到outputDir，如果是覆盖，就输出到temp
        LangStat stat = new LangStat();
        Path wrotePath = needReplace ? outputTempDir : outputDir;
        extracted.save(wrotePath, stat);
        stat.dump();

        if (needReplace && checkWrite) {
            // 测试：fastexcel的xlsx文件写入是否正确（用再读取一次，然后比较的方式）, 可以不做
            LangText wrote = LangText.ofFinder(TextByIdFinder.loadOneLang(wrotePath));
            if (!wrote.equalsWithLog(extracted)) {
                throw new RuntimeException("wrote files not match extracted files, SHOULD NOT HAPPEN");
            }
        }

        String todoFileName = Todo.getTodoFileName(lang);
        Path todoFilePath = langsDir.resolve(todoFileName);
        Path todoInBackup = backupDir.resolve(todoFileName);
        // 是覆盖，此时需要先备份原有的，然后temp->outputDir（通过3实现内容相同，就用原有的）
        if (needReplace) {
            // 1.先把 <outputDir> -> <outputBackupDir>
            Utils.moveDirFilesToAnotherDir(outputDir, outputBackupDir);
            Utils.moveOneFile(todoFilePath, todoInBackup);

            // 2.然后把<outputTempDir> -> <outputDir> ，删除<outputTempDir>
            Utils.moveDirFilesToAnotherDir(outputTempDir, outputDir);
            if (!outputTempDir.toFile().delete()) {
                throw new RuntimeException("delete temp directory = %s failed".formatted(outputTempDir));
            }

            // 3.最后把内容相同的file 从 <outputBackupDir> ---拷贝到---> <outputDir>
            for (var e : extracted.entrySet()) {
                String fn = e.getKey() + ".xlsx";
                Map<String, TextByIdFinder> cur = e.getValue();
                Path dstFile = outputDir.resolve(fn);
                Path backupFile = outputBackupDir.resolve(fn);
                if (Files.exists(backupFile)) {
                    Map<String, TextByIdFinder> oldInBackup = TextByIdFinder.loadOneFile(backupFile);
                    if (oldInBackup.equals(cur)) {
                        Files.copy(backupFile, dstFile, StandardCopyOption.REPLACE_EXISTING,
                                StandardCopyOption.COPY_ATTRIBUTES);
                    } else {
                        Logger.log(("modify ") + dstFile.toAbsolutePath().normalize());
                    }
                } else {
                    Logger.log(("create ") + dstFile.toAbsolutePath().normalize());
                }
            }
        }

        // 额外生成一份_todo_[lang].xlsx汇总文件，跟[lang]文件夹在同一级目录，方便多个语言_todo文件一起选择打包发送
        Todo todo = Todo.ofLangText(extracted);
        boolean isKeepSame = false;
        boolean exist = false;
        if (needReplace) {
            if (Files.exists(todoInBackup)) {
                Todo oldInBackup = Todo.read(todoInBackup.toFile());
                if (oldInBackup.equals(todo)) {
                    Files.copy(todoInBackup, todoFilePath, StandardCopyOption.REPLACE_EXISTING,
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
