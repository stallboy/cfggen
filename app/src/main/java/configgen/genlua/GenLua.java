package configgen.genlua;

import configgen.gen.Context;
import configgen.gen.Generator;
import configgen.gen.Parameter;
import configgen.schema.*;
import configgen.util.CachedFiles;
import configgen.util.CachedIndentPrinter;
import configgen.value.CfgValue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static configgen.value.CfgValue.VStruct;
import static configgen.value.CfgValue.VTable;

public class GenLua extends Generator {

    private final String dir;
    private final String pkg;
    private final String encoding;
    private final boolean useEmmyLua;
    private final boolean preload;
    private final boolean useShared;
    private final boolean useSharedEmptyTable;
    private final boolean packBool;

    private CfgValue cfgValue;
    private CfgSchema cfgSchema;
    private File dstDir;
    private boolean isLangSwitch;
    private final boolean noStr;
    private final boolean rForOldShared;

    public GenLua(Parameter parameter) {
        super(parameter);
        dir = parameter.get("dir", ".");
        pkg = parameter.get("pkg", "cfg");
        encoding = parameter.get("encoding", "UTF-8");

        useEmmyLua = parameter.has("emmylua");
        preload = parameter.has("preload");
        useSharedEmptyTable = parameter.has("sharedemptytable");
        useShared = parameter.has("shared");

        packBool = parameter.has("packbool");
        rForOldShared = parameter.has("rforoldshared");
        noStr = parameter.has("nostr");
        parameter.end();
    }

    @Override
    public void generate(Context ctx) throws IOException {
        AContext.getInstance().init(pkg, ctx.getLangSwitch(), useSharedEmptyTable, useShared,
                packBool, noStr, rForOldShared);
        isLangSwitch = AContext.getInstance().nullableLangSwitchSupport() != null;

        Path dstDirPath = Paths.get(dir).resolve(pkg.replace('.', '/'));
        dstDir = dstDirPath.toFile();
        cfgValue = ctx.makeValue(tag);
        cfgSchema = cfgValue.schema();

        StringBuilder fileDst = new StringBuilder(512 * 1024); //优化gc alloc
        StringBuilder cache = new StringBuilder(512 * 1024);
        StringBuilder tmp = new StringBuilder(128);
        try (CachedIndentPrinter ps = createCode(new File(dstDir, "_cfgs.lua"), encoding, fileDst, cache, tmp)) {
            generate_cfgs(ps);
        }
        if (preload) {
            try (CachedIndentPrinter ps = createCode(new File(dstDir, "_loads.lua"), encoding, fileDst, cache, tmp)) {
                generate_loads(ps);
            }
        }
        try (CachedIndentPrinter ps = createCode(new File(dstDir, "_beans.lua"), encoding, fileDst, cache, tmp)) {
            generate_beans(ps);
        }

        StringBuilder lineCache = new StringBuilder(256);

        for (VTable v : cfgValue.sortedTables()) {
            try (CachedIndentPrinter ps = createCode(new File(dstDir, Name.tablePath(v.name())), encoding, fileDst, cache, tmp)) {
                try {
                    generate_table(v, ps, lineCache);
                } catch (Throwable e) {
                    throw new AssertionError(v.name() + ",这个表生成lua代码出错", e);
                }
            }
        }

        AContext.getInstance().getStatistics().print();

        if (AContext.getInstance().nullableLangSwitchSupport() != null) {
            Map<String, List<String>> lang2Texts = AContext.getInstance().nullableLangSwitchSupport().getLang2Texts();
            for (Map.Entry<String, List<String>> e : lang2Texts.entrySet()) {
                String lang = e.getKey();
                List<String> texts = e.getValue();
                try (CachedIndentPrinter ps = createCode(new File(dstDir, lang + ".lua"), encoding, fileDst, cache, tmp)) {
                    generate_lang(ps, texts, lineCache);
                }
            }
            copyFile(dstDirPath, "mkcfg.lua", encoding);
        }


        CachedFiles.keepMetaAndDeleteOtherFiles(dstDir);
    }

