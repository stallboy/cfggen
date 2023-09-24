package configgen.schema;

public enum CfgSchemaFilterByTag {
    INSTANCE;

    public CfgSchema filter(CfgSchema cfg, String tag){
        return cfg;
    }
}
