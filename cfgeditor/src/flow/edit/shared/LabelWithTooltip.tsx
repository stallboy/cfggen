import {memo} from "react";
import {Tooltip} from "antd";

interface LabelProps {
    name: string;
    comment?: string;
    isAutoFontSize?: boolean;
}

export const LabelWithTooltip = memo(function LabelWithTooltip({
                                                                    name,
                                                                    comment,
                                                                    isAutoFontSize,
                                                                }: LabelProps) {
    const content = autoSizeName(name, isAutoFontSize);

    if (comment && comment.length > 0) {
        return <Tooltip title={comment}>{content}</Tooltip>;
    }
    return content;
});

function autoSizeName(name: string, autoSize?: boolean) {
    const shouldShrink = name.length >= 9 && autoSize;
    const style = shouldShrink ? {fontSize: "0.75em"} : undefined;
    const Wrapper = shouldShrink ? "span" : "i";

    return <Wrapper style={style}>{name}</Wrapper>;
}
