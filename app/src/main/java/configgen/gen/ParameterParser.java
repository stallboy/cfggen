package configgen.gen;

import configgen.util.ArgParser;

import java.util.HashMap;
import java.util.Map;

class ParameterParser implements Parameter {
    private final String arg;
    private final String genId;
    private final Map<String, String> params;

    ParameterParser(String arg) {
        this.arg = arg;
        ArgParser.IdAndMap im = ArgParser.parseToIdAndMap(arg);
        genId = im.id();
        params = im.map();
    }

    @Override
    public Parameter copy() {
        return new ParameterParser(arg);
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

    String genId() {
        return genId;
    }

    void assureNoExtra() {
        if (!params.isEmpty()) {
            throw new AssertionError("-gen " + genId + " not support parameter: " + params);
        }
    }

    @Override
    public String toString() {
        return arg;
    }
}
