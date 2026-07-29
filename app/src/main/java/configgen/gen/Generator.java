package configgen.gen;

import configgen.ctx.Context;
import configgen.util.CachedFileOutputStream;
import configgen.util.CachedFiles;
import configgen.util.CachedIndentPrinter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

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

    /**
     * invokeAll 提交全部任务并逐个 get 等待完成，结果按提交顺序返回；
     * 任务异常解包传播（RuntimeException/Error 原样抛出，其余包成 RuntimeException），
     * InterruptedException 恢复中断标记后包成 RuntimeException。
     */
    protected static <T> List<T> invokeAllAndWait(ExecutorService pool, List<Callable<T>> tasks) {
        try {
            List<T> results = new ArrayList<>(tasks.size());
            for (Future<T> f : pool.invokeAll(tasks)) {
                results.add(f.get());
            }
            return results;
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException re) throw re;
            if (cause instanceof Error err) throw err;
            throw new RuntimeException(cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

}
