import {useMemo} from "react";
import type {CSSProperties} from "react";
import {DEFAULT_EDIT_NODE_WIDTH} from "../../dimensions.ts";

export function useRefItemStyles(width?: number, bgColor?: string) {
    const rowStyle: CSSProperties = useMemo(
        () => ({
            marginBottom: 10,
            position: "relative",
            ...(bgColor && {backgroundColor: bgColor}),
        }),
        [bgColor]
    );

    const handleOutStyle: CSSProperties = useMemo(
        () => ({
            position: "absolute",
            left: `${(width ?? DEFAULT_EDIT_NODE_WIDTH) - 10}px`,
            backgroundColor: "blue",
        }),
        [width]
    );

    return {rowStyle, handleOutStyle};
}
