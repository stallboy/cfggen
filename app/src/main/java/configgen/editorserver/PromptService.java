package configgen.editorserver;

import configgen.genjson.AICfg;
import configgen.genjson.PromptGen;
import configgen.value.*;

import java.nio.file.Files;
import java.nio.file.Path;

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


    public static PromptResult gen(CfgValue cfgValue, AICfg aiCfg, Path aiDir, String table) {
        if (aiCfg == null) {
            return new PromptResult(AICfgNotSet, "", "");
        }
        if (table == null || table.isEmpty()) {
            return new PromptResult(tableNotSet, "", "");
        }
        CfgValue.VTable vTable = cfgValue.getTable(table);
        if (vTable == null) {
            return new PromptResult(tableNotFound, table, "");
        }
        String promptFile;
        try {
            promptFile = aiCfg.assureFindPromptFile(table, aiDir);
        } catch (Exception e) {
            return new PromptResult(promptFileNotFound, table, "");
        }
        String prompt = PromptGen.genPrompt(cfgValue, table, promptFile, aiCfg.findTable(table), false);
        String init = aiCfg.findInit(table);
        return new PromptResult(ok, prompt, init);
    }

}
