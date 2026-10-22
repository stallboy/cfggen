@import configgen.genlua.LuaRelationsModel
@import configgen.naming.GenNaming
@import configgen.schema.RefKey
@param LuaRelationsModel model
-- Private snapshot state lives at cfg[0], outside schema's string namespace.
do
    local cfg = ${model.pkg()}
    local mkcfg = cfg._mk
    @if(model.hasLong())
    assert(math.type and math.maxinteger >= 9223372036854775807, 'Object references with long fields require 64-bit Lua 5.3+')
    @endif
    local state = { nodes = setmetatable({}, {__mode='k'}), tables = {}, loaded = {} }
    local shapes = {
        @for(var type : model.types())
        ['${type.owner().fullName()}'] = { fields = ${model.fields(type.owner())}, indexes = {
            @for(var index : type.indexes())
            { field = ${index.field() + 1}, keys = ${model.ints(index.keys())} },
            @endfor
        }, plans = {
            @for(var plan : type.plans())
            { root = '${plan.root()}', name = '${GenNaming.refFieldName(plan.reference())}', nullable = ${((RefKey.RefSimple) plan.reference().refKey()).nullable()}, sources = {
                @for(var source : plan.sources())
                { scope = '${source.scope()}', fields = ${model.ints(source.fields())} },
                @endfor
            }, target = '${plan.target()}', recordKey = ${model.ints(plan.recordKey())}, steps = {
                @for(var step : plan.steps())
                { field = ${step.field() + 1}, kind = ${step.kind()}, keys = ${model.ints(step.keys())} },
                @endfor
            } },
            @endfor
        } },
        @endfor
    }
@template.lua.RelationResolver(model = model)

