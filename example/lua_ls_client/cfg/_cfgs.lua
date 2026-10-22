local cfg = {}

cfg._mk = require "cfg.mkcfg"
-- Private snapshot state lives at cfg[0], outside schema's string namespace.
do
    local cfg = cfg
    local mkcfg = cfg._mk
    assert(math.type and math.maxinteger >= 9223372036854775807, 'Object references with long fields require 64-bit Lua 5.3+')
    local state = { nodes = setmetatable({}, {__mode='k'}), tables = {}, loaded = {} }
    local shapes = {
        ['LevelRank'] = { fields = {'level','rank'}, indexes = {
        }, plans = {
        } },
        ['Position'] = { fields = {'x','y','z'}, indexes = {
        }, plans = {
        } },
        ['Range'] = { fields = {'min','max'}, indexes = {
        }, plans = {
        } },
        ['ai.TriggerTick.ByLevel'] = { fields = {'init','coefficient'}, indexes = {
        }, plans = {
        } },
        ['ai.TriggerTick.ByServerUpDay'] = { fields = {'init','coefficient1','coefficient2'}, indexes = {
        }, plans = {
        } },
        ['ai.TriggerTick.ConstValue'] = { fields = {'value'}, indexes = {
        }, plans = {
        } },
        ['ai.ai'] = { fields = {'iD','desc','condID','trigTick','trigOdds','actionID','deathRemove'}, indexes = {
        }, plans = {
        } },
        ['ai.ai_action'] = { fields = {'iD','desc','formulaID','argIList','argSList'}, indexes = {
        }, plans = {
        } },
        ['ai.ai_condition'] = { fields = {'iD','desc','formulaID','argIList','argSList'}, indexes = {
        }, plans = {
        } },
        ['combat.AbilityUse'] = { fields = {'binding','machines'}, indexes = {
        }, plans = {
            { root = 'combat.cast', name = 'RefAbility', nullable = false, sources = {
                { scope = '', fields = {1,1} },
            }, target = 'table:combat.ability', recordKey = {1}, steps = {
            } },
        } },
        ['combat.Anchor'] = { fields = {}, indexes = {
        }, plans = {
        } },
        ['combat.AnchorUse'] = { fields = {}, indexes = {
        }, plans = {
            { root = 'combat.ability', name = 'RefAnchor', nullable = false, sources = {
            }, target = 'ancestor:combat.Machine', recordKey = {}, steps = {
                { field = 9, kind = 0, keys = {} },
            } },
        } },
        ['combat.Binding'] = { fields = {'abilityId'}, indexes = {
        }, plans = {
        } },
        ['combat.BookEntry'] = { fields = {'name'}, indexes = {
        }, plans = {
        } },
        ['combat.Combo'] = { fields = {'weapon','tier','airborne','effect'}, indexes = {
        }, plans = {
        } },
        ['combat.Command.Jump'] = { fields = {'key'}, indexes = {
        }, plans = {
            { root = 'combat.encounter', name = 'NullableRefKey', nullable = true, sources = {
                { scope = '', fields = {1} },
            }, target = 'ancestor:combat.Command.StateMachine', recordKey = {}, steps = {
                { field = 1, kind = 1, keys = {1} },
            } },
        } },
        ['combat.Command.Noop'] = { fields = {}, indexes = {
        }, plans = {
        } },
        ['combat.Command.StateMachine'] = { fields = {'phases'}, indexes = {
            { field = 1, keys = {1} },
        }, plans = {
        } },
        ['combat.Effect.Damage'] = { fields = {'amount'}, indexes = {
        }, plans = {
        } },
        ['combat.Effect.Heal'] = { fields = {'amount'}, indexes = {
        }, plans = {
        } },
        ['combat.Effect.None'] = { fields = {}, indexes = {
        }, plans = {
        } },
        ['combat.Exit'] = { fields = {'effect'}, indexes = {
        }, plans = {
        } },
        ['combat.Frame'] = { fields = {'tick','effect'}, indexes = {
        }, plans = {
        } },
        ['combat.Impact'] = { fields = {'ordinal','damage'}, indexes = {
        }, plans = {
        } },
        ['combat.Machine'] = { fields = {'code','phases','impacts','frames','stances','variants','combos','markers','anchor','anchors','onExit','initial','impact','frame','stance','variant','weapon','tier','airborne'}, indexes = {
            { field = 2, keys = {1} },
            { field = 3, keys = {1} },
            { field = 4, keys = {1} },
            { field = 5, keys = {1} },
            { field = 6, keys = {1} },
            { field = 7, keys = {1,2,3} },
            { field = 8, keys = {1} },
        }, plans = {
            { root = 'combat.ability', name = 'RefInitial', nullable = false, sources = {
                { scope = '', fields = {12} },
            }, target = 'ancestor:combat.Machine', recordKey = {}, steps = {
                { field = 2, kind = 1, keys = {1} },
            } },
            { root = 'combat.ability', name = 'RefImpact', nullable = false, sources = {
                { scope = '', fields = {13} },
            }, target = 'ancestor:combat.Machine', recordKey = {}, steps = {
                { field = 3, kind = 1, keys = {1} },
            } },
            { root = 'combat.ability', name = 'RefFrame', nullable = false, sources = {
                { scope = '', fields = {14} },
            }, target = 'ancestor:combat.Machine', recordKey = {}, steps = {
                { field = 4, kind = 1, keys = {1} },
            } },
            { root = 'combat.ability', name = 'RefStance', nullable = false, sources = {
                { scope = '', fields = {15} },
            }, target = 'ancestor:combat.Machine', recordKey = {}, steps = {
                { field = 5, kind = 1, keys = {1} },
            } },
            { root = 'combat.ability', name = 'RefVariant', nullable = false, sources = {
                { scope = '', fields = {16} },
            }, target = 'ancestor:combat.Machine', recordKey = {}, steps = {
                { field = 6, kind = 1, keys = {1} },
            } },
            { root = 'combat.ability', name = 'RefCombo', nullable = false, sources = {
                { scope = '', fields = {17} },
                { scope = '', fields = {18} },
                { scope = '', fields = {19} },
            }, target = 'ancestor:combat.Machine', recordKey = {}, steps = {
                { field = 7, kind = 1, keys = {1,2,3} },
            } },
        } },
        ['combat.MachineUse'] = { fields = {'machineKey','targets'}, indexes = {
        }, plans = {
        } },
        ['combat.Marker'] = { fields = {'key'}, indexes = {
        }, plans = {
        } },
        ['combat.NestedPhase'] = { fields = {'key','action'}, indexes = {
        }, plans = {
        } },
        ['combat.OptionalAim'] = { fields = {'abilityId','machineKey','phaseKey'}, indexes = {
        }, plans = {
            { root = 'combat.cast', name = 'NullableRefTarget', nullable = true, sources = {
                { scope = '', fields = {1} },
                { scope = '', fields = {2} },
                { scope = '', fields = {3} },
            }, target = 'table:combat.ability', recordKey = {1}, steps = {
                { field = 4, kind = 1, keys = {1} },
                { field = 2, kind = 1, keys = {1} },
            } },
        } },
        ['combat.Origin'] = { fields = {'x','y'}, indexes = {
        }, plans = {
        } },
        ['combat.Phase'] = { fields = {'key','next'}, indexes = {
        }, plans = {
            { root = 'combat.ability', name = 'NullableRefNext', nullable = true, sources = {
                { scope = '', fields = {2} },
            }, target = 'ancestor:combat.Machine', recordKey = {}, steps = {
                { field = 2, kind = 1, keys = {1} },
            } },
            { root = 'combat.ability', name = 'RefExit', nullable = false, sources = {
            }, target = 'ancestor:combat.Machine', recordKey = {}, steps = {
                { field = 11, kind = 0, keys = {} },
                { field = 1, kind = 0, keys = {} },
            } },
        } },
        ['combat.PhaseKey'] = { fields = {'name','variant'}, indexes = {
        }, plans = {
        } },
        ['combat.PhaseUse'] = { fields = {'phaseKey'}, indexes = {
        }, plans = {
            { root = 'combat.cast', name = 'RefTarget', nullable = false, sources = {
                { scope = 'ancestor:combat.AbilityUse', fields = {1,1} },
                { scope = 'ancestor:combat.MachineUse', fields = {1} },
                { scope = '', fields = {1} },
            }, target = 'table:combat.ability', recordKey = {1}, steps = {
                { field = 4, kind = 1, keys = {1} },
                { field = 2, kind = 1, keys = {1} },
            } },
            { root = 'combat.cast', name = 'RefByCode', nullable = false, sources = {
                { scope = 'root', fields = {2} },
                { scope = 'ancestor:combat.MachineUse', fields = {1} },
                { scope = '', fields = {1} },
            }, target = 'table:combat.ability', recordKey = {2}, steps = {
                { field = 4, kind = 1, keys = {1} },
                { field = 2, kind = 1, keys = {1} },
            } },
            { root = 'combat.cast', name = 'RefOrigin', nullable = false, sources = {
            }, target = 'root', recordKey = {}, steps = {
                { field = 3, kind = 0, keys = {} },
            } },
        } },
        ['combat.PhaseVariant'] = { fields = {'key','effect'}, indexes = {
        }, plans = {
        } },
        ['combat.Reward'] = { fields = {'count'}, indexes = {
        }, plans = {
        } },
        ['combat.Stance'] = { fields = {'airborne','effect'}, indexes = {
        }, plans = {
        } },
        ['combat.ability'] = { fields = {'id','code','origin','machines','startMachine','startPhase'}, indexes = {
            { field = 4, keys = {1} },
        }, plans = {
            { root = 'combat.ability', name = 'RefInitial', nullable = false, sources = {
                { scope = '', fields = {5} },
                { scope = '', fields = {6} },
            }, target = 'root', recordKey = {}, steps = {
                { field = 4, kind = 1, keys = {1} },
                { field = 2, kind = 1, keys = {1} },
            } },
            { root = 'combat.ability', name = 'RefSpawn', nullable = false, sources = {
            }, target = 'root', recordKey = {}, steps = {
                { field = 3, kind = 0, keys = {} },
            } },
        } },
        ['combat.branchreward'] = { fields = {'key','reward'}, indexes = {
        }, plans = {
        } },
        ['combat.cast'] = { fields = {'id','abilityCode','origin','uses','optional','school','tier','airborne','bookCode','edition','rewardKey'}, indexes = {
        }, plans = {
            { root = 'combat.cast', name = 'RefBook', nullable = false, sources = {
                { scope = '', fields = {6} },
                { scope = '', fields = {7} },
                { scope = '', fields = {8} },
            }, target = 'table:combat.phasebook', recordKey = {1,2,3}, steps = {
                { field = 6, kind = 0, keys = {} },
            } },
            { root = 'combat.cast', name = 'RefBookByCode', nullable = false, sources = {
                { scope = '', fields = {9} },
                { scope = '', fields = {10} },
            }, target = 'table:combat.phasebook', recordKey = {4,5}, steps = {
                { field = 6, kind = 0, keys = {} },
            } },
            { root = 'combat.cast', name = 'RefBookRecord', nullable = false, sources = {
                { scope = 'root', fields = {6} },
                { scope = 'root', fields = {7} },
                { scope = 'root', fields = {8} },
            }, target = 'table:combat.phasebook', recordKey = {1,2,3}, steps = {
            } },
            { root = 'combat.cast', name = 'RefReward', nullable = false, sources = {
                { scope = '', fields = {11} },
            }, target = 'table:combat.branchreward', recordKey = {1}, steps = {
                { field = 2, kind = 0, keys = {} },
            } },
        } },
        ['combat.encounter'] = { fields = {'id','command'}, indexes = {
        }, plans = {
        } },
        ['combat.phasebook'] = { fields = {'school','tier','airborne','code','edition','entry'}, indexes = {
        }, plans = {
        } },
        ['combat.timeline'] = { fields = {'id','events','channels','slots','clock','modes','eventKey','channel','slot','frame','airborne','fallback'}, indexes = {
        }, plans = {
            { root = 'combat.timeline', name = 'RefEventKey', nullable = false, sources = {
                { scope = '', fields = {7} },
            }, target = 'root', recordKey = {}, steps = {
                { field = 2, kind = 2, keys = {} },
            } },
            { root = 'combat.timeline', name = 'RefChannel', nullable = false, sources = {
                { scope = '', fields = {8} },
            }, target = 'root', recordKey = {}, steps = {
                { field = 3, kind = 2, keys = {} },
            } },
            { root = 'combat.timeline', name = 'RefSlot', nullable = false, sources = {
                { scope = '', fields = {9} },
            }, target = 'root', recordKey = {}, steps = {
                { field = 4, kind = 2, keys = {} },
            } },
            { root = 'combat.timeline', name = 'RefFrame', nullable = false, sources = {
                { scope = '', fields = {10} },
            }, target = 'root', recordKey = {}, steps = {
                { field = 5, kind = 2, keys = {} },
            } },
            { root = 'combat.timeline', name = 'RefAirborne', nullable = false, sources = {
                { scope = '', fields = {11} },
            }, target = 'root', recordKey = {}, steps = {
                { field = 6, kind = 2, keys = {} },
            } },
            { root = 'combat.timeline', name = 'RefDefault', nullable = false, sources = {
            }, target = 'root', recordKey = {}, steps = {
                { field = 12, kind = 0, keys = {} },
            } },
        } },
        ['equip.TestPackBean'] = { fields = {'name','iRange'}, indexes = {
        }, plans = {
        } },
        ['equip.ability'] = { fields = {'id','name'}, indexes = {
        }, plans = {
        } },
        ['equip.equip_config'] = { fields = {'entry','stone_count_for_set','draw_protect_name','broadcastid','broadcast_least_quality','week_reward_mailid'}, indexes = {
        }, plans = {
        } },
        ['equip.jewelry'] = { fields = {'iD','name','iconFile','lvlRank','jType','suitID','keyAbility','keyAbilityValue','salePrice','description'}, indexes = {
        }, plans = {
        } },
        ['equip.jewelry_random'] = { fields = {'lvlRank','attackRange','otherRange','testPack'}, indexes = {
        }, plans = {
        } },
        ['equip.jewelry_suit'] = { fields = {'suitID','ename','name','ability1','ability1Value','ability2','ability2Value','ability3','ability3Value','suitList'}, indexes = {
        }, plans = {
        } },
        ['equip.jewelry_type'] = { fields = {'typeName'}, indexes = {
        }, plans = {
        } },
        ['equip.rank'] = { fields = {'rankID','rankName','rankShowName'}, indexes = {
        }, plans = {
        } },
        ['other.ArgCaptureMode'] = { fields = {'name','id','comment'}, indexes = {
        }, plans = {
        } },
        ['other.DropItem'] = { fields = {'chance','itemids','countmin','countmax'}, indexes = {
        }, plans = {
        } },
        ['other.drop'] = { fields = {'dropid','name','items','testmap'}, indexes = {
        }, plans = {
        } },
        ['other.key_test'] = { fields = {'id1','id2','id3','ids','enumTest','enumList'}, indexes = {
        }, plans = {
        } },
        ['other.loot'] = { fields = {'lootid','ename','name','chanceList'}, indexes = {
        }, plans = {
        } },
        ['other.loot_item'] = { fields = {'lootid','itemid','chance','countmin','countmax'}, indexes = {
        }, plans = {
        } },
        ['other.monster'] = { fields = {'id','posList','lootId','lootItemId','enumMap1','enumMap2'}, indexes = {
        }, plans = {
        } },
        ['other.sign_in'] = { fields = {'id','item2countMap','vipitem2vipcountMap','viplevel','iconFile'}, indexes = {
        }, plans = {
        } },
        ['task.TestDefaultBean'] = { fields = {'testInt','testBool','testString','testSubBean','testList','testList2','testMap'}, indexes = {
        }, plans = {
        } },
        ['task.complete_condition_type'] = { fields = {'id','name'}, indexes = {
        }, plans = {
        } },
        ['task.completecondition.Chat'] = { fields = {'msg'}, indexes = {
        }, plans = {
        } },
        ['task.completecondition.CollectItem'] = { fields = {'itemid','count'}, indexes = {
        }, plans = {
        } },
        ['task.completecondition.ConditionAnd'] = { fields = {'cond1','cond2'}, indexes = {
        }, plans = {
        } },
        ['task.completecondition.KillMonster'] = { fields = {'monsterid','count'}, indexes = {
        }, plans = {
        } },
        ['task.completecondition.TalkNpc'] = { fields = {'npcid'}, indexes = {
        }, plans = {
        } },
        ['task.completecondition.TestNoColumn'] = { fields = {}, indexes = {
        }, plans = {
        } },
        ['task.completecondition.aa'] = { fields = {}, indexes = {
        }, plans = {
        } },
        ['task.task'] = { fields = {'taskid','name','nexttask','completecondition','exp','testDefaultBean'}, indexes = {
        }, plans = {
        } },
        ['task.task2'] = { fields = {'taskid','name','nexttask','completecondition','exp','testBool','testString','testStruct','testList','testListStruct','testListInterface'}, indexes = {
        }, plans = {
        } },
        ['task.task_extra_exp'] = { fields = {'taskid','extraexp','test1','test2','fielda','fieldb','fieldc','fieldd'}, indexes = {
        }, plans = {
        } },
    }
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
            if not state.loaded['ai.ai'] then require 'cfg.ai.ai' end
            assert(state.loaded['ai.ai'], 'Missing table: ai.ai')
            if not state.loaded['ai.ai_action'] then require 'cfg.ai.ai_action' end
            assert(state.loaded['ai.ai_action'], 'Missing table: ai.ai_action')
            if not state.loaded['ai.ai_condition'] then require 'cfg.ai.ai_condition' end
            assert(state.loaded['ai.ai_condition'], 'Missing table: ai.ai_condition')
            if not state.loaded['combat.ability'] then require 'cfg.combat.ability' end
            assert(state.loaded['combat.ability'], 'Missing table: combat.ability')
            if not state.loaded['combat.branchreward'] then require 'cfg.combat.branchreward' end
            assert(state.loaded['combat.branchreward'], 'Missing table: combat.branchreward')
            if not state.loaded['combat.cast'] then require 'cfg.combat.cast' end
            assert(state.loaded['combat.cast'], 'Missing table: combat.cast')
            if not state.loaded['combat.encounter'] then require 'cfg.combat.encounter' end
            assert(state.loaded['combat.encounter'], 'Missing table: combat.encounter')
            if not state.loaded['combat.phasebook'] then require 'cfg.combat.phasebook' end
            assert(state.loaded['combat.phasebook'], 'Missing table: combat.phasebook')
            if not state.loaded['combat.timeline'] then require 'cfg.combat.timeline' end
            assert(state.loaded['combat.timeline'], 'Missing table: combat.timeline')
            if not state.loaded['equip.ability'] then require 'cfg.equip.ability' end
            assert(state.loaded['equip.ability'], 'Missing table: equip.ability')
            if not state.loaded['equip.equip_config'] then require 'cfg.equip.equip_config' end
            assert(state.loaded['equip.equip_config'], 'Missing table: equip.equip_config')
            if not state.loaded['equip.jewelry'] then require 'cfg.equip.jewelry' end
            assert(state.loaded['equip.jewelry'], 'Missing table: equip.jewelry')
            if not state.loaded['equip.jewelry_random'] then require 'cfg.equip.jewelry_random' end
            assert(state.loaded['equip.jewelry_random'], 'Missing table: equip.jewelry_random')
            if not state.loaded['equip.jewelry_suit'] then require 'cfg.equip.jewelry_suit' end
            assert(state.loaded['equip.jewelry_suit'], 'Missing table: equip.jewelry_suit')
            if not state.loaded['equip.jewelry_type'] then require 'cfg.equip.jewelry_type' end
            assert(state.loaded['equip.jewelry_type'], 'Missing table: equip.jewelry_type')
            if not state.loaded['equip.rank'] then require 'cfg.equip.rank' end
            assert(state.loaded['equip.rank'], 'Missing table: equip.rank')
            if not state.loaded['other.ArgCaptureMode'] then require 'cfg.other.argcapturemode' end
            assert(state.loaded['other.ArgCaptureMode'], 'Missing table: other.ArgCaptureMode')
            if not state.loaded['other.drop'] then require 'cfg.other.drop' end
            assert(state.loaded['other.drop'], 'Missing table: other.drop')
            if not state.loaded['other.key_test'] then require 'cfg.other.key_test' end
            assert(state.loaded['other.key_test'], 'Missing table: other.key_test')
            if not state.loaded['other.loot'] then require 'cfg.other.loot' end
            assert(state.loaded['other.loot'], 'Missing table: other.loot')
            if not state.loaded['other.loot_item'] then require 'cfg.other.loot_item' end
            assert(state.loaded['other.loot_item'], 'Missing table: other.loot_item')
            if not state.loaded['other.monster'] then require 'cfg.other.monster' end
            assert(state.loaded['other.monster'], 'Missing table: other.monster')
            if not state.loaded['other.sign_in'] then require 'cfg.other.sign_in' end
            assert(state.loaded['other.sign_in'], 'Missing table: other.sign_in')
            if not state.loaded['task.complete_condition_type'] then require 'cfg.task.complete_condition_type' end
            assert(state.loaded['task.complete_condition_type'], 'Missing table: task.complete_condition_type')
            if not state.loaded['task.task'] then require 'cfg.task.task' end
            assert(state.loaded['task.task'], 'Missing table: task.task')
            if not state.loaded['task.task2'] then require 'cfg.task.task2' end
            assert(state.loaded['task.task2'], 'Missing table: task.task2')
            if not state.loaded['task.task_extra_exp'] then require 'cfg.task.task_extra_exp' end
            assert(state.loaded['task.task_extra_exp'], 'Missing table: task.task_extra_exp')
            resolve()
        end)
        if not ok then state.started = false; error(failure) end
    end
    cfg[0] = state
