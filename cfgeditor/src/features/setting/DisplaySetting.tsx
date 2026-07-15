import {memo} from "react";
import {Divider} from "antd";
import {NodeShowSetting} from "./NodeShowSetting.tsx";
import {FlowVisualizationSetting} from "./FlowVisualizationSetting.tsx";

/**
 * "显示" tab：节点显示/布局/颜色（NodeShowSetting）+ 节点尺寸/边/间距（FlowVisualizationSetting）。
 * 两者都改 nodeShow，且都用 onValuesChange 即时生效。
 */
export const DisplaySetting = memo(function DisplaySetting() {
    return <>
        <NodeShowSetting/>
        <Divider/>
        <FlowVisualizationSetting/>
    </>;
});
