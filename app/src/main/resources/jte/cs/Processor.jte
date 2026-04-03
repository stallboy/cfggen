@import configgen.gencs.ProcessorModel
@import configgen.schema.HasRef
@import configgen.schema.TableSchema
@param ProcessorModel model
@if(!model.topPkg.equals("Config"))
using Config;
@endif


namespace ${model.topPkg};

public static class Processor
{
    // 从 bytes 文件加载（新格式）
    public static void Process(ConfigReader reader)
    {
        var configNulls = new List<string>
        {
            @for(TableSchema table : model.tableSchemas)
                "${table.name()}",
            @endfor
        };

        // 读取表数量
        int tableCount = reader.ReadInt32();

        for (int i = 0; i < tableCount; i++)
        {
            // 读取表名
            string tableName = reader.ReadTableName();
            // 读取表大小
            int tableSize = reader.ReadInt32();

            // 根据表名分发到对应的 Initialize 方法
            switch(tableName)
            {
                @for(TableSchema table : model.tableSchemas)
                    case "${table.name()}":
                        configNulls.Remove(tableName);
                        ${model.fullName(table)}.Initialize(reader);
                        break;
                @endfor
                default:
                    // 未知表，跳过
                    reader.SkipBytes(tableSize);
                    break;
            }
        }
        foreach (var t in configNulls)
            reader.TableNotInData(t);
        // 解析外键引用
        @for(TableSchema table : model.tableSchemas)
            @if(HasRef.hasRef(table))
                ${model.fullName(table)}.Resolve(reader);
            @endif
        @endfor
    }
}
