package configgen.schema.cfg;

import configgen.schema.CfgSchema;
import configgen.schema.Nameable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class CfgUtil {

    public static Map<String, CfgSchema> separate(CfgSchema root) {
        Map<String, CfgSchema> cfgMap = new LinkedHashMap<>();
        for (Nameable item : root.items()) {
            String ns = item.namespace();
            CfgSchema cfg = cfgMap.get(ns);
            if (cfg == null) {
                cfg = CfgSchema.of();
                cfgMap.put(ns, cfg);
            }
            cfg.items().add(item);
        }

        for (Map.Entry<String, String> entry : root.fileEndComments().entrySet()) {
            CfgSchema module = cfgMap.computeIfAbsent(entry.getKey(), ignored -> CfgSchema.of());
            module.setFileEndComment("", entry.getValue());
        }

        return cfgMap;
    }

    private static final Pattern identifierPattern = Pattern.compile("[a-zA-Z_][a-zA-Z0-9_]*");

    public static boolean isIdentifier(String name) {
        return identifierPattern.matcher(name).matches();
    }
}
