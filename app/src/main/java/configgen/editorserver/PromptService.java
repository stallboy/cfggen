package configgen.editorserver;

import configgen.ctx.Context;
import configgen.schema.*;
import configgen.schema.cfg.CfgWriter;
import configgen.util.Logger;
import configgen.value.*;

import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import static configgen.editorserver.PromptService.PromptResultCode.*;


public class PromptService {

    public record PromptRequest(String role,
                                String table,
                                List<AIExample> examples,
                                List<String> explains) {
    }

    public record AIExample(String id,
                            String description) {
    }

    public record PromptResult(PromptResultCode resultCode,
                               String prompt,
                               String answer) {

    }

    public enum PromptResultCode {
        ok,
        tableNotSet,
        tableNotFound,
        tableNotJson,
        exampleIdParseErr,
        exampleIdNotFound,
        exampleDescriptionEmpty,
    }

    private record ResolvedExample(String id,
                                   String description,
                                   CfgValue.VStruct record) {
    }

    private final CfgValue cfgValue;
    private final TableSchemaRefGraph graph;
    private final PromptRequest req;

    public PromptService(CfgValue cfgValue, TableSchemaRefGraph graph, PromptRequest req) {
        this.cfgValue = cfgValue;
        this.graph = graph;
        this.req = req;
    }

    public PromptResult gen() {
        String table = req.table;
        if (table == null || table.isEmpty()) {
            return ofErr(tableNotSet);
        }

        CfgValue.VTable vTable = cfgValue.vTableMap().get(table);
        if (vTable == null) {
            return ofErr(tableNotFound);
        }

        if (!vTable.schema().meta().isJson()) {
            return ofErr(tableNotJson);
        }

        List<ResolvedExample> resolvedExamples = new ArrayList<>(req.examples.size());
        for (AIExample ex : req.examples) {
            if (ex.id == null) {
                return ofErr(exampleIdParseErr);
            }
            ValueErrs errs = ValueErrs.of();
            CfgValue.Value pkValue = ValuePack.unpackTablePrimaryKey(ex.id, vTable.schema(), errs);

            if (!errs.errs().isEmpty()) {
                for (ValueErrs.VErr err : errs.errs()) {
                    System.err.println(err);
                }
                return ofErr(exampleIdParseErr);
            }

            CfgValue.VStruct vRecord = vTable.primaryKeyMap().get(pkValue);
            if (vRecord == null) {
                return ofErr(exampleIdNotFound);
            }
            if (ex.description == null || ex.description.isEmpty()) {
                return ofErr(exampleDescriptionEmpty);
            }
            resolvedExamples.add(new ResolvedExample(ex.id, ex.description, vRecord));
        }
        return new PromptResult(ok, genPrompt(vTable.schema(), resolvedExamples), genPromptAnswer());
    }

    private StringBuilder sb;

    private String genPrompt(TableSchema tableSchema, List<ResolvedExample> resolvedExamples) {
        sb = new StringBuilder(4096);

        add("""
                - Role: {0}和json生成专家
                - Background: 以下会给出{1}相关的结构定义、相关联csv表内容、原理
                \t- {1}相关的结构定义，结构定义中的```->```指明了此字段外键到另一个表。如下
                \t```
                """, req.role, req.table);
        addSchema(tableSchema);
        add("""
                \t```
                \t- 相关csv表内容,表中第一行是表头程序用名称，之后是具体数据
                """);
        addRelatedCsv(tableSchema);
        for (String explain : req.explains) {
            add(explain);
            add("\n");
        }
        add("""
                - Profile: 你是一位经验丰富逻辑严密的{0}，擅长把需求描述转变为符合结构的json数据
                - Skills: 你具备深厚的行业经验、对json格式有深入理解，并且能够灵活运用各种编程语言和工具来生成和验证json数据
                - Goals: 把描述转变为符合{1}结构定义的json数据
                - Constrains: 生成的json数据必须严格遵守定义的数据结构，确保数据的一致性和有效性。遵守以下规则
                \t- 如果对象里字段为默认值，则可以忽略此字段
                \t\t- 字段类型为number，默认为0
                \t\t- 字段类型为array，默认为[]
                \t\t- 字段类型为str，默认为空字符串
                \t- 对象要加入$type字段，来表明此对象的类型
                \t\t- $type的值 不能是结构定义里的，interface名称，而必须是具体的struct名称。
                \t- 对象可以加入$note字段，作为注释，不用全部都加，最好这些注释合起来组成了描述
                \t- json中不要包含```//```开头的注释
                - OutputFormat: json
                """, req.role, req.table);
        addExamples(resolvedExamples);
//        add("-Initialization: 在第一次对话中，请直接输出以下：{0}",
//                genPromptAnswer());
        return sb.toString();
    }

