package configgen.value;

import configgen.schema.StructSchema;

import java.util.ArrayList;
import java.util.List;

import static configgen.value.CfgValue.*;
import static configgen.value.ForeachFinder.FContainerEach.*;

/**
 * 根据schema路径，来找value
 */
public class ForeachFinder {

    public interface FinderVisitor {
        void visit(Value value);
    }

    public static final class Finder {
        private final List<Find> path;
        private String type = "";

        public Finder(List<Find> path) {
            this.path = path;
        }

        public static Finder of() {
            return new Finder(List.of());
        }

        public Finder copyAdd(Find find) {
            List<Find> p = new ArrayList<>(path.size() + 1);
            p.addAll(path);
            p.add(find);
            return new Finder(p);
        }

        public Finder copyAdd(Find find1, Find find2) {
            List<Find> p = new ArrayList<>(path.size() + 2);
            p.addAll(path);
            p.add(find1);
            p.add(find2);
            return new Finder(p);
        }

        public List<Find> path() {
            return path;
        }

        public String type() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return "Finder{" +
                    "path=" + path +
                    ", type='" + type + '\'' +
                    '}';
        }
    }

    public sealed interface Find {
    }

    public record FStructField(int index) implements Find {
    }

    public enum FContainerEach implements Find {
        LIST_ITEM,
        MAP_KEY,
        MAP_VALUE
    }

    public record FInterfaceImpl(StructSchema impl) implements Find {
    }


    public static void foreachVTable(FinderVisitor visitor, VTable vTable, Finder finder) {
        for (VStruct vStruct : vTable.valueList()) {
            foreachValue(visitor, vStruct, finder, 0);
        }
    }

    public static void foreachValue(FinderVisitor visitor, Value value, Finder finder, int index) {
        if (index == finder.path.size()) {
            visitor.visit(value);
            return;
        }
        Find find = finder.path.get(index);
        switch (find) {
            case FStructField fStructField -> {
                VStruct vStruct = (VStruct) value;
                Value v = vStruct.values().get(fStructField.index);
                foreachValue(visitor, v, finder, index + 1);
            }

            case FInterfaceImpl fInterfaceImpl -> {
                VInterface vInterface = (VInterface) value;
                VStruct v = vInterface.child();
                if (v.schema() == fInterfaceImpl.impl) {
                    foreachValue(visitor, v, finder, index + 1);
                }
            }

            case LIST_ITEM -> {
                VList vList = (VList) value;
                for (SimpleValue v : vList.valueList()) {
                    foreachValue(visitor, v, finder, index + 1);
                }
            }
            case MAP_KEY -> {
                VMap vMap = (VMap) value;
                for (SimpleValue v : vMap.valueMap().keySet()) {
                    foreachValue(visitor, v, finder, index + 1);
                }
            }
            case MAP_VALUE -> {
                VMap vMap = (VMap) value;
                for (SimpleValue v : vMap.valueMap().values()) {
                    foreachValue(visitor, v, finder, index + 1);
                }
            }
        }
    }
}
