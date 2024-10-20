import {
    EntityEdit,
    EntityEditField,
    EntityEditFieldOption,
    EntitySharedSetting,
    FuncSubmitType,
    FuncType
} from "./entityModel.ts";
import {
    Button,
    ConfigProvider,
    Flex,
    Form,
    InputNumber,
    Select,
    Space,
    Switch, Tag,
    Tooltip,
} from "antd";
import TextArea from "antd/es/input/TextArea";
import {ArrowDownOutlined, ArrowUpOutlined, MinusSquareTwoTone, PlusSquareTwoTone} from "@ant-design/icons";
import {CSSProperties, memo, useCallback, useEffect, useMemo} from "react";
import {useTranslation} from "react-i18next";
import {Handle, Position} from "@xyflow/react";
import {ActionIcon} from "@ant-design/pro-editor";
import {getFieldBackgroundColor} from "./colors.ts";
import {HappyProvider} from '@ant-design/happy-work-theme';
import {CustomAutoComplete} from "./CustomAutoComplete.tsx";

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

function primitiveControl(field: EntityEditField,
                          style: any) {

    const {eleType, autoCompleteOptions} = field;
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

function StructRefItem({field, bgColor}: {
    field: EntityEditField,
    bgColor?: string
}) {
    const thisRowStyle: CSSProperties = useMemo(() => bgColor == undefined ? rowFlexStyle : {
        marginBottom: 10,
        position: 'relative',
        backgroundColor: bgColor
    }, [bgColor]);
    return <Flex key={field.name} gap='middle' justify="flex-end" style={thisRowStyle}>
        <Tag color={'blue'}> <LabelWithTooltip field={field}/> </Tag>
        {field.handleOut && <Handle type='source' position={Position.Right} id={field.name}
                                    style={handleOutStyle}/>}
    </Flex>
}

function FuncAddFormItem({field, bgColor}: {
    field: EntityEditField,
    bgColor?: string
}) {
    const func = field.value as FuncType;
    const thisRowStyle: CSSProperties = useMemo(() => bgColor == undefined ? rowFlexStyle : {
        marginBottom: 10,
        position: 'relative',
        backgroundColor: bgColor
    }, [bgColor]);
    return <Flex key={field.name} gap='middle' justify="flex-end" style={thisRowStyle}>
        <HappyProvider>
            <Button className='nodrag' onClick={func} icon={<PlusSquareTwoTone/>}> <LabelWithTooltip field={field}/> </Button>
        </HappyProvider>
        {field.handleOut && <Handle type='source' position={Position.Right} id={field.name}
                                    style={handleOutStyle}/>}
    </Flex>;
}

function PrimitiveFormItem({field, bgColor}: {
    field: EntityEditField,
    bgColor?: string
}) {
    const form = Form.useFormInstance();
    useEffect(() => {
        form.setFieldValue(field.name, field.value);
    }, [form, field.name, field.value]);

    let props = useMemo(() => field.eleType == 'bool' ? {valuePropName: "checked"} : {}, [field.eleType])

    const thisItemStyle = useMemo(() => {
        return bgColor == undefined ? {} : {style: {backgroundColor: bgColor}}
    }, [bgColor]);

    return <Form.Item name={field.name} key={field.name} label={<LabelWithTooltip field={field}/>}
                      initialValue={field.value} {...props} {...thisItemStyle}>
        {primitiveControl(field, thisItemStyle)}
    </Form.Item>;
}

function LabelWithTooltip({field} : {field: EntityEditField}) {
    let type = '';
    switch (field.type) {
        case "arrayOfPrimitive":
            type = `list<${field.eleType}>`
            break;
        case "primitive":
        case 'structRef':
            type = field.eleType;
            break;
        case "funcAdd":
            type = `list<${field.eleType}>`
            break;
        case "interface":
            type = field.eleType;
            break;
    }
    return <Tooltip placement="topLeft" title={`${field.name} : ${type} ${field.comment ? field.comment : ""}`}>
        {field.name}
    </Tooltip>;
}

const autoCompleteItemStyle = {style: {width: 170}}

function ArrayOfPrimitiveFormItem({field, bgColor}: {
    field: EntityEditField,
    bgColor?: string
}) {
    const form = Form.useFormInstance();
    const thisItemStyle: CSSProperties = useMemo(() => bgColor == undefined ? {} : {
        backgroundColor: bgColor
    }, [bgColor]);

    useEffect(() => {
        form.setFieldValue(field.name, field.value);
    }, [field.name, field.value, form]);

    const itemStyle = field.autoCompleteOptions != null ? autoCompleteItemStyle : empty;
    const iconSize = field.autoCompleteOptions != null ? 10 : 'default';


    return <Form.List name={field.name} key={field.name} initialValue={field.value as any[]}>
        {(fields, {add, remove, move}) => (
            <>
                {fields.map((f, index) => (
                    <Form.Item {...(index === 0 ? formItemLayout : formItemLayoutWithOutLabel)}
                               label={index === 0 ? <LabelWithTooltip field={field}/> : ''}
                               key={f.key}
                               style={thisItemStyle}>

                        <Space align='baseline' size={2}>
                            <Form.Item key={f.key} name={f.name} {...itemStyle}>
                                {primitiveControl(field, empty)}
                            </Form.Item>
                            <ActionIcon className='nodrag'
                                        size={iconSize}
                                        icon={<MinusSquareTwoTone twoToneColor='red'/>}
                                        onClick={() => remove(f.name)}
                            />
                            {index != 0 &&
                                <ActionIcon className='nodrag'
                                            size={iconSize}
                                            icon={<ArrowUpOutlined/>}
                                            onClick={() => move(index, index - 1)}/>
                            }

                            {
                                index != fields.length - 1 &&
                                <ActionIcon className='nodrag'
                                            size={iconSize}
                                            icon={<ArrowDownOutlined/>}
                                            onClick={() => move(index, index + 1)}/>
                            }

                        </Space>

                    </Form.Item>
                ))}
                <Form.Item {...(fields.length === 0 ? formItemLayout : formItemLayoutWithOutLabel)}
                           label={fields.length === 0 ? <LabelWithTooltip field={field}/> : ''}>
                    <ActionIcon className='nodrag'
                                icon={<PlusSquareTwoTone/>}
                                onClick={() => add(defaultPrimitiveValue(field))}
                    />

                </Form.Item>
            </>
        )
        }

    </Form.List>
}


function FuncSubmitFormItem({field}: {
    field: EntityEditField
}) {
    const [t] = useTranslation();
    const func = field.value as FuncSubmitType;
    return <Form.Item {...formItemLayoutWithOutLabel} key={field.name}>
        <Space size={50}>
            <HappyProvider>
                <Button className='nodrag' type="primary" htmlType="submit" onClick={() => func.funcSubmit()}>
                    {t('addOrUpdate')}
                </Button>
            </HappyProvider>
            <Button className='nodrag' type="default" onClick={() => func.funcClear()}>
                {t('setDefaultValue')}
            </Button>
        </Space>
    </Form.Item>
}


function InterfaceFormItem({field, sharedSetting}: {
    field: EntityEditField,
    sharedSetting?: EntitySharedSetting
}) {
    const form = Form.useFormInstance();
    useEffect(() => {
        form.setFieldValue(field.name, field.value);
    }, [field.name, field.value, form]);


    const onSelectChange = useCallback((value: string) => {
        field.interfaceOnChangeImpl!(value);
    }, [field]);

    const options = field.autoCompleteOptions?.options!;
    const filters = getFilter(false, options.length > 5);

    return <>
        <Form.Item name={field.name} key={field.name} label={<LabelWithTooltip field={field}/>}
                   initialValue={field.value}>
            <Select className='nodrag' options={options}
                    {...filters}
                    onChange={onSelectChange}/>
        </Form.Item>

        {FieldsFormItem(field.implFields as EntityEditField[], sharedSetting)}
    </>
}

function FieldFormItem(field: EntityEditField, sharedSetting?: EntitySharedSetting) {
    const bgColor = getFieldBackgroundColor(field, sharedSetting?.nodeShow)
    switch (field.type) {
        case "structRef":
            return <StructRefItem key={field.name} field={field} bgColor={bgColor}/>;
        case "arrayOfPrimitive":
            return <ArrayOfPrimitiveFormItem key={field.name} field={field} bgColor={bgColor}/>;
        case "primitive":
            return <PrimitiveFormItem key={field.name} field={field} bgColor={bgColor}/>;
        case "funcAdd":
            return <FuncAddFormItem key={field.name} field={field} bgColor={bgColor}/>;
        case "interface":
            return <InterfaceFormItem key={field.name} field={field} sharedSetting={sharedSetting}/>;
        case "funcSubmit":
            return <FuncSubmitFormItem key={field.name} field={field}/>;
    }
}

function FieldsFormItem(fields: EntityEditField[], sharedSetting?: EntitySharedSetting) {
    return fields.map((field) => FieldFormItem(field, sharedSetting));
}

const theme = {
    components: {
        Form: {
            itemMarginBottom: 8,
        },
    },
}
const formStyle = {backgroundColor: "white", borderRadius: 15, padding: 10}

export const EntityForm = memo(function EntityForm({edit, sharedSetting}: {
    edit: EntityEdit;
    sharedSetting?: EntitySharedSetting,
}) {
    const [_form] = Form.useForm();

    const onValuesChange = useCallback((_changedFields: any, allFields: any) => {
        edit.editOnUpdateValues(allFields);
    }, [edit]);

    return <ConfigProvider theme={theme}>
        <Form {...formLayout}
              form={_form}
              onValuesChange={onValuesChange}
              style={formStyle}>
            {FieldsFormItem(edit.editFields, sharedSetting)}
        </Form>
    </ConfigProvider>
});
