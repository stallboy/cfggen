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

export function EntityNodeComponent(props: { data: EntityNode, emit: RenderEmit<any> }) {
    if (props.data.entity?.nodeType == EntityNodeType.Ref) {
        return <Node styles={() => refStyles} {...props} />;
    } else {
        return <Node {...props} />;
    }
}
