import {
    ArrowDownOutlined, ArrowsAltOutlined,
    ArrowUpOutlined,
    LeftOutlined,
    MinusSquareTwoTone,
    PlusSquareTwoTone,
    RightOutlined,
} from "@ant-design/icons";
import {
    Button,
    ConfigProvider,
    Flex,
    Form,
    InputNumber,
    Select,
    Space,
    Switch,
    Tag,
    Tooltip,
} from "antd";
import TextArea from "antd/es/input/TextArea";
import {Handle, NodeProps, Position} from "@xyflow/react";
import {useHotkeys} from "react-hotkeys-hook";
import {useTranslation} from "react-i18next";
import {CSSProperties, Fragment, memo, useCallback, useEffect, useMemo, useState} from "react";

import {CustomAutoComplete} from "./CustomAutoComplete.tsx";
import {getFieldBackgroundColor} from "./colors.ts";
import {
    EntityEdit,
    EntityEditField,
    EntityEditFieldOption,
    EntityEditFieldOptions,
    EntitySharedSetting,
    FuncAddType,
    FuncSubmitType,
    FuncType,
    InterfaceEditField, PrimitiveType,
    PrimitiveValue, StructRefEditField,
} from "./entityModel.ts";
import {EntityNode} from "./FlowGraph.tsx";

// ============================================================================
// 常量定义
// ============================================================================

const DEFAULT_NODE_WIDTH = 280;

const FORM_LAYOUT = {
    labelCol: {xs: {span: 24}, sm: {span: 6}},
    wrapperCol: {xs: {span: 24}, sm: {span: 18}},
};

const FORM_ITEM_LAYOUT_WITHOUT_LABEL = {
    wrapperCol: {
        xs: {span: 24, offset: 0},
        sm: {span: 18, offset: 6},
    },
};

const TEXT_AREA_AUTO_SIZE = {minRows: 1, maxRows: 10};

const AUTO_COMPLETE_ITEM_STYLE = {style: {width: 170}};

// 数字类型集合
const NUMBER_TYPES = new Set<string>(['int', 'long', 'float']);

// Select 过滤器配置
const FILTER_EMPTY = {};

const FILTER_SEARCH = {
    showSearch: true,
    filterOption: (input: string, option?: EntityEditFieldOption) => {
        if (!option) return false;
        return option.labelstr.toLowerCase().includes(input.toLowerCase());
    },
};


// 主题配置
const FORM_THEME = {
    components: {
        Form: {
            itemMarginBottom: 8,
        },
    },
};

const FORM_STYLE = {backgroundColor: "white", borderRadius: 15, padding: 10};

// ============================================================================
// 类型定义
// ============================================================================

export interface FilterOption {
    filterSort?: (a: EntityEditFieldOption, b: EntityEditFieldOption) => number;
    showSearch?: boolean;
    filterOption?: (input: string, option?: EntityEditFieldOption) => boolean;
}

// ============================================================================
// 类型守卫函数
// ============================================================================

function hasAutoCompleteOptions(
    field: EntityEditField
): field is EntityEditField & { autoCompleteOptions: EntityEditFieldOptions } {
    return field.type === "primitive" || field.type === "arrayOfPrimitive" || field.type === "interface";
}

// ============================================================================
// 工具函数
// ============================================================================

function getFilter(_isValueInteger: boolean, useSearch: boolean): FilterOption {
    return useSearch ? FILTER_SEARCH : FILTER_EMPTY;
}

function getDefaultPrimitiveValue(field: EntityEditField): PrimitiveValue {
    const {eleType} = field;

    // 有自动完成选项时使用第一个选项值
    if (hasAutoCompleteOptions(field) && field.autoCompleteOptions?.options.length) {
        return field.autoCompleteOptions.options[0].value as PrimitiveValue;
    }

    // 根据类型返回默认值
    switch (eleType) {
        case "bool":
            return false;
        case "int":
        case "long":
        case "float":
            return 0;
        default:
            return "";
    }
}

function isArrayPrimitiveBoolOrNumber(field: EntityEditField): boolean {
    // 有自动完成选项时不展开
    if (hasAutoCompleteOptions(field) && field.autoCompleteOptions?.options.length) {
        return false;
    } else if (field.eleType == 'bool') {
        return true;
    } else {
        return NUMBER_TYPES.has(field.eleType);
    }
}

// ============================================================================
// 原始类型控件渲染
// ============================================================================

