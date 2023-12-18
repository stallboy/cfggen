import {EntityEditField} from "../model/graphModel.ts";
import {ConfigProvider, Form, InputNumber, Space, Switch} from "antd";
import TextArea from "antd/es/input/TextArea";


const setOfNumber = new Set<string>(['int', 'long', 'float']);

function makePrimitiveItem(field: EntityEditField) {
    let control;
    if (field.eleType == 'bool') {
        control = <Switch defaultChecked={field.value as boolean}/>;
    } else if (setOfNumber.has(field.eleType as string)) {
        control = <InputNumber defaultValue={field.value as number}/>;
    } else {
        control = <TextArea defaultValue={field.value as string}/>;
    }

    return <Form.Item name={field.name} key={field.name} label={field.name} tooltip={field.comment}>
        {control}
    </Form.Item>;
}

export function EntityForm({fields}: {
    fields: EntityEditField[];
}) {
    let form = <Form labelCol={{span: 8}} wrapperCol={{span: 16}}
                     layout="horizontal" style={{maxWidth: 400}}>
        {fields.map((field, _index) => {
            switch (field.type) {
                case "arrayOfPrimitive":
                    break;
                case "primitive":
                    return makePrimitiveItem(field);
                case "func":
                    break;
                case "interface":
                    break;

            }

            return <Space key={field.name}/>
        })}
    </Form>


    return <ConfigProvider
        theme={{
            components: {
                Form: {
                    itemMarginBottom: 8,
                },
            },
        }}>
        {form}
    </ConfigProvider>
}
