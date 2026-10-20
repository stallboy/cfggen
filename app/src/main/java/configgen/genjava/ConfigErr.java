package configgen.genjava;

public class ConfigErr extends RuntimeException {
    private final java.util.List<LoadValueErrs.Error> errors;

    public ConfigErr(Exception e) {
        super(e);
        errors = java.util.List.of();
    }

    public ConfigErr(String s) {
        super(s);
        errors = java.util.List.of();
    }

    public ConfigErr(LoadValueErrs.Error error) { this(java.util.List.of(error)); }

    public ConfigErr(java.util.List<LoadValueErrs.Error> errors) {
        super("Configuration validation failed: " + errors);
        this.errors = java.util.List.copyOf(errors);
    }

    public java.util.List<LoadValueErrs.Error> getErrors() { return errors; }
}
