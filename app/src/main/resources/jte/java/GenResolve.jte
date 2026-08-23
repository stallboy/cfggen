@import configgen.genjava.code.*
@import configgen.schema.*
@import configgen.util.StringUtil
@param configgen.genjava.code.GenCfg cfg
@param Structural structural
public void _resolve(${cfg.codeTopPkg()}.ConfigMgr mgr) {
        @for(FieldSchema field : structural.fields())
            @if(HasRef.hasRef(field.type()))
                !{
                    String ln = StringUtil.lower1(field.name());
                }
                @if(field.type() instanceof FieldType.StructRef)
                    ${ln}._resolve(mgr);
                @elseif(field.type() instanceof FieldType.FList fList)
                    for (${TypeStr.boxType(cfg, fList.item())} e : ${ln}) {
                        e._resolve(mgr);
                    }
                @elseif(field.type() instanceof FieldType.FMap fMap)
                    for (${TypeStr.boxType(cfg, fMap.value())} v : ${ln}.values()) {
                        v._resolve(mgr);
                    }
                @endif
            @endif
        @endfor

        @if(!structural.foreignKeys().isEmpty())
            _resolveDirect(mgr);
        @endif
    }
