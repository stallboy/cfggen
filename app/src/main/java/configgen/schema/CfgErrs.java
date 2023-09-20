package configgen.schema;

import java.util.List;

public record CfgErrs(List<Err> errs) {
    void addErr(Err err) {
        errs.add(err);
    }
}
