package configgen.genjava.code;

import configgen.gen.Context;
import configgen.gen.Generator;
import configgen.gen.Parameter;
import configgen.genjava.GenJavaUtil;
import configgen.schema.*;
import configgen.util.CachedFiles;
import configgen.util.CachedIndentPrinter;
import configgen.util.Logger;
import configgen.value.CfgValue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static configgen.value.CfgValue.VTable;

public class GenJavaCode extends Generator {
    private File dstDir;
    private final String dir;
    private final String pkg;
    private final String encoding;
    private final String buildersFilename;
    private Set<String> needBuilderTables = null;
    private final int schemaNumPerFile;


    public GenJavaCode(Parameter parameter) {
        super(parameter);
        dir = parameter.get("dir", "config", "目录");
        pkg = parameter.get("pkg", "config", "包名");
        encoding = parameter.get("encoding", "UTF-8", "生成代码文件的编码");
        buildersFilename = parameter.get("builders", null, "对这些table生成对应的builder，默认为null");
        schemaNumPerFile = Integer.parseInt(
                parameter.get("schemaNumPerFile", "100",
                        "当配表数量过多时生成的ConfigCodeSchema会超过java编译器限制，用此参数来分文件"));

        if (buildersFilename != null) {
            readNeedBuilderTables();
        }
        parameter.end();
    }

    private void readNeedBuilderTables() {
        Path fn = Path.of(buildersFilename).normalize();
        if (Files.exists(fn)) {
            try {
                needBuilderTables = new HashSet<>();
                List<String> lines = Files.readAllLines(fn, StandardCharsets.UTF_8);
                needBuilderTables.addAll(lines);
            } catch (IOException e) {
                Logger.log(STR. "读\{ fn.toAbsolutePath() }文件异常, 忽略此文件" );
            }
        }
    }


    @Override
    public void generate(Context ctx) throws IOException {

        CfgValue cfgValue = ctx.makeValue(tag);
        dstDir = Paths.get(dir).resolve(pkg.replace('.', '/')).toFile();

        Name.codeTopPkg = pkg;
        GenStructuralClassTablePart.mapsInMgr.clear();
        boolean isLangSwitch = ctx.getLangSwitch() != null;
        TypeStr.isLangSwitch = isLangSwitch; //辅助Text的类型声明和创建

        for (Nameable nameable : cfgValue.schema().items()) {
            switch (nameable) {
                case StructSchema structSchema -> {
                    generateStructClass(structSchema);
                }
                case InterfaceSchema interfaceSchema -> {
                    generateInterfaceClass(interfaceSchema);
                    for (StructSchema impl : interfaceSchema.impls()) {
                        generateStructClass(impl);
                    }
                }
                case TableSchema _ -> {
                }
            }
        }
        for (VTable vtable : cfgValue.tables()) {
            generateTableClass(vtable);
        }

        if (isLangSwitch) { //生成Text这个Bean
            try (CachedIndentPrinter ps = createCode(new File(dstDir, "Text.java"), encoding)) {
                GenText.generate(ctx.getLangSwitch(), ps);
            }
        }

        try (CachedIndentPrinter ps = createCode(new File(dstDir, "ConfigMgr.java"), encoding)) {
            GenConfigMgr.generate(ps);
        }

        try (CachedIndentPrinter ps = createCode(new File(dstDir, "ConfigLoader.java"), encoding)) {
            GenConfigLoader.generate(ps);
        }

        try (CachedIndentPrinter ps = createCode(new File(dstDir, "ConfigMgrLoader.java"), encoding)) {
            GenConfigMgrLoader.generate(cfgValue, ps);
        }

        GenConfigCodeSchema.generateAll(this, schemaNumPerFile, cfgValue, ctx.getLangSwitch());

        CachedFiles.deleteOtherFiles(dstDir);
    }

    CachedIndentPrinter createCodeFile(String fileName) {
        return createCode(new File(dstDir, fileName), encoding);
    }


    private void generateStructClass(StructSchema struct) {
        NameableName name = new NameableName(struct);
        try (CachedIndentPrinter ps = createCode(dstDir.toPath().resolve(name.path).toFile(), encoding)) {
            GenStructuralClass.generate(struct, null, name, ps, false);
        }
    }

    private void generateInterfaceClass(InterfaceSchema interfaceSchema) {
        NameableName name = new NameableName(interfaceSchema);
        try (CachedIndentPrinter ps = createCode(dstDir.toPath().resolve(name.path).toFile(), encoding)) {
            GenInterface.generate(interfaceSchema, name, ps);
        }
    }

    private void generateTableClass(VTable vTable) {
        boolean isNeedReadData = true;
        String dataPostfix = "";
        TableSchema schema = vTable.schema();
        if (schema.entry() instanceof EntryType.EntryBase entryBase) {
            String entryPostfix = "";
            boolean isEnum = entryBase instanceof EntryType.EEnum;
            if (isEnum) {
                if (GenJavaUtil.isEnumAndHasOnlyPrimaryKeyAndEnumStr(schema)) {
                    isNeedReadData = false;
                } else {
                    dataPostfix = "_Detail";
                }
            } else {
                entryPostfix = "_Entry";
            }

            NameableName name = new NameableName(schema, entryPostfix);
            NameableName dataName = new NameableName(schema, dataPostfix);
            File javaFile = dstDir.toPath().resolve(name.path).toFile();
            try (CachedIndentPrinter ps = createCode(javaFile, encoding)) {
                GenEntryOrEnumClass.generate(vTable, entryBase, name, ps, isNeedReadData, dataName);
            }
        }

        if (isNeedReadData) {
            NameableName name = new NameableName(schema, dataPostfix);
            boolean isTableNeedBuilder = needBuilderTables != null && needBuilderTables.contains(vTable.name());
            File javaFile = dstDir.toPath().resolve(name.path).toFile();
            try (CachedIndentPrinter ps = createCode(javaFile, encoding)) {
                GenStructuralClass.generate(vTable.schema(), vTable, name, ps, isTableNeedBuilder);
            }

            if (isTableNeedBuilder) {
                String builderPath = name.path.substring(0, name.path.length() - 5) + "Builder.java";
                File builderFile = dstDir.toPath().resolve(builderPath).toFile();
                try (CachedIndentPrinter ps = createCode(builderFile, encoding)) {
                    GenStructuralClassTablePart.generateTableBuilder(vTable.schema(), name, ps);
                }
            }

        }
    }

}
