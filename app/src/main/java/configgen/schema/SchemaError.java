package configgen.schema;

public class SchemaError extends RuntimeException {

    private final SchemaErrs errs;

    public SchemaError(SchemaErrs errs) {
        this.errs = errs;
    }

    public SchemaErrs getErrs() {
        return errs;
    }
}
