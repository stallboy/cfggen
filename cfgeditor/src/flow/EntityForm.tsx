import {EntityEdit, EntityEditField, EntityEditFieldOption, FuncSubmitType, FuncType} from "./entityModel.ts";
import {
    AutoComplete,
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
import {CSSProperties, memo, useCallback, useEffect} from "react";
import {useTranslation} from "react-i18next";
import {Handle, Position} from "@xyflow/react";
import {ActionIcon} from "@ant-design/pro-editor";

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
    return (!!option) && (option.value.toString().includes(inputValue) || option.label.includes(inputValue));
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

function PrimitiveControl(field: EntityEditField) {
    let control;
    const {eleType, autoCompleteOptions} = field;
    if (autoCompleteOptions && autoCompleteOptions.options.length > 0) {
        const {options, isValueInteger, isEnum} = autoCompleteOptions;

        let filters: any = {}

        if (isValueInteger) {
            filters.filterSort = filterNumberSort;
        }
        if (!isEnum || (options.length > 5)) {
            filters.showSearch = true;
            filters.filterOption = filterOption;
        }
        if (isEnum) {
            control = <Select className='nodrag' options={options} {...filters}/>
        } else {
            control = <AutoComplete className='nodrag' options={options} {...filters} style={{width: 100}}/>
        }

    } else if (eleType == 'bool') {
        control = <Switch className='nodrag'/>;
    } else if (setOfNumber.has(eleType)) {
        control = <InputNumber className='nodrag'/>;
    } else {
        control = <TextArea className='nodrag' autoSize={{minRows: 1, maxRows: 10}}/>;
    }
    return control;
}

const rowFlexStyle: CSSProperties = {marginBottom: 10, position: 'relative'}
const handleOutStyle: CSSProperties = {position: 'absolute', left: '270px', backgroundColor: 'blue'}

function StructRefItem({field}: { field: EntityEditField }) {
    return <Flex key={field.name} gap='middle' justify="flex-end" style={rowFlexStyle}>
        <Tag color={'blue'}>{field.name}</Tag>
        {field.handleOut && <Handle type='source' position={Position.Right} id={field.name}
                                    style={handleOutStyle}/>}
    </Flex>
}

function FuncAddFormItem({field}: { field: EntityEditField }) {
    let func = field.value as FuncType;
    return <Flex key={field.name} gap='middle' justify="flex-end" style={rowFlexStyle}>
        <Button className='nodrag' onClick={func} icon={<PlusSquareTwoTone/>}> {field.name} </Button>
        {field.handleOut && <Handle type='source' position={Position.Right} id={field.name}
                                    style={handleOutStyle}/>}
    </Flex>;
}

function PrimitiveFormItem({field}: { field: EntityEditField }) {
    const form = Form.useFormInstance();
    useEffect(() => {
        form.setFieldValue(field.name, field.value);
    }, [field.value]);


    let props = {}
    if (field.eleType == 'bool') {
        props = {valuePropName: "checked"}
    }

    return <Form.Item name={field.name} key={field.name} label={makeLabel(field)}
                      initialValue={field.value} {...props}>
        {PrimitiveControl(field)}
    </Form.Item>;
}

function makeLabel(field: EntityEditField) {
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

function ArrayOfPrimitiveFormItem({field}: { field: EntityEditField }) {
    const form = Form.useFormInstance();
    useEffect(() => {
        form.setFieldValue(field.name, field.value);
    }, [field.value]);

    return <Form.List name={field.name} key={field.name} initialValue={field.value as any[]}>
        {(fields, {add, remove, move}) => (
            <>
                {fields.map((f, index) => (
                    <Form.Item {...(index === 0 ? formItemLayout : formItemLayoutWithOutLabel)}
                               label={index === 0 ? makeLabel(field) : ''}
                               key={f.key}>

                        <Space align='baseline' size={1}>
                            <Form.Item {...f} >
                                {PrimitiveControl(field)}
                            </Form.Item>
                            <ActionIcon className='nodrag'
                                        icon={<MinusSquareTwoTone twoToneColor='red'/>}
                                        onClick={() => remove(f.name)}
                            />
                            {index != 0 &&
                                <ActionIcon className='nodrag'
                                            icon={<ArrowUpOutlined/>}
                                            onClick={() => move(index, index - 1)}/>
                            }

                            {index != fields.length - 1 &&
                                <ActionIcon className='nodrag'
                                            icon={<ArrowDownOutlined/>}
                                            onClick={() => move(index, index + 1)}/>
                            }

                        </Space>

                    </Form.Item>
                ))}
                <Form.Item {...(fields.length === 0 ? formItemLayout : formItemLayoutWithOutLabel)}
                           label={fields.length === 0 ? makeLabel(field) : ''}>
                    <ActionIcon className='nodrag'
                                icon={<PlusSquareTwoTone/>}
                                onClick={() => add(defaultPrimitiveValue(field))}
                    />

                </Form.Item>
            </>
        )}
    </Form.List>
}


function FuncSubmitFormItem({field}: { field: EntityEditField }) {
    const [t] = useTranslation();
    let func = field.value as FuncSubmitType;
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
}

function InterfaceFormItem({field}: { field: EntityEditField }): any {
    const form = Form.useFormInstance();
    useEffect(() => {
        form.setFieldValue(field.name, field.value);
    }, [field.value]);

    let options = field.autoCompleteOptions?.options;
    let implSelect = <Form.Item name={field.name} key={field.name} label={makeLabel(field)}
                                initialValue={field.value}>
        <Select className='nodrag' options={options}
                filterOption={filterOption}
                onChange={(value, _) => {
                    field.interfaceOnChangeImpl!!(value);
                }}/>

    </Form.Item>;

    return <>
        {implSelect}
        {FieldsFormItem(field.implFields as EntityEditField[])}
    </>
}

function FieldFormItem(field: EntityEditField) {
    switch (field.type) {
        case "structRef":
            return <StructRefItem key={field.name} field={field}/>;
        case "arrayOfPrimitive":
            return <ArrayOfPrimitiveFormItem key={field.name} field={field}/>;
        case "primitive":
            return <PrimitiveFormItem key={field.name} field={field}/>;
        case "funcAdd":
            return <FuncAddFormItem key={field.name} field={field}/>;
        case "interface":
            return <InterfaceFormItem key={field.name} field={field}/>;
        case "funcSubmit":
            return <FuncSubmitFormItem key={field.name} field={field}/>;
    }
}

function FieldsFormItem(fields: EntityEditField[]) {
    return fields.map((field, _index) => FieldFormItem(field));
}

const theme = {
    components: {
        Form: {
            itemMarginBottom: 8,
        },
    },
}
const formStyle = {backgroundColor: "white", borderRadius: 15, padding: 10}

export const EntityForm = memo(function EntityForm({edit}: {
    edit: EntityEdit;
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
            {FieldsFormItem(edit.editFields)}
        </Form>
    </ConfigProvider>
});
