@import configgen.gengo.CfgMgrModel
@import configgen.gengo.GoName
@import configgen.gengo.StructModel
@import configgen.naming.GenNaming
@import configgen.schema.RefKey
@import configgen.schema.InterfaceSchema
@import configgen.util.StringUtil
@param CfgMgrModel model
    shapeOf := func(value any) *shape {
        switch value.(type) {
            @for(var type : model.relations())
            case *${new GoName(type.owner()).className}: return shapes["${type.owner().fullName()}"]
            @endfor
            default: return nil
        }
    }
    fields := map[any][]any{}
    read := func(value any) []any {
        if result, ok := fields[value]; ok { return result }
        result := shapeOf(value).fields(value); fields[value] = result; return result
    }
    keyOf := func(parts []any) string {
        values := make([]any, len(parts))
        for i, part := range parts { if shapeOf(part) != nil { part = read(part) }; values[i] = part }
        text, err := json.Marshal(values); if err != nil { panic(err) }; return string(text)
    }
    indexes := map[*listView]map[string]map[string]any{}
    lookup := func(list *listView, keys []int) map[string]any {
        definitions := indexes[list]; if definitions == nil { definitions = map[string]map[string]any{}; indexes[list] = definitions }
        definitionBytes, _ := json.Marshal(keys); definition := string(definitionBytes)
        if result, ok := definitions[definition]; ok { return result }
        result := map[string]any{}; definitions[definition] = result
        for _, row := range list.rows {
            members := read(row); parts := make([]any, len(keys)); for i, field := range keys { parts[i] = members[field] }
            key := keyOf(parts); if _, exists := result[key]; exists { panic("duplicate local index key: " + key) }; result[key] = row
        }
        return result
    }
    mapLookup := func(view *mapView) map[string]any {
        if view.lookup == nil {
            view.lookup = map[string]any{}
            for _, pair := range view.entries {
                key := keyOf([]any{pair[0]}); if _, exists := view.lookup[key]; exists { panic("duplicate map key: " + key) }; view.lookup[key] = pair[1]
            }
        }
        return view.lookup
    }
    tables := map[string]*listView{}
    @for(var table : model.cfgValue().schema().sortedTables())
    {
        list := &listView{}
        for _, row := range Get${new GoName(table).className}Mgr().GetAll() { list.rows = append(list.rows, row) }
        tables["${table.name()}"] = list
    }
    @endfor
    occurrences := []*occurrence{}
    var walk func(any, any, string, *occurrence)
    walk = func(value, root any, table string, parent *occurrence) {
        if shape := shapeOf(value); shape != nil {
            at := &occurrence{value, root, table, parent}
            if len(shape.plans) != 0 { occurrences = append(occurrences, at) }
            members := read(value)
            for _, index := range shape.indexes { lookup(members[index.field].(*listView), index.keys) }
            for _, member := range members { walk(member, root, table, at) }
        } else {
            switch container := value.(type) {
            case *listView: for _, item := range container.rows { walk(item, root, table, parent) }
            case *mapView:
                // Scalar keys are unique in Go maps; struct pointers still need comparison by value.
                if len(container.entries) != 0 && shapeOf(container.entries[0][0]) != nil { mapLookup(container) }
                for _, pair := range container.entries { walk(pair[0], root, table, parent); walk(pair[1], root, table, parent) }
            }
        }
    }
    for table, list := range tables { for _, row := range list.rows { walk(row, row, table, nil) } }
    scope := func(name string, at *occurrence) any {
        if name == "" { return at.value }; if name == "root" { return at.root }
        for current := at; current != nil; current = current.parent { if "ancestor:" + shapeOf(current.value).name == name { return current.value } }
        return nil
    }
    for _, at := range occurrences {
        shape := shapeOf(at.value); slots := 0
        for _, plan := range shape.plans { if plan.slot >= slots { slots = plan.slot+1 } }
        refs := make([]any, slots)
        for _, plan := range shape.plans {
            if plan.root != at.table { continue }
            keys := make([]any, len(plan.sources)); valid := true
            for i, source := range plan.sources {
                value := scope(source.scope, at)
                if value == nil { valid = false; continue }
                for _, field := range source.fields { value = read(value)[field] }
                keys[i] = value
            }
            external := len(plan.target) >= 6 && plan.target[:6] == "table:"
            var target any
            if external { target = tables[plan.target[6:]] } else { target = scope(plan.target, at) }
            if !valid || target == nil { panic("missing reference scope: " + shape.name + "." + plan.name) }
            used := 0
            if external { target = lookup(target.(*listView), plan.recordKey)[keyOf(keys[:len(plan.recordKey)])]; used = len(plan.recordKey) }
            for _, step := range plan.steps {
                if target == nil { break }
                member := read(target)[step.field]
                switch step.kind {
                case 0: target = member
                case 1: target = lookup(member.(*listView), step.keys)[keyOf(keys[used:used+len(step.keys)])]
                case 2: target = mapLookup(member.(*mapView))[keyOf(keys[used:used+1])]
                }
                if step.kind == 2 { used++ } else { used += len(step.keys) }
            }
            if target == nil && !plan.nullable { panic("missing object reference: " + shape.name + "." + plan.name + " " + keyOf(keys)) }
            refs[plan.slot] = target
        }
        shape.bind(at.value, refs)
    }
}

