import {makeFixedPage, setFixedPagesConf, store, useLocationData} from "./store.ts";
import {memo, useEffect} from "react";
import {useTranslation} from "react-i18next";
import {Button, Form, Input, Space} from "antd";
import {formItemLayoutWithOutLabel, formLayout} from "./BasicSetting.tsx";
import {CloseOutlined} from "@ant-design/icons";
import {Schema} from "../table/schemaUtil.ts";
import {STable} from "../table/schemaModel.ts";
import {FixedPage, FixedPagesConf} from "./storageJson.ts";


function onFinishPageConf(values: any) {
    console.log(values);
    setFixedPagesConf(values);
}

export const OpFixPages = memo(function FixedPagesSetting({schema, curTable}: {
    schema: Schema | undefined;
    curTable: STable | null;
}) {
    const {curPage} = useLocationData();
    const {t} = useTranslation();
    const {curTableId, curId} = useLocationData();
    const {pageConf} = store;
    const [form] = Form.useForm();

    function onFixCurrentPageClick() {
        const page = makeFixedPage(curTableId, curId);
        const newPageConf: FixedPagesConf = {pages: [...pageConf.pages, page]};
        setFixedPagesConf(newPageConf);
        form.setFieldsValue(newPageConf);
    }

    return <Form form={form} name="fixedPagesConf"  {...formLayout} onFinish={onFinishPageConf} autoComplete="off">
        {(schema && curTable && curPage == 'recordRef') &&
            <Form.Item {...formItemLayoutWithOutLabel}>
                <Button type="primary" onClick={onFixCurrentPageClick}>
                    {t('fixCurrentPage')}
                </Button>
            </Form.Item>}

        <FixedPages pages={pageConf.pages}/>
        <Form.Item {...formItemLayoutWithOutLabel}>
            <Button type="primary" htmlType="submit">
                {t('setFixedPagesConf')}
            </Button>
        </Form.Item>
    </Form>
});

const FixedPages = memo(function FixedPages({pages}: {
    pages: FixedPage[];
}) {
    const {t} = useTranslation();
    const form = Form.useFormInstance();
    useEffect(() => {
        form.setFieldValue('pages', pages);
    }, [pages]);

    return <Form.Item label={t('pages')}>
        <Form.List name="pages" initialValue={pages}>
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
    </Form.Item>;
});
