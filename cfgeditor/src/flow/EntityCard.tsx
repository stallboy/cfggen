import {Card, Descriptions, Tooltip} from "antd";
import {Entity} from "./entityModel.ts";
import {DescriptionsItemType} from "antd/es/descriptions";
import {CSSProperties, memo, ReactElement} from "react";
import {convertFileSrc} from "@tauri-apps/api/tauri";
import {getDsLenAndDesc} from "./getDsLenAndDesc.tsx";

const imageStyle: CSSProperties = {maxHeight: '220px', objectFit: 'scale-down'};

interface Props {
    [prop: string]: string | ReactElement;
}

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
    let cover: Props = {};
    if (image && window.__TAURI__) {
        const imageUrl = convertFileSrc(image);
        cover = {cover: <img alt="img" style={imageStyle} src={imageUrl}/>}
        hasCover = true;
        info++;
    }

    let hasTitle = false;
    let title: Props = {};
    if (brief.title) {
        if (query) {
            title = {title: <Highlight text={brief.title} keyword={query}/>};
        } else {
            title = {title: brief.title};
        }
        hasTitle = true;
        info++;
    }


    let description: Props = {}
    const ds = brief.descriptions;
    const [showDsLen, desc] = getDsLenAndDesc(brief, nodeShow);
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
            description: <>
                <Descriptions column={1} bordered size={"small"} items={items}/>
                <div style={{whiteSpace: "break-spaces"}}>{
                    query ? <Highlight text={desc} keyword={query}/> : desc}
                </div>
            </>
        }
        info++;
    } else if (desc) {
        description = {
            description: <div style={{whiteSpace: "break-spaces"}}>{
                query ? <Highlight text={desc} keyword={query}/> : desc}
            </div>
        }
        info++;
    }

    if (info > 1) {
        return <Card {...cover}>
            <Card.Meta {...title} {...description}/>
        </Card>;
    } else if (info == 1) {
        if (hasCover) {
            return cover.cover;
        } else if (hasTitle) {
            return <Card title={title.title}/>;
        } else {
            return <Card>{description.description}</Card>;
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

