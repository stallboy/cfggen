import type {DisplayField, Entity, EntityEditField} from "@/domain/entityModel";
import {EntityType, isCardEntity, isEditableEntity, isReadOnlyEntity} from "@/domain/entityModel";
import type {NodeShowType} from "@/domain/storageJson";


// 需要提取值的字段类型
const VALUE_FIELD_TYPES = new Set(["primitive", "arrayOfPrimitive", "interface"]);

// ============================================================================
// 默认颜色值
// ============================================================================
// NODE_SHOW_DEFAULTS 是 nodeShow 各颜色字段缺失时的"兜底默认色"单一来源，
// 供 colors.ts 内部、colorUtils、测试断言共用，避免 #0898b5/#207b4a 等 hex 散落多处。
//
// 注意：这些 DEFAULT 与 antd token 刻意解耦——colors.ts 是纯函数、无 hook 上下文，
// 拿不到 useToken；若未来要跟随主题，需在调用点（FlowNode）useToken 后把解析值传入。
// store.ts 里的同值是持久化进 NodeShowType 的初始值（另一关注点，且 oxlint 禁 store→flow），
// 二者刻意保持同值但不互相 import。
export const NODE_SHOW_DEFAULTS = {
    nodeColor: "#0898b5",
    nodeRefColor: "#207b4a",
    nodeRef2Color: "#006d75",
    nodeRefInColor: "#003eb3",
    edgeColor: "#0898b5",
} as const;

// ============================================================================
// 节点背景色
// ============================================================================

/**
 * 节点背景色：按"值 → 标签 → 实体类型"三级优先级解析。
 *
 * @param entity  实体（取值串/标签/entityType）
 * @param nodeShow 已解析的配色配置（含 per-graph override）。FlowNode 传入 entity.sharedSetting?.nodeShow。
 *                 显式入参（而非内部深读 entity.sharedSetting?.nodeShow）是为了让调用方把 nodeShow
 *                 放进 useMemo deps——否则 entity 引用不变时改主题色会 stale（见 FlowNode color memo）。
 */
export function getNodeBackgroundColor(entity: Entity, nodeShow?: NodeShowType): string {
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


function getTypeColor(entityType: EntityType | undefined, nodeShow: NodeShowType | undefined): string {
    switch (entityType) {
        case EntityType.Ref:
            return nodeShow?.nodeRefColor ?? NODE_SHOW_DEFAULTS.nodeRefColor;
        case EntityType.Ref2:
            return nodeShow?.nodeRef2Color ?? NODE_SHOW_DEFAULTS.nodeRef2Color;
        case EntityType.RefIn:
            return nodeShow?.nodeRefInColor ?? NODE_SHOW_DEFAULTS.nodeRefInColor;
        default:
            return nodeShow?.nodeColor ?? NODE_SHOW_DEFAULTS.nodeColor;
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
    return nodeShow?.edgeColor ?? NODE_SHOW_DEFAULTS.edgeColor;
}
