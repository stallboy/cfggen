import {Card} from "antd";
import {EntityBrief, ShowDescriptionType} from "../model/entityModel.ts";


export function EntityCard({brief, query, showDescription}: {
    brief: EntityBrief
    query?: string;
    showDescription? : ShowDescriptionType
}) {
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

    let desc = brief.value;
    switch (showDescription){
        case "show":
            desc = brief.description ?? "";
            break;
        case "showFallbackValue":
            desc = (brief.description && brief.description.length > 0) ? brief.description : brief.value
            break;
        case "showValue":
            break;
        case "none":
            desc = "";
            break;
    }

    return <Card hoverable style={{width: 240}} {...cover}>
        <Card.Meta {...title} description={query ? <Highlight text={desc} keyword={query}/> : desc}/>
    </Card>;
}

export function Highlight({text, keyword}: {
    text: string;
    keyword: string;
}) {
    return text.split(new RegExp(`(${keyword})`, "gi"))
        .map((c, i) => c === keyword ? <mark key={i}>{c}</mark> : c);
}
