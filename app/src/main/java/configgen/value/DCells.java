package configgen.value;

import configgen.util.ListParser;
import configgen.util.NestListParser;

import java.util.List;
import java.util.stream.Collectors;

import static configgen.data.CfgData.DCell;

public class DCells {
    public static List<DCell> parseFunc(DCell cell) {
        return NestListParser.parseFunction(cell.value()).stream().map(cell::createSub).collect(Collectors.toList());
    }

    public static List<DCell> parseNestList(DCell cell) {
        return NestListParser.parseNestList(cell.value()).stream().map(cell::createSub).collect(Collectors.toList());
    }

    public static List<DCell> parseList(DCell cell, char separator) {
        return ListParser.parseList(cell.value(), separator).stream().map(cell::createSub).collect(Collectors.toList());
    }

    public static boolean isFunc(DCell cell) {
        String v = cell.value().trim();
        if (!v.isEmpty()) {
            char c = v.charAt(0);
            return ('a' <= c && c <= 'z') || ('A' <= c && c <= 'Z');
        }
        return false;
    }
}
