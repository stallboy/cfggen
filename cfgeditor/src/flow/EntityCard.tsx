import {Card, Descriptions, Tooltip} from "antd";
import type {DescriptionsProps} from "antd";
// antd 最佳实践：用根导出的 DescriptionsProps 派生类型，避免深路径 antd/es/*
type DescriptionsItemType = NonNullable<DescriptionsProps['items']>[number];
import {convertFileSrc, isTauri} from "@tauri-apps/api/core";
import {CSSProperties, memo, ReactElement} from "react";
import {CardEntity, Entity, isCardEntity} from "@/domain/entityModel";
import type {NodeShowType} from "@/domain/storageJson";
import {getDsLenAndDesc} from "./layout/getDsLenAndDesc.ts";
import {Highlight} from "./Highlight.tsx";
import {useMyStore} from "@/store/store";

// ============================================================================
// 常量定义
// ============================================================================

const IMAGE_STYLE: CSSProperties = {maxHeight: "220px", objectFit: "scale-down"};

const DESC_STYLE: CSSProperties = {whiteSpace: "break-spaces"};

// ============================================================================
// 描述构建函数
// ============================================================================

function buildDescriptionItems(
    descriptions: CardEntity["brief"]["descriptions"],
    count: number
): DescriptionsItemType[] {
    if (!descriptions || count <= 0) return [];

    return Array.from({length: count}, (_, i) => {
        const d = descriptions[i];
        return {
            key: i,
            label: <Tooltip title={d.comment}>{d.field}</Tooltip>,
            children: d.value,
        };
    });
}

function buildCardDescription(
    descriptions: CardEntity["brief"]["descriptions"],
    showCount: number,
    text: string,
    keyword?: string
): ReactElement | undefined {
    if (!text) return undefined;

    const content = keyword ? <Highlight text={text} keyword={keyword}/> : text;

    if (showCount > 0 && descriptions) {
        const items = buildDescriptionItems(descriptions, showCount);
        return (
            <>
                <Descriptions column={1} bordered size="small" items={items}/>
                <div style={DESC_STYLE}>{content}</div>
            </>
        );
    }

    return <div style={DESC_STYLE}>{content}</div>;
}

// ============================================================================
// 主组件
// ============================================================================

export const EntityCard = memo(function EntityCard({entity, image, nodeShow}: {
    entity: Entity;
    image?: string;
    nodeShow?: NodeShowType;
}) {
    // query 无 per-graph override，走全局 store（resso per-key 订阅）。hook 须在 early return 之前。
    const {query} = useMyStore();

    if (!isCardEntity(entity) || !entity.brief) {
        return <></>;
    }

    const {brief} = entity;

    // 构建封面
    const coverEl = image && isTauri()
        ? <img alt="img" style={IMAGE_STYLE} src={convertFileSrc(image)}/>
        : undefined;

    // 构建标题
    const titleEl = brief.title
        ? (query ? <Highlight text={brief.title} keyword={query}/> : brief.title)
        : undefined;

    // 构建描述
    const [showDsLen, descText] = getDsLenAndDesc(brief, nodeShow);
    const descEl = buildCardDescription(brief.descriptions, showDsLen, descText ?? "", query);

    // 计算有效内容数量
    const hasContent = [coverEl, titleEl, descEl].filter(Boolean).length;
    if (hasContent === 0) return <></>;

    // 单项内容直接返回
    if (hasContent === 1) {
        if (coverEl) return coverEl;
        if (titleEl) return <Card title={titleEl}/>;
        if (descEl) return <Card>{descEl}</Card>;
    }

    // 多项内容使用 Card + Meta
    return (
        <Card cover={coverEl}>
            <Card.Meta title={titleEl} description={descEl}/>
        </Card>
    );
});
