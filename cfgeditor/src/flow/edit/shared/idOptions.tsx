import {STable} from "@/api/schemaModel.ts";
import {EntityEditFieldOption} from "@/domain/entityModel.ts";
import {Flex} from "antd";
import {CSSProperties} from "react";
import {NEW_RECORD_ID} from "@/domain/schema.ts";

const suffixStyle: CSSProperties = {
    color: '#597ef7',
    fontSize: '0.85em',
    textOverflow: "ellipsis", // 建议用 ellipsis (省略号) 比 clip 更友好
    whiteSpace: 'nowrap',
    overflow: 'hidden',
    flex: 1,                // 核心：占据剩余所有空间
    minWidth: 0,            // 核心：允许收缩，触发截断
    textAlign: 'right',     // 让 title 依然靠右展示
    marginLeft: '8px'       // 给 ID 和 Title 之间留一点固定间距
}

export function getIdOptions(sTable: STable, valueToInteger: boolean = false): EntityEditFieldOption[] {
    const options = [];
    for (const {id, title} of sTable.recordIds) {
        const isShowTitle = title && title != id;
        options.push({
            label: isShowTitle ? <Flex align="flex-end" style={{width: '100%'}}>
                <span style={{flexShrink: 0}}>{id}</span>
                <span style={suffixStyle}>{title}</span>
            </Flex> : id,
            labelstr: isShowTitle ? `${id} ${title}` : id,
            value: valueToInteger ? parseInt(id) : id,
            title: title ?? ''
        });
    }
    return options;
}

export function getIdOptionsWithNew(sTable: STable, valueToInteger: boolean = false): EntityEditFieldOption[] {
    const options = getIdOptions(sTable, valueToInteger);

    // 当没有记录时添加新记录选项
    if (options.length === 0) {
        options.push({
            label: <>➕ new</>,
            labelstr: NEW_RECORD_ID,
            value: NEW_RECORD_ID,
            title: 'Create new record'
        });
    }

    return options;
}
