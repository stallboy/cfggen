import {invalidateAllQueries, navTo, store, useLocationData} from "../setting/store.ts";

import {memo, useCallback, useState} from "react";
import {Schema} from "../table/schemaUtil.ts";
import {useMutation,} from "@tanstack/react-query";
import {addOrUpdateRecord} from "../api.ts";
import {Button, Card, Form, Input, List, Result} from "antd";

import {RecordEditResult} from "../record/recordModel.ts";
import {useNavigate} from "react-router-dom";
import {formLayout} from "../setting/BasicSetting.tsx";
import {useTranslation} from "react-i18next";
import {ResultStatusType} from "antd/es/result";
import {applyNewEditingObject} from "../record/editingObject.ts";

interface AddJsonProps {
    table: string;
    json: string;
}

export const AddJson = memo(function AddJson({schema}: {
    schema: Schema | undefined;
}) {
    const {t} = useTranslation();
    const {server} = store;
    const navigate = useNavigate();
    const {curTableId} = useLocationData();
    let editable = false;
    if (schema && schema.isEditable) {
        const sTable = schema.getSTable(curTableId);
        if (sTable && sTable.isEditable) {
            editable = true;
        }
    }

    const [result, setResult] = useState<RecordEditResult | Error | undefined>();

    const addOrUpdateRecordMutation = useMutation<RecordEditResult, Error, AddJsonProps>({
        mutationFn: (addJsonProps: AddJsonProps) =>
            addOrUpdateRecord(server, addJsonProps.table, JSON.parse(addJsonProps.json)),


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

    const onAddJson = useCallback((addJsonProps: any) => {
        applyNewEditingObject(JSON.parse(addJsonProps.json));
        // addOrUpdateRecordMutation.mutate(values)
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
        <Card title={curTableId}>
            <Form name="addJson"  {...formLayout} onFinish={onAddJson} autoComplete="off">
                <Form.Item name='json' label={t('json')}>
                    <Input.TextArea placeholder="json" autoSize={{minRows: 5, maxRows: 20}}/>
                </Form.Item>
                <Form.Item wrapperCol={{offset: 4, span: 20}}>
                    <Button type="primary" htmlType="submit">
                        {t('addJson')}
                    </Button>
                </Form.Item>
            </Form>
        </Card>
        {res}
    </>;
});


