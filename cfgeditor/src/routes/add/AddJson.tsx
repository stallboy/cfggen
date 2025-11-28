import {invalidateAllQueries, navTo, useMyStore, useLocationData} from "../../store/store.ts";

import {memo, useCallback, useState} from "react";
import {Schema} from "../table/schemaUtil.tsx";
import {useMutation,} from "@tanstack/react-query";
import {addOrUpdateRecord} from "../api.ts";
import {Button, Typography, Form, Input, List, Result, Space} from "antd";

import {RecordEditResult} from "../record/recordModel.ts";
import {useNavigate} from "react-router-dom";
import {useTranslation} from "react-i18next";
import {ResultStatusType} from "antd/es/result";
import {applyNewEditingObject} from "../record/editingObject.ts";

const {Title} = Typography;

interface AddJsonProps {
    json: string;
}

export const AddJson = memo(function AddJson({schema}: {
    schema: Schema | undefined;
}) {
    const {t} = useTranslation();
    const {server} = useMyStore();
    const navigate = useNavigate();
    const {curTableId} = useLocationData();
    let editable = false;
    if (schema && schema.isEditable) {
        const sTable = schema.getSTable(curTableId);
        if (sTable) {
            editable = true;
        }
    }

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
        applyNewEditingObject(JSON.parse(json));
    }, [form]);


    const onAddJson = useCallback((addJsonProps: AddJsonProps) => {
        addOrUpdateRecordMutation.mutate(addJsonProps.json)
    }, [addOrUpdateRecordMutation]);


    if (!editable) {
        return <Result title={'not editable'}/>;
    }

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
                        {t('show')}
                    </Button>
                </Space>
            </Form.Item>
        </Form>

        {res}
    </>
        ;
});


