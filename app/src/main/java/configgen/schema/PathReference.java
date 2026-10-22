package configgen.schema;

import java.util.List;
import java.util.Objects;

/** New single-target reference syntax, separate from legacy container references. */
public record PathReference(List<ReferencePath> sources, ReferencePath target, boolean inline) {
    public PathReference {
        sources = List.copyOf(sources);
        Objects.requireNonNull(target);
        if (target.start() == ReferencePath.Start.CURRENT) {
            throw new IllegalArgumentException("target requires an explicit start");
        }
    }
}
