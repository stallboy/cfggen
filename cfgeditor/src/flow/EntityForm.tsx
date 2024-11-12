import {
    EntityEdit,
    EntityEditField,
    EntityEditFieldOption, EntityEditFieldOptions,
    EntitySharedSetting,
    FuncSubmitType,
    FuncType
} from "./entityModel.ts";
import {Button, ConfigProvider, Flex, Form, InputNumber, Select, Space, Switch, Tag, Tooltip,} from "antd";
import TextArea from "antd/es/input/TextArea";
import {
    ArrowDownOutlined,
    ArrowUpOutlined,
    MinusSquareTwoTone,
    PlusSquareTwoTone,
    RightOutlined,
} from "@ant-design/icons";
import {CSSProperties, memo, useCallback, useMemo, useState} from "react";
import {useTranslation} from "react-i18next";
import {Handle, NodeProps, Position} from "@xyflow/react";
import {getFieldBackgroundColor} from "./colors.ts";
import {CustomAutoComplete} from "./CustomAutoComplete.tsx";
import {EntityNode} from "./FlowGraph.tsx";

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
    return (!!option) && (option.value.toString().toLowerCase().includes(iv) || option.label.toLowerCase().includes(iv));
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

function getFilter(isValueInteger: boolean, useSearch: boolean) {
    if (isValueInteger) {
        return useSearch ? filter_integerAndSearch : filter_integer;
    } else {
        return useSearch ? filter_search : empty;
    }
}

function primitiveControl(eleType: string, autoCompleteOptions?: EntityEditFieldOptions, style?: any) {
    if (autoCompleteOptions && autoCompleteOptions.options.length > 0) {
        const {options, isValueInteger, isEnum} = autoCompleteOptions;
        const filters: any = getFilter(isValueInteger, options.length > 5);

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
        return <InputNumber className='nodrag'  {...style} />;
    } else {
        return <TextArea className='nodrag' autoSize={textAreaAutoSize} {...style}/>;
    }

}

const rowFlexStyle: CSSProperties = {marginBottom: 10, position: 'relative'}
const handleOutStyle: CSSProperties = {position: 'absolute', left: '270px', backgroundColor: 'blue'}

interface StructRefItemProps {
    name: string,
    comment?: string,
    handleOut?: boolean,
    bgColor?: string
}

