import {navTo, useMyStore, useLocationData} from "@/store/store.ts";
import {invalidateAllExceptLayout} from "@/services/queryClient.ts";

import {memo, useCallback, useState} from "react";
import {useMutation,} from "@tanstack/react-query";
import {addOrUpdateRecord, checkJson} from "@/api/apiClient.ts";
import {Button, Typography, Form, Input, List, Result, Space} from "antd";
import type {ResultProps} from "antd";

import {RecordEditResult} from "@/api/recordModel.ts";
import {useNavigate} from "react-router";
import {useTranslation} from "react-i18next";
import {getCurrentEditingSession, isDeeplyEqual} from "@/services/editingSession.ts";
import {Schema} from "@/domain/schema.ts";
import {parseCheckedEditorRecord} from "@/domain/editorValueEncoding.ts";

const {Title} = Typography;
// antd 最佳实践：用根导出的 ResultProps 派生类型，避免深路径 antd/es/*
type ResultStatusType = ResultProps['status'];

interface AddJsonProps {
    json: string;
}

export const AddJson = memo(function AddJson({schema}: {schema: Schema}) {
    const {t} = useTranslation();
    const {server} = useMyStore();
    const navigate = useNavigate();
    const {curTableId} = useLocationData();

    const [result, setResult] = useState<RecordEditResult | Error | undefined>();
    const validateJson = useCallback(async (raw: string) => {
        const checked = await checkJson(server, curTableId, raw);
        return parseCheckedEditorRecord(schema, curTableId, checked);
    }, [server, curTableId, schema]);

    const addOrUpdateRecordMutation = useMutation<RecordEditResult, Error, string>({
        mutationFn: async (json: string) =>
            addOrUpdateRecord(server, curTableId, await validateJson(json)),

        onMutate: () => {
            // 发起新请求前清掉上次的陈旧结果，避免飞行中/下次打开面板时还展示旧结果
            setResult(undefined);
        },
        onError: (error) => {
            setResult(error);
        },
        onSuccess: (editResult) => {
            setResult(editResult);
            if (editResult.resultCode == 'updateOk' || editResult.resultCode == 'addOk') {
                invalidateAllExceptLayout();   // 排除 layout：避免用重渲前旧闭包 refetch 出陈旧布局（同 Record.tsx，见 helper 注释 + cfgeditor-layout-key-track-structure）
                navigate(navTo('record', editResult.table, editResult.id, true));
            }
        },
    });

    const [form] = Form.useForm();

    const onShow = useCallback(async () => {
        const session = getCurrentEditingSession();
        if (!session) return;
        setResult(undefined);
        const before = structuredClone(session.getEditingObject());
        const json = form.getFieldValue('json')
        try {
            const checked = await validateJson(json);
            if (getCurrentEditingSession() !== session || !isDeeplyEqual(before, session.getEditingObject())) {
                throw new Error('Record changed while validating JSON; load it again to replace the current data');
            }
            session.replaceEditingObject(checked);
        } catch (e) {
            // 文本为空或 JSON 非法时给用户可见反馈，而不是让异常在事件处理器里静默抛出
            setResult(e as Error);
        }
    }, [form, validateJson]);


    const onAddJson = useCallback((addJsonProps: AddJsonProps) => {
        addOrUpdateRecordMutation.mutate(addJsonProps.json)
    }, [addOrUpdateRecordMutation]);


    let res = <></>
    if (result != undefined) {

        if ('message' in result) {
            res = <Result status="error" title={result.name} subTitle={result.message}/>
        } else {
            let status: ResultStatusType = "error";
            let extra;
            if (result.resultCode == 'updateOk' || result.resultCode == 'addOk') {
                status = "success";
            } else {
                extra = <List
                    dataSource={result.valueErrs}
                    renderItem={(err) => (
                        <List.Item>
                            <p>{err}</p>
                        </List.Item>
                    )}/>

            }
            res = <Result status={status}
                          title={result.resultCode}
                          subTitle={result.table + "," + result.id}
                          extra={extra}
            />

        }
    }

    return <>
        <Title level={4} style={{marginTop: -4}}>{curTableId}</Title>
        <Form name="addJson" form={form} layout={"vertical"} onFinish={onAddJson} autoComplete="off">
            <Form.Item name='json' label={t('json')}>
                <Input.TextArea placeholder="json" autoSize={{minRows: 8, maxRows: 20}}/>
            </Form.Item>
            <Form.Item wrapperCol={{offset: 4, span: 20}}>
                <Space>
                    <Button type="primary" htmlType="submit">
                        {t('addJson')}
                    </Button>

                    <Button type="primary" onClick={onShow}>
                        {t('loadIntoForm')}
                    </Button>
                </Space>
            </Form.Item>
        </Form>

        {res}
    </>
        ;
});


