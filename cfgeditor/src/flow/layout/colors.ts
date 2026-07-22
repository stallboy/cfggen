import type {DisplayField, Entity, EntityEditField} from "@/domain/entityModel";
import {EntityType, isCardEntity, isEditableEntity, isReadOnlyEntity} from "@/domain/entityModel";
import type {NodeShowType} from "@/domain/storageJson";

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
 * @param nodeShow 已解析的配色配置（含 per-graph override）。FlowNode 传入 nodeProps.data.nodeShow
 *                 （nodeShow 经 node.data 下发，不再盖章到 entity.sharedSetting）。
 *                 显式入参是为了让调用方把 nodeShow 放进 useMemo deps——否则 entity 引用不变时改主题色会 stale。
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
        // 需要提取值的字段类型：primitive / arrayOfPrimitive / interface。
        // 用显式判别比较（而非 Set.has）——TS 只认判别式收窄，否则取不到具体成员的 value。
        if (field.type === "primitive" || field.type === "arrayOfPrimitive" || field.type === "interface") {
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

// ============================================================================
// 可读文字色（按背景自动反色）
// ============================================================================

// 节点底色可被用户经 nodeColorsByValue/Label 任意配置（包括浅色）。
// 节点标题/资源按钮原硬编码 #fff，浅底色上白字会糊掉不可读——按底色自动反色解决。
//
// 用 YIQ 感知亮度（Bootstrap color-yiq 同款）而非 WCAG 相对亮度：
// 阈值 150 刻意偏低，保证本仓默认调色板（#0898b5/#207b4a/#006d75/#003eb3，YIQ 均 <150）
// 全部保留白字、视觉一致；只有真正浅色底（黄/浅蓝/粉等，YIQ≥150）才翻黑字。
// 若改用 WCAG 0.179 阈值，#0898b5(YIQ≈112, L≈0.26) 会被判为"该用黑字"，
// 导致主节点色翻黑、与其它默认色白字不一致——故取 YIQ。
const READABLE_BRIGHTNESS_THRESHOLD = 150;

export function getReadableTextColor(bg: string): string {
    return perceivedBrightness(bg) >= READABLE_BRIGHTNESS_THRESHOLD ? '#000000' : '#ffffff';
}

function perceivedBrightness(hex: string): number {
    const n = parseHexToRgb(hex);
    if (n === null) return 0; // 解析失败按暗色处理 → 白字（与原硬编码 #fff 行为一致）
    const r = (n >> 16) & 0xff;
    const g = (n >> 8) & 0xff;
    const b = n & 0xff;
    return (r * 299 + g * 587 + b * 114) / 1000;
}

// 解析 #rgb / #rrggbb / #rrggbbaa 为 0xrrggbb 数字；非法返回 null。
function parseHexToRgb(hex: string): number | null {
    if (typeof hex !== 'string') return null;
    let s = hex.trim().replace(/^#/, '');
    if (s.length === 3) s = s.split('').map(c => c + c).join('');
    if (s.length === 8) s = s.slice(0, 6); // 带 alpha：取 rgb 部分
    return /^[0-9a-fA-F]{6}$/.test(s) ? parseInt(s, 16) : null;
}
