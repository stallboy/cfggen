import {Card, Descriptions, Tooltip} from "antd";
import {Entity} from "./entityModel.ts";
import {DescriptionsItemType} from "antd/es/descriptions";
import {memo} from "react";

export const EntityCard = memo(function EntityCard({entity}: {
    entity: Entity,
}) {
    const {brief, nodeShow, query} = entity;
    if (!brief) {
        return <></>;
    }

    let hasContent = false;
    let cover = {};
    if (brief.img) {
        cover = {cover: <img alt="img" src={brief.img}/>}
        hasContent = true;
    }

    let title = {};
    if (brief.title) {
        if (query) {
            title = {title: <Highlight text={brief.title} keyword={query}/>};
        } else {
            title = {title: brief.title};
        }
        hasContent = true;
    }

    let ds = brief.descriptions;
    let desc: string | null = null;
    if (nodeShow) {
        switch (nodeShow.showDescription) {
            case "show":
                desc = ds && ds.length > 0 ? ds[ds.length - 1].value : "";
                break;
            case "showFallbackValue":
                desc = ds && ds.length > 0 ? ds[ds.length - 1].value : brief.value;
                break;
            case "showValue":
                desc = brief.value;
                break;
            case "none":
                break;
        }
    }

    let description = {}
    if (desc && ds && ds.length > 1) {
        const items: DescriptionsItemType[] = [];
        for (let i = 0; i < ds.length - 1; i++) {
            const d = ds[i];
            items.push({
                key: i,
                label: <Tooltip title={d.comment}> {d.field} </Tooltip>,
                children: d.value,
            })
        }

        hasContent = true;
        description = {
            description: <><Descriptions column={1} bordered size={"small"} items={items}/>
                {query ? <Highlight text={desc} keyword={query}/> : desc} </>
        }
    } else if (desc) {
        hasContent = true;
        description = {
            description: query ? <Highlight text={desc} keyword={query}/> : desc
        }
    }

    if (!hasContent) {
        return <></>;
    }

    return <Card hoverable {...cover}>
        <Card.Meta {...title} {...description}/>
    </Card>;
});

export function Highlight({text, keyword}: {
    text: string;
    keyword: string;
}) {
    return text.split(new RegExp(`(${keyword})`, "gi"))
        .map((c, i) => c === keyword ? <mark key={i}>{c}</mark> : c);
}