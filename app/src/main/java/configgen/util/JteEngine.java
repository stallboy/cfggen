package configgen.util;

import gg.jte.CodeResolver;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.TemplateOutput;
import gg.jte.output.StringOutput;
import gg.jte.resolve.DirectoryCodeResolver;
import gg.jte.resolve.ResourceCodeResolver;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class JteEngine {

    public static final TemplateEngine engine;

    static {
        engine = TemplateEngine.create(new ResourceCodeResolver("jte"), ContentType.Plain);
        engine.setTrimControlStructures(true);
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
