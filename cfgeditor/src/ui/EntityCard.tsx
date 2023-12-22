import {Card} from "antd";
import {EntityBrief} from "../model/entityModel.ts";


export function EntityCard({brief}: {
    brief: EntityBrief
}) {
    let cover = {};
    if (brief.img) {
        cover = {cover: <img alt="img" src={brief.img}/>}
    }

    let title = {};
    if (brief.title) {
        title = {title: brief.title};
    }

    return <Card hoverable style={{width: 240}}        {...cover}>
        <Card.Meta {...title} description={brief.description ?? brief.value}/>
    </Card>;
}