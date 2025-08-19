package configgen.genlua;

import configgen.i18n.LangSwitchable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class AContext {
    private static final AContext instance = new AContext();

    static AContext getInstance() {
        return instance;
    }

    private String pkgPrefixStr;
    private LangSwitchSupport nullableLangSwitchSupport;
    private boolean sharedEmptyTable;
    private boolean shared;
    private boolean packBool;
    private boolean noStr; //只用于测试

    private String emptyTableStr;
    private String listMapPrefixStr;
    private String listMapPostfixStr;

    private final Set<String> forbidLocalNames = new HashSet<>(Arrays.asList("Beans", "this", "mk",
                                                                             "A", //表示共享Table
                                                                             "E", //表示emptyTable
                                                                             "R"  //表示为共享Table的一个包装方法 --> 后改为list，map的封装，用于检测修改
    ));

    private AStat statistics;

    void init(String pkg, LangSwitchable ls, boolean shareEmptyTable, boolean share,
              boolean packBool, boolean noStr, boolean rForOldShared) {

        nullableLangSwitchSupport = ls != null ? new LangSwitchSupport(ls) : null;
        sharedEmptyTable = shareEmptyTable;
        shared = share;
        this.packBool = packBool;
        this.noStr = noStr;

        if (sharedEmptyTable) {
            emptyTableStr = "E";
        } else {
            emptyTableStr = "{}";
        }

        if (rForOldShared) {
            listMapPrefixStr = "{";
            listMapPostfixStr = "}";
        } else {
            listMapPrefixStr = "R({";
            listMapPostfixStr = "})";
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
