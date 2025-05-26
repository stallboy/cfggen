package configgen.util;

import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.TemplateOutput;
import gg.jte.resolve.ResourceCodeResolver;

public class JteEngine {

    public static final TemplateEngine engine =
            TemplateEngine.create(new ResourceCodeResolver("jte"), ContentType.Plain);

    public static void render(String name, Object model, TemplateOutput output) {
        engine.render(name, model, output);
    }
}
