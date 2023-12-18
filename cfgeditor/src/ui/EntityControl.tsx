import {ClassicPreset} from "rete";
import {Entity, FieldsShowType} from "../model/graphModel.ts";
import {Collapse, Space} from "antd";
import {EntityTable} from "./EntityTable.tsx";
import {EntityForm} from "./EntityForm.tsx";


function dummpyOnChange(_key: string | string[]) {
}

export class EntityControl extends ClassicPreset.Control {
    onChange: (key: string | string[]) => void = dummpyOnChange;

    constructor(public entity: Entity) {
        super();
    }
}

export function EntityControlComponent(props: { data: EntityControl }) {
    let entity: Entity = props.data.entity;
    if (entity.fieldsShow == FieldsShowType.Edit) {
        let fields = entity.editFields;
        if (!fields || fields.length == 0) {
            return <Space/>
        }

        return <EntityForm fields={fields}/>
    }


    let fields = entity.fields;
    if (fields.length == 0) {
        return <Space/>
    }

    let tab = <EntityTable fields={fields}/>;
    if (entity.fieldsShow == FieldsShowType.Direct) {
        return tab;
    }

    let items = [{key: '1', label: `${fields.length} fields`, children: tab}];
    switch (entity.fieldsShow) {
        case FieldsShowType.Expand:
            return <Collapse defaultActiveKey={'1'} items={items} onChange={props.data.onChange}/>
        case FieldsShowType.Fold:
            return <Collapse items={items} onChange={props.data.onChange}/>
    }
}
