import {makeFixedPage, makeUnrefPage, setFixedPagesConf, useMyStore, useLocationData, isFixedRefPage} from "@/store/store.ts";
import {memo, useCallback, useEffect, useMemo} from "react";
import {useTranslation} from "react-i18next";
import {Button, Form, Input, Space} from "antd";
import {CloseOutlined} from "@ant-design/icons";
import {Schema} from "@/domain/schema.ts";
import {STable} from "@/api/schemaModel.ts";
import {FixedPage, FixedPagesConf} from "@/domain/storageJson.ts";

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
    const [form] = Form.useForm();

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
    // 用useMemo缓存，避免每次渲染生成新数组导致下面的同步effect反复重置表单、冲掉未提交编辑
    const pages: OnePage[] = useMemo(() => pageConf.pages.map(p => {
        if (isFixedRefPage(p)) {
            return {label: p.label, table: p.table, id: p.id};
        } else {
            return {label: p.label, table: p.table};
        }
    }), [pageConf]);

    const SetPages = function (values: { pages: OnePage[] }) {
        // 按 (table, id) 标识找回原始页面（unref页无id，用table+类型区分），
        // 不能按索引回配——Form.List删除行后表单索引与pageConf.pages索引会错位。
        // 同 (table, id) 重复出现时按出现顺序依次消费匹配。
        const remaining = [...pageConf.pages];
        const newPages: FixedPage[] = [];
        for (const formPage of values.pages) {
            let index: number;
            if (isOneRefPage(formPage)) {
                index = remaining.findIndex(p => isFixedRefPage(p) && p.table === formPage.table && p.id === formPage.id);
            } else {
                index = remaining.findIndex(p => !isFixedRefPage(p) && p.table === formPage.table);
            }
            if (index >= 0) {
                const originalPage = remaining[index];
                remaining.splice(index, 1);
                newPages.push({
                    ...originalPage,
                    label: formPage.label,
                });
            }
        }

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
