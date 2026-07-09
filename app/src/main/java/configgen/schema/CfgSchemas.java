package configgen.schema;

import configgen.ctx.DirectoryStructure;
import configgen.schema.cfg.CfgReader;
import configgen.schema.cfg.CfgUtil;
import configgen.schema.cfg.CfgWriter;
import configgen.util.CachedFiles;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static configgen.ctx.DirectoryStructure.*;

public class CfgSchemas {

    public static CfgSchema readFromDir(DirectoryStructure sourceStructure) {
        List<CfgFileInfo> files = new ArrayList<>(sourceStructure.getCfgFiles());
        List<Callable<CfgSchema>> tasks = new ArrayList<>(files.size());
        for (CfgFileInfo c : files) {
            tasks.add(() -> CfgReader.INSTANCE.readToSchema(c.path(), c.pkgNameDot()));
        }

        CfgSchema destination = CfgSchema.of();
        // 各 cfg 文件相互独立，可并行解析；invokeAll 保证结果顺序与提交顺序一致
        try (ExecutorService executor = Executors.newWorkStealingPool()) {
            List<Future<CfgSchema>> futures = executor.invokeAll(tasks);
            for (Future<CfgSchema> f : futures) {
                CfgSchema one = f.get();
                for (Nameable n : one.items()) {
                    destination.add(n);
                }
                for (Map.Entry<String, String> e : one.fileEndComments().entrySet()) {
                    destination.setFileEndComment(e.getKey(), e.getValue());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return destination;
    }

    public static void writeToDir(Path destination, CfgSchema root) {
        Path absoluteDst = destination.toAbsolutePath().normalize();
        Map<String, CfgSchema> modules = CfgUtil.separate(root);
        for (Map.Entry<String, CfgSchema> entry : modules.entrySet()) {
            String ns = entry.getKey();
            CfgSchema cfg = entry.getValue();
            Path dst = CfgUtil.getCfgFilePathByNamespace(ns, absoluteDst);
            writeToOneFile(dst, cfg);
        }
    }

    private static void writeToOneFile(Path dst, CfgSchema cfg) {
        String content = CfgWriter.stringify(cfg, true, false);
        try {
            CachedFiles.writeFile(dst, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
