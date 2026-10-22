package configgen.schema;

import java.util.HashMap;
import java.util.Map;

/** Removes only dependencies made unavailable by tag filtering, using the canonical resolver. */
final class FilteredReferences {
    private FilteredReferences() {}

    static void prune(CfgSchema filtered, CfgSchemaErrs diagnostics) {
        boolean removed;
        do {
            removed = false;
            CfgSchemaErrs validation = filtered.resolve();
            Map<String, Structural> owners = new HashMap<>();
            ForeachSchema.foreachStructural(s -> owners.put(s.fullName(), s), filtered);
            for (CfgSchemaErrs.Err error : validation.errs()) {
                if (error instanceof CfgSchemaErrs.InvalidIndex invalid) {
                    Structural owner = owners.get(invalid.structural());
                    FieldSchema field = owner == null ? null : owner.findField(invalid.field());
                    if (field != null && field.meta().data().remove("index") != null) {
                        removed = true;
                        diagnostics.addWeakWarn(new CfgSchemaErrs.FilteredReferenceDependency(
                                invalid.structural(), invalid.field(), invalid.reason()));
                    }
                    continue;
                }
                if (!(error instanceof CfgSchemaErrs.InvalidReference invalid)) continue;
                Structural owner = owners.get(invalid.structural());
                if (owner == null) continue;
                if (owner.foreignKeys().removeIf(f -> f.pathReference() != null
                        && f.name().equals(invalid.reference()))) {
                    removed = true;
                    diagnostics.addWeakWarn(new CfgSchemaErrs.FilteredReferenceDependency(
                            invalid.structural(), invalid.reference(), invalid.reason()));
                }
            }
            // Each iteration removes a declaration. Unrelated schema errors remain for the caller.
        } while (removed);
    }
}
