package configgen.gen;

import configgen.util.Localize;

import java.util.LinkedHashMap;
import java.util.Map;

record Usage(String genId,
             Map<String, Info> infos) implements Parameter {

    private record Info(String def,
                        boolean isFlag,
                        String messageId) {
    }

    static Usage of(String genId) {
        return new Usage(genId, new LinkedHashMap<>());
    }

    void print() {
        for (Map.Entry<String, Info> entry : infos.entrySet()) {
            String key = entry.getKey();
            Info info = entry.getValue();
            String messageId = info.messageId != null ? info.messageId : genId + "." + key;
            if (info.isFlag) {
                System.out.printf("        %-20s %s,%s\n", key,
                        Localize.getMessage(messageId),
                        Localize.getMessage("Gen.DefaultFalse"));
            } else {
                String def = info.def;
                if (def == null) {
                    def = "null";
                }
                System.out.printf("        %-20s %s\n", key + "=" + def,
                        Localize.getMessage(messageId)
                );
            }
        }
    }

    @Override
    public String get(String key, String def) {
        infos.put(key, new Info(def, false, null));
        return def;
    }

    @Override
    public boolean has(String key) {
        infos.put(key, new Info("false", true, null));
        return false;
    }

    @Override
    public String get(String key, String def, String messageId) {
        infos.put(key, new Info(def, false, messageId));
        return def;
    }
}
