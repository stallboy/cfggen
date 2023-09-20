package configgen.schema.cfg;

import configgen.schema.CfgSchema;
import configgen.schema.Fieldable;
import configgen.schema.TableSchema;

import java.util.LinkedHashMap;
import java.util.Map;

public class Cfgs {

    public static Map<String, CfgSchema> separate(CfgSchema root) {
        Map<String, CfgSchema> cfgMap = new LinkedHashMap<>();
        for (Fieldable f : root.structs().values()) {
            String ns = f.namespace();
            CfgSchema cfg = cfgMap.get(ns);
            if (cfg == null) {
                cfg = CfgSchema.of();
                cfgMap.put(ns, cfg);
            }
            cfg.add(f);
        }

        for (TableSchema t : root.tables().values()) {
            String ns = t.namespace();
            CfgSchema cfg = cfgMap.get(ns);
            if (cfg == null) {
                cfg = CfgSchema.of();
                cfgMap.put(ns, cfg);
            }
            cfg.add(t);
        }

        return cfgMap;
    }


}
