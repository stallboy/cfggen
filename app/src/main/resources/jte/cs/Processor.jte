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

        public static void Process(Config.Stream os)
        {
            var configNulls = new List<string>
            {
                @for(TableSchema table : model.tableSchemas)
                    "${table.name()}",
                @endfor
            };
            for(;;)
            {
                var csv = os.ReadCfg();
                if (csv == null)
                    break;
                switch(csv)
                {
                    @for(TableSchema table : model.tableSchemas)
                        case "${table.name()}":
                            configNulls.Remove(csv);
                            ${model.fullName(table)}.Initialize(os, Errors);
                            break;
                    @endfor
                    default:
                        Errors.ConfigDataAdd(csv);
                        break;
                }
            }
            foreach (var csv in configNulls)
                Errors.ConfigNull(csv);
            @for(TableSchema table : model.tableSchemas)
                @if(HasRef.hasRef(table))
                    ${model.fullName(table)}.Resolve(Errors);
                @endif
            @endfor
        }

    }
}
