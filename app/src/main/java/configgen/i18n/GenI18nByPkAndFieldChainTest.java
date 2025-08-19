package configgen.i18n;

import configgen.ctx.Context;
import configgen.gen.Generator;
import configgen.gen.Parameter;
import configgen.util.Logger;

import java.io.IOException;
import java.util.*;


/**
 * 内部测试，怕fastexcel write会有问题
 */
public final class GenI18nByPkAndFieldChainTest extends Generator {

    public GenI18nByPkAndFieldChainTest(Parameter parameter) {
        super(parameter);
        new GenI18nByPkAndFieldChain(parameter);
    }

    @Override
    public void generate(Context ctx) throws IOException {
        new GenI18nByPkAndFieldChain(parameter.copy()).generate(ctx.copy());

        // 只要没打印 create或modify就说明没问题
        for (int i = 0; i < 100; i++) {
            Logger.log("%d", i + 2);
            new GenI18nByPkAndFieldChain(parameter.copy()).generate(ctx.copy());
        }
    }

}