function primitiveControl(field: EntityEditField, style: CSSProperties) {
    const {eleType} = field;
    const autoCompleteOptions = hasAutoCompleteOptions(field) ? field.autoCompleteOptions : undefined;

    if (autoCompleteOptions?.options.length) {
        const {options, isValueInteger, isEnum} = autoCompleteOptions;
        const filters = getFilter(isValueInteger, options.length > 5);

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
            return <TextArea className="nodrag" autoSize={TEXT_AREA_AUTO_SIZE} style={style}/>;
    }
}

// ============================================================================
// 样式 Hook
// ============================================================================

function useRefItemStyles(width?: number, bgColor?: string) {
    const rowStyle: CSSProperties = useMemo(
        () => ({
            marginBottom: 10,
            position: "relative",
            ...(bgColor && {backgroundColor: bgColor}),
        }),
        [bgColor]
    );

    const handleOutStyle: CSSProperties = useMemo(
        () => ({
            position: "absolute",
            left: `${(width ?? DEFAULT_NODE_WIDTH) - 10}px`,
            backgroundColor: "blue",
        }),
        [width]
    );

    return {rowStyle, handleOutStyle};
}

// ============================================================================
// 标签组件
// ============================================================================

interface LabelProps {
    name: string;
    comment?: string;
    isAutoFontSize?: boolean;
}

const LabelWithTooltip = memo(function LabelWithTooltip({
                                                            name,
                                                            comment,
                                                            isAutoFontSize,
                                                        }: LabelProps) {
    const content = autoSizeName(name, isAutoFontSize);

    if (comment && comment.length > 0) {
        return <Tooltip title={comment}>{content}</Tooltip>;
    }
    return content;
});

function autoSizeName(name: string, autoSize?: boolean) {
    const shouldShrink = name.length >= 9 && autoSize;
    const style = shouldShrink ? {fontSize: "0.75em"} : undefined;
    const Wrapper = shouldShrink ? "span" : "i";

    return <Wrapper style={style}>{name}</Wrapper>;
}

// ============================================================================
// 结构体引用项组件
// ============================================================================

interface StructRefItemProps {
    name: string;
    comment?: string;
    handleOut?: boolean;
    bgColor?: string;
    width?: number;
}

const StructRefItem = memo(function StructRefItem({
                                                      name,
                                                      comment,
                                                      handleOut,
                                                      bgColor,
                                                      width,
                                                  }: StructRefItemProps) {
    const {rowStyle, handleOutStyle} = useRefItemStyles(width, bgColor);

    return (
        <Flex gap="middle" justify="flex-end" style={rowStyle}>
            <Tag color="blue">
                <LabelWithTooltip name={name} comment={comment}/>
            </Tag>
            {handleOut && (
                <Handle id={name} type="source" position={Position.Right} style={handleOutStyle}/>
            )}
        </Flex>
    );
});

// ============================================================================
// 内嵌原始类型字段组件
// ============================================================================

interface EmbeddedSimpleStructuralItemProps {
    field: StructRefEditField;
    edit: EntityEdit,
    nodeProps: NodeProps<EntityNode>,
    bgColor?: string;
}

