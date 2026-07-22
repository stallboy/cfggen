import type {ReactNode} from "react";
import type {NodeProps} from "@xyflow/react";
import {EntityEdit, EntityEditField, FuncType} from "@/domain/entityModel";
import type {NodeShowType} from "@/domain/storageJson";
import type {EntityNode} from "../FlowGraph.tsx";
import {getFieldBackgroundColor} from "../layout/colors.ts";
import {getEditNodeWidth} from "../layout/dimensions.ts";
import {StructRefItem} from "./fields/StructRefItem.tsx";
import {EmbeddedSimpleStructuralItem} from "./fields/EmbeddedSimpleStructuralItem.tsx";
import {ArrayOfPrimitiveFormItem} from "./fields/ArrayOfPrimitiveFormItem.tsx";
import {PrimitiveFormItem} from "./fields/PrimitiveFormItem.tsx";
import {FuncAddFormItem} from "./fields/FuncAddFormItem.tsx";
import {InterfaceFormItem} from "./fields/InterfaceFormItem.tsx";
import {FuncSubmitFormItem} from "./fields/FuncSubmitFormItem.tsx";

interface FieldRenderProps {
    field: EntityEditField;
    edit: EntityEdit;
    nodeProps: NodeProps<EntityNode>;
    nodeShow?: NodeShowType;
}

// 字段判别组件（B2：原 renderFieldItem/renderFieldItems 函数 → 组件）。
// key 落在组件边界（EntityForm 的 edit.fields.map 与 InterfaceFormItem 的 implFields.map），
// 而非函数式渲染那样每次重渲产生新 element、靠子组件 memo 比较运气。
// 递归：interface 的 implFields 由 InterfaceFormItem 经 renderField 回调再次进入本组件
// （注入回调而非 import，打破 FieldRenderer ↔ InterfaceFormItem 循环依赖）。
export function FieldRenderer({field, edit, nodeProps, nodeShow}: FieldRenderProps): ReactNode {
    const bgColor = getFieldBackgroundColor(field, nodeShow);
    const width = getEditNodeWidth(nodeShow);

    switch (field.type) {
        case "structRef":
            // 判断是否为内嵌模式
            if (field.embeddedField) {
                return (
                    <EmbeddedSimpleStructuralItem
                        field={field}
                        edit={edit}
                        nodeProps={nodeProps}
                        bgColor={bgColor}
                    />
                );
            }

            // 正常 structRef 显示
            return (
                <StructRefItem
                    name={field.name}
                    comment={field.comment}
                    handleOut={field.handleOut}
                    bgColor={bgColor}
                    width={width}
                />
            );

        case "arrayOfPrimitive":
            return <ArrayOfPrimitiveFormItem field={field} bgColor={bgColor}/>;

        case "primitive":
            return <PrimitiveFormItem field={field} bgColor={bgColor}/>;

        case "funcAdd":
            return (
                <FuncAddFormItem
                    name={field.name}
                    comment={field.comment}
                    handleOut={field.handleOut}
                    func={field.value as FuncType}
                    nodeProps={nodeProps}
                    bgColor={bgColor}
                    width={width}
                    listFold={field.listFold}
                    foldColor={nodeShow?.editFoldColor}
                />
            );

        case "interface":
            return (
                <InterfaceFormItem
                    field={field}
                    nodeProps={nodeProps}
                    renderField={(f) => <FieldRenderer field={f} edit={edit} nodeProps={nodeProps} nodeShow={nodeShow}/>}
                />
            );

        case "funcSubmit":
            return <FuncSubmitFormItem field={field}/>;

        default:
            // 理论上不会到达这里
            return <></>;
    }
}
