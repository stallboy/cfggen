import type {CSSProperties} from "react";
import {BookOutlined} from "@ant-design/icons";

// 纯图标按钮的统一外观：无边框、透明底（按钮语义全靠图标 + aria-label 表达）。
// FlowNode / NodeNote / NodeToolbar / NoteShowOrEdit 共用单源（原四处复制同一字面量）。
export const iconButtonStyle: CSSProperties = {borderWidth: 0, backgroundColor: 'transparent'};

// note 入口/编辑按钮的统一图标。
export const bookIcon = <BookOutlined/>;
