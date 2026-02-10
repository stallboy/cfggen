package configgen.genlua;

import configgen.ctx.Context;
import configgen.gen.GeneratorWithTag;
import configgen.gen.Parameter;
import configgen.schema.*;
import configgen.util.CachedFiles;
import configgen.util.CachedIndentPrinter;
import configgen.util.CachedIndentPrinter.CacheConfig;
import configgen.util.FileUtil;
import configgen.value.CfgValue;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import static configgen.value.CfgValue.VStruct;
import static configgen.value.CfgValue.VTable;

public class LuaCodeGenerator extends GeneratorWithTag {
    private final String dir;
    private final String pkg;
    private final String encoding;
    private final boolean useEmmyLua;
    private String setHandlerName;
    private String handlerName;
    private final boolean preload;
    private final boolean useShared;
    private final boolean useSharedEmptyTable;
    private final boolean packBool;

    private CfgValue cfgValue;
    private CfgSchema cfgSchema;
    private Path dstDir;
    private CacheConfig cacheConfig;
    private boolean isLangSwitch;
    private final boolean noStr;
    private final boolean rForOldShared;

    public LuaCodeGenerator(Parameter parameter) {
        super(parameter);
        dir = parameter.get("dir", ".");
        pkg = parameter.get("pkg", "cfg");
        encoding = parameter.get("encoding", "UTF-8");

        useEmmyLua = parameter.has("emmylua");
        if (useEmmyLua) {
            setHandlerName = parameter.get("sethandlername", null);
            handlerName = parameter.get("handlername", null);
        }
        preload = parameter.has("preload");
        useSharedEmptyTable = parameter.has("sharedemptytable");
        useShared = parameter.has("shared");

        packBool = parameter.has("packbool");
        rForOldShared = parameter.has("rforoldshared");
        noStr = parameter.has("nostr");
    }

    @Override
    public void generate(Context ctx) throws IOException {
        AContext.getInstance().init(pkg, ctx.nullableLangSwitch(), useSharedEmptyTable, useShared,
                packBool, noStr, rForOldShared);
        isLangSwitch = AContext.getInstance().nullableLangSwitchSupport() != null;

        dstDir = Paths.get(dir).resolve(pkg.replace('.', '/'));
        cacheConfig = CacheConfig.of(512 * 1024); //优化gc alloc

        cfgValue = ctx.makeValue(tag);
        cfgSchema = cfgValue.schema();


        try (var ps = createCode("_cfgs.lua")) {
            generate_cfgs(ps);
        }
        if (preload) {
            try (var ps = createCode("_loads.lua")) {
                generate_loads(ps);
            }
        }
        try (var ps = createCode("_beans.lua")) {
            generate_beans(ps);
        }

        StringBuilder lineCache = new StringBuilder(256);

        for (VTable v : cfgValue.sortedTables()) {
            try (var ps = createCode(Name.tablePath(v.name()))) {
                try {
                    generate_table(v, ps, lineCache);
                } catch (Throwable e) {
                    throw new AssertionError("ERR generating lua code for " + v.name(), e);
                }
            }
        }

        AContext.getInstance().getStatistics().print();

        if (AContext.getInstance().nullableLangSwitchSupport() != null) {
            Map<String, List<String>> lang2Texts = AContext.getInstance().nullableLangSwitchSupport().getLang2Texts();
            for (Map.Entry<String, List<String>> e : lang2Texts.entrySet()) {
                String lang = e.getKey();
                List<String> texts = e.getValue();
                try (var ps = createCode(lang + ".lua")) {
                    generate_lang(ps, texts, lineCache);
                }
            }
        }

        FileUtil.copyFileIfNotExist("/support/lua/mkcfg.lua",
                "src/main/resources/support/mkcfg/mkcfg.lua",
                dstDir.resolve("mkcfg.lua"),
                encoding);

        CachedFiles.keepMetaAndDeleteOtherFiles(dstDir.toFile());
    }

    private CachedIndentPrinter createCode(String fn) {
        return cacheConfig.printer(dstDir.resolve(fn), encoding);
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

            // 因为package.loaded[_last_lang] = nil 可能导致_last_lang的c#对应的实际文件内存被删除，再require就出错了，所以注释掉
//            ps.println1("if %s._last_lang then", pkg);
//            ps.println2("package.loaded[\"%s.\" .. %s._last_lang] = nil", pkg, pkg);
//            ps.println1("end");

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
                        if (setHandlerName != null && handlerName != null) {
                            ps.println("---@field %s fun(self:%s, ...)", handlerName, full);
                        }
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
                            if (setHandlerName != null && handlerName != null) {
                                ps.println("---@field %s fun(%s :fun)", setHandlerName, handlerName);
                                ps.println("---@field %s fun(self: %s, ...)", handlerName, fulln);
                            }
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

        if (isLangSwitch) {
            AContext.getInstance().nullableLangSwitchSupport().enterTable(table.name());
        }

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
        if (m instanceof Metadata.MetaInt(int value)) {
            extraSplit = value;
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


        int extraFileCnt;
        {
            ///////////////////////////////////// 正常模式
            extraFileCnt = ps.enableCache(extraSplit, vTable.valueList().size());

            boolean hasLangSwitchAndText = AContext.getInstance().nullableLangSwitchSupport() != null &&
                    HasText.hasText(vTable.schema());

            if (!hasLangSwitchAndText) {
                // 无多语言切换，或此table没有text，这直接不用走pk & field chain做id，去取多语言翻译
                ValueStringify stringify = new ValueStringify(lineCache, ctx, "mk", null);
                for (VStruct vStruct : vTable.valueList()) {
                    lineCache.setLength(0);
                    stringify.addValue(vStruct, List.of());
                    ps.println(lineCache.toString());
                }
            } else {
                // pk & field chain做id, 取多语言翻译
                for (Map.Entry<CfgValue.Value, VStruct> e : vTable.primaryKeyMap().entrySet()) {
                    CfgValue.Value pk = e.getKey();
                    VStruct vStruct = e.getValue();
                    lineCache.setLength(0);
                    new ValueStringify(lineCache, ctx, "mk", pk.packStr()).addValue(vStruct, List.of());
                    ps.println(lineCache.toString());
                }
            }

        }

        ps.disableCache();

        generate_sharedLocalNamesAndER(ps, ctx);

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

            try (var extraPs = createCode(Name.tableExtraPath(vTable.name(), extraIdx + 1))) {

                extraPs.println("local %s = require \"%s._cfgs\"", pkg, pkg);
                if (HasSubFieldable.hasSubFieldable(table)) {
                    extraPs.println("local Beans = %s._beans", pkg);
                }
                extraPs.println();

                generate_sharedLocalNamesAndER(extraPs, ctx);

                extraPs.println("return function(mk)");
                ps.printExtraCacheTo(extraPs, extraIdx);
                extraPs.println("end");
            }
        }

        ps.println();
        ps.println("return this");
    }

    private void generate_sharedLocalNamesAndER(CachedIndentPrinter ps, Ctx ctx) {
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
    }
}
