@import configgen.gents.TsCodeGenerator
@import configgen.schema.HasRef
@import configgen.schema.TableSchema
@param TsCodeGenerator model

export class Processor {

    // 从 bytes 文件加载（新格式）
    static Process(os: Stream, errors: LoadErrors): void {
        const configNulls = new Set<string>([
            @for(TableSchema table : model.cfgSchema.sortedTables())
                "${table.name()}",
            @endfor
        ]);

        // 读取表数量
        const tableCount = os.ReadSize();

        for (let i = 0; i < tableCount; i++) {
            // 读取表名
            const tableName = os.ReadString();
            // 读取表大小
            const tableSize = os.ReadSize();

            // 根据表名分发到对应的 Initialize 方法
            switch(tableName) {
                @for(TableSchema table : model.cfgSchema.sortedTables())
                    case "${table.name()}":
                        configNulls.delete(tableName);
                        ${model.className(table)}.Initialize(os, errors);
                        break;
                @endfor
                default:
                    // 未知表，跳过
                    os.SkipBytes(tableSize);
                    break;
            }
        }

        // 检查缺失的表
        for (const t of configNulls) {
            errors.ConfigNull(t);
        }

        // 解析外键引用
        @for(TableSchema table : model.cfgSchema.sortedTables())
            @if(HasRef.hasRef(table))
                ${model.className(table)}.Resolve(errors);
            @endif
        @endfor
    }
}