const StructRefItem = memo(function ({name, comment, handleOut, bgColor}: StructRefItemProps) {
    const thisRowStyle: CSSProperties = useMemo(() => bgColor == undefined ? rowFlexStyle : {
        marginBottom: 10,
        position: 'relative',
        backgroundColor: bgColor
    }, [bgColor]);
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
    func: FuncType;
}

const FuncAddFormItem = memo(function ({name, comment, handleOut, bgColor, func}: FuncAddFormItemProps) {
    const thisRowStyle: CSSProperties = useMemo(() => bgColor == undefined ? rowFlexStyle : {
        marginBottom: 10,
        position: 'relative',
        backgroundColor: bgColor
    }, [bgColor]);
    return <Flex key={name} gap='middle' justify="flex-end" style={thisRowStyle}>
        <Button className='nodrag' onClick={func} icon={<PlusSquareTwoTone/>}>
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
    let props = useMemo(() => field.eleType == 'bool' ? {valuePropName: "checked"} : {}, [field.eleType])

    const thisItemStyle = useMemo(() => {
        return bgColor == undefined ? {} : {style: {backgroundColor: bgColor}}
    }, [bgColor]);

    const primitiveCtrl = useMemo(() => primitiveControl(field.eleType, field.autoCompleteOptions, thisItemStyle),
        [field.eleType, field.autoCompleteOptions, thisItemStyle]);

    return <Form.Item name={field.name} key={field.name}
                      label={<LabelWithTooltip name={field.name} comment={field.comment}/>}
                      initialValue={field.value}
                      {...props}
                      {...thisItemStyle}>
        {primitiveCtrl}
    </Form.Item>;
});

const LabelWithTooltip = memo(function ({name, comment}: { name: string, comment?: string }) {
    return (comment != undefined && comment.length > 0) ?
        <Tooltip title={comment}><i>{name}</i></Tooltip> :
        <>{name}</>;
});

const autoCompleteItemStyle = {style: {width: 170}}


const ArrayOfPrimitiveFormItem = memo(function ({field, bgColor}: {
    field: EntityEditField,
    bgColor?: string
}) {
    const thisItemStyle: CSSProperties = useMemo(() => bgColor == undefined ? {} : {
        backgroundColor: bgColor
    }, [bgColor]);

    const itemStyle = field.autoCompleteOptions != null ? autoCompleteItemStyle : empty;

    return <Form.Item {...formItemLayout}
                      label={<LabelWithTooltip name={field.name} comment={field.comment}/>}
                      style={thisItemStyle}>
        <Form.List name={field.name} key={field.name} initialValue={field.value as any[]}>
            {(fields, {add, remove, move}) => (
                <>
                    {fields.map((f, index) => (
                        <Space key={f.key} align='baseline' size={2}>
                            <Form.Item name={f.name} {...itemStyle}>
                                {primitiveControl(field.eleType, field.autoCompleteOptions)}
                            </Form.Item>
                            <ArrayItemDropdownButton fold={field.autoCompleteOptions != null}
                                                     remove={() => remove(f.name)}
                                                     up={index != 0 ? () => move(index, index - 1) : undefined}
                                                     down={index != fields.length - 1 ? () => move(index, index + 1) : undefined}/>
                        </Space>
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


function ArrayItemDropdownButton({fold, remove, up, down}: {
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
        <Button className='nodrag' icon={<RightOutlined/>} onClick={toggleExpand}/>
        {expand && <>{removeB}{upB}{downB}</>}
    </Space>
}

const FuncSubmitFormItem = memo(function ({field}: {
    field: EntityEditField
}) {
    const [t] = useTranslation();
    const func = field.value as FuncSubmitType;
    return <Form.Item {...formItemLayoutWithOutLabel} key={field.name}>
        <Space size={50}>
            <Button className='nodrag' type="primary" htmlType="submit" onClick={() => func.funcSubmit()}>
                {t('addOrUpdate')}
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
    const onSelectChange = useCallback((value: string) => {
        field.interfaceOnChangeImpl!(value, nodeProps.data.entity.id, {x: nodeProps.positionAbsoluteX, y: nodeProps.positionAbsoluteY});
    }, [field]);

    const options = field.autoCompleteOptions?.options!;
    const filters = getFilter(false, options.length > 5);

    const formItem = useMemo(() => (
        <Form.Item name={field.name} key={field.name} label={">"}
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
                                  bgColor={bgColor}/>;
        case "arrayOfPrimitive":
            return <ArrayOfPrimitiveFormItem key={field.name} field={field} bgColor={bgColor}/>;
        case "primitive":
            return <PrimitiveFormItem key={field.name} field={field} bgColor={bgColor}/>;
        case "funcAdd":
            return <FuncAddFormItem key={field.name}
                                    name={field.name} comment={field.comment} handleOut={field.handleOut}
                                    func={field.value as FuncType}
                                    bgColor={bgColor}/>;
        case "interface":
            return <InterfaceFormItem key={field.name} field={field} nodeProps={nodeProps} sharedSetting={sharedSetting}/>;
        case "funcSubmit":
            return <FuncSubmitFormItem key={field.name} field={field}/>;
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
    const onValuesChange = useCallback((_changedFields: any, allFields: any) => {
        edit.editOnUpdateValues(allFields);
    }, [edit]);

    return <ConfigProvider theme={theme}>
        <Form {...formLayout}
              form={_form}
              onValuesChange={onValuesChange}
              style={formStyle}>
            {fieldsFormItem(edit.editFields, nodeProps, sharedSetting)}
        </Form>
    </ConfigProvider>
});
