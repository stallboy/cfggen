package configgen.schema;

public class SchemaStat {
    private int structCnt;
    private int interfaceCnt;
    private int implCnt;
    private int tableCnt;
    private int fieldCnt;

    private int tBoolCnt;
    private int tIntCnt;
    private int tLongCnt;
    private int tFloatCnt;
    private int tStrCnt;
    private int tTextCnt;
    private int tResCnt;
    private int tListCnt;
    private int tMapCnt;
    private int tStructRefCnt;

    private int fPackCnt;
    private int fSepCnt;
    private int fFixCnt;
    private int fBlockCnt;

    private int eEntryCnt;
    private int eEnumCnt;

    private int refCnt;
    private int listRefCnt;
    private int listItemRefCnt;
    private int mapValueRefCnt;
    private int structRefCnt;

    private int uniqKeyCnt;
    private int multiKeyCnt;
    private int multi2KeyCnt;
    private int multi3KeyCnt;


    public SchemaStat(CfgSchema cfg) {
        for (Nameable item : cfg.items()) {
            switch (item) {
                case InterfaceSchema interfaceSchema -> {
                    interfaceCnt++;
                    parseInterface(interfaceSchema);

                }
                case StructSchema structSchema -> {
                    structCnt++;
                    parseStruct(structSchema);
                }
                case TableSchema tableSchema -> {
                    tableCnt++;
                    parseTable(tableSchema);
                }
            }
        }
    }

    private void parseTable(TableSchema tableSchema) {

    }

    private void parseStruct(StructSchema structSchema) {

    }

    private void parseInterface(InterfaceSchema sInterface){
        for (StructSchema impl : sInterface.impls()) {
            implCnt++;

        }
    }

    public int structCnt() {
        return structCnt;
    }

    public int interfaceCnt() {
        return interfaceCnt;
    }

    public int implCnt() {
        return implCnt;
    }

    public int tableCnt() {
        return tableCnt;
    }

    public int fieldCnt() {
        return fieldCnt;
    }

    public int tBoolCnt() {
        return tBoolCnt;
    }

    public int tIntCnt() {
        return tIntCnt;
    }

    public int tLongCnt() {
        return tLongCnt;
    }

    public int tFloatCnt() {
        return tFloatCnt;
    }

    public int tStrCnt() {
        return tStrCnt;
    }

    public int tTextCnt() {
        return tTextCnt;
    }

    public int tResCnt() {
        return tResCnt;
    }

    public int tListCnt() {
        return tListCnt;
    }

    public int tMapCnt() {
        return tMapCnt;
    }

    public int tStructRefCnt() {
        return tStructRefCnt;
    }

    public int fPackCnt() {
        return fPackCnt;
    }

    public int fSepCnt() {
        return fSepCnt;
    }

    public int fFixCnt() {
        return fFixCnt;
    }

    public int fBlockCnt() {
        return fBlockCnt;
    }

    public int eEntryCnt() {
        return eEntryCnt;
    }

    public int eEnumCnt() {
        return eEnumCnt;
    }

    public int refCnt() {
        return refCnt;
    }

    public int listRefCnt() {
        return listRefCnt;
    }

    public int listItemRefCnt() {
        return listItemRefCnt;
    }

    public int mapValueRefCnt() {
        return mapValueRefCnt;
    }

    public int structRefCnt() {
        return structRefCnt;
    }

    public int uniqKeyCnt() {
        return uniqKeyCnt;
    }

    public int multiKeyCnt() {
        return multiKeyCnt;
    }

    public int multi2KeyCnt() {
        return multi2KeyCnt;
    }

    public int multi3KeyCnt() {
        return multi3KeyCnt;
    }

    @Override
    public String toString() {
        return "SchemaStat{" +
                "structCnt=" + structCnt +
                ", interfaceCnt=" + interfaceCnt +
                ", implCnt=" + implCnt +
                ", tableCnt=" + tableCnt +
                ", fieldCnt=" + fieldCnt +
                ", tBoolCnt=" + tBoolCnt +
                ", tIntCnt=" + tIntCnt +
                ", tLongCnt=" + tLongCnt +
                ", tFloatCnt=" + tFloatCnt +
                ", tStrCnt=" + tStrCnt +
                ", tTextCnt=" + tTextCnt +
                ", tResCnt=" + tResCnt +
                ", tListCnt=" + tListCnt +
                ", tMapCnt=" + tMapCnt +
                ", tStructRefCnt=" + tStructRefCnt +
                ", fPackCnt=" + fPackCnt +
                ", fSepCnt=" + fSepCnt +
                ", fFixCnt=" + fFixCnt +
                ", fBlockCnt=" + fBlockCnt +
                ", eEntryCnt=" + eEntryCnt +
                ", eEnumCnt=" + eEnumCnt +
                ", refCnt=" + refCnt +
                ", listRefCnt=" + listRefCnt +
                ", listItemRefCnt=" + listItemRefCnt +
                ", mapValueRefCnt=" + mapValueRefCnt +
                ", structRefCnt=" + structRefCnt +
                ", uniqKeyCnt=" + uniqKeyCnt +
                ", multiKeyCnt=" + multiKeyCnt +
                ", multi2KeyCnt=" + multi2KeyCnt +
                ", multi3KeyCnt=" + multi3KeyCnt +
                '}';
    }
}
