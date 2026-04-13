package configgen.util;

import gg.jte.CodeResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import gg.jte.resolve.DirectoryCodeResolver;
import gg.jte.resolve.ResourceCodeResolver;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class JteEngine {

    public static final TemplateEngine engine;

    static {
        TemplateEngine engine1;
        try {
            // 1. 获取当前项目工作目录的绝对路径
            String currentDirPath = Paths.get("").toAbsolutePath().toString();

            // 2. 将当前路径转换为一个安全的文件名后缀 (使用 hashCode 转 16 进制)
            // 这样同一个项目路径总能得到相同的 Key，不同项目路径 Key 不同
            String dirKey = Integer.toHexString(currentDirPath.hashCode());

            // 3. 获取系统临时目录，并拼接出我们专属的复用目录
            String sysTempDir = System.getProperty("java.io.tmpdir");
            Path tempClassDir = Paths.get(sysTempDir, "jte-classes-" + dirKey);

            // 4. 确保该目录存在（如果不存在则创建它）
            if (!Files.exists(tempClassDir)) {
                Files.createDirectories(tempClassDir);
            }

            // 5. 调用 create 方法传入我们计算好的目录
            engine1 = TemplateEngine.create(new ResourceCodeResolver("jte"), tempClassDir, ContentType.Plain);
            engine1.setTrimControlStructures(true);
        } catch (IOException e) {
            Logger.log("Failed to create temp classes directory, fallback to cwd. %s", e.getMessage());
            engine1 = TemplateEngine.create(new ResourceCodeResolver("jte"), ContentType.Plain);
            engine1.setTrimControlStructures(true);

        }
        engine = engine1;
    }

    public static void render(String name, Object model, TemplateOutput output) {
        engine.render(name, model, output);
    }

    public static void render(String name, Map<String, Object> params, TemplateOutput output) {
        engine.render(name, params, output);
    }

    public static String renderTryFileFirst(String filePath,
                                            @NotNull String fileInResources,
                                            Object model) {
        TemplateOutput prompt = new StringOutput();
        Path root = Path.of(".");
        if (filePath != null && Files.exists(root.resolve(filePath))) {
            CodeResolver codeResolver = new DirectoryCodeResolver(root);
            TemplateEngine templateEngine = TemplateEngine.create(codeResolver, ContentType.Plain);
            templateEngine.render(filePath, model, prompt);
        } else { //内置的
            JteEngine.render(fileInResources, model, prompt);
        }
        return prompt.toString();
    }
}
