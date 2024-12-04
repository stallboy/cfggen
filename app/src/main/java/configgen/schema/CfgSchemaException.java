package configgen.schema;

public class CfgSchemaException extends RuntimeException {

    private final CfgSchemaErrs errs;

    public CfgSchemaException(CfgSchemaErrs errs) {
        this.errs = errs;
    }

    public CfgSchemaErrs getErrs() {
        return errs;
    }
}
