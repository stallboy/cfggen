package configgen.value;

public class CfgValueException extends RuntimeException{
    private final CfgValueErrs errs;

    public CfgValueException(CfgValueErrs errs) {
        this.errs = errs;
    }

    public CfgValueErrs getErrs() {
        return errs;
    }
}
