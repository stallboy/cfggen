package configgen.gen;

import java.util.HashMap;
import java.util.Map;

class Argument implements Parameter {
    private final String arg;
    final String type;
    private final Map<String, String> params = new HashMap<>();

    Argument(String arg) {
        this.arg = arg;
        String[] sp = arg.split(",");
        type = sp[0];
        for (int i = 1; i < sp.length; i++) {
            String s = sp[i];
            int c = s.indexOf(':');
            if (c == -1) {
                c = s.indexOf('=');
            }

            if (c == -1) {
                params.put(s.trim().toLowerCase(), null);
            } else {
                params.put(s.substring(0, c).trim().toLowerCase(), s.substring(c + 1).trim().toLowerCase());
            }
        }
    }

    @Override
    public String get(String key, String def) {
        String v = params.remove(key.toLowerCase());
        return v != null ? v : def;
    }

    @Override
    public boolean has(String key) {
        if (params.containsKey(key.toLowerCase())) {
            String v = params.remove(key.toLowerCase());
            if (v != null) {
                return Boolean.parseBoolean(v);
            } else {
                return true;
            }
        } else {
            return false;
        }
    }

    @Override
    public void end() {
        if (!params.isEmpty()) {
            throw new AssertionError("-gen " + type + " not support parameter: " + params);
        }
    }

    @Override
    public String toString() {
        return arg;
    }
}
