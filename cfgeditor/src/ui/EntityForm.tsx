import {EntityEditField} from "../model/graphModel.ts";
import {Button, ConfigProvider, Form, InputNumber, Select, Switch, Tooltip} from "antd";
import TextArea from "antd/es/input/TextArea";
import {CloseOutlined, MinusCircleOutlined, PlusCircleOutlined, PlusCircleTwoTone} from "@ant-design/icons";


const setOfNumber = new Set<string>(['int', 'long', 'float']);

function makePrimitiveControl(field: EntityEditField, isFromArray: boolean = false) {
    let style = isFromArray ? {style: {width: '92%'}} : {};
    let control;
    if (field.autoCompleteOptions) {
        let options = []
        for (let op of field.autoCompleteOptions) {
            options.push({label: op, value: op});
        }
        control = <Select options={options}/>
    } else if (field.eleType == 'bool') {
        control = <Switch  {...style}/>;
    } else if (setOfNumber.has(field.eleType)) {
        control = <InputNumber {...style}/>;
    } else {
        control = <TextArea {...style}/>;
    }
    return control;
}

function makePrimitiveFormItem(field: EntityEditField) {
    let props = {}
    if (field.eleType == 'bool') {
        props = {valuePropName: "checked"}
    }
    return <Form.Item name={field.name} key={field.name} label={makeLabel(field)} initialValue={field.value} {...props}>
        {makePrimitiveControl(field)}
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

function makeArrayOfPrimitiveFormItem(editField: EntityEditField) {
    return <Form.List name={editField.name} key={editField.name} initialValue={editField.value as any[]}>
        {(fields, {add, remove}) => (
            <>
                {fields.map((field, index) => (
                    <Form.Item {...(index === 0 ? formItemLayout : formItemLayoutWithOutLabel)}
                               label={index === 0 ? makeLabel(editField) : ''}
                               key={field.key}>
                        <Form.Item {...field} noStyle>
                            {makePrimitiveControl(editField, true)}
                        </Form.Item>

                        <MinusCircleOutlined className="dynamic-delete-button"
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

function makeFuncAddFormItem(field: EntityEditField) {
    let func = field.value as (() => void);
    return <Form.Item {...formItemLayout} key={field.name}
                      label={makeLabel(field)}>
        <PlusCircleTwoTone onClick={() => func()}/>
    </Form.Item>
}

function makeFuncSubmitFormItem(field: EntityEditField) {
    let func = field.value as (() => void);
    return <Form.Item {...formItemLayoutWithOutLabel} key={field.name}>
        <Button type="primary" htmlType="submit" onClick={() => func()}>
            更新
        </Button>
    </Form.Item>
}

function makeFuncDeleteFormItem(field: EntityEditField) {
    let func = field.value as (() => void);
    return <Form.Item {...formItemLayoutWithOutLabel} key={field.name}>
        <Button type="primary" danger onClick={() => func()}>
            <CloseOutlined/>
        </Button>
    </Form.Item>
}

function makeInterfaceFormItem(field: EntityEditField): any {
    let options = []
    for (let op of field.autoCompleteOptions as string[]) {
        options.push({label: op, value: op});
    }
    let implSelect = <Form.Item name={field.name} key={field.name} label={makeLabel(field)} initialValue={field.value}>
        <Select options={options}/>
    </Form.Item>;

    return [implSelect, ...makeFieldsFormItem(field.implFields as EntityEditField[])]
}

function makeFieldFormItem(field: EntityEditField) {
    switch (field.type) {
        case "arrayOfPrimitive":
            return makeArrayOfPrimitiveFormItem(field);
        case "primitive":
            return makePrimitiveFormItem(field);
        case "funcAdd":
            return makeFuncAddFormItem(field);
        case "interface":
            return makeInterfaceFormItem(field);
        case "funcSubmit":
            return makeFuncSubmitFormItem(field);
        case "funcDelete":
            return makeFuncDeleteFormItem(field);
    }
}

function makeFieldsFormItem(fields: EntityEditField[]) {
    return fields.map((field, _index) => {
        return makeFieldFormItem(field);
    });
}


export function EntityForm({fields}: {
    fields: EntityEditField[];
}) {

    if (fields.length > 0) {
        let f = fields[fields.length - 1]
        if (f.type == 'funcSubmit') {

        }
    }

    function onValuesChange(changedFields: any, allFields: any) {
        console.log(changedFields, allFields);
    }

    let form = <Form labelCol={{span: 6}} wrapperCol={{span: 18}}
                     onValuesChange={onValuesChange}
                     style={{maxWidth: 400, backgroundColor: "white", borderRadius: 15, padding: 10}}>
        {makeFieldsFormItem(fields)}
    </Form>

    return <ConfigProvider theme={{
        components: {
            Form: {
                itemMarginBottom: 8,
            },
        },
    }}>
        {form}
    </ConfigProvider>
}
