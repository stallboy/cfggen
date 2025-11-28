import {
    EntityEdit,
    EntityEditField,
    EntityEditFieldOption, EntityEditFieldOptions,
    EntitySharedSetting, FuncAddType,
    FuncSubmitType,
    FuncType
} from "./entityModel.ts";
import {Button, ConfigProvider, Flex, Form, InputNumber, Select, Space, Switch, Tag, Tooltip,} from "antd";
import TextArea from "antd/es/input/TextArea";
import {
    ArrowDownOutlined,
    ArrowUpOutlined, LeftOutlined,
    MinusSquareTwoTone,
    PlusSquareTwoTone,
    RightOutlined,
} from "@ant-design/icons";
import {CSSProperties, memo, useCallback, useEffect, useMemo, useState} from "react";
import {useTranslation} from "react-i18next";
import {Handle, NodeProps, Position} from "@xyflow/react";
import {getFieldBackgroundColor} from "./colors.ts";
import {CustomAutoComplete} from "./CustomAutoComplete.tsx";
import {EntityNode} from "./FlowGraph.tsx";
import {useHotkeys} from "react-hotkeys-hook";

const formLayout = {
    labelCol: {xs: {span: 24}, sm: {span: 6},},
    wrapperCol: {xs: {span: 24}, sm: {span: 18},},
};

const formItemLayout = formLayout;

const formItemLayoutWithOutLabel = {
    wrapperCol: {
        xs: {span: 24, offset: 0},
        sm: {span: 18, offset: 6},
    },
};

const setOfNumber = new Set<string>(['int', 'long', 'float']);

function filterOption(inputValue: string, option?: EntityEditFieldOption): boolean {
    const iv = inputValue.toLowerCase();
    return (!!option) && option.labelstr.toLowerCase().includes(iv);
}

function filterNumberSort(optionA: EntityEditFieldOption, optionB: EntityEditFieldOption): number {
    return (optionA.value as number) - (optionB.value as number);
}


function defaultPrimitiveValue(field: EntityEditField) {
    const {eleType, autoCompleteOptions} = field;
    if (autoCompleteOptions && autoCompleteOptions.options.length > 0) {
        const {options} = autoCompleteOptions;
        return options[0].value
    } else if (eleType == 'bool') {
        return false;
    } else if (setOfNumber.has(eleType)) {
        return 0;
    } else {
        return '';
    }
}

const textAreaAutoSize = {minRows: 1, maxRows: 10}


const empty = {}
const filter_integer = {filterSort: filterNumberSort}
const filter_search = {showSearch: true, filterOption: filterOption}
const filter_integerAndSearch = {filterSort: filterNumberSort, showSearch: true, filterOption: filterOption}

export interface FilterOption {
    filterSort?: (optionA: EntityEditFieldOption, optionB: EntityEditFieldOption) => number;
    showSearch?: boolean;
    filterOption?: (inputValue: string, option?: EntityEditFieldOption) => boolean
}

function getFilter(isValueInteger: boolean, useSearch: boolean): FilterOption {
    if (isValueInteger) {
        return useSearch ? filter_integerAndSearch : filter_integer;
    } else {
        return useSearch ? filter_search : empty;
    }
}

function isArrayPrimitiveBoolOrNumber(eleType: string, autoCompleteOptions: EntityEditFieldOptions | undefined) {
    if (autoCompleteOptions && autoCompleteOptions.options.length > 0) {
        return false
    } else if (eleType == 'bool') {
        return true;
    } else {
        return setOfNumber.has(eleType);
    }
}

function primitiveControl(eleType: string, autoCompleteOptions: EntityEditFieldOptions | undefined, style: CSSProperties) {
    if (autoCompleteOptions && autoCompleteOptions.options.length > 0) {
        const {options, isValueInteger, isEnum} = autoCompleteOptions;
        const filters = getFilter(isValueInteger, options.length > 5);

        if (isEnum) {
            return <Select className='nodrag' options={options} {...filters}/>
        } else {
            return <CustomAutoComplete options={options} filters={filters}/>
        }
        // else {
        //     return <AutoComplete className='nodrag' options={options} {...filters} />
        // }

    } else if (eleType == 'bool') {
        return <Switch className='nodrag'/>;
    } else if (setOfNumber.has(eleType)) {
        return <InputNumber className='nodrag' style={style}/>;
    } else {
        return <TextArea className='nodrag' autoSize={textAreaAutoSize} style={style}/>;
    }

}

