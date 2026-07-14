import {memo} from "react";
import {Button, Flex, Form} from "antd";
import {PlusSquareTwoTone} from "@ant-design/icons";
import {useSyncFieldValue} from "../shared/useSyncFieldValue.ts";
import {primitiveControl} from "../shared/primitiveControl.tsx";
import {fieldItemStyle, getDefaultPrimitiveValue, hasAutoCompleteOptions, isArrayPrimitiveBoolOrNumber} from "../shared/fieldUtils.ts";
import {LabelWithTooltip} from "../shared/LabelWithTooltip.tsx";
import {AUTO_COMPLETE_ITEM_STYLE, FILTER_EMPTY, FORM_LAYOUT} from "../shared/constants.ts";
import type {PrimitiveFormItemProps} from "../shared/types.ts";
import {ArrayItemExpandButton} from "./ArrayItemExpandButton.tsx";

export const ArrayOfPrimitiveFormItem = memo(function ArrayOfPrimitiveFormItem({
                                                                                    field,
                                                                                    bgColor,
                                                                                }: PrimitiveFormItemProps) {
    const form = Form.useFormInstance();
    useSyncFieldValue(form, field);

    const itemStyle = fieldItemStyle(bgColor);
    const hasOptions = hasAutoCompleteOptions(field) && field.autoCompleteOptions != null;
    const inputItemStyle = hasOptions ? AUTO_COMPLETE_ITEM_STYLE : FILTER_EMPTY;

    return (
        <Form.Item
            {...FORM_LAYOUT}
            label={<LabelWithTooltip name={field.name} comment={field.comment} isAutoFontSize/>}
            style={itemStyle}
        >
            <Form.List name={field.name} initialValue={field.value as unknown[]}>
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
