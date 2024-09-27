package configgen.editorserver;

import configgen.schema.TableSchema;
import configgen.value.CfgValue;
import configgen.value.ValueErrs;
import configgen.value.ValuePack;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import static configgen.editorserver.PromptService.PromptResultCode.*;


public class PromptService {

    public record AIExample(String id,
                            String description) {
    }

    public record PromptRequest(String role,
                                String table,
                                List<AIExample> examples) {
    }

    public record PromptResponse(PromptResultCode resultCode,
                                 String prompt) {

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
    private final PromptRequest req;

    public PromptService(CfgValue cfgValue, PromptRequest req) {
        this.cfgValue = cfgValue;
        this.req = req;
    }

    public PromptResponse gen() {
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
        return new PromptResponse(ok, genPrompt(vTable.schema(), resolvedExamples));
    }

    private StringBuilder sb;

    private String genPrompt(TableSchema tableSchema, List<ResolvedExample> resolvedExamples) {
        sb = new StringBuilder(4096);

        add("""
                - Role: {0}和json生成专家
                - Background: 以下会给出{1}相关的结构定义，和相关联csv表内容，然后根据描述来生成{1}结构的json文件
                    - {1}相关的结构定义，结构定义中的```->```指明了此字段外键到另一个表。如下
                """, req.role, req.table);

        add("\t```");
        addSchema(tableSchema);
        add("\t```");
        add("\t- 相关csv表内容,表中第一行是表头程序用名称，之后是具体数据\n");
        addRelatedCsv(tableSchema);
        add("""
                - Profile: 你是一位经验丰富逻辑严密的{0}，擅长把需求描述转变为符合结构的json数据
                - Skills: 你具备深厚的行业经验、对json格式有深入理解，并且能够灵活运用各种编程语言和工具来生成和验证json数据
                - Goals: 把描述转变为符合{1}结构定义的json数据
                - Constrains: 生成的json数据必须严格遵守定义的数据结构，确保数据的一致性和有效性。遵守以下规则
                    - 如果对象里字段为默认值，则可以忽略此字段
                        - 字段类型为number，默认为0
                        - 字段类型为array，默认为[]
                        - 字段类型为str，默认为空字符串
                    - 对象要加入$type字段，来表明此对象的类型
                      - $type的值 不能是结构定义里的，interface名称，而必须是具体的struct名称。
                    - 对象可以加入$note字段，作为注释，不用全部都加，最好这些注释合起来组成了描述
                    - json中不要包含```//```开头的注释
                - OutputFormat: json
                - Examples:
                """, req.role, req.table);
        addExamples(resolvedExamples);
        add("-Initialization: 在第一次对话中，请直接输出以下：您好！请提供id和想要的buff描述。我将根据这些信息为您生成符合{0}结构的json数据",
                req.table);
        return sb.toString();
    }

    private void addSchema(TableSchema tableSchema) {
        
    }
    private void addRelatedCsv(TableSchema tableSchema) {

    }

    private void addExamples(List<ResolvedExample> resolvedExamples) {

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

    private PromptResponse ofErr(PromptResultCode code) {
        return new PromptResponse(code, "");
    }

}