    private String genPromptAnswer() {
        return "您好！请提供id和想要的%s描述。我将根据这些信息为您生成符合%s结构的json数据".formatted(req.table, req.table);
    }

    private void addSchema(TableSchema tableSchema) {
        Map<String, Nameable> allIncludedStructs = IncludedStructs.findAllIncludedStructs(tableSchema);
        for (Nameable s : allIncludedStructs.values()) {
            if (s instanceof StructSchema ss && ss.nullableInterface() != null) {
                continue;
            }
            CfgWriter cfgWriter = new CfgWriter(sb, false, false);
            cfgWriter.writeNamable(s, "\t");
        }
    }

    private void addRelatedCsv(TableSchema tableSchema) {
        Collection<TableSchema> refOutTables = graph.getRefOutTables(tableSchema);
        if (refOutTables != null) {
            for (TableSchema refOutTable : refOutTables) {
                if (refOutTable.entry() instanceof EntryType.EEnum en) {
                    add("""
                            \t\t- {0}
                            \t\t```
                            """, refOutTable.name());
                    addOneRelatedCsv(refOutTable, en);
                    add("""
                            \t\t```
                            """);
                }
            }

        }
    }

    private void addOneRelatedCsv(TableSchema tableSchema, EntryType.EEnum en) {
        Set<String> fieldNames = new LinkedHashSet<>(tableSchema.primaryKey().fields());
        fieldNames.add(en.field());
        String title = tableSchema.meta().getStr("title", null);
        if (title != null) {
            fieldNames.add(title);
        }

        CfgValue.VTable vTable = cfgValue.getTable(tableSchema.name());
        ValueToCsv.writeAsCsv(sb, vTable, fieldNames, "\t\t");
    }

    private void addExamples(List<ResolvedExample> resolvedExamples) {
        if (!resolvedExamples.isEmpty()) {
            add("""
                    - Examples:
                    """);
        }
        for (ResolvedExample e : resolvedExamples) {
            add("""
                    \t- {0}，{1}
                    \t```json
                    """, e.id, e.description);
            String jsonString = ValueToJson.toJsonStr(e.record);
            String result = Arrays.stream(jsonString.split("\n"))
                    .map(line -> "\t" + line)
                    .collect(Collectors.joining("\n"));

            add(result + "\n");
            add("""
                    \t```
                    """);
        }
    }

    private void add(String str) {
        sb.append(str);
    }

    private void add(String fmt, String arg1) {
        sb.append(MessageFormat.format(fmt, arg1));
    }

    private void add(String fmt, String arg1, String arg2) {
        sb.append(MessageFormat.format(fmt, arg1, arg2));
    }

    private PromptResult ofErr(PromptResultCode code) {
        return new PromptResult(code, "", "");
    }

    public static void main(String[] args) {
        Logger.setWarningEnabled(false);
        Context ctx = new Context(Path.of("."));
        CfgValue cfgValue = ctx.makeValue();
        TableSchemaRefGraph graph = new TableSchemaRefGraph(cfgValue.schema());
        PromptService service = new PromptService(cfgValue, graph,
                new PromptRequest("资深游戏策划", "skill.buff", List.of(
                        new AIExample("410305", "所有【暴怒技能】造成的伤害提高30%"),
                        new AIExample("410425", "每秒额外回复6点能量")
                ), List.of()));

        PromptResult r = service.gen();
        System.out.println(r.prompt);
    }
}
