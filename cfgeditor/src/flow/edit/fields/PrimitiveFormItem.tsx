import {memo, useMemo} from "react";
import {Form} from "antd";
import {useSyncFieldValue} from "../shared/useSyncFieldValue.ts";
import {primitiveControl} from "../shared/primitiveControl.tsx";
import {fieldItemStyle} from "../shared/fieldUtils.ts";
import {LabelWithTooltip} from "../shared/LabelWithTooltip.tsx";
import type {PrimitiveFormItemProps} from "../shared/types.ts";

export const PrimitiveFormItem = memo(function PrimitiveFormItem({field, bgColor}: PrimitiveFormItemProps) {
    const form = Form.useFormInstance();
    useSyncFieldValue(form, field);

    const boolProps = useMemo(
        () => (field.eleType === "bool" ? {valuePropName: "checked" as const} : {}),
        [field.eleType]
    );

    // control memo deps 用 bgColor（原始值）而非 fieldItemStyle(bgColor) 的返回值——后者每次新对象会使 memo 失效。
    const control = useMemo(() => primitiveControl(field, fieldItemStyle(bgColor)), [field, bgColor]);

    return (
        <Form.Item
            name={field.name}
            label={<LabelWithTooltip name={field.name} comment={field.comment} isAutoFontSize/>}
            initialValue={field.value}
            {...boolProps}
            style={fieldItemStyle(bgColor)}
        >
            {control}
        </Form.Item>
    );
});
