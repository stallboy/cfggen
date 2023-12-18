import {EntityEditField} from "../model/graphModel.ts";
import {Empty, Form} from "antd";

export function EntityForm({fields}: {
    fields: EntityEditField[];
}) {
    return <Form labelCol={{span: 4}} wrapperCol={{span: 14}}
                 layout="horizontal" style={{maxWidth: 400}}>
        {fields.map((_field, _index) => {

            return <Empty/>
        })}
    </Form>
}