    private void generate_lang(CachedIndentPrinter ps, List<String> idToStr, StringBuilder lineCache) {
        ps.println("return {");
        for (String str : idToStr) {
            lineCache.setLength(0);
            ValueStringify.getLuaString(lineCache, str);
            ps.println1(lineCache + ",");
        }
        ps.println("}");
    }


    private void generate_cfgs(CachedIndentPrinter ps) {
        ps.println("local %s = {}", pkg);
        ps.println();

        String mkcfgFrom = "common";
        if (isLangSwitch) {
            mkcfgFrom = pkg;
        }
        ps.println("%s._mk = require \"%s.mkcfg\"", pkg, mkcfgFrom);
        if (!preload) {
            ps.println("local pre = %s._mk.pretable", pkg);
        }
        ps.println();

        if (isLangSwitch) {
            ps.println("%s._last_lang = nil", pkg);
            ps.println("function %s._set_lang(lang)", pkg);
            ps.println1("if %s._last_lang == lang then", pkg);
            ps.println2("return");
            ps.println1("end");

            ps.println1("if %s._last_lang then", pkg);
            ps.println2("package.loaded[\"%s.\" .. %s._last_lang] = nil", pkg, pkg);
            ps.println1("end");

            ps.println1("%s._last_lang = lang", pkg);
            ps.println1("%s._mk.i18n = require(\"%s.\" .. lang)", pkg, pkg);
            ps.println("end");
            ps.println();
        }

        Set<String> context = new HashSet<>();
        context.add(pkg);
        for (TableSchema table : cfgValue.schema().sortedTables()) {
            String full = Name.fullName(table);
            definePkg(full, ps, context);

            if (useEmmyLua) {
                ps.println("---@type %s", full);
            }
            if (preload) {
                ps.println("%s = {}", full);
            } else {
                ps.println("%s = pre(\"%s\")", full, full);
            }
            context.add(full);
        }

        ps.println();
        ps.println("return %s", pkg);
    }


    private void definePkg(String beanName, CachedIndentPrinter ps, Set<String> context) {
        List<String> seps = Arrays.asList(beanName.split("\\."));
        for (int i = 0; i < seps.size() - 1; i++) {
            String pkg = String.join(".", seps.subList(0, i + 1));
            if (context.add(pkg)) {
                ps.println(pkg + " = {}");
            }
        }
    }

    private void generate_loads(CachedIndentPrinter ps) {
        ps.println("local require = require");
        ps.println();
        for (TableSchema table : cfgValue.schema().sortedTables()) {
            ps.println("require \"%s\"", Name.fullName(table));
        }
        ps.println();
    }


