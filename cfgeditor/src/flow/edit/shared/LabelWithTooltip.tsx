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
    // 原 short 名用 <i>（UA 默认 italic）、long 名用 <span>，同表单内长短名斜体/正体不一致。
    // 统一 <span>：去斜体，长短名排版一致；需要缩小时仅调 fontSize。
    const shouldShrink = name.length >= 9 && autoSize;
    const style = shouldShrink ? {fontSize: "0.75em"} : undefined;

    return <span style={style}>{name}</span>;
}
