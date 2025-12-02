package configgen.editorserver;

import configgen.ctx.Context;
import configgen.genbyai.PromptGen;
import configgen.genbyai.PromptGen.Prompt;
import configgen.value.*;

import static configgen.editorserver.PromptService.PromptResultCode.*;


public class PromptService {

    public record PromptResult(PromptResultCode resultCode,
                               String prompt,
                               String init) {
    }

    public enum PromptResultCode {
        ok,
        AICfgNotSet,
        tableNotSet,
        tableNotFound,
        promptFileNotFound,
    }


    public static PromptResult gen(Context context,
                                   CfgValue cfgValue,
                                   String table) {

        if (table == null || table.isEmpty()) {
            return new PromptResult(tableNotSet, "", "");
        }
        CfgValue.VTable vTable = cfgValue.getTable(table);
        if (vTable == null) {
            return new PromptResult(tableNotFound, table, "");
        }

        Prompt prompt = PromptGen.genPrompt(context, cfgValue, vTable);
        return new PromptResult(ok, prompt.prompt(), prompt.init());
    }

}
