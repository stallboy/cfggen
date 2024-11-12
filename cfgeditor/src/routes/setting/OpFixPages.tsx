import {makeFixedPage, setFixedPagesConf, store, useLocationData} from "./store.ts";
import {memo, useCallback, useEffect} from "react";
import {useTranslation} from "react-i18next";
import {Button, Form, Input, Space} from "antd";
import {formItemLayoutWithOutLabel, formLayout} from "./BasicSetting.tsx";
import {CloseOutlined} from "@ant-design/icons";
import {Schema} from "../table/schemaUtil.ts";
import {STable} from "../table/schemaModel.ts";
import {FixedPagesConf} from "./storageJson.ts";
import {useForm} from "antd/es/form/Form";


function onFinishPageConf(values: any) {
    // console.log(values);
    setFixedPagesConf(values);
}

export const OpFixPages = memo(function ({schema, curTable}: {
    schema: Schema | undefined;
    curTable: STable | null;
}) {
    const {curPage} = useLocationData();
    const {t} = useTranslation();
    const {curTableId, curId} = useLocationData();
    const {pageConf} = store;
    const [form] = useForm();

    const onFixCurrentPageClick = useCallback(function () {
        const page = makeFixedPage(curTableId, curId);
        const newPageConf: FixedPagesConf = {pages: [...pageConf.pages, page]};
        setFixedPagesConf(newPageConf);
    }, [curTableId, curId, pageConf, form]);

    useEffect(() => {
        form.resetFields();
    }, [pageConf, form]);

    return <Form form={form} name="fixedPagesConf"  {...formLayout} initialValues={pageConf}
                 onFinish={onFinishPageConf}
                 autoComplete="off">
        <Form.Item label={t('pages')}>
            <Form.List name="pages">
                {(fields, {remove}) => (
                    <div style={{display: 'flex', flexDirection: 'column', rowGap: 16}}>
                        {fields.map(({key, name}) => (
                            <Space key={key}>
                                <Form.Item name={[name, 'label']} noStyle>
                                    <Input placeholder="label"/>
                                </Form.Item>
                                <Form.Item name={[name, 'table']} noStyle>
                                    <Input disabled placeholder="table"/>
                                </Form.Item>
                                <Form.Item name={[name, 'id']} noStyle>
                                    <Input disabled placeholder="id"/>
                                </Form.Item>
                                <CloseOutlined onClick={() => remove(name)}/>
                            </Space>
                        ))}
                    </div>
                )}
            </Form.List>
        </Form.Item>

        <Form.Item {...formItemLayoutWithOutLabel}>
            <Space>
                <Button type="primary" htmlType="submit">
                    {t('setFixedPagesConf')}
                </Button>
                {(schema && curTable && curPage == 'recordRef') &&
                    <Button type="primary" onClick={onFixCurrentPageClick}>
                        {t('fixCurrentPage')}
                    </Button>}
            </Space>
        </Form.Item>
    </Form>

});
