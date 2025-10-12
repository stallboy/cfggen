package configgen.util;

import java.util.*;

public class ArgParser {

    public record IdAndMap(String id,
                           Map<String, String> map) {
    }

    public static IdAndMap parseToIdAndMap(String arg) {
        String[] sp = arg.split(",");
        return new IdAndMap(sp[0], parseMap(sp, 1));
    }

    public static Map<String, String> parseToMap(String arg) {
        if (arg == null || arg.isEmpty()) {
            return Collections.emptyMap();
        }
        String[] sp = arg.split(",");
        return parseMap(sp, 0);
    }

    private static Map<String, String> parseMap(String[] sp, int fromIndex) {
        Map<String, String> map = new LinkedHashMap<>();
        for (int i = fromIndex; i < sp.length; i++) {
            String s = sp[i];
            int c = s.indexOf(':');
            if (c == -1) {
                c = s.indexOf('=');
            }

            if (c == -1) {
                map.put(s.trim().toLowerCase(), null);
            } else {
                map.put(s.substring(0, c).trim().toLowerCase(), s.substring(c + 1).trim());
            }
        }
        return map;
    }

    public static Set<String> parseToSet(String arg) {
        if (arg == null || arg.isEmpty()) {
            return Collections.emptySet();
        }
        String[] sp = arg.split(",");
        return new LinkedHashSet<>(Arrays.asList(sp));
    }


}
