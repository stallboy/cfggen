import {Card, Descriptions, Tooltip} from "antd";
import {Entity, EntityBrief} from "./entityModel.ts";
import {DescriptionsItemType} from "antd/es/descriptions";
import {CSSProperties, memo} from "react";
import {NodeShowType} from "../routes/setting/storageJson.ts";
import {convertFileSrc} from "@tauri-apps/api/tauri";

const imageStyle: CSSProperties = {maxHeight: '220px', objectFit: 'scale-down'};

export const EntityCard = memo(function EntityCard({entity, image}: {
    entity: Entity,
    image?: string;
}) {
    const {brief, sharedSetting} = entity;
    const query = sharedSetting?.query;
    const nodeShow = sharedSetting?.nodeShow;

    if (!brief) {
        return <></>;
    }

    let info = 0;
    let hasCover = false;
    let cover: any = {};
    if (image && window.__TAURI__) {
        const imageUrl = convertFileSrc(image);
        cover = {cover: <img alt="img" style={imageStyle} src={imageUrl}/>}
        hasCover = true;
        info++;
    }

    let hasTitle = false;
    let title: any = {};
    if (brief.title) {
        if (query) {
            title = {title: <Highlight text={brief.title} keyword={query}/>};
        } else {
            title = {title: brief.title};
        }
        hasTitle = true;
        info++;
    }


    let description: any = {}
    let ds = brief.descriptions;
    let [showDsLen, desc] = getDsLenAndDesc(brief, nodeShow);
    if (desc && ds && showDsLen > 0) {
        const items: DescriptionsItemType[] = [];

        for (let i = 0; i < showDsLen; i++) {
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
        info++;
    } else if (desc) {
        description = {
            description: query ? <Highlight text={desc} keyword={query}/> : desc
        }
        info++;
    }

    if (info > 1) {
        return <Card hoverable {...cover}>
            <Card.Meta {...title} {...description}/>
        </Card>;

    } else if (info == 1) {
        if (hasCover) {
            return cover.cover;
        } else if (hasTitle) {
            return title.title;
        } else {
            return description.description;
        }
    } else {
        return <></>;
    }

});

export function Highlight({text, keyword}: {
    text: string;
    keyword: string;
}) {
    return text.split(new RegExp(`(${keyword})`, "gi"))
        .map((c, i) => c === keyword ? <mark key={i}>{c}</mark> : c);
}

export function getDsLenAndDesc(brief: EntityBrief, nodeShow?: NodeShowType): [number, string | null] {
    let ds = brief.descriptions;
    let desc: string | null = null;
    let showDsLen: number = 0;
    if (nodeShow) {
        switch (nodeShow.showDescription) {
            case "show":
                if (ds && ds.length > 0) {
                    desc = ds[ds.length - 1].value;
                    showDsLen = ds.length - 1;
                }
                break;
            case "showFallbackValue":
                if (ds && ds.length > 0) {
                    desc = ds[ds.length - 1].value;
                    showDsLen = ds.length - 1;
                } else {
                    desc = brief.value;
                }
                break;
            case "showValue":
                desc = brief.value;
                if (ds) {
                    showDsLen = ds.length;
                }
                break;
            case "none":
                break;
        }
    }

    return [showDsLen, desc];
}