const EmbeddedSimpleStructuralItem = memo(
    function EmbeddedSimpleStructuralItem({field, edit, nodeProps}: EmbeddedSimpleStructuralItemProps) {
        const entity = nodeProps.data.entity;
        const embeddedData = field.embeddedField!;

        // 组合comment：field.comment + embeddedData.note
        const fieldComment = useMemo(() => {
            const parts: string[] = [];
            if (field.comment) parts.push(field.comment);
            if (embeddedData.note) parts.push(embeddedData.note);
            return parts.join(' ');
        }, [field.comment, embeddedData.note]);

        // 字段名称Tag颜色（有note时黄色）
        const fieldNameTagColor = embeddedData.note ? '#876800' : 'blue';

        // 点击展开
        const handleExpand = useCallback(() => {
            if (embeddedData.embeddedFieldChain && edit.editOnUpdateFold) {
                edit.editOnUpdateFold(
                    false,
                    {
                        id: entity.id,
                        x: nodeProps.positionAbsoluteX,
                        y: nodeProps.positionAbsoluteY,
                    },
                    embeddedData.embeddedFieldChain
                );
            }
        }, [embeddedData.embeddedFieldChain, edit, entity.id, nodeProps.positionAbsoluteX, nodeProps.positionAbsoluteY]);

        // 格式化单个值的显示
        const formatDisplayValue = useCallback((value: PrimitiveValue, type: PrimitiveType): string => {
            if (value === undefined || value === null) {
                return '-';
            }
            if (type === 'bool') {
                return value ? '✓' : '✗';
            }
            return String(value);
        }, []);

        // 渲染单个值Tag
        const renderValueTag = useCallback((
            value: PrimitiveValue,
            type: PrimitiveType,
            name: string,
            comment?: string
        ) => {
            const displayValue = formatDisplayValue(value, type);
            const valueStyle: CSSProperties = {
                color: (value === undefined || value === null || value === '')
                    ? '#999'
                    : undefined,
            };

            // 值的comment组合：name + comment
            const valueComment = comment ? `${name}: ${comment}` : name;

            return (
                <Tag color="blue" style={valueStyle}>
                    <LabelWithTooltip
                        name={displayValue}
                        comment={valueComment}
                    />
                </Tag>
            );
        }, [formatDisplayValue]);

        return (
            <Flex gap="small" justify="flex-end" align="center">
                {/* 显示字段名称（根据note显示黄色或蓝色Tag） */}
                <Tag color={fieldNameTagColor}>
                    <LabelWithTooltip
                        name={field.name}
                        comment={fieldComment || undefined}
                    />
                </Tag>

                {/* 显示implName（如果存在且非defaultImpl） */}
                {embeddedData.implName && (
                    <Tag color="cyan">
                        {embeddedData.implName}
                    </Tag>
                )}

                {/* 显示值（可能有多个） */}
                {embeddedData.fields.map(f => (
                    <Fragment key={f.name}>
                        {renderValueTag(f.value, f.type, f.name, f.comment)}
                    </Fragment>
                ))}

                {/* 展开按钮 */}
                <Button
                    className="nodrag"
                    type="text"
                    style={{color: '#1890ff'}}
                    icon={<ArrowsAltOutlined />}
                    onClick={handleExpand}
                />
            </Flex>
        );
    });

// ============================================================================
// 函数添加表单项组件
// ============================================================================

interface FuncAddFormItemProps extends StructRefItemProps {
    func: FuncAddType;
    nodeProps: NodeProps<EntityNode>;
}

const FuncAddFormItem = memo(function FuncAddFormItem({
                                                          name,
                                                          comment,
                                                          handleOut,
                                                          bgColor,
                                                          func,
                                                          nodeProps,
                                                          width,
                                                      }: FuncAddFormItemProps) {
    const {rowStyle, handleOutStyle} = useRefItemStyles(width, bgColor);

    const handleAdd = useCallback(() => {
        func({
            id: nodeProps.data.entity.id,
            x: nodeProps.positionAbsoluteX,
            y: nodeProps.positionAbsoluteY,
        });
    }, [func, nodeProps]);

    return (
        <Flex gap="middle" justify="flex-end" style={rowStyle}>
            <Button className="nodrag" onClick={handleAdd} icon={<PlusSquareTwoTone/>}>
                <LabelWithTooltip name={name} comment={comment}/>
            </Button>
            {handleOut && (
                <Handle id={name} type="source" position={Position.Right} style={handleOutStyle}/>
            )}
        </Flex>
    );
});

// ============================================================================
// 原始类型表单项组件
// ============================================================================

interface PrimitiveFormItemProps {
    field: EntityEditField;
    bgColor?: string;
}

const PrimitiveFormItem = memo(function PrimitiveFormItem({field, bgColor}: PrimitiveFormItemProps) {
    const form = Form.useFormInstance();

    useEffect(() => {
        form.setFieldValue(field.name, field.value);
    }, [field.name, field.value, form]);

    const itemStyle: CSSProperties = useMemo(
        () => (bgColor === undefined ? {} : {backgroundColor: bgColor}),
        [bgColor]
    );

    const boolProps = useMemo(
        () => (field.eleType === "bool" ? {valuePropName: "checked" as const} : {}),
        [field.eleType]
    );

    const control = useMemo(() => primitiveControl(field, itemStyle), [field, itemStyle]);

    return (
        <Form.Item
            key={field.name}
            name={field.name}
            label={<LabelWithTooltip name={field.name} comment={field.comment} isAutoFontSize/>}
            initialValue={field.value}
            {...boolProps}
            style={itemStyle}
        >
            {control}
        </Form.Item>
    );
});

