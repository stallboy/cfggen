import { memo, useEffect } from "react";
import { useMyStore } from "../store/store.ts";

/**
 * 管理流程可视化相关的 CSS 变量
 */
export const FlowStyleManager = memo(function FlowStyleManager() {
    const { nodeShow } = useMyStore();

    useEffect(() => {
        // 设置 CSS 变量
        const root = document.documentElement;

        // 设置边粗细 CSS 变量，使用安全访问和默认值
        root.style.setProperty('--edge-stroke-width', `${nodeShow?.edgeStrokeWidth ?? 3}px`);

        // 可以在这里添加其他 CSS 变量
        // root.style.setProperty('--node-width', `${nodeShow?.nodeWidth ?? 240}px`);
        // root.style.setProperty('--edit-node-width', `${nodeShow?.editNodeWidth ?? 280}px`);

        // 清理函数
        return () => {
            root.style.removeProperty('--edge-stroke-width');
            // root.style.removeProperty('--node-width');
            // root.style.removeProperty('--edit-node-width');
        };
    }, [nodeShow?.edgeStrokeWidth]);

    return null; // 这个组件不渲染任何内容
});