const rowFlexStyle: CSSProperties = {marginBottom: 10, position: 'relative'}

interface StructRefItemProps {
    name: string,
    comment?: string,
    handleOut?: boolean,
    bgColor?: string,
    width?: number
}

const StructRefItem = memo(function ({name, comment, handleOut, bgColor, width}: StructRefItemProps) {
    const thisRowStyle: CSSProperties = useMemo(() => bgColor == undefined ? rowFlexStyle : {
        marginBottom: 10,
        position: 'relative',
        backgroundColor: bgColor
    }, [bgColor]);

    const handleOutStyle: CSSProperties = useMemo(() => {
        const nodeWidth = width ?? 280;
        return {position: 'absolute', left: `${nodeWidth - 10}px`, backgroundColor: 'blue'}
    }, [width]);

    return <Flex key={name} gap='middle' justify="flex-end" style={thisRowStyle}>
        <Tag color={'blue'}>
            <LabelWithTooltip name={name} comment={comment}/>
        </Tag>
        {handleOut &&
            <Handle id={name} type='source'
                    position={Position.Right}
                    style={handleOutStyle}/>}
    </Flex>
});

interface FuncAddFormItemProps extends StructRefItemProps {
    func: FuncAddType;
    nodeProps: NodeProps<EntityNode>,
}

const FuncAddFormItem = memo(function ({
                                           name,
                                           comment,
                                           handleOut,
                                           bgColor,
                                           func,
                                           nodeProps,
                                           width
                                       }: FuncAddFormItemProps) {
    const thisRowStyle: CSSProperties = useMemo(() => bgColor == undefined ? rowFlexStyle : {
        marginBottom: 10,
        position: 'relative',
        backgroundColor: bgColor
    }, [bgColor]);

    const handleOutStyle: CSSProperties = useMemo(() => {
        const nodeWidth = width ?? 280;
        return {position: 'absolute', left: `${nodeWidth - 10}px`, backgroundColor: 'blue'}
    }, [width]);

    const addFunc = useCallback(() => {
        func({
            id: nodeProps.data.entity.id,
            x: nodeProps.positionAbsoluteX,
            y: nodeProps.positionAbsoluteY
        });
    }, [func, nodeProps]);

    return <Flex key={name} gap='middle' justify="flex-end" style={thisRowStyle}>
        <Button className='nodrag' onClick={addFunc} icon={<PlusSquareTwoTone/>}>
            <LabelWithTooltip name={name} comment={comment}/>
        </Button>
        {handleOut && <Handle id={name} type='source'
                              position={Position.Right}
                              style={handleOutStyle}/>}
    </Flex>;
});

const PrimitiveFormItem = memo(function ({field, bgColor}: {
    field: EntityEditField,
    bgColor?: string
}) {
    const props = useMemo(() => field.eleType == 'bool' ? {valuePropName: "checked"} : {}, [field.eleType])

    const form = Form.useFormInstance();
    useEffect(() => {
        form.setFieldValue(field.name, field.value);
    }, [field.name, field.value, form]);

    const thisItemStyle: CSSProperties = useMemo(() => {
        return bgColor == undefined ? {} : {backgroundColor: bgColor}
    }, [bgColor]);

    const primitiveCtrl = useMemo(() => primitiveControl(field.eleType, field.autoCompleteOptions, thisItemStyle),
        [field.eleType, field.autoCompleteOptions, thisItemStyle]);

    return <Form.Item key={field.name}
                      name={field.name}
                      label={<LabelWithTooltip name={field.name} comment={field.comment} isAutoFontSize={true}/>}
                      initialValue={field.value}
                      {...props}
                      style={thisItemStyle}>
        {primitiveCtrl}
    </Form.Item>;
});

const LabelWithTooltip = memo(function ({name, comment, isAutoFontSize}: {
    name: string, comment?: string, isAutoFontSize?: boolean
}) {
    return (comment != undefined && comment.length > 0) ?
        <Tooltip title={comment}>{autoSizeName(name, true, isAutoFontSize)}</Tooltip> :
        autoSizeName(name, false, isAutoFontSize);
});

