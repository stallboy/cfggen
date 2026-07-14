import {useMemo} from "react";
import type {CSSProperties} from "react";
import {theme} from "antd";
import {DEFAULT_EDIT_NODE_WIDTH} from "../../layout/dimensions.ts";

export function useRefItemStyles(width?: number, bgColor?: string) {
    const {token} = theme.useToken();

    const rowStyle: CSSProperties = useMemo(
        () => ({
            marginBottom: 10,
            position: "relative",
            ...(bgColor && {backgroundColor: bgColor}),
        }),
        [bgColor]
    );

    // 连接点原硬编码纯 blue（#0000ff，过饱和），换 antd colorPrimary——既校准为品牌主色，
    // 又给字段级 handle 一个统一的"可连接点"高亮（与节点级 handle 融入底色区分开）。
    const handleOutStyle: CSSProperties = useMemo(
        () => ({
            position: "absolute",
            left: `${(width ?? DEFAULT_EDIT_NODE_WIDTH) - 10}px`,
            backgroundColor: token.colorPrimary,
        }),
        [width, token.colorPrimary]
    );

    return {rowStyle, handleOutStyle};
}
