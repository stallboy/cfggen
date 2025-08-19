package configgen.i18n;

import configgen.ctx.Context;
import configgen.gen.Generator;
import configgen.gen.Parameter;
import configgen.util.Logger;

import java.io.IOException;


/**
 * 内部测试，怕fastexcel write会有问题
 */
public final class GenI18nByIdTest extends Generator {

    public GenI18nByIdTest(Parameter parameter) {
        super(parameter);
        new GenI18nById(parameter);
    }

    @Override
    public void generate(Context ctx) throws IOException {
        new GenI18nById(parameter.copy()).generate(ctx.copy());

        // 只要没打印 create或modify就说明没问题
        for (int i = 0; i < 100; i++) {
            Logger.log("%d", i + 2);
            new GenI18nById(parameter.copy()).generate(ctx.copy());
        }
    }

}
