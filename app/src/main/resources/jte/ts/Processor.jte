@import configgen.gents.TsCodeGenerator
@import configgen.schema.HasRef
@import configgen.schema.TableSchema
@param TsCodeGenerator model

export class Processor {
    static Process(os: Stream, errors: LoadErrors) {
        !{var sortedTables = model.cfgSchema.sortedTables();}
        const configs = new Set<string>([
            @for(TableSchema table : sortedTables)
                "${table.name()}",
            @endfor
        ]);

        for(;;) {
            const cfg = os.ReadCfg();
            if (cfg === null) {
                break;
            }
            switch(cfg) {
                @for(TableSchema table : sortedTables)
                    case "${table.name()}":
                        configs.delete(cfg);
                        ${model.className(table)}.Initialize(os, errors);
                        break;
                @endfor
                default:
                    errors.ConfigDataAdd(cfg);
                    break;
            }
        }
        errors.ConfigsNull(configs);

        @for(TableSchema table : sortedTables)
            @if(HasRef.hasRef(table))
                ${model.className(table)}.Resolve(errors);
            @endif
        @endfor
    }
}