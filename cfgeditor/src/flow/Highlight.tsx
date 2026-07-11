import {ReactElement} from "react";

// 通用文本高亮组件。原先寄居在领域专用的 EntityCard 里，被 FlowNode/EntityProperties/EntityCard
// 三处共用——cohesion 问题，抽到独立文件。escapeRegExp 保持私有。

// 转义正则元字符——keyword 来自用户搜索框，输入 ( * [ 等会让 new RegExp 抛 SyntaxError 导致渲染崩溃。
const escapeRegExp = (s: string) => s.replace(/[.*+?^${}()|[\]\\]/g, "\\$&");

export function Highlight({text, keyword}: {text: string; keyword: string}): ReactElement {
    if (!keyword) {
        return <>{text}</>;
    }
    const parts = text.split(new RegExp(`(${escapeRegExp(keyword)})`, "gi"));
    return <>{parts.map((part, i) =>
        part.toLowerCase() === keyword.toLowerCase() ? <mark key={i}>{part}</mark> : part
    )}</>;
}
