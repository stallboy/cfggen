import {EntityEditField, FuncSubmitType, FuncType} from "../model/entityModel.ts";
import {AutoComplete, Button, ConfigProvider, Form, InputNumber, Select, Space, Switch, Tooltip} from "antd";
import TextArea from "antd/es/input/TextArea";
import {
    ClearOutlined,
    CloseOutlined,
    MinusCircleOutlined,
    PlusCircleOutlined,
    PlusCircleTwoTone
} from "@ant-design/icons";
import {Drag} from "rete-react-plugin";
import {useRef} from "react";
import {useTranslation} from "react-i18next";

const formItemLayout = {
    labelCol: {
        xs: {span: 24},
        sm: {span: 6},
    },
    wrapperCol: {
        xs: {span: 24},
        sm: {span: 18},
    },
};

const formItemLayoutWithOutLabel = {
    wrapperCol: {
        xs: {span: 24, offset: 0},
        sm: {span: 18, offset: 6},
    },
};

const setOfNumber = new Set<string>(['int', 'long', 'float']);

function PrimitiveControl(field: EntityEditField, isFromArray: boolean = false) {
    let style = isFromArray ? {style: {width: '88%'}} : {};
    let control;
    const {eleType, autoCompleteOptions} = field;
    if (autoCompleteOptions && autoCompleteOptions.length > 0) {
        let options = []
        for (let op of autoCompleteOptions) {
            options.push({label: op, value: op});
        }
        control = <AutoComplete options={options} filterOption={(inputValue, option) =>
            option!.value.toUpperCase().includes(inputValue.toUpperCase())
        }/>
    } else if (eleType == 'bool') {
        control = <Switch  {...style}/>;
    } else if (setOfNumber.has(eleType)) {
        control = <InputNumber {...style} />;
    } else {
        control = <TextArea autoSize={true} {...style}/>;
    }
    return control;
}

function PrimitiveFormItem(field: EntityEditField) {
    let props = {}
    if (field.eleType == 'bool') {
        props = {valuePropName: "checked"}
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
    return <Form.List name={editField.name} key={editField.name} initialValue={editField.value as any[]}>
        {(fields, {add, remove}) => (
            <>
                {fields.map((field, index) => (
                    <Form.Item {...(index === 0 ? formItemLayout : formItemLayoutWithOutLabel)}
                               label={index === 0 ? makeLabel(editField) : ''}
                               key={field.key}>
                        <Form.Item {...field} noStyle>
                            {PrimitiveControl(editField, true)}
                        </Form.Item>

                        <MinusCircleOutlined key={`delete-${field.key}`}
                                             className="dynamic-delete-button"
                                             onClick={() => remove(field.name)}/>

                    </Form.Item>
                ))}
                <Form.Item {...(fields.length === 0 ? formItemLayout : formItemLayoutWithOutLabel)}
                           label={fields.length === 0 ? makeLabel(editField) : ''}>
                    <PlusCircleOutlined onClick={() => add()}/>
                </Form.Item>
            </>
        )}
    </Form.List>
}

function FuncAddFormItem(field: EntityEditField) {
    let func = field.value as FuncType;
    return <Form.Item {...formItemLayout} key={field.name}
                      label={makeLabel(field)}>
        <PlusCircleTwoTone onClick={() => func()}/>
    </Form.Item>
}

function FuncSubmitFormItem(field: EntityEditField) {
    const [t] = useTranslation();
    let func = field.value as FuncSubmitType;
    return <Form.Item {...formItemLayoutWithOutLabel} key={field.name}>
        <Space size={50}>
            <Button type="primary" htmlType="submit" onClick={() => func.funcSubmit()}>
                {t('addOrUpdate')}
            </Button>
            <ClearOutlined onClick={() => func.funcClear()}/>
        </Space>
    </Form.Item>
}

function FuncDeleteFormItem(field: EntityEditField) {
    let func = field.value as FuncType;
    return <Form.Item {...formItemLayoutWithOutLabel} key={field.name}>
        <Button type="primary" danger onClick={() => func()}>
            <CloseOutlined/>
        </Button>
    </Form.Item>
}

function InterfaceFormItem(field: EntityEditField): any {
    let options = []
    for (let op of field.autoCompleteOptions as string[]) {
        options.push({label: op, value: op});
    }
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


export function EntityForm({fields, onUpdateValues}: {
    fields: EntityEditField[];
    onUpdateValues: (values: any) => void;
}) {
    const ref = useRef<HTMLDivElement>(null);
    Drag.useNoDrag(ref)

    function onValuesChange(_changedFields: any, allFields: any) {
        onUpdateValues(allFields);
    }

    let form = <Form labelCol={{span: 6}} wrapperCol={{span: 18}}
                     onValuesChange={onValuesChange}
                     style={{maxWidth: 400, backgroundColor: "white", borderRadius: 15, padding: 10}}>
        {FieldsFormItem(fields)}
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
}
