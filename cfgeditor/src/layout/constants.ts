/**
 * HeaderBar 浮层的渲染高度（= antd 默认控件高度 32px）。
 * HeaderBar 是有意的 overlay——让 FlowGraph 占满全屏（见 docs/ui-redesign.md §3），
 * 代价是遮挡文字类侧栏面板的顶部。文字面板套 SidePanelShell 统一避让这个高度。
 * 若改动 HeaderBar 高度，同步改这里。
 */
export const HEADER_HEIGHT = 32;
