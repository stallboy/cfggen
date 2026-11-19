package configgen.gen;

import org.jspecify.annotations.Nullable;

public abstract class GeneratorWithTag extends Generator {
    protected @Nullable String tag;

    public GeneratorWithTag(Parameter parameter) {
        super(parameter);
        tag = parameter.get("own", null);
    }
}
