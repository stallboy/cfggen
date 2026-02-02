import {makeFixedPage, makeUnrefPage, setFixedPagesConf, useMyStore, useLocationData, isFixedRefPage} from "../../store/store.ts";
import {memo, useCallback, useEffect} from "react";
import {useTranslation} from "react-i18next";
import {Button, Form, Input, Space} from "antd";
import {CloseOutlined} from "@ant-design/icons";
import {Schema} from "../table/schemaUtil.tsx";
import {STable} from "../../api/schemaModel.ts";
import {FixedPage, FixedPagesConf} from "../../store/storageJson.ts";
import {useForm} from "antd/es/form/Form";

// OnePage使用自己的Union类型定义，用于表单显示
interface OneRefPage {
    label: string;
    table: string;
    id: string;
}

interface OneUnrefPage {
    label: string;
    table: string;
}

type OnePage = OneRefPage | OneUnrefPage;

// 类型守卫
function isOneRefPage(page: OnePage): page is OneRefPage {
    return 'id' in page;
}

export const FixPages = memo(function ({schema, curTable}: {
    schema: Schema | undefined;
    curTable: STable | null;
}) {
    const {t} = useTranslation();
    const {curPage, curTableId, curId} = useLocationData();
    const {pageConf} = useMyStore();
    const [form] = useForm();

    const onFixCurrentPageClick = useCallback(function () {
        // 根据当前页面类型创建不同的fixed page
        if (curPage === 'recordUnref') {
            const page = makeUnrefPage(curTableId);
            const newPageConf: FixedPagesConf = {pages: [...pageConf.pages, page]};
            setFixedPagesConf(newPageConf);
        } else {
            const page = makeFixedPage(curTableId, curId);
            const newPageConf: FixedPagesConf = {pages: [...pageConf.pages, page]};
            setFixedPagesConf(newPageConf);
        }
    }, [curTableId, curId, pageConf, curPage]);

    // 将FixedPage映射为OnePage（表单使用）
    const pages: OnePage[] = pageConf.pages.map(p => {
        if (isFixedRefPage(p)) {
            return {label: p.label, table: p.table, id: p.id};
        } else {
            return {label: p.label, table: p.table};
        }
    });

    const SetPages = function (values: { pages: OnePage[] }) {
        const newPages: FixedPage[] = values.pages.map((formPage, index) => {
            // 使用索引位置来匹配原始页面，而不是label
            // 这样修改label后仍然能找到对应的原始页面
            const originalPage = pageConf.pages[index];
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

    // 判断当前是否为未引用记录页面
    const isUnrefPage = curPage === 'recordUnref';

    return <Form form={form} name="fixedPagesConf"
                 onFinish={SetPages} layout={"vertical"}
                 autoComplete="off">
        <Form.Item label={t('pages')}>
            <Form.List name="pages">
                {(fields, {remove}) => (
                    <div style={{display: 'flex', flexDirection: 'column', rowGap: 16}}>
                        {fields.map(({key, name}) => {
                            const page = pages[name];
                            const isUnref = page && !isOneRefPage(page);

                            return (
                                <Space key={key}>
                                    <Form.Item name={[name, 'label']} noStyle>
                                        <Input placeholder="label"/>
                                    </Form.Item>
                                    <Form.Item name={[name, 'table']} noStyle>
                                        <Input disabled placeholder="table"/>
                                    </Form.Item>
                                    {!isUnref && (
                                        <Form.Item name={[name, 'id']} noStyle>
                                            <Input disabled placeholder="id"/>
                                        </Form.Item>
                                    )}
                                    <CloseOutlined onClick={() => remove(name)}/>
                                </Space>
                            );
                        })}
                    </div>
                )}
            </Form.List>
        </Form.Item>

        <Form.Item>
            <Space>
                <Button type="primary" htmlType="submit">
                    {t('setFixedPagesConf')}
                </Button>
                {/* 按钮显示逻辑：支持recordRef和recordUnref类型 */}
                {(schema && curTable && (curPage == 'recordRef' || isUnrefPage)) &&
                    <Button type="primary" onClick={onFixCurrentPageClick}>
                        {t('fixCurrentPage')}
                    </Button>}
            </Space>
        </Form.Item>
    </Form>

});
