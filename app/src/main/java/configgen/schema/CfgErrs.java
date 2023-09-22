package configgen.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record CfgErrs(List<Err> errs) {
    public static CfgErrs of() {
        return new CfgErrs(new ArrayList<>());
    }

    public CfgErrs {
        Objects.requireNonNull(errs);
    }

    void addErr(Err err) {
        errs.add(err);
    }
}
