import {memo} from "react";
import {Button, Flex, Form} from "antd";
import {PlusSquareTwoTone} from "@ant-design/icons";
import {useSyncFieldValue} from "../shared/useSyncFieldValue.ts";
import {primitiveControl} from "../shared/primitiveControl.tsx";
import {ArrayPrimitiveEditField} from "@/domain/entityModel";
import {fieldItemStyle, getAutoCompleteOptions, getDefaultPrimitiveValue, isArrayPrimitiveBoolOrNumber} from "../shared/fieldUtils.ts";
import {LabelWithTooltip} from "../shared/LabelWithTooltip.tsx";
import {AUTO_COMPLETE_ITEM_STYLE, FORM_LAYOUT} from "../shared/constants.ts";
import {ArrayItemExpandButton} from "./ArrayItemExpandButton.tsx";

export interface ArrayPrimitiveFormItemProps {
    field: ArrayPrimitiveEditField;
    bgColor?: string;
}

export const ArrayOfPrimitiveFormItem = memo(function ArrayOfPrimitiveFormItem({
                                                                                    field,
                                                                                    bgColor,
                                                                                }: ArrayPrimitiveFormItemProps) {
    const form = Form.useFormInstance();
    useSyncFieldValue(form, field);

    const itemStyle = fieldItemStyle(bgColor);
    // 有 autoComplete 选项时输入项固定宽度（AUTO_COMPLETE_ITEM_STYLE）；无则不展开额外 props（undefined）。
    // （原为 FILTER_EMPTY 借位——那是 FilterOption 不是 style，靠它恰好是 {} 才不炸。）
    const hasOptions = getAutoCompleteOptions(field) !== undefined;
    const inputItemStyle = hasOptions ? AUTO_COMPLETE_ITEM_STYLE : undefined;

    return (
        <Form.Item
            {...FORM_LAYOUT}
            label={<LabelWithTooltip name={field.name} comment={field.comment} isAutoFontSize/>}
            style={itemStyle}
        >
            <Form.List name={field.name} initialValue={field.value}>
                {(fields, {add, remove, move}) => (
                    <>
                        {fields.map((f, index) => (
                            <Flex key={f.key} align="center" justify="space-between" style={{width: "100%"}}>
                                <Form.Item name={f.name} {...inputItemStyle} style={{flex: 1, marginBottom: 0}}>
                                    {primitiveControl(field, itemStyle)}
                                </Form.Item>
                                <ArrayItemExpandButton
                                    fold={!isArrayPrimitiveBoolOrNumber(field)}
                                    onRemove={() => remove(f.name)}
                                    onMoveUp={index > 0 ? () => move(index, index - 1) : undefined}
                                    onMoveDown={index < fields.length - 1 ? () => move(index, index + 1) : undefined}
                                />
                            </Flex>
                        ))}
                        <Button
                            className="nodrag"
                            icon={<PlusSquareTwoTone/>}
                            onClick={() => add(getDefaultPrimitiveValue(field))}
                        />
                    </>
                )}
            </Form.List>
        </Form.Item>
    );
});
