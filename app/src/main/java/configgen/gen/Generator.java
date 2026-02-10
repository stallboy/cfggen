package configgen.gen;

import configgen.ctx.Context;
import configgen.util.CachedFileOutputStream;
import configgen.util.CachedFiles;
import configgen.util.CachedIndentPrinter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class Generator {
    protected final Parameter parameter;


    /**
     * @param parameter 此接口有2个实现类，一个用于收集usage，一个用于实际参数解析
     *                  从而实现在各Generator的参数需求，只在构造函数里写一次就ok
     */
    public Generator(Parameter parameter) {
        this.parameter = parameter;
    }

    public abstract void generate(Context ctx) throws IOException;

}