function autoSizeName(name: string, hasTooltip: boolean, autoSizeName?: boolean) {
    if (name.length < 9 || !autoSizeName) {
        return hasTooltip ? <i>{name}</i> : <>{name}</>;
    } else {
        return hasTooltip ? <i style={{fontSize: '0.75em'}}>{name}</i> :
            <span style={{fontSize: '0.75em'}}>{name}</span>;
    }
}

const autoCompleteItemStyle = {style: {width: 170}}


const ArrayOfPrimitiveFormItem = memo(function ({field, bgColor}: {
    field: EntityEditField,
    bgColor?: string
}) {
    const form = Form.useFormInstance();
    useEffect(() => {
        form.setFieldValue(field.name, field.value);
    }, [field.name, field.value, form]);

    const thisItemStyle: CSSProperties = useMemo(() => bgColor == undefined ? {} : {
        backgroundColor: bgColor
    }, [bgColor]);

    const itemStyle = field.autoCompleteOptions != null ? autoCompleteItemStyle : empty;

    return <Form.Item {...formItemLayout}
                      label={<LabelWithTooltip name={field.name} comment={field.comment} isAutoFontSize={true}/>}
                      style={thisItemStyle}>
        <Form.List name={field.name} key={field.name} initialValue={field.value as unknown[]}>
            {(fields, {add, remove, move}) => (
                <>
                    {fields.map((f, index) => (
                        <Flex key={f.key} align='center' justify='space-between' style={{width: '100%'}}>
                            <Form.Item name={f.name} {...itemStyle} style={{flex: 1, marginBottom: 0}}>
                                {primitiveControl(field.eleType, field.autoCompleteOptions, thisItemStyle)}
                            </Form.Item>
                            <ArrayItemExpandButton
                                fold={!isArrayPrimitiveBoolOrNumber(field.eleType, field.autoCompleteOptions)}
                                remove={() => remove(f.name)}
                                up={index != 0 ? () => move(index, index - 1) : undefined}
                                down={index != fields.length - 1 ? () => move(index, index + 1) : undefined}/>
                        </Flex>
                    ))}
                    <Button className='nodrag'
                            icon={<PlusSquareTwoTone/>}
                            onClick={() => add(defaultPrimitiveValue(field))}
                    />

                </>
            )
            }

        </Form.List>

    </Form.Item>
});


function ArrayItemExpandButton({fold, remove, up, down}: {
    fold: boolean,
    remove: () => void,
    up?: () => void,
    down?: () => void
}) {

    const [expand, setExpand] = useState<boolean>(false);
    const toggleExpand = useCallback(() => {
        setExpand(!expand)
    }, [expand, setExpand]);
    const removeB = <Button className='nodrag'
                            icon={<MinusSquareTwoTone/>}
                            onClick={remove}/>
    let upB;
    if (up) {
        upB = <Button className='nodrag'
                      icon={<ArrowUpOutlined/>}
                      onClick={up}/>
    }

    let downB;
    if (down) {
        downB = <Button className='nodrag'
                        icon={<ArrowDownOutlined/>}
                        onClick={down}/>
    }

    if (upB == undefined && downB == undefined) {
        return removeB;
    }

    if (!fold) {
        return <>{removeB}{upB}{downB}</>
    }


    return <Space size={'small'}>
        <Button className='nodrag' icon={expand ? <LeftOutlined/> : <RightOutlined/>} onClick={toggleExpand}/>
        {expand && <>{removeB}{upB}{downB}</>}
    </Space>
}

const FuncSubmitFormItem = memo(function ({field}: {
    field: EntityEditField
}) {
    const [t] = useTranslation();
    const func = field.value as FuncSubmitType;
    useHotkeys('alt+s', () => func.funcSubmit());
    return <Form.Item {...formItemLayoutWithOutLabel} key={field.name}>
        <Space size={50}>
            <Button className='nodrag' type="primary" htmlType="submit" onClick={() => func.funcSubmit()}>
                <Tooltip title={t('addOrUpdateTooltip')}> {t('addOrUpdate')}</Tooltip>
            </Button>
            <Button className='nodrag' type="default" onClick={() => func.funcClear()}>
                {t('setDefaultValue')}
            </Button>
        </Space>
    </Form.Item>
});


