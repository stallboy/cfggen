import {memo} from "react";
import {Form} from "antd";
import type {NodeProps} from "@xyflow/react";
import {EntityEdit} from "@/domain/entityModel";
import type {NodeShowType} from "@/domain/storageJson";
import type {EntityNode} from "../FlowGraph.tsx";
import {FORM_LAYOUT, FORM_STYLE} from "./shared/constants.ts";
import {FieldRenderer} from "./FieldRenderer.tsx";

interface EntityFormProps {
    edit: EntityEdit;
    nodeProps: NodeProps<EntityNode>;
    nodeShow?: NodeShowType;
}

export const EntityForm = memo(function EntityForm({edit, nodeProps, nodeShow}: EntityFormProps) {
    const [form] = Form.useForm();

    // alt+s「提交」由 CfgEditorApp 全局单点注册，直达 getCurrentEditingSession().submit()：
    // funcSubmit 字段（仅根 STable 追加）唯一逻辑即 session.submit()（见 recordEditEntityCreator.ts），
    // 提交是「当前编辑会话」的全局语义，本组件无需参与命令路由。
    // form里单个字段的改变不会引起这个界面更新，只更新jsonObject对象
    // initialValue放在每个Form.Item里
    // 参考: https://github.com/ant-design/ant-design/issues/56102
    return (
        <Form
            {...FORM_LAYOUT}
            form={form}
            onValuesChange={(changed, allValues) => {
                // allValues 写回 editingObject（完整 array 安全）；changed 喂 coalescing 合并 key + Form.List 长度 diff。
                edit.editOnUpdateValues(changed, allValues);
            }}
            style={FORM_STYLE}
        >
            {edit.fields.map(field => (
                <FieldRenderer key={field.name} field={field} edit={edit} nodeProps={nodeProps} nodeShow={nodeShow}/>
            ))}
        </Form>
    );
});
