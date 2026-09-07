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
        String lowered = key.toLowerCase();
        if (!params.containsKey(lowered)) {
            return false;
        }
        String v = params.remove(lowered);
        // 无值flag（如 -gen java,beautifulName）在map里存null，视为true
        if (v == null || v.isEmpty()) {
            return true;
        }
        // 严格解析：Boolean.parseBoolean对垃圾值（如yes、ok、ture）静默返回false，
        // 用户以为开了开关实际没开，必须报错
        if (v.equalsIgnoreCase("true")) {
            return true;
        }
        if (v.equalsIgnoreCase("false")) {
            return false;
        }
        throw new CliException("invalid boolean value for parameter '" + key + "': " + v + " (expect true/false), arg: " + arg);
    }

    public String id() {
        return id;
    }

    void assureNoExtra() {
        if (!params.isEmpty()) {
            throw new CliException("unsupported parameter(s) for '" + id + "': " + params.keySet() + ", arg: " + arg);
        }
    }

    @Override
    public String toString() {
        return arg;
    }
}