    private void generate_beans(CachedIndentPrinter ps) {
        ps.println("local %s = require \"%s._cfgs\"", pkg, pkg);
        ps.println();

        ps.println("local Beans = {}");
        ps.println("%s._beans = Beans", pkg);
        ps.println();
        ps.println("local bean = %s._mk.bean", pkg);
        ps.println("local action = %s._mk.action", pkg);

        if (isLangSwitch) {
            ps.println("local i18n_bean = %s._mk.i18n_bean", pkg);
            ps.println("local i18n_action = %s._mk.i18n_action", pkg);
        }
        ps.println();

        Set<String> context = new HashSet<>();
        context.add("Beans");
        for (Fieldable fieldable : cfgSchema.sortedFieldables()) {
            String full = Name.fullName(fieldable);
            definePkg(full, ps, context);
            context.add(full);

            switch (fieldable) {

                case InterfaceSchema sInterface -> {
                    if (useEmmyLua) {
                        ps.println("---@class %s", full);
                        ps.println();
                        ps.println("---@type %s", full);
                    }
                    ps.println("%s = {}", full);
                    ps.println();

                    for (StructSchema impl : sInterface.impls()) {
                        // function mkcfg.action(typeName, refs, ...)
                        String fulln = Name.fullName(impl);
                        definePkg(fulln, ps, context);
                        context.add(fulln);
                        String func = "action";
                        String textFieldsStr = "";
                        if (isLangSwitch) {
                            textFieldsStr = TypeStr.getLuaTextFieldsString(impl);
                            if (!textFieldsStr.isEmpty()) {
                                func = "i18n_action";
                            }
                        }

                        if (useEmmyLua) {
                            ps.println("---@class %s : %s", fulln, full);
                            ps.printlnIf(TypeStr.getLuaFieldsStringEmmyLua(impl));
                            ps.printlnIf(TypeStr.getLuaRefsStringEmmyLua(impl));
                            ps.println();
                            ps.println("---@type %s", fulln);
                        }

                        if (impl.fields().isEmpty()) {
                            //这里来个优化，加上()直接生成实例，而不是类，注意生成数据时对应不加()
                            ps.println("%s = %s(\"%s\")()", fulln, func, impl.name());
                        } else {
                            ps.println("%s = %s(\"%s\", %s, %s%s\n    )", fulln, func, impl.name(),
                                    TypeStr.getLuaRefsString(impl),
                                    textFieldsStr,
                                    TypeStr.getLuaFieldsString(impl));
                        }
                        ps.println();
                    }

                }
                case StructSchema struct -> {
                    String func = "bean";
                    String textFieldsStr = "";
                    if (isLangSwitch) {
                        textFieldsStr = TypeStr.getLuaTextFieldsString(struct);
                        if (!textFieldsStr.isEmpty()) {
                            func = "i18n_bean";
                        }
                    }

                    if (useEmmyLua) {
                        ps.println("---@class %s", full);
                        ps.printlnIf(TypeStr.getLuaFieldsStringEmmyLua(struct));
                        ps.printlnIf(TypeStr.getLuaRefsStringEmmyLua(struct));
                        ps.println();
                        ps.println("---@type %s", full);
                    }

                    if (struct.fields().isEmpty()) {
                        //这里来个优化，加上()直接生成实例，而不是类，注意生成数据时对应不加()
                        ps.println("%s = %s()()", full, func);
                    } else {
                        ps.println("%s = %s(%s, %s%s\n    )", full, func,
                                TypeStr.getLuaRefsString(struct),
                                textFieldsStr,
                                TypeStr.getLuaFieldsString(struct));
                    }
                    ps.println();
                }
            }
        }
        ps.println();
        ps.println("return Beans");
    }

