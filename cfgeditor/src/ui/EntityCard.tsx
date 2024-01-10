import {Card, Descriptions, Tooltip} from "antd";
import {EntityBrief, ShowDescriptionType} from "../model/entityModel.ts";
import {DescriptionsItemType} from "antd/es/descriptions";


export function EntityCard({brief, query, showDescription}: {
    brief: EntityBrief
    query?: string;
    showDescription?: ShowDescriptionType,
}) {
    if (!brief.img && !brief.title && (!brief.descriptions || brief.descriptions.length == 0)
        && brief.value.length == 0) {
        return <></>
    }

    let cover = {};
    if (brief.img) {
        cover = {cover: <img alt="img" src={brief.img}/>}
    }

    let title = {};
    if (brief.title) {
        if (query) {
            title = {title: <Highlight text={brief.title} keyword={query}/>};
        } else {
            title = {title: brief.title};
        }
    }

    let ds = brief.descriptions;
    let desc: string | null = null;
    switch (showDescription) {
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

        description = {
            description: <><Descriptions column={1} bordered size={"small"} items={items}/>
                {query ? <Highlight text={desc} keyword={query}/> : desc} </>
        }
    } else if (desc) {
        description = {
            description: query ? <Highlight text={desc} keyword={query}/> : desc
        }
    }

    return <Card hoverable style={{width: 240}}  {...cover}>
        <Card.Meta {...title} {...description}/>
    </Card>;
}

export function Highlight({text, keyword}: {
    text: string;
    keyword: string;
}) {
    return text.split(new RegExp(`(${keyword})`, "gi"))
        .map((c, i) => c === keyword ? <mark key={i}>{c}</mark> : c);
}
