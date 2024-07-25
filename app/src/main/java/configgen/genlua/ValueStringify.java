package configgen.genlua;

import java.util.*;

import static configgen.value.CfgValue.*;

class ValueStringify {

    private static final Set<String> keywords = new HashSet<>(Arrays.asList(
            "break", "goto", "do", "end", "for", "in", "repeat", "util", "while",
            "if", "then", "elseif", "function", "local", "nil", "true", "false"));


    static void getLuaString(StringBuilder res, String value) {
        String val = toLuaStringLiteral(value);
        res.append("\"").append(val).append("\"");
    }

    private static String toLuaStringLiteral(String value) {
        String val = value.replace("\r\n", "\\n");
        val = val.replace("\n", "\\n");
        val = val.replace("\"", "\\\"");
        return val;
    }

    //////////////////////////////////////////// per vtable

    private final StringBuilder res;
    private final Ctx ctx;
    private final String beanTypeStr;
    private final boolean isKey;

    private ValueStringify key;
    private ValueStringify notKey;

    ValueStringify(StringBuilder res, Ctx ctx, String beanTypeStr) {
        this.res = res;
        this.ctx = ctx;
        this.beanTypeStr = beanTypeStr;
        this.isKey = false;

        key = new ValueStringify(res, ctx, true);
        notKey = new ValueStringify(res, ctx, false);
        key.key = key;
        key.notKey = notKey;
        notKey.key = key;
        notKey.notKey = notKey;
    }

    private ValueStringify(StringBuilder res, Ctx ctx, boolean isKey) {
        this.res = res;
        this.ctx = ctx;
        this.beanTypeStr = null;
        this.isKey = isKey;
    }


    private void add(String val) {
        if (isKey) {
            res.append('[').append(val).append(']');
        } else {
            res.append(val);
        }
    }

    void addValue(Value value) {
        switch (value) {
            case VBool vBool -> add(vBool.value() ? "true" : "false");
            case VInt vInt -> add(String.valueOf(vInt.value()));
            case VLong vLong -> add(String.valueOf(vLong.value()));
            case VFloat vFloat -> add(String.valueOf(vFloat.value()));
            case VString vStr -> addString(vStr.value());
            case VText vText -> addVText(vText);
            case VStruct vStruct -> addVStruct(vStruct, null);
            case VInterface vInterface -> addVInterface(vInterface);
            case VList vList -> addVList(vList);
            case VMap vMap -> addVMap(vMap);
        }
    }

    private void addVText(VText value) {
        if (AContext.getInstance().nullableLangSwitchSupport() != null && !isKey) { // text字段仅用于asValue，不能用于asKey
            int id = AContext.getInstance().nullableLangSwitchSupport().enterText(value.value()) + 1;
            res.append(id);
        }else{
            addString(value.value());
        }
    }

    private void addString(String string) {
        String val = toLuaStringLiteral(string);
        if (isKey) {
            if (keywords.contains(val) || val.contains("-") || val.contains("=") || val.contains(",")) {
                res.append("[\"").append(val).append("\"]");
            } else {
                res.append(val);
            }
        } else {
            if (AContext.getInstance().isNoStr()) {
                res.append("''");
            } else {
                res.append("\"").append(val).append("\"");
            }
        }
    }


    private void addVList(VList value) {
        int sz = value.valueList().size();
        if (sz == 0) { //优化，避免重复创建空table
            ctx.ctxShared().incEmptyTableUseCount();
            res.append(AContext.getInstance().getEmptyTableStr());

        } else {
            String vstr = getSharedCompositeBriefName(value);
            if (vstr != null) { //优化，重用相同的table
                res.append(vstr);

            } else {
                ctx.ctxShared().incListTableUseCount();
                res.append(AContext.getInstance().getListMapPrefixStr());
                int idx = 0;
                for (Value eleValue : value.valueList()) {
                    notKey.addValue(eleValue);
                    idx++;
                    if (idx != sz) {
                        res.append(", ");
                    }
                }
                res.append(AContext.getInstance().getListMapPostfixStr());
            }
        }
    }

    private String getSharedCompositeBriefName(CompositeValue value) {
        if (value.isShared()) {
            return ctx.ctxShared().getSharedName(value); //优化，重用相同的table
        }
        return null;
    }


    private void addVMap(VMap value) {
        int sz = value.valueMap().size();
        if (sz == 0) { //优化，避免重复创建空table
            ctx.ctxShared().incEmptyTableUseCount();
            res.append(AContext.getInstance().getEmptyTableStr());

        } else {
            String vstr = getSharedCompositeBriefName(value);
            if (vstr != null) { //优化，重用相同的table
                res.append(vstr);

            } else {
                ctx.ctxShared().incMapTableUseCount();
                res.append(AContext.getInstance().getListMapPrefixStr());
                int idx = 0;
                for (Map.Entry<SimpleValue, SimpleValue> e : value.valueMap().entrySet()) {
                    key.addValue(e.getKey());
                    res.append(" = ");
                    notKey.addValue(e.getValue());
                    idx++;
                    if (idx != sz) {
                        res.append(", ");
                    }
                }
                res.append(AContext.getInstance().getListMapPostfixStr());
            }
        }
    }

    private void addVInterface(VInterface value) {
        addVStruct(value.child(), value);

    }

    private void addVStruct(VStruct val, VInterface nullableInterface) {
        CompositeValue value = nullableInterface != null ? nullableInterface : val;

        String beanType = beanTypeStr;
        if (beanType == null) {
            beanType = ctx.ctxName().getLocalName(Name.fullName(val.schema()));
        }

        String vstr = getSharedCompositeBriefName(value);
        if (vstr != null) { //优化，重用相同的table
            res.append(vstr);

        } else {
            AStat statistics = AContext.getInstance().getStatistics();
            if (beanTypeStr != null) {
                statistics.useRecordTable();
            } else if (nullableInterface != null) {
                statistics.useInterfaceTable();
            } else {
                statistics.useStructTable();
            }
            res.append(beanType);
            int sz = val.values().size();
            if (sz > 0) { // 这里来个优化，如果没有参数不加()，因为beanType其实直接就是个实例
                res.append("(");
                int idx = 0;
                boolean meetBool = false;
                boolean doPack = TypeStr.isDoPackBool(val.schema());
                for (Value fieldValue : val.values()) {
                    if (doPack && fieldValue instanceof VBool) { //从第一个遇到的bool开始搞
                        if (!meetBool) {
                            meetBool = true;

                            BitSet bs = new BitSet();
                            int cnt = 0;
                            for (Value fv : val.values()) {
                                if (fv instanceof VBool fbv) {
                                    if (fbv.value()) {
                                        bs.set(cnt);
                                    }
                                    cnt++;
                                }
                            }
                            idx += cnt;
                            statistics.usePackBool(cnt - 1);

                            long v = 0;
                            if (!bs.isEmpty()) {
                                v = bs.toLongArray()[0];
                            }
                            if (cnt < 32) {
                                res.append("0x").append(Long.toHexString(v));
                            } else {
                                res.append(v);
                            }

                            if (idx != sz) {
                                res.append(", ");
                            }


                        }
                    } else {
                        idx++;
                        notKey.addValue(fieldValue);
                        if (idx != sz) {
                            res.append(", ");
                        }
                    }


                }
                res.append(")");
            }
        }

    }
}
