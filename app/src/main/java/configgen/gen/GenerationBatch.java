package configgen.gen;

import configgen.ctx.Context;
import configgen.util.CachedFiles;
import configgen.util.Logger;

import java.io.IOException;
import java.util.List;

final class GenerationBatch {
    private GenerationBatch() {
    }

    static synchronized void run(Context context, List<Generator> generators) throws IOException {
        CachedFiles outputs = new CachedFiles();
        for (Generator generator : generators) {
            Logger.verbose("-----generate %s", generator.parameter);
            try {
                generator.generate(context, outputs);
            } catch (IOException e) {
                throw new IOException("generate " + generator.parameter + " failed (" + e.getMessage() + ")", e);
            }
            Logger.profile("generate " + generator.parameter);
        }
        outputs.finalizeRun();
    }
}
