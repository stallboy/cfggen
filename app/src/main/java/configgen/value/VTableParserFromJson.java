package configgen.value;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import configgen.schema.TableSchema;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static configgen.value.CfgValue.*;
import static configgen.value.CfgValue.VTable;

public class VTableParserFromJson {
    private final TableSchema subTableSchema;
    private final Path jsonDir;
    private final TableSchema tableSchema;
    private final ValueErrs errs;
    private final ValueFromJson parser;

    public VTableParserFromJson(TableSchema subTableSchema, Path jsonDir, TableSchema tableSchema,
                                TextI18n.TableI18n nullableTableI18n, ValueErrs errs) {
        this.subTableSchema = subTableSchema;
        this.jsonDir = jsonDir;
        this.tableSchema = tableSchema;
        this.parser = new ValueFromJson(subTableSchema, nullableTableI18n);
        this.errs = errs;
    }

    public VTable parseTable() {
        List<VStruct> valueList = new ArrayList<>();
        if (Files.isDirectory(jsonDir)) {
            File[] files = jsonDir.toFile().listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".json")) {
                        try {
                            String str = Files.readString(file.toPath());
                            try {
                                VStruct vStruct = parser.fromJson(str);
                                valueList.add(vStruct);
                            } catch (ValueFromJson.JsonParseException e) {
                                errs.addErr(new ValueErrs.JsonFileParseErr(file.getName(), tableSchema.name()));
                            }
                        } catch (IOException e) {
                            errs.addErr(new ValueErrs.JsonFileReadErr(file.getName()));
                        }
                    }
                }
            }
        }
        return new VTableCreator(subTableSchema, tableSchema, errs).create(valueList);
    }


}
