package configgen.genlua;

import configgen.schema.FieldSchema;

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

    /// ///////////////////////////////////////// per vtable

    private final StringBuilder res;
    private final Ctx ctx;
    private final String beanTypeStr;
    private final String pkStr;
    private final boolean isKey;

    private ValueStringify key;
    private ValueStringify notKey;

    /**
     * @param pkStr 可为null。当设置时，此时AContext.getInstance().nullableLangSwitchSupport()！=null，并且此table含有text
     */
    public ValueStringify(StringBuilder res, Ctx ctx, String beanTypeStr, String pkStr) {
        this.res = res;
        this.ctx = ctx;
        this.beanTypeStr = beanTypeStr;
        this.pkStr = pkStr;
        if (pkStr != null) {
            if (AContext.getInstance().nullableLangSwitchSupport() == null) {
                throw new IllegalArgumentException("Don't set pkStr when no LangSwitch");
            }
        }
        this.isKey = false;

        key = new ValueStringify(res, ctx, pkStr, true);
        notKey = new ValueStringify(res, ctx, pkStr, false);
        key.key = key;
        key.notKey = notKey;
        notKey.key = key;
        notKey.notKey = notKey;
    }

    private ValueStringify(StringBuilder res, Ctx ctx, String pkStr, boolean isKey) {
        this.res = res;
        this.ctx = ctx;
        this.beanTypeStr = null;
        this.pkStr = pkStr;
        this.isKey = isKey;
    }


    private void add(String val) {
        if (isKey) {
            res.append('[').append(val).append(']');
        } else {
            res.append(val);
        }
    }

    private boolean hasLangSwitchAndText() {
        return pkStr != null;
    }

    private List<String> subChain(List<String> old, String e) {
        if (hasLangSwitchAndText()) {
            List<String> res = new ArrayList<>(old.size() + 1);
            res.addAll(old);
            res.add(e);
            return res;
        } else {
            return old; //但pkStr为null时，表示不需要LangSwitch
        }
    }

    public void addValue(Value value, List<String> fieldChain) {
        switch (value) {
            case VBool vBool -> add(vBool.value() ? "true" : "false");
            case VInt vInt -> add(String.valueOf(vInt.value()));
            case VLong vLong -> add(String.valueOf(vLong.value()));
            case VFloat vFloat -> add(String.valueOf(vFloat.value()));
            case VString vStr -> addString(vStr.value());
            case VText vText -> addVText(vText, fieldChain);
            case VStruct vStruct -> addVStruct(vStruct, null, fieldChain);
            case VInterface vInterface -> addVInterface(vInterface, fieldChain);
            case VList vList -> addVList(vList, fieldChain);
            case VMap vMap -> addVMap(vMap, fieldChain);
        }
    }

    /**********************************************************
     * 之后的实现，需要保证fieldChain的法则跟value.ForeachValue中一致
     */
    private void addVText(VText value, List<String> fieldChain) {
        if (hasLangSwitchAndText()) {
            int id = AContext.getInstance().nullableLangSwitchSupport().enterText(pkStr, fieldChain, value.value()) + 1;
            res.append(id);
        } else {
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


    private void addVList(VList value, List<String> fieldChain) {
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
                    notKey.addValue(eleValue, subChain(fieldChain, String.valueOf(idx)));
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


    private void addVMap(VMap value, List<String> fieldChain) {
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
                    key.addValue(e.getKey(), subChain(fieldChain, String.format("%dk", idx)));
                    res.append(" = ");
                    notKey.addValue(e.getValue(), subChain(fieldChain, String.format("%dv", idx)));
                    idx++;
                    if (idx != sz) {
                        res.append(", ");
                    }
                }
                res.append(AContext.getInstance().getListMapPostfixStr());
            }
        }
    }

    private void addVInterface(VInterface value, List<String> fieldChain) {
        addVStruct(value.child(), value, fieldChain);
    }

    private void addVStruct(VStruct vStruct, VInterface nullableInterface, List<String> fieldChain) {
        CompositeValue value = nullableInterface != null ? nullableInterface : vStruct;

        String beanType = beanTypeStr;
        if (beanType == null) {
            beanType = ctx.ctxName().getLocalName(Name.fullName(vStruct.schema()));
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
            int sz = vStruct.values().size();
            if (sz > 0) { // 这里来个优化，如果没有参数不加()，因为beanType其实直接就是个实例
                res.append("(");
                int idx = 0;
                boolean meetBool = false;
                boolean doPack = TypeStr.isDoPackBool(vStruct.schema());
                int i = 0;
                for (FieldSchema field : vStruct.schema().fields()) {
                    Value fieldValue = vStruct.values().get(i);
                    i++;
                    if (doPack && fieldValue instanceof VBool) { //从第一个遇到的bool开始搞
                        if (!meetBool) {
                            meetBool = true;

                            BitSet bs = new BitSet();
                            int cnt = 0;
                            for (Value fv : vStruct.values()) {
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
                        notKey.addValue(fieldValue, subChain(fieldChain, field.name()));
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
