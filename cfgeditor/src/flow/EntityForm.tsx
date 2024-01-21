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
import {MinusSquareTwoTone, PlusSquareTwoTone} from "@ant-design/icons";
import {memo, useRef} from "react";
import {useTranslation} from "react-i18next";
import {DefaultOptionType} from "antd/es/select";
import {Handle, Position} from "reactflow";
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

function PrimitiveControl(field: EntityEditField) {
    let control;
    const {eleType, autoCompleteOptions} = field;
    if (autoCompleteOptions && autoCompleteOptions.options.length > 0) {
        let filterSorts = {};
        if (autoCompleteOptions.isValueInteger) {
            filterSorts = {
                filterSort: (optionA: DefaultOptionType, optionB: DefaultOptionType) =>
                    parseInt(optionA.value as string) - parseInt(optionB.value as string)
            };
        }
        control = <AutoComplete className='nodrag' options={autoCompleteOptions.options}
                                {...filterSorts}
                                filterOption={(inputValue, option) =>
                                    option!.value.toUpperCase().includes(inputValue.toUpperCase())
                                }/>
    } else if (eleType == 'bool') {
        control = <Switch className='nodrag'/>;
    } else if (setOfNumber.has(eleType)) {
        control = <InputNumber className='nodrag'/>;
    } else {
        control = <TextArea className='nodrag' autoSize={{minRows: 1, maxRows: 10}}/>;
    }
    return control;
}

function StructRefItem(field: EntityEditField) {
    return <Flex key={field.name} gap={'middle'} justify="flex-end" style={{marginBottom: 10, position: 'relative'}}>
        <Tag color={'blue'}>{makeLabel(field)}</Tag>
        {field.handleOut && <Handle type='source' position={Position.Right} id={field.name}
                                    style={{
                                        position: 'absolute',
                                        left: '340px',
                                        backgroundColor: 'blue'
                                    }}/>}
    </Flex>
}


function FuncAddFormItem(field: EntityEditField) {
    let func = field.value as FuncType;
    return <Flex key={field.name} gap={'middle'} justify="flex-end" style={{marginBottom: 10, position: 'relative'}}>
        <Button onClick={func} icon={<PlusSquareTwoTone/>}> {makeLabel(field)} </Button>
        {field.handleOut && <Handle type='source' position={Position.Right} id={field.name}
                                    style={{
                                        position: 'absolute',
                                        left: '340px',
                                        backgroundColor: 'blue'
                                    }}/>}
    </Flex>;
}

function PrimitiveFormItem(field: EntityEditField) {
    let props = {}
    if (field.eleType == 'bool') {
        props = {valuePropName: "checked"}
    }

    if (field.value.toString().startsWith('帮助')) {
        console.log('ff', field.value);
    }
    return <Form.Item name={field.name} key={field.name} label={makeLabel(field)} initialValue={field.value} {...props}>
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

function ArrayOfPrimitiveFormItem(editField: EntityEditField) {
    const {t} = useTranslation();
    return <Form.List name={editField.name} key={editField.name} initialValue={editField.value as any[]}>
        {(fields, {add, remove}) => (
            <>
                {fields.map((field, index) => (
                    <Form.Item {...(index === 0 ? formItemLayout : formItemLayoutWithOutLabel)}
                               label={index === 0 ? makeLabel(editField) : ''}
                               key={field.key}>

                        <Space>
                            <Form.Item {...field} >
                                {PrimitiveControl(editField)}
                            </Form.Item>
                            <ActionIcon title={t('delete')}
                                        icon={<MinusSquareTwoTone twoToneColor='red'/>}
                                        onClick={() => remove(field.name)}
                            />

                        </Space>

                    </Form.Item>
                ))}
                <Form.Item {...(fields.length === 0 ? formItemLayout : formItemLayoutWithOutLabel)}
                           label={fields.length === 0 ? makeLabel(editField) : ''}>
                    <Button onClick={add} icon={<PlusSquareTwoTone/>}> {editField.name} </Button>

                </Form.Item>
            </>
        )}
    </Form.List>
}


function FuncSubmitFormItem(field: EntityEditField) {
    const [t] = useTranslation();
    let func = field.value as FuncSubmitType;
    return <Form.Item {...formItemLayoutWithOutLabel} key={field.name}>
        <Space size={50}>
            <Button type="primary" htmlType="submit" onClick={() => func.funcSubmit()}>
                {t('addOrUpdate')}
            </Button>
            <Button type="default" onClick={() => func.funcClear()}>
                {t('setDefaultValue')}
            </Button>
        </Space>
    </Form.Item>
}

function FuncDeleteFormItem(field: EntityEditField) {
    const {t} = useTranslation();
    let func = field.value as FuncType;
    return <Form.Item {...formItemLayoutWithOutLabel} key={field.name}>
        <ActionIcon title={t('delete')}
                    icon={<MinusSquareTwoTone twoToneColor='red'/>}
                    onClick={func}
        />
    </Form.Item>
}

function InterfaceFormItem(field: EntityEditField): any {
    let options = field.autoCompleteOptions?.options as EntityEditFieldOption[]

    let implSelect = <Form.Item name={field.name} key={field.name} label={makeLabel(field)} initialValue={field.value}>
        <Select showSearch options={options} filterOption={(inputValue, option) =>
            option!.value.toUpperCase().includes(inputValue.toUpperCase())
        } onChange={(value, _) => {
            field.interfaceOnChangeImpl!!(value);
        }}/>
    </Form.Item>;

    return [implSelect, ...FieldsFormItem(field.implFields as EntityEditField[])]
}

function FieldFormItem(field: EntityEditField) {
    switch (field.type) {
        case "structRef":
            return StructRefItem(field);
        case "arrayOfPrimitive":
            return ArrayOfPrimitiveFormItem(field);
        case "primitive":
            return PrimitiveFormItem(field);
        case "funcAdd":
            return FuncAddFormItem(field);
        case "interface":
            return InterfaceFormItem(field);
        case "funcSubmit":
            return FuncSubmitFormItem(field);
        case "funcDelete":
            return FuncDeleteFormItem(field);
    }
}

function FieldsFormItem(fields: EntityEditField[]) {
    return fields.map((field, _index) => {
        return FieldFormItem(field);
    });
}


export const EntityForm = memo(function EntityForm({edit}: {
    edit: EntityEdit;
}) {
    const ref = useRef<HTMLDivElement>(null);

    function onValuesChange(_changedFields: any, allFields: any) {
        edit.editOnUpdateValues(allFields);
    }

    let form = <Form {...formLayout}
                     onValuesChange={onValuesChange}
                     style={{maxWidth: 600, backgroundColor: "white", borderRadius: 15, padding: 10}}>
        {FieldsFormItem(edit.editFields)}
    </Form>

    return <ConfigProvider theme={{
        components: {
            Form: {
                itemMarginBottom: 8,
            },
        },
    }}>
        <div ref={ref}> {form} </div>

    </ConfigProvider>
});
