package configgen.editorserver;

import configgen.tool.AICfg;
import configgen.tool.AIPromptGen;
import configgen.value.*;

import java.nio.file.Path;

import static configgen.editorserver.PromptService.PromptResultCode.*;


public class PromptService {

    public record PromptResult(PromptResultCode resultCode,
                               String prompt) {
    }

    public enum PromptResultCode {
        ok,
        AICfgNotSet,
        tableNotSet,
        tableNotFound
    }


    public static PromptResult gen(CfgValue cfgValue, AICfg aiCfg, Path aiDir, String table) {
        if (aiCfg == null) {
            return new PromptResult(AICfgNotSet, "");
        }
        if (table == null || table.isEmpty()) {
            return new PromptResult(tableNotSet, "");
        }

        String promptFile = aiCfg.findPromptFile(table, aiDir);
        CfgValue.VTable vTable = cfgValue.vTableMap().get(table);
        if (vTable == null) {
            return new PromptResult(tableNotFound, table);
        }

        String promptStr = AIPromptGen.genPrompt(cfgValue, table, promptFile, aiCfg.findTable(table), false);
        return new PromptResult(ok, promptStr);
    }

}