// ============================================================================
// 数组原始类型表单项组件
// ============================================================================

const ArrayOfPrimitiveFormItem = memo(function ArrayOfPrimitiveFormItem({
                                                                            field,
                                                                            bgColor,
                                                                        }: PrimitiveFormItemProps) {
    const form = Form.useFormInstance();

    useEffect(() => {
        form.setFieldValue(field.name, field.value);
    }, [field.name, field.value, form]);

    const itemStyle: CSSProperties = useMemo(
        () => (bgColor === undefined ? {} : {backgroundColor: bgColor}),
        [bgColor]
    );

    const hasOptions = hasAutoCompleteOptions(field) && field.autoCompleteOptions != null;
    const inputItemStyle = hasOptions ? AUTO_COMPLETE_ITEM_STYLE : FILTER_EMPTY;

    return (
        <Form.Item
            {...FORM_LAYOUT}
            label={<LabelWithTooltip name={field.name} comment={field.comment} isAutoFontSize/>}
            style={itemStyle}
        >
            <Form.List name={field.name} key={field.name} initialValue={field.value as unknown[]}>
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


// ============================================================================
// 数组项展开按钮组件
// ============================================================================

interface ArrayItemExpandButtonProps {
    fold: boolean;
    onRemove: () => void;
    onMoveUp?: () => void;
    onMoveDown?: () => void;
}

function ArrayItemExpandButton({fold, onRemove, onMoveUp, onMoveDown}: ArrayItemExpandButtonProps) {
    const [expand, setExpand] = useState(false);

    const toggleExpand = useCallback(() => {
        setExpand((prev) => !prev);
    }, []);

    const removeButton = <Button className="nodrag" icon={<MinusSquareTwoTone/>} onClick={onRemove}/>;

    // 没有上下移动按钮时，直接返回删除按钮
    if (!onMoveUp && !onMoveDown) {
        return removeButton;
    }

    // 不需要折叠时，显示所有按钮
    if (!fold) {
        return (
            <>
                {removeButton}
                {onMoveUp && <Button className="nodrag" icon={<ArrowUpOutlined/>} onClick={onMoveUp}/>}
                {onMoveDown && <Button className="nodrag" icon={<ArrowDownOutlined/>} onClick={onMoveDown}/>}
            </>
        );
    }

    // 需要折叠时，使用展开/收起按钮
    return (
        <Space size="small">
            <Button className="nodrag" icon={expand ? <LeftOutlined/> : <RightOutlined/>} onClick={toggleExpand}/>
            {expand && (
                <>
                    {removeButton}
                    {onMoveUp && <Button className="nodrag" icon={<ArrowUpOutlined/>} onClick={onMoveUp}/>}
                    {onMoveDown && <Button className="nodrag" icon={<ArrowDownOutlined/>} onClick={onMoveDown}/>}
                </>
            )}
        </Space>
    );
}

// ============================================================================
// 提交函数表单项组件
// ============================================================================

const FuncSubmitFormItem = memo(function FuncSubmitFormItem({field}: PrimitiveFormItemProps) {
    const [t] = useTranslation();
    const func = field.value as FuncSubmitType;

    useHotkeys("alt+s", () => func.funcSubmit());

    return (
        <Form.Item {...FORM_ITEM_LAYOUT_WITHOUT_LABEL} key={field.name}>
            <Space size={50}>
                <Button className="nodrag" type="primary" htmlType="submit" onClick={() => func.funcSubmit()}>
                    <Tooltip title={t("addOrUpdateTooltip")}>{t("addOrUpdate")}</Tooltip>
                </Button>
                <Button className="nodrag" type="default" onClick={() => func.funcClear()}>
                    {t("setDefaultValue")}
                </Button>
            </Space>
        </Form.Item>
    );
});

// ============================================================================
// 接口表单项组件
// ============================================================================

interface InterfaceFormItemProps {
    field: InterfaceEditField;
    edit: EntityEdit,
    nodeProps: NodeProps<EntityNode>;
    sharedSetting?: EntitySharedSetting;
}

const InterfaceFormItem = memo(
    function InterfaceFormItem({field, edit, nodeProps, sharedSetting}: InterfaceFormItemProps) {
        const form = Form.useFormInstance();

        // edit后，key又都相同，initialValue改变，所以form需要设置回去
        // 参考: https://zhuanlan.zhihu.com/p/375753910
        useEffect(() => {
            form.setFieldValue(field.name, field.value);
        }, [field.name, field.value, form]);

        const handleSelectChange = useCallback(
            (value: string) => {
                field.interfaceOnChangeImpl(value, {
                    id: nodeProps.data.entity.id,
                    x: nodeProps.positionAbsoluteX,
                    y: nodeProps.positionAbsoluteY,
                });
            },
            [field, nodeProps.data.entity.id, nodeProps.positionAbsoluteX, nodeProps.positionAbsoluteY]
        );

        const options = field.autoCompleteOptions?.options;
        const filters = getFilter(false, (options?.length ?? 0) > 5);

        const formItem = useMemo(
            () => (
                <Form.Item key={field.name} name={field.name} label=">" initialValue={field.value}>
                    <Select className="nodrag" options={options} {...filters} onChange={handleSelectChange}/>
                </Form.Item>
            ),
            [field.name, field.value, options, filters, handleSelectChange]
        );

        return (
            <>
                {formItem}
                {renderFieldItems(field.implFields, edit, nodeProps, sharedSetting)}
            </>
        );
    });

// ============================================================================
// 字段渲染函数
// ============================================================================

interface FieldRenderProps {
    field: EntityEditField;
    edit: EntityEdit,
    nodeProps: NodeProps<EntityNode>;
    sharedSetting?: EntitySharedSetting;
}

function renderFieldItem({field, edit, nodeProps, sharedSetting}: FieldRenderProps) {
    const bgColor = getFieldBackgroundColor(field, sharedSetting?.nodeShow);
    const width = sharedSetting?.nodeShow?.editNodeWidth;

    switch (field.type) {
        case "structRef":
            // 判断是否为内嵌模式
            if (field.embeddedField) {
                return (
                    <EmbeddedSimpleStructuralItem
                        key={field.name}
                        field={field}
                        edit={edit}
                        nodeProps={nodeProps}
                        bgColor={bgColor}
                    />
                );
            }

            // 正常structRef显示
            return (
                <StructRefItem
                    key={field.name}
                    name={field.name}
                    comment={field.comment}
                    handleOut={field.handleOut}
                    bgColor={bgColor}
                    width={width}
                />
            );

        case "arrayOfPrimitive":
            return (
                <ArrayOfPrimitiveFormItem key={field.name} field={field} bgColor={bgColor}/>
            );

        case "primitive":
            return (
                <PrimitiveFormItem key={field.name} field={field} bgColor={bgColor}/>
            );

        case "funcAdd":
            return (
                <FuncAddFormItem
                    key={field.name}
                    name={field.name}
                    comment={field.comment}
                    handleOut={field.handleOut}
                    func={field.value as FuncType}
                    nodeProps={nodeProps}
                    bgColor={bgColor}
                    width={width}
                />
            );

        case "interface":
            return (
                <InterfaceFormItem key={field.name} field={field} edit={edit}
                                   nodeProps={nodeProps} sharedSetting={sharedSetting}/>
            );

        case "funcSubmit":
            return (
                <FuncSubmitFormItem key={field.name} field={field}/>
            );

        default:
            // 理论上不会到达这里
            return <></>;
    }
}

function renderFieldItems(fields: EntityEditField[], edit: EntityEdit,
                          nodeProps: NodeProps<EntityNode>, sharedSetting?: EntitySharedSetting) {
    return fields.map((field) => renderFieldItem({field, edit, nodeProps, sharedSetting}));
}

// ============================================================================
// 主表单组件
// ============================================================================

interface EntityFormProps {
    edit: EntityEdit;
    nodeProps: NodeProps<EntityNode>;
    sharedSetting?: EntitySharedSetting;
}

export const EntityForm = memo(function EntityForm({edit, nodeProps, sharedSetting}: EntityFormProps) {
    const [form] = Form.useForm();

    // form里单个字段的改变不会引起这个界面更新，只更新jsonObject对象
    // initialValue放在每个Form.Item里
    // 参考: https://github.com/ant-design/ant-design/issues/56102
    return (
        <ConfigProvider theme={FORM_THEME}>
            <Form
                {...FORM_LAYOUT}
                form={form}
                onValuesChange={() => {
                    edit.editOnUpdateValues(form.getFieldsValue(true));
                }}
                style={FORM_STYLE}
            >
                {renderFieldItems(edit.fields, edit, nodeProps, sharedSetting)}
            </Form>
        </ConfigProvider>
    );
});
