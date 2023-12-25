import {ClassicPreset} from "rete";
import {EntityControl} from "./EntityControl.tsx";
import {Entity, EntityType, KeywordColor} from "../model/entityModel.ts";
import {Presets, RenderEmit} from "rete-react-plugin";
import {css} from "styled-components";

const {Node} = Presets.classic;

export class EntityNode extends ClassicPreset.Node<
    { [key in string]: ClassicPreset.Socket },
    { [key in string]: ClassicPreset.Socket },
    {
        [key in string]:
        | EntityControl
        | ClassicPreset.Control
        | ClassicPreset.InputControl<"number">
        | ClassicPreset.InputControl<"text">;
    }
> {
    width = 280;
    height = 200;

    entity?: Entity;
    keywordColors?: KeywordColor[];
}

const normalStyles = css<{ selected?: boolean }>`
    background: #1677ff;
`;

const refStyles = css<{ selected?: boolean }>`
    background: #237804;
`;

const ref2Styles = css<{ selected?: boolean }>`
    background: #006d75;
`;

const refInStyles = css<{ selected?: boolean }>`
    background: #003eb3;
`;

export function EntityNodeComponent(props: { data: EntityNode, emit: RenderEmit<any> }) {
    let styles = null;
    let entity = props.data.entity;

    if (entity && entity.brief && props.data.keywordColors && props.data.keywordColors.length > 0) {

        let color: string | null = null;
        for (let keywordColor of props.data.keywordColors) {
            if (entity.brief.value.includes(keywordColor.keyword)) {
                color = keywordColor.color;
                break;
            }
        }

        if (color != null) {
            styles = {
                styles: () => css<{ selected?: boolean }>`
                    background: ${color};
                `
            };
        }
    }

    if (styles) {
        return <Node {...styles} {...props} />;
    }

    let type = entity?.entityType;
    switch (type) {
        case EntityType.Ref:
            return <Node styles={() => refStyles} {...props} />;
        case EntityType.Ref2:
            return <Node styles={() => ref2Styles} {...props} />;
        case EntityType.RefIn:
            return <Node styles={() => refInStyles} {...props} />;
    }
    return <Node styles={() => normalStyles} {...props}  />;

}
