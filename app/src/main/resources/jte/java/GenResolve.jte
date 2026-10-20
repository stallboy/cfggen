@import configgen.genjava.code.*
@import configgen.schema.*
@import configgen.util.StringUtil
@param configgen.genjava.code.GenCfg cfg
@param Structural structural
@param String errorsClass
public void _resolve(${cfg.codeTopPkg()}.ConfigMgr mgr, ${errorsClass} errors) {
        @for(FieldSchema field : structural.fields())
            @if(HasRef.hasRef(field.type()))
                !{
                    String ln = StringUtil.lower1(field.name());
                }
                @if(field.type() instanceof FieldType.StructRef)
                    this.${ln}._resolve(mgr, errors);
                @elseif(field.type() instanceof FieldType.FList fList)
                    for (${TypeStr.boxType(cfg, fList.item())} e : this.${ln}) {
                        e._resolve(mgr, errors);
                    }
                @elseif(field.type() instanceof FieldType.FMap fMap)
                    @if(HasRef.hasRef(fMap.key()))
                        for (var key : this.${ln}.keySet()) key._resolve(mgr, errors);
                    @endif
                    @if(HasRef.hasRef(fMap.value()))
                        for (var value : this.${ln}.values()) value._resolve(mgr, errors);
                    @endif
                @endif
            @endif
        @endfor

        @if(!structural.foreignKeys().isEmpty())
@template.java.GenResolveDirect(cfg = cfg, structural = structural)
        @endif
    }
