import { memo, useEffect } from "react";
import { useMyStore } from "@/store/store";

/**
 * 管理流程可视化相关的全局 CSS 变量。
 *
 * 从「每个 FlowGraph 实例挂载」提升到「CfgEditorApp 顶层只挂一次」。CSS 变量本就全局唯一，
 * 多实例并存时一个实例 unmount 的 cleanup 会 removeProperty 抹掉另一实例仍在用的变量 → 右画布失样式。
 * 单实例常驻后，cleanup 不再 removeProperty（app 生命周期内变量始终在）。
 */
export const FlowStyleManager = memo(function FlowStyleManager() {
    const { nodeShow } = useMyStore();

    useEffect(() => {
        // 边粗细 CSS 变量：挂在 documentElement，配合 style.css 的 svg .react-flow__edge-path。
        // 安全访问 + 默认值；app 生命周期内常驻，不 removeProperty。
        document.documentElement.style.setProperty('--edge-stroke-width', `${nodeShow?.edgeStrokeWidth ?? 3}px`);
    }, [nodeShow?.edgeStrokeWidth]);

    return null; // 这个组件不渲染任何内容
});
