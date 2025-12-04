package configgen.tool;

import configgen.ctx.Context;
import configgen.gen.Generator;
import configgen.gen.Parameter;

import java.io.IOException;

public class GenVerifier extends Generator {

    public GenVerifier(Parameter parameter) {
        super(parameter);
    }

    @Override
    public void generate(Context ctx) throws IOException {
        ctx.makeValue();
    }
}
