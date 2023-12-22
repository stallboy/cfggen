import {ClassicPreset} from "rete";
import {Entity, FieldsShowType} from "../model/entityModel.ts";
import {Collapse, Space} from "antd";
import {EntityTable} from "./EntityTable.tsx";
import {EntityForm} from "./EntityForm.tsx";
import {useEffect, useRef} from "react";
import {EntityCard} from "./EntityCard.tsx";

export class EntityControl extends ClassicPreset.Control {
    constructor(public entity: Entity,
                public changeHeightCallback: (height: number) => void) {
        super();
    }
}

export function EntityControlComponent(props: { data: EntityControl }) {
    const ref = useRef<HTMLDivElement>(null);


    useEffect(() => {
        if (ref.current) {
            // console.log(ref.current.offsetHeight);
            props.data.changeHeightCallback(ref.current.offsetHeight);

            const resize = new ResizeObserver(() => {
                if (ref.current) {
                    props.data.changeHeightCallback(ref.current.offsetHeight);
                }
            });
            resize.observe(ref.current);
            return () => {
                if (ref.current) {
                    resize.unobserve(ref.current);
                }
            };
        }
    }, []);

    let content;
    let entity: Entity = props.data.entity;
    if (entity.editFields) {
        if (entity.editFields.length == 0) {
            content = <Space/>;
        } else {
            content = <EntityForm fields={entity.editFields} onUpdateValues={entity.editOnUpdateValues!!}/>;
        }
    } else if (entity.fields) {
        let fields = entity.fields;
        if (fields.length == 0) {
            content = <Space/>;
        } else {
            let tab = <EntityTable fields={fields}/>;
            if (entity.fieldsShow == FieldsShowType.Direct) {
                content = tab;
            } else {
                let items = [{key: '1', label: `${fields.length} fields`, children: tab}];
                if (entity.fieldsShow == FieldsShowType.Expand) {
                    content = <Collapse defaultActiveKey={'1'} items={items}/>;
                } else {
                    content = <Collapse items={items}/>
                }
            }
        }
    } else if (entity.brief) {
        content = <EntityCard brief={entity.brief}/>;

    } else {
        content = <Space/>;
    }

    return <div ref={ref}> {content}</div>

}
