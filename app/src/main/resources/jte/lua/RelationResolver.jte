@import configgen.genlua.LuaRelationsModel
@import configgen.naming.GenNaming
@import configgen.schema.RefKey
@param LuaRelationsModel model
    local function read(value, field)
        return value[shapes[state.nodes[value]].fields[field]]
    end
    local encode
    encode = function(value)
        local kind = type(value)
        if kind == 'table' then
            local name = assert(state.nodes[value], 'Unregistered struct key')
            local parts = {'o', #name, ':', name}
            for i in ipairs(shapes[name].fields) do
                local part = encode(read(value, i))
                parts[#parts+1] = #part .. ':' .. part
            end
            return table.concat(parts)
        elseif kind == 'string' then return 's' .. #value .. ':' .. value
        elseif kind == 'number' then return 'n' .. tostring(math.tointeger and math.tointeger(value) or value)
        elseif kind == 'boolean' then return value and 'b1' or 'b0'
        end
        error('Invalid reference key: ' .. kind)
    end
    local function key(parts)
        local encoded = {}
        for i, value in ipairs(parts) do local part = encode(value); encoded[i] = #part .. ':' .. part end
        return table.concat(encoded)
    end
    function state.node(name, value)
        state.nodes[value] = name
        return value
    end
    function state.bean(name, construct)
        return function(...) return state.node(name, construct(...)) end
    end
    -- Retain the public scalar-key API; composite/struct keys use full value tuples.
    local function publicKey(parts)
        if #parts == 1 and type(parts[1]) ~= 'table' then return parts[1] end
        return key(parts)
    end
    function state.table(name, target, keys, metadata, textFields)
        local rows = {}
        state.tables[name] = rows
        target.Metadata = metadata
        local construct = mkcfg.i18n_bean(metadata.refs, textFields, (unpack or table.unpack)(metadata.fields))
        for _, definition in ipairs(keys) do
            local lookup = {}
            target[definition[1]] = lookup
            target[definition[2]] = function(...) return lookup[publicKey({...})] end
        end
        local function put(row, replace)
            for _, definition in ipairs(keys) do
                local parts = {}
                for i, field in ipairs(definition[3]) do parts[i] = row[field] end
                local lookup, value = target[definition[1]], publicKey(parts)
                assert(replace or lookup[value] == nil, 'Duplicate table key: ' .. name)
                lookup[value] = row
            end
        end
        target._add = function(...)
            local row = state.node(name, construct(...))
            put(row, true)
            if not metadata.enumidx and #keys == 1 and #keys[1][3] == 1 then return row end
        end
        target._del = function(...) target[keys[1][1]][publicKey({...})] = nil end
        return function(...)
            local row = state.node(name, construct(...))
            rows[#rows+1] = row
            put(row, false)
            local symbol = metadata.enumidx and row[metadata.enumidx]
            if symbol and symbol ~= '' then target[symbol] = row end
            return row
        end
    end
    function state.pretable(module)
        local loaded = false
        return setmetatable({}, { __index = function(target, field)
            if not loaded then loaded = true; require(module) end
            return rawget(target, field)
        end })
    end
    local function resolve()
        local occurrences, indexes, maps = {}, {}, {}
        local function index(rows, fields)
            local definitions = indexes[rows]
            if not definitions then definitions = {}; indexes[rows] = definitions end
            local definition = table.concat(fields, ',')
            if definitions[definition] then return definitions[definition] end
            local lookup = {}; definitions[definition] = lookup
            for _, row in ipairs(rows) do
                local parts = {}; for i, field in ipairs(fields) do parts[i] = read(row, field) end
                local value = key(parts)
                assert(lookup[value] == nil, 'Duplicate local index key: ' .. value)
                lookup[value] = row
            end
            return lookup
        end
        local function mapIndex(values)
            if maps[values] then return maps[values] end
            local lookup = {}; maps[values] = lookup
            for source, value in pairs(values) do
                local encoded = key({source})
                assert(lookup[encoded] == nil, 'Duplicate map key: ' .. encoded)
                lookup[encoded] = value
            end
            return lookup
        end
        local walk
        walk = function(value, root, owner, parent)
            if type(value) ~= 'table' then return end
            local shape = shapes[state.nodes[value]]
            if shape then
                local at = {value=value, root=root, owner=owner, parent=parent}
                if #shape.plans > 0 then occurrences[#occurrences+1] = at end
                for _, definition in ipairs(shape.indexes) do index(read(value, definition.field), definition.keys) end
                for i in ipairs(shape.fields) do walk(read(value, i), root, owner, at) end
            else
                for source, item in pairs(value) do
                    local kind = type(source)
                    -- Lua already compares scalar keys by value; struct keys need canonical comparison.
                    if kind ~= 'number' and kind ~= 'string' and kind ~= 'boolean' then mapIndex(value) end
                    walk(source, root, owner, parent); walk(item, root, owner, parent)
                end
            end
        end
        for owner, rows in pairs(state.tables) do for _, row in ipairs(rows) do walk(row, row, owner, nil) end end
        local function scope(name, at)
            if name == '' then return at.value end
            if name == 'root' then return at.root end
            while at do
                local value = at.value
                if 'ancestor:' .. state.nodes[value] == name then return value end
                at = at.parent
            end
            return nil
        end
        local function slice(values, start, count)
            local result = {}; for i = 1, count do result[i] = values[start+i-1] end; return result
        end
        for _, at in ipairs(occurrences) do
            local shape = shapes[state.nodes[at.value]]
            for _, plan in ipairs(shape.plans) do
                if plan.root == at.owner then
                    local keys, valid = {}, true
                    for i, source in ipairs(plan.sources) do
                        local value = scope(source.scope, at)
                        if value ~= nil then
                            for _, field in ipairs(source.fields) do value = read(value, field) end
                            keys[i] = value
                        else valid = false end
                    end
                    local external = plan.target:sub(1,6) == 'table:'
                    local target
                    if external then target = state.tables[plan.target:sub(7)] else target = scope(plan.target, at) end
                    assert(valid and target ~= nil, 'Missing reference scope: ' .. state.nodes[at.value] .. '.' .. plan.name)
                    local used = 1
                    if external then target = index(target, plan.recordKey)[key(slice(keys, used, #plan.recordKey))]; used = used + #plan.recordKey end
                    for _, step in ipairs(plan.steps) do
                        if target == nil then break end
                        local member = read(target, step.field)
                        if step.kind == 0 then target = member
                        elseif step.kind == 1 then target = index(member, step.keys)[key(slice(keys, used, #step.keys))]
                        else target = mapIndex(member)[key({keys[used]})] end
                        used = used + (step.kind == 2 and 1 or #step.keys)
                    end
                    assert(target ~= nil or plan.nullable, 'Missing object reference: ' .. state.nodes[at.value] .. '.' .. plan.name .. ' ' .. key(keys))
                    rawset(at.value, plan.name, target)
                end
            end
        end
    end
    function state.complete(name)
        state.loaded[name] = true
        if state.started then return end
        state.started = true
        local ok, failure = pcall(function()
            @for(var table : model.schema().sortedTables())
            if not state.loaded['${table.name()}'] then require '${model.pkg()}.${table.name().toLowerCase(java.util.Locale.ROOT)}' end
            assert(state.loaded['${table.name()}'], 'Missing table: ${table.name()}')
            @endfor
            resolve()
        end)
        if not ok then state.started = false; error(failure) end
    end
    cfg[0] = state
end