const InterfaceFormItem = memo(function ({field, nodeProps, sharedSetting}: {
    field: EntityEditField,
    nodeProps: NodeProps<EntityNode>
    sharedSetting?: EntitySharedSetting
}) {
    // 需要这个useEffect，参考https://zhuanlan.zhihu.com/p/375753910
    // edit后，key又都相同，initialValue改变，所以需要form需要设置回去
    const form = Form.useFormInstance();
    useEffect(() => {
        form.setFieldValue(field.name, field.value);
    }, [field.name, field.value, form]);

    const onSelectChange = useCallback((value: string) => {
        field.interfaceOnChangeImpl!(value, {
            id: nodeProps.data.entity.id,
            x: nodeProps.positionAbsoluteX,
            y: nodeProps.positionAbsoluteY
        });
    }, [field.interfaceOnChangeImpl, nodeProps.data.entity.id, nodeProps.positionAbsoluteX, nodeProps.positionAbsoluteY]);

    const options = field.autoCompleteOptions?.options;
    const filters = getFilter(false, !!options && options.length > 5);

    const formItem = useMemo(() => (
        <Form.Item key={field.name}
                   name={field.name}
                   label={">"}
                   initialValue={field.value}>
            <Select className='nodrag' options={options}
                    {...filters}
                    onChange={onSelectChange}/>
        </Form.Item>
    ), [field.name, field.value, options, filters, onSelectChange]);

    return <>
        {formItem}
        {fieldsFormItem(field.implFields as EntityEditField[], nodeProps, sharedSetting)}
    </>
});

function fieldFormItem(field: EntityEditField, nodeProps: NodeProps<EntityNode>, sharedSetting?: EntitySharedSetting,) {
    const bgColor = getFieldBackgroundColor(field, sharedSetting?.nodeShow)
    switch (field.type) {
        case "structRef":
            return <StructRefItem key={field.name}
                                  name={field.name} comment={field.comment} handleOut={field.handleOut}
                                  bgColor={bgColor} width={sharedSetting?.nodeShow?.editNodeWidth}/>;
        case "arrayOfPrimitive":
            return <ArrayOfPrimitiveFormItem key={field.name}
                                             field={field}
                                             bgColor={bgColor}/>;
        case "primitive":
            return <PrimitiveFormItem key={field.name}
                                      field={field}
                                      bgColor={bgColor}/>;
        case "funcAdd":
            return <FuncAddFormItem key={field.name}
                                    name={field.name} comment={field.comment} handleOut={field.handleOut}
                                    func={field.value as FuncType}
                                    nodeProps={nodeProps} // 增加子struct时，本节点位置不变
                                    bgColor={bgColor} width={sharedSetting?.nodeShow?.editNodeWidth}/>;
        case "interface":
            return <InterfaceFormItem key={field.name}
                                      field={field}
                                      nodeProps={nodeProps} // 改变impl时，本节点位置不变
                                      sharedSetting={sharedSetting}/>;
        case "funcSubmit":
            return <FuncSubmitFormItem key={field.name}
                                       field={field}/>;
    }
}

function fieldsFormItem(fields: EntityEditField[], nodeProps: NodeProps<EntityNode>, sharedSetting?: EntitySharedSetting) {
    return fields.map((field) => fieldFormItem(field, nodeProps, sharedSetting,));
}

const theme = {
    components: {
        Form: {
            itemMarginBottom: 8,
        },
    },
}
const formStyle = {backgroundColor: "white", borderRadius: 15, padding: 10}

export const EntityForm = memo(function ({edit, nodeProps, sharedSetting}: {
    edit: EntityEdit;
    nodeProps: NodeProps<EntityNode>
    sharedSetting?: EntitySharedSetting,
}) {
    const [_form] = Form.useForm();

    // form里单个字段的改变不会引起这个界面更新，只更新jsonObject对象
    // initialValue放在每个Form.Item里

    return <ConfigProvider theme={theme}>
        <Form {...formLayout}
              form={_form}
              onValuesChange={(_changedFields, allFields) => {
                  edit.editOnUpdateValues(allFields);
              }}
              style={formStyle}>
            {fieldsFormItem(edit.editFields, nodeProps, sharedSetting)}
        </Form>
    </ConfigProvider>
});
