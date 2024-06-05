package configgen.value;

public class ValueError extends RuntimeException{
    private final ValueErrs errs;

    public ValueError(ValueErrs errs) {
        this.errs = errs;
    }

    public ValueErrs getErrs() {
        return errs;
    }
}
