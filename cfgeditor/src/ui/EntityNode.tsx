import {ClassicPreset} from "rete";
import {TableControl} from "./TableControl.tsx";
import {Entity, EntityNodeType} from "../graphModel.ts";
import {Presets, RenderEmit} from "rete-react-plugin";
import {css} from "styled-components";

const {Node} = Presets.classic;

export class EntityNode extends ClassicPreset.Node<
    { [key in string]: ClassicPreset.Socket },
    { [key in string]: ClassicPreset.Socket },
    {
        [key in string]:
        | TableControl
        | ClassicPreset.Control
        | ClassicPreset.InputControl<"number">
        | ClassicPreset.InputControl<"text">;
    }
> {
    width = 280;
    height = 440;

    entity?: Entity;
}

const refStyles = css<{ selected?: boolean }>`
    background: darkcyan;
    ${(props) => props.selected && css`
        background: #ffd92c;
    `}`;

const ref2Styles = css<{ selected?: boolean }>`
    background: darkblue;
    ${(props) => props.selected && css`
        background: #ffd92c;
    `}`;

const refInStyles = css<{ selected?: boolean }>`
    background: mediumslateblue;
    ${(props) => props.selected && css`
        background: #ffd92c;
    `}`;

export function EntityNodeComponent(props: { data: EntityNode, emit: RenderEmit<any> }) {
    let type = props.data.entity?.nodeType;
    switch (type) {
        case EntityNodeType.Ref:
            return <Node styles={() => refStyles} {...props} />;
        case EntityNodeType.Ref2:
            return <Node styles={() => ref2Styles} {...props} />;
        case EntityNodeType.RefIn:
            return <Node styles={() => refInStyles} {...props} />;
    }
    return <Node {...props} />;

}
