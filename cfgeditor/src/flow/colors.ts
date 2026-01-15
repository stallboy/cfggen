import type {DisplayField, Entity, EntityEditField} from "./entityModel.ts";
import {EntityType, isCardEntity, isEditableEntity, isReadOnlyEntity} from "./entityModel.ts";
import type {NodeShowType} from "../store/storageJson.ts";


// 需要提取值的字段类型
const VALUE_FIELD_TYPES = new Set(["primitive", "arrayOfPrimitive", "interface"]);

// ============================================================================
// 节点背景色
// ============================================================================

export function getNodeBackgroundColor(entity: Entity): string {
    const {nodeShow} = entity.sharedSetting ?? {};

    // 1. 按值着色（优先级最高）
    if (nodeShow?.nodeColorsByValue.length) {
        const valueStr = getEntityValueString(entity);
        if (valueStr) {
            for (const {keyword, color} of nodeShow.nodeColorsByValue) {
                if (valueStr.includes(keyword)) {
                    return color;
                }
            }
        }
    }

    // 2. 按标签着色
    if (nodeShow?.nodeColorsByLabel.length) {
        for (const {keyword, color} of nodeShow.nodeColorsByLabel) {
            if (entity.label.includes(keyword)) {
                return color;
            }
        }
    }

    // 3. 按实体类型着色
    return getTypeColor(entity.entityType, nodeShow);
}

// 默认颜色值
const DEFAULT_NODE_COLOR = "#0898b5";
const DEFAULT_REF_COLOR = "#207b4a";
const DEFAULT_REF2_COLOR = "#006d75";
const DEFAULT_REF_IN_COLOR = "#003eb3";
const DEFAULT_EDGE_COLOR = "#0898b5";


function getTypeColor(entityType: EntityType | undefined, nodeShow: NodeShowType | undefined): string {
    switch (entityType) {
        case EntityType.Ref:
            return nodeShow?.nodeRefColor ?? DEFAULT_REF_COLOR;
        case EntityType.Ref2:
            return nodeShow?.nodeRef2Color ?? DEFAULT_REF2_COLOR;
        case EntityType.RefIn:
            return nodeShow?.nodeRefInColor ?? DEFAULT_REF_IN_COLOR;
        default:
            return nodeShow?.nodeColor ?? DEFAULT_NODE_COLOR;
    }
}

// ============================================================================
// 实体值字符串获取
// ============================================================================

function getEntityValueString(entity: Entity): string | undefined {
    if (isCardEntity(entity)) {
        return entity.brief.value;
    }

    if (isReadOnlyEntity(entity)) {
        return entity.fields.map((f) => f.value).join(",");
    }

    if (isEditableEntity(entity)) {
        const values: string[] = [];
        collectEditFieldValues(values, entity.edit.fields);
        return values.join(",");
    }

    return undefined;
}

function collectEditFieldValues(values: string[], fields: EntityEditField[]): void {
    for (const field of fields) {
        if (VALUE_FIELD_TYPES.has(field.type)) {
            values.push(String(field.value));
        }
        if (field.type === "interface") {
            collectEditFieldValues(values, field.implFields);
        }
    }
}

// ============================================================================
// 字段背景色
// ============================================================================

export function getFieldBackgroundColor(
    field: DisplayField | EntityEditField,
    nodeShow?: NodeShowType
): string | undefined {
    if (!nodeShow?.fieldColorsByName.length) return undefined;

    for (const {keyword, color} of nodeShow.fieldColorsByName) {
        if (field.name === keyword) {
            return color;
        }
    }

    return undefined;
}

// ============================================================================
// 边颜色
// ============================================================================

export function getEdgeColor(nodeShow?: NodeShowType): string {
    return nodeShow?.edgeColor ?? DEFAULT_EDGE_COLOR;
}
