package configgen.data;

import java.util.List;

public sealed interface Source permits CfgData.DCell, Source.DCellList, Source.DFile {

    static Source of() {
        return new DCellList(List.of());
    }

    static Source of(List<CfgData.DCell> cells) {
        if (cells.size() == 1) {
            return cells.getFirst();
        } else {
            return new DCellList(cells);
        }
    }

    static Source.DFile of(String fileName) {
        return new DFile(fileName);
    }

    record DCellList(List<CfgData.DCell> cells) implements Source {

    }

    record DFile(String fileName) implements Source {

    }

}
