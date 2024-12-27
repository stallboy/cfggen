package configgen.gen;

public abstract class GeneratorWithTag extends Generator {
    protected String tag;

    public GeneratorWithTag(Parameter parameter) {
        super(parameter);
        tag = parameter.get("own", null, "Gen.Tag");
    }
}
