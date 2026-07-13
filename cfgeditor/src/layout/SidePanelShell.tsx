import {CSSProperties, ReactNode} from "react";
import {HEADER_HEIGHT} from "./constants.ts";

const shellStyle: CSSProperties = {height: '100%', display: 'flex', flexDirection: 'column'};
const spacerStyle: CSSProperties = {flexShrink: 0, height: HEADER_HEIGHT};
const bodyStyle: CSSProperties = {flex: 1, minHeight: 0, overflow: 'auto'};

/**
 * 文字类侧栏面板统一外壳：顶部固定占位避让 HeaderBar 浮层 + 下方独立滚动。
 *
 * 用 flex 列而非 paddingTop——paddingTop 在 overflow 容器里会随内容滚走，
 * 导致内容滚到 header 下面（见 docs/ui-redesign.md §3）。
 *
 * 仅用于 Finder/Add/Setting 等「有文字内容、不能被浮层遮」的面板；
 * 画布类（FlowGraph/RecordRef/固定页）不套，保持全屏被 overlay 遮。
 */
export function SidePanelShell({children}: {children: ReactNode}) {
    return (
        <div style={shellStyle}>
            <div style={spacerStyle}/>
            <div style={bodyStyle}>{children}</div>
        </div>
    );
}
