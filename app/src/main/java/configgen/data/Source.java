package configgen.data;

import java.util.ArrayList;
import java.util.List;

import static configgen.data.CfgData.*;
import static configgen.data.Source.*;

/**
 * DCell、DCellList：来自于csv或excel文件
 * DFile：来自于json文件
 */
public sealed interface Source permits DCell, DCellList, DFile {

    static DCellList of() {
        return new DCellList(List.of());
    }

    static Source of(List<DCell> cells) {
        if (cells.size() == 1) {
            return cells.getFirst();
        } else {
            return new DCellList(cells);
        }
    }

    record DCellList(List<DCell> cells) implements Source {
    }

    record DFile(String fileName, String inStruct, List<String> path) implements Source {
        public static DFile of(String fileName, String inStruct) {
            return new DFile(fileName, inStruct, List.of());
        }

        public DFile inStruct(String struct) {
            return new DFile(fileName, struct, path);
        }

        public DFile child(String field) {
            List<String> newPath = new ArrayList<>(path);
            newPath.add(field);
            return new DFile(fileName, inStruct, newPath);
        }

        public DFile lastAppend(String impl) {
            if (path.isEmpty()) {
                return child(impl);
            }
            List<String> newPath = new ArrayList<>(path.subList(0, path.size() - 1));
            newPath.add(path.getLast() + impl);
            return new DFile(fileName, inStruct, newPath);
        }

        public DFile parent() {
            if (path.isEmpty()) {
                return this;
            }
            List<String> parent = path.subList(0, path.size() - 1);
            return new DFile(fileName, inStruct, parent);
        }
    }
}
