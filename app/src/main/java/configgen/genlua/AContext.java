package configgen.genlua;

import configgen.i18n.LangSwitchable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/// 一次 lua 生成的全局配置与统计。原先是 static 单例（getInstance + init），并发生成时
/// 互相覆盖；现由 LuaCodeGenerator.generate() 每次构造一份实例，沿 Ctx 链传递。
class AContext {
    private final String pkgPrefixStr;
    private final LangSwitchSupport nullableLangSwitchSupport;
    private final boolean shared;
    private final boolean packBool;
    private final boolean noStr; //只用于测试

    private final String emptyTableStr;
    private final String listMapPrefixStr;
    private final String listMapPostfixStr;

    private final Set<String> forbidLocalNames = new HashSet<>(Arrays.asList("Beans", "this", "mk",
                                                                             "A", //表示共享Table
                                                                             "E", //表示emptyTable
                                                                             "R"  //表示为共享Table的一个包装方法 --> 后改为list，map的封装，用于检测修改
    ));

    private final AStat statistics;

    AContext(String pkg, LangSwitchable ls, boolean shareEmptyTable, boolean shared,
             boolean packBool, boolean noStr, boolean rForOldShared) {

        nullableLangSwitchSupport = ls != null ? new LangSwitchSupport(ls) : null;
        this.shared = shared;
        this.packBool = packBool;
        this.noStr = noStr;

        if (shareEmptyTable) {
            emptyTableStr = "E";
        } else {
            emptyTableStr = "{}";
        }

        if (rForOldShared) {
            listMapPrefixStr = "R({";
            listMapPostfixStr = "}";
        } else {
            listMapPrefixStr = "{";
            listMapPostfixStr = "}";
        }

        if (pkg.isEmpty()) {
            pkgPrefixStr = "";
        } else {
            pkgPrefixStr = pkg + ".";
            forbidLocalNames.add(pkg);
        }

        statistics = new AStat();
    }


    boolean isForbidName(String name) {
        return forbidLocalNames.contains(name);
    }

    LangSwitchSupport nullableLangSwitchSupport() {
        return nullableLangSwitchSupport;
    }

    boolean isShared() {
        return shared;
    }

    boolean isPackBool() {
        return packBool;
    }

    boolean isNoStr() {
        return noStr;
    }


    String getEmptyTableStr() {
        return emptyTableStr;
    }

    String getPkgPrefixStr() {
        return pkgPrefixStr;
    }


    public String getListMapPrefixStr() {
        return listMapPrefixStr;
    }

    public String getListMapPostfixStr() {
        return listMapPostfixStr;
    }


    AStat getStatistics() {
        return statistics;
    }

}