    private void generate_table(VTable vTable, CachedIndentPrinter ps, StringBuilder lineCache) {
        TableSchema table = vTable.schema();

        ps.println("local %s = require \"%s._cfgs\"", pkg, pkg);
        if (HasSubFieldable.hasSubFieldable(table)) {
            ps.println("local Beans = %s._beans", pkg);
        }
        ps.println();

        String fullName = Name.fullName(table);
        if (useEmmyLua) {
            ps.println("---@class %s", fullName);
            ps.println(TypeStr.getLuaFieldsStringEmmyLua(table));
            ps.printlnIf(TypeStr.getLuaUniqKeysStringEmmyLua(table));
            ps.printlnIf(TypeStr.getLuaEnumStringEmmyLua(vTable));

            ps.println("---@field %s table<any,%s>", Name.primaryKeyMapName, fullName);
            ps.printlnIf(TypeStr.getLuaRefsStringEmmyLua(table));
            ps.println();
        }

        ps.println("local this = %s", fullName);
        ps.println();

        int extraSplit = 0;
        Metadata.MetaValue m = table.meta().get("extraSplit");
        if (m instanceof Metadata.MetaInt mi) {
            extraSplit = mi.value();
        }

        boolean tryUseShared = useShared && extraSplit == 0;

        Ctx ctx = new Ctx(vTable);
        if (tryUseShared) {
            ctx.parseShared();
        }

        String func = "table";
        String textFieldsStr = "";
        if (isLangSwitch) {
            textFieldsStr = TypeStr.getLuaTextFieldsString(table);
            if (!textFieldsStr.isEmpty()) {
                func = "i18n_table";
            }
        }

        // function mkcfg.table(self, uniqkeys, enumidx, refs, ...)
        ps.println("local mk = %s._mk.%s(this, %s, %s, %s, %s%s\n    )", pkg, func,
                TypeStr.getLuaUniqKeysString(ctx),
                TypeStr.getLuaEnumString(ctx),
                TypeStr.getLuaRefsString(table),
                textFieldsStr,
                TypeStr.getLuaFieldsString(table));
        ps.println();

        if (isLangSwitch) {
            AContext.getInstance().nullableLangSwitchSupport().enterTable(table.name());
        }


        int extraFileCnt;
        {
            ///////////////////////////////////// 正常模式
            ValueStringify stringify = new ValueStringify(lineCache, ctx, "mk");
            extraFileCnt = ps.enableCache(extraSplit, vTable.valueList().size());

            for (VStruct vStruct : vTable.valueList()) {
                lineCache.setLength(0);
                stringify.addValue(vStruct);
                ps.println(lineCache.toString());
            }
        }

        ps.disableCache();


        if (!ctx.ctxName().getLocalNameMap().isEmpty()) { // 对收集到的引用local化，lua执行会快点
            for (Map.Entry<String, String> entry : ctx.ctxName().getLocalNameMap().entrySet()) {
                ps.println("local %s = %s", entry.getValue(), entry.getKey());
            }
            ps.println();
        }

        boolean hasER = false;
        if (useSharedEmptyTable && ctx.ctxShared().getEmptyTableUseCount() > 0) { // 共享空表
            ps.println("local E = %s._mk.E", pkg);
            hasER = true;
        }
        if (!rForOldShared && ctx.ctxShared().hasListTableOrMapTable()) {
            ps.println("local R = %s._mk.R", pkg);
            hasER = true;
        }
        if (hasER) {
            ps.println();
        }

        if (tryUseShared && !ctx.ctxShared().getSharedList().isEmpty()) { // 共享相同的表
            if (rForOldShared) { //只为保持跟武林一致
                ps.println("local R = %s._mk.R", pkg); // 给lua个机会设置__newindex，做运行时检测
                ps.println("local A = {}");
                for (CtxShared.CompositeValueStr vstr : ctx.ctxShared().getSharedList()) {
                    ps.println("%s = R(%s)", vstr.getName(), vstr.getValueStr());
                }
            } else {
                ps.println("local A = {}");
                for (CtxShared.CompositeValueStr vstr : ctx.ctxShared().getSharedList()) {
                    ps.println("%s = %s", vstr.getName(), vstr.getValueStr());
                }
            }

            ps.println();
        }

        // 再打印cache
        ps.printCache();

        if (extraFileCnt > 0) {
            ps.println();
        }
        for (int extraIdx = 0; extraIdx < extraFileCnt; extraIdx++) {
            ps.println("require \"%s_%d\"(mk)", fullName, extraIdx + 1);

            try (CachedIndentPrinter extraPs = createCode(new File(dstDir, Name.tableExtraPath(vTable.name(), extraIdx + 1)), encoding)) {

                extraPs.println("local %s = require \"%s._cfgs\"", pkg, pkg);
                if (HasSubFieldable.hasSubFieldable(table)) {
                    extraPs.println("local Beans = %s._beans", pkg);
                }
                extraPs.println();

                if (!ctx.ctxName().getLocalNameMap().isEmpty()) { // 对收集到的引用local化，lua执行会快点
                    for (Map.Entry<String, String> entry : ctx.ctxName().getLocalNameMap().entrySet()) {
                        extraPs.println("local %s = %s", entry.getValue(), entry.getKey());
                    }
                    extraPs.println();
                }

                if (useSharedEmptyTable && ctx.ctxShared().getEmptyTableUseCount() > 0) { // 共享空表
                    extraPs.println("local E = %s._mk.E", pkg);
                    extraPs.println();
                }

                extraPs.println("return function(mk)");
                ps.printExtraCacheTo(extraPs, extraIdx);
                extraPs.println("end");
            }
        }

        ps.println();
        ps.println("return this");
    }
}
