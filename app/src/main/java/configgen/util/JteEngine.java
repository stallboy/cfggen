package configgen.util;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.TemplateOutput;
import gg.jte.resolve.ResourceCodeResolver;

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
}
