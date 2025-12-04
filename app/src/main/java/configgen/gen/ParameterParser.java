package configgen.gen;

import configgen.util.ArgParser;

import java.util.Map;

public class ParameterParser implements Parameter {
    private final String arg;
    private final String id;
    private final Map<String, String> params;

    public ParameterParser(String arg) {
        this.arg = arg;
        ArgParser.IdAndMap im = ArgParser.parseToIdAndMap(arg);
        id = im.id();
        params = im.map();
    }

    @Override
    public String get(String key, String def, String messageId) {
        String v = params.remove(key.toLowerCase());
        return v != null ? v : def;
    }

    @Override
    public boolean has(String key, String messageId) {
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

    public String id() {
        return id;
    }

    void assureNoExtra() {
        if (!params.isEmpty()) {
            throw new AssertionError("-gen " + id + " not support parameter: " + params);
        }
    }

    @Override
    public String toString() {
        return arg;
    }
}
