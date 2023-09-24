package configgen.schema;

import java.util.List;
import java.util.Map;

public enum CfgSchemaFilterByDataHeader {
    INSTANCE;

    public CfgSchema filter(CfgSchema cfg, Map<String, List<String>> dataHeaders) {
        return cfg;
    }
}
