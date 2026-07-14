import type {CSSProperties} from "react";
import {Input, InputNumber, Select, Switch} from "antd";
import {EntityEditField} from "@/domain/entityModel";
import {CustomAutoComplete} from "../../CustomAutoComplete.tsx";
import {getFilter, hasAutoCompleteOptions} from "./fieldUtils.ts";

export function primitiveControl(field: EntityEditField, style: CSSProperties) {
    const {eleType} = field;
    const autoCompleteOptions = hasAutoCompleteOptions(field) ? field.autoCompleteOptions : undefined;

    if (autoCompleteOptions?.options.length) {
        const {options, isEnum} = autoCompleteOptions;
        const filters = getFilter(options.length > 5);

        if (isEnum) {
            return <Select className="nodrag" options={options} {...filters}/>;
        }
        return <CustomAutoComplete options={options} filters={filters}/>;
    }

    switch (eleType) {
        case "bool":
            return <Switch className="nodrag"/>;
        case "int":
        case "long":
        case "float":
            return <InputNumber className="nodrag" style={style}/>;
        default:
            // 去 autoSize：rc-textarea 的 ResizeObserver 随节点数放大成 rAF 风暴，是 /edit/record/ 大图
            // 加载卡顿的根因（memory: cfgeditor-load-jank-textarea-autosize）。str 可能含换行用 rows={1}（滚动），
            // text 用 rows={4}。固定 rows → 无 ResizeObserver → 无 rAF 风暴。
            if (field.eleType === "text") {
                return <Input.TextArea className="nodrag" rows={4} style={style}/>;
            }
            return <Input.TextArea className="nodrag" rows={1} style={style}/>;
    }
}
