import {Fragment, memo, useCallback, useMemo} from "react";
import type {ReactNode} from "react";
import {Form, Select} from "antd";
import type {NodeProps} from "@xyflow/react";
import {EntityEditField, InterfaceEditField} from "@/domain/entityModel";
import type {EntityNode} from "../../FlowGraph.tsx";
import {nodeAnchor} from "../../nodeAnchor.ts";
import {useSyncFieldValue} from "../shared/useSyncFieldValue.ts";
import {getFilter} from "../shared/fieldUtils.ts";

interface InterfaceFormItemProps {
    field: InterfaceEditField;
    nodeProps: NodeProps<EntityNode>;
    // implFields 递归渲染由 FieldRenderer 注入（避免 InterfaceFormItem ↔ FieldRenderer 循环 import）。
    renderField: (field: EntityEditField) => ReactNode;
}

export const InterfaceFormItem = memo(
    function InterfaceFormItem({field, nodeProps, renderField}: InterfaceFormItemProps) {
        const form = Form.useFormInstance();
        useSyncFieldValue(form, field);

        const handleSelectChange = useCallback(
            (value: string) => {
                field.interfaceOnChangeImpl(value, nodeAnchor(nodeProps));
            },
            [field, nodeProps]
        );

        const options = field.autoCompleteOptions?.options;
        const filters = getFilter((options?.length ?? 0) > 5);

        const formItem = useMemo(
            () => (
                <Form.Item name={field.name} label=">" initialValue={field.value}>
                    <Select className="nodrag" options={options} {...filters} onChange={handleSelectChange}/>
                </Form.Item>
            ),
            [field.name, field.value, options, filters, handleSelectChange]
        );

        return (
            <>
                {formItem}
                {field.implFields.map(f => (
                    <Fragment key={f.name}>{renderField(f)}</Fragment>
                ))}
            </>
        );
    });
