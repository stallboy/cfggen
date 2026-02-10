@import configgen.gencs.ProcessorModel
@import configgen.schema.HasRef
@import configgen.schema.TableSchema
@param ProcessorModel model
using System.Collections.Generic;
@if(!model.topPkg.equals("Config"))
    using Config;
@endif


namespace ${model.topPkg}
{
    public static class Processor
    {
        public static readonly LoadErrors Errors = new LoadErrors();

        // 从 bytes 文件加载（新格式）
        public static void Process(Config.Stream os)
        {
            var configNulls = new List<string>
            {
                @for(TableSchema table : model.tableSchemas)
                    "${table.name()}",
                @endfor
            };

            // 读取表数量
            int tableCount = os.ReadInt32();

            for (int i = 0; i < tableCount; i++)
            {
                // 从 StringPool 读取表名
                string tableName = os.ReadString();
                // 读取表大小
                int tableSize = os.ReadInt32();

                // 根据表名分发到对应的 Initialize 方法
                switch(tableName)
                {
                    @for(TableSchema table : model.tableSchemas)
                        case "${table.name()}":
                            configNulls.Remove(tableName);
                            ${model.fullName(table)}.Initialize(os, Errors);
                            break;
                    @endfor
                    default:
                        // 未知表，跳过
                        os.SkipBytes(tableSize);
                        break;
                }
            }
            foreach (var t in configNulls)
                Errors.ConfigNull(t);
            // 解析外键引用
            @for(TableSchema table : model.tableSchemas)
                @if(HasRef.hasRef(table))
                    ${model.fullName(table)}.Resolve(Errors);
                @endif
            @endfor
        }
    }
}