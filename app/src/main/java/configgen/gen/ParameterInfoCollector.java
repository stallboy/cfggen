package configgen.gen;

import configgen.util.LocaleUtil;
import configgen.util.Logger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ParameterInfoCollector implements Parameter {
    private final String klass;
    private final String id;
    private final Map<String, Info> infos;
    private String title;
    private List<String> extra;

    public ParameterInfoCollector(String klass, String id) {
        this.klass = klass;
        this.id = id;
        this.infos = new LinkedHashMap<>();
    }


    private record Info(String def,
                        boolean isFlag,
                        String messageId) {
    }

    public void print() {
        String titleMessageId = klass + "." + id;
        String titleStr = LocaleUtil.getLocaleString(titleMessageId, title);
        if (titleStr != null && !titleStr.isBlank()) {
            Logger.log("    -%-15s %s", klass + " " + id, titleStr);
        } else {
            Logger.log("    -%s %s", klass, id);
        }

        for (Map.Entry<String, Info> entry : infos.entrySet()) {
            String key = entry.getKey();
            Info info = entry.getValue();
            String messageId = info.messageId != null ? info.messageId : id + "." + key;
            if (info.isFlag) {
                Logger.log("        %-20s %s,%s", key,
                        LocaleUtil.getLocaleString(messageId, ""),
                        LocaleUtil.getLocaleString("DefaultFalse", "default false"));
            } else {
                String def = info.def;
                if (def == null) {
                    def = "null";
                }
                Logger.log("        %-20s %s", key + "=" + def,
                        LocaleUtil.getLocaleString(messageId, "")
                );
            }
        }

        if (extra != null) {
            for (String s : extra) {
                Logger.log("            %s", s);
            }
        }
    }

    @Override
    public String get(String key, String def, String messageId) {
        infos.put(key, new Info(def, false, messageId));
        return def;
    }

    @Override
    public boolean has(String key, String messageId) {
        infos.put(key, new Info("false", true, messageId));
        return false;
    }

    @Override
    public void title(String title) {
        this.title = title;
    }

    @Override
    public void extra(List<String> extra) {
        this.extra = extra;
    }
}
