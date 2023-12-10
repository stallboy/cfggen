import {EntityNode} from "./EntityNode.tsx";
import {ClassicPreset} from "rete";
import {EntityConnectionType} from "../graphModel.ts";
import {css} from "styled-components";
import {Presets} from "rete-react-plugin";

const {Connection} = Presets.classic;

export class EntityConnection<A extends EntityNode, B extends EntityNode> extends ClassicPreset.Connection<A, B> {
    connectionType: EntityConnectionType = EntityConnectionType.Normal;
}

const refStyles = css`
    stroke: #00000045;
    stroke-dasharray: 10 5;
    animation: dash 1s linear infinite;
    stroke-dashoffset: 45;
    @keyframes dash {
        to {
            stroke-dashoffset: 0;
        }
    }`;

export function EntityConnectionComponent(props: {
    data: EntityConnection<EntityNode, EntityNode> & { isLoop?: boolean };
}) {
    if (props.data.connectionType == EntityConnectionType.Ref) {
        return <Connection {...props} styles={() => refStyles}/>;
    } else {
        return <Connection {...props} />;
    }
}
