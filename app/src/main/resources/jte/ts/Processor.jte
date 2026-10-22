@import configgen.gents.TsCodeGenerator
@import configgen.schema.HasRef
@import configgen.schema.TableSchema
@param TsCodeGenerator model

export class Processor {
    private static readonly TableNames = [
        @for(TableSchema table : model.cfgSchema.sortedTables())
            "${table.name()}",
        @endfor
    ];

    static ProcessPackage(pack: ConfigPackage, errors: LoadErrors): Stream {
        return Loader.LoadPackage(pack, Processor.Process, errors${model.nullableLanguageSwitch == null ? "" : ", [" + model.languageArguments() + "]"});
    }

    // 从 bytes 文件加载（新格式）
    static Process(os: Stream, errors: LoadErrors): void {
        // 读取表数量
        const tables = os.ReadTables();
        for (const name of Processor.TableNames) {
            if (!tables.has(name)) {
                throw new Error("Missing table in standalone package: " + name);
            }
        }
        for (const [tableName, tableInput] of tables) {

            // 根据表名分发到对应的 Initialize 方法
            switch(tableName) {
                @for(TableSchema table : model.cfgSchema.sortedTables())
                    case "${table.name()}":
                        ${model.className(table)}.Initialize(tableInput, errors);
                        tableInput.RequireEnd();
                        break;
                @endfor
                default:
                    // 未知表，跳过
                    break;
            }
        }

        // 解析外键引用
        @for(TableSchema table : model.cfgSchema.sortedTables())
            @if(HasRef.hasRef(table))
                ${model.className(table)}.Resolve(errors);
            @endif
        @endfor
        @if(model.hasRelations())
        $resolveRelations$(errors);
        @endif
    }
}