end



local pre = cfg[0].pretable

cfg._last_lang = nil
function cfg._set_lang(lang)
    if cfg._last_lang == lang then
        return
    end
    cfg._last_lang = lang
    cfg._mk.i18n = require("cfg." .. lang)
end

cfg.ai = {}
---@type cfg.ai.ai
cfg.ai.ai = pre("cfg.ai.ai")
---@type cfg.ai.ai_action
cfg.ai.ai_action = pre("cfg.ai.ai_action")
---@type cfg.ai.ai_condition
cfg.ai.ai_condition = pre("cfg.ai.ai_condition")
cfg.combat = {}
---@type cfg.combat.ability
cfg.combat.ability = pre("cfg.combat.ability")
---@type cfg.combat.branchreward
cfg.combat.branchreward = pre("cfg.combat.branchreward")
---@type cfg.combat.cast
cfg.combat.cast = pre("cfg.combat.cast")
---@type cfg.combat.encounter
cfg.combat.encounter = pre("cfg.combat.encounter")
---@type cfg.combat.phasebook
cfg.combat.phasebook = pre("cfg.combat.phasebook")
---@type cfg.combat.timeline
cfg.combat.timeline = pre("cfg.combat.timeline")
cfg.equip = {}
---@type cfg.equip.ability
cfg.equip.ability = pre("cfg.equip.ability")
---@type cfg.equip.equip_config
cfg.equip.equip_config = pre("cfg.equip.equip_config")
---@type cfg.equip.jewelry
cfg.equip.jewelry = pre("cfg.equip.jewelry")
---@type cfg.equip.jewelry_random
cfg.equip.jewelry_random = pre("cfg.equip.jewelry_random")
---@type cfg.equip.jewelry_suit
cfg.equip.jewelry_suit = pre("cfg.equip.jewelry_suit")
---@type cfg.equip.jewelry_type
cfg.equip.jewelry_type = pre("cfg.equip.jewelry_type")
---@type cfg.equip.rank
cfg.equip.rank = pre("cfg.equip.rank")
cfg.other = {}
---@type cfg.other.argcapturemode
cfg.other.argcapturemode = pre("cfg.other.argcapturemode")
---@type cfg.other.drop
cfg.other.drop = pre("cfg.other.drop")
---@type cfg.other.key_test
cfg.other.key_test = pre("cfg.other.key_test")
---@type cfg.other.loot
cfg.other.loot = pre("cfg.other.loot")
---@type cfg.other.loot_item
cfg.other.loot_item = pre("cfg.other.loot_item")
---@type cfg.other.monster
cfg.other.monster = pre("cfg.other.monster")
---@type cfg.other.sign_in
cfg.other.sign_in = pre("cfg.other.sign_in")
cfg.task = {}
---@type cfg.task.complete_condition_type
cfg.task.complete_condition_type = pre("cfg.task.complete_condition_type")
---@type cfg.task.task
cfg.task.task = pre("cfg.task.task")
---@type cfg.task.task2
cfg.task.task2 = pre("cfg.task.task2")
---@type cfg.task.task_extra_exp
cfg.task.task_extra_exp = pre("cfg.task.task_extra_exp")

return cfg
