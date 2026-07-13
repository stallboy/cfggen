import {navTo, useMyStore, useLocationData} from "@/store/store";
import {invalidateAllQueries} from "@/queryClient";

import {memo, useCallback, useState} from "react";
import {useMutation,} from "@tanstack/react-query";
import {addOrUpdateRecord} from "@/api/api";
import {Button, Typography, Form, Input, List, Result, Space} from "antd";
import type {ResultProps} from "antd";

import {RecordEditResult} from "@/api/recordModel";
import {useNavigate} from "react-router";
import {useTranslation} from "react-i18next";
import {getCurrentEditingSession} from "@/services/editingSession";

const {Title} = Typography;
// antd 最佳实践：用根导出的 ResultProps 派生类型，避免深路径 antd/es/*
type ResultStatusType = ResultProps['status'];

interface AddJsonProps {
    json: string;
}

export const AddJson = memo(function AddJson() {
    const {t} = useTranslation();
    const {server} = useMyStore();
    const navigate = useNavigate();
    const {curTableId} = useLocationData();

    const [result, setResult] = useState<RecordEditResult | Error | undefined>();

    const addOrUpdateRecordMutation = useMutation<RecordEditResult, Error, string>({
        mutationFn: (json: string) =>
            addOrUpdateRecord(server, curTableId, JSON.parse(json)),


        onError: (error) => {
            setResult(error);
        },
        onSuccess: (editResult) => {
            setResult(editResult);
            if (editResult.resultCode == 'updateOk' || editResult.resultCode == 'addOk') {
                invalidateAllQueries();
                navigate(navTo('record', editResult.table, editResult.id, true));
            }
        },
    });

    const [form] = Form.useForm();

    const onShow = useCallback(() => {
        const json = form.getFieldValue('json')
        getCurrentEditingSession()?.replaceEditingObject(JSON.parse(json));
    }, [form]);


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


