import {makeFixedPage, setFixedPagesConf, useMyStore, useLocationData} from "../../store/store.ts";
import {memo, useCallback, useEffect} from "react";
import {useTranslation} from "react-i18next";
import {Button, Form, Input, Space} from "antd";
import {CloseOutlined} from "@ant-design/icons";
import {Schema} from "../table/schemaUtil.tsx";
import {STable} from "../../api/schemaModel.ts";
import {FixedPage, FixedPagesConf} from "../../store/storageJson.ts";
import {useForm} from "antd/es/form/Form";

interface OnePage {
    label: string;
    table: string;
    id: string;
}

export const FixPages = memo(function ({schema, curTable}: {
    schema: Schema | undefined;
    curTable: STable | null;
}) {
    const {curPage} = useLocationData();
    const {t} = useTranslation();
    const {curTableId, curId} = useLocationData();
    const {pageConf} = useMyStore();
    const [form] = useForm();

    const onFixCurrentPageClick = useCallback(function () {
        const page = makeFixedPage(curTableId, curId);
        const newPageConf: FixedPagesConf = {pages: [...pageConf.pages, page]};
        setFixedPagesConf(newPageConf);
    }, [curTableId, curId, pageConf]);


    const pages: OnePage[] = pageConf.pages.map((p) => {
        return {label: p.label, table: p.table, id: p.id};
    });

    const SetPages = function (values: { pages: OnePage[] }) {
        const newPages: FixedPage[] = values.pages.map(formPage => {
            const originalPage = pageConf.pages.find(p => p.id === formPage.id && p.table === formPage.table);
            // The originalPage should always be found because the form is populated from pageConf.pages
            if (originalPage) {
                return {
                    ...originalPage,
                    label: formPage.label,
                };
            }
            return null;
        }).filter((p): p is FixedPage => p !== null);

        // 处理重复的 label，只保留最后一个
        const uniquePages = new Map<string, FixedPage>();
        newPages.forEach(page => {
            uniquePages.set(page.label, page);
        });

        setFixedPagesConf({pages: Array.from(uniquePages.values())});
    }

    useEffect(() => {
        form.setFieldsValue({ pages });
    }, [pages, form]);

    return <Form form={form} name="fixedPagesConf"
                 onFinish={SetPages} layout={"vertical"}
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

        <Form.Item>
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
