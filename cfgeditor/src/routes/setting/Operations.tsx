import {memo} from "react";
import {useTranslation} from "react-i18next";
import {App, Button, Divider, Form, Input, InputNumber, Radio, Space} from "antd";
import {
    DragPanelType,
    navTo,
    setDragPanel,
    setFix,
    setFixNull,
    setImageSizeScale, setServer,
    store,
    useLocationData
} from "./store.ts";
import {CloseOutlined} from "@ant-design/icons";
import {Schema} from "../table/schemaUtil.ts";
import {STable} from "../table/schemaModel.ts";
import {useMutation, useQueryClient} from "@tanstack/react-query";
import {RecordEditResult} from "../record/recordModel.ts";
import {deleteRecord} from "../../io/api.ts";
import {useNavigate} from "react-router-dom";

const formLayout = {
    labelCol: {xs: {span: 24}, sm: {span: 6},},
    wrapperCol: {xs: {span: 24}, sm: {span: 18},},
};

export const Operations = memo(function Operations({schema, curTable, onToPng}: {
    schema: Schema | undefined;
    curTable: STable | null;
    onToPng: () => void;
}) {
    const {curPage} = useLocationData();
    const {t} = useTranslation();
    const {
        server, dragPanel,
        imageSizeScale, fix
    } = store;

    const {curTableId, curId} = useLocationData();
    const {notification} = App.useApp();
    const queryClient = useQueryClient();
    const navigate = useNavigate();

    const deleteRecordMutation = useMutation<RecordEditResult, Error>({
        mutationFn: () => deleteRecord(server, curTableId, curId),

        onError: (error, _variables, _context) => {
            notification.error({
                message: `deleteRecord ${curTableId}/${curId} err: ${error.message}`,
                placement: 'topRight',
                duration: 4
            });
            queryClient.clear();
        },
        onSuccess: (editResult, _variables, _context) => {
            if (editResult.resultCode == 'deleteOk') {
                console.log(editResult);
                notification.info({
                    message: `deleteRecord ${curTableId}/${curId} ${editResult.resultCode}`,
                    placement: 'topRight',
                    duration: 3
                });
                queryClient.clear();
                navigate(navTo(curPage, curTableId, curId));
            } else {
                notification.warning({
                    message: `deleteRecord ${curTableId}/${curId}  ${editResult.resultCode}`,
                    placement: 'topRight',
                    duration: 4
                });
            }
        },
    });


    let deleteRecordButton;
    if (schema && curTable && schema.isEditable && curTable.isEditable) {
        deleteRecordButton = <Button type="primary" danger onClick={() => deleteRecordMutation.mutate()}>
            <CloseOutlined/>{t('deleteCurRecord')}
        </Button>
    }

    let addFixButton;
    let removeFixButton;
    if (schema && curTable && curPage == 'recordRef') {
        addFixButton = <Form.Item wrapperCol={{offset: 10}}>
            <Button type="primary" onClick={() => setFix(curTableId, curId)}>
                {t('addFix')}
            </Button>
        </Form.Item>
    }
    if (fix != null) {
        removeFixButton = <Form.Item wrapperCol={{offset: 10}}>
            <Button type="primary" onClick={setFixNull}>
                {t('removeFix')}
            </Button>
        </Form.Item>
    }


    return <Form {...formLayout} layout={'horizontal'}
                 initialValues={{imageSizeScale, server}}>
        <Form.Item name='imageSizeScale' label={t('imageSizeScale')}>
            <InputNumber min={1} max={256} onChange={setImageSizeScale}/>
        </Form.Item>

        <Form.Item wrapperCol={{offset: 6}}>
            <Button type="primary" onClick={onToPng}>
                {t('toPng')}
            </Button>
        </Form.Item>

        <Form.Item name='dragePanel' initialValue={dragPanel} label={t('dragPanel')}>
            <Radio.Group onChange={(e) => setDragPanel(e.target.value as DragPanelType)}
                         optionType='button' buttonStyle="solid" options={[
                {label: t('recordRef'), value: 'recordRef'},
                {label: t('fix'), value: 'fix'},
                {label: t('none'), value: 'none'}]}/>
        </Form.Item>

        {addFixButton}
        {removeFixButton}


        <Form.Item label={t('curServer')}>
            {server}
        </Form.Item>
        <Form.Item name='server' label={t('newServer')}>
            <Input.Search enterButton={t('connect')} onSearch={(value: string) => setServer(value)}/>
        </Form.Item>

        <Divider/>
        <Space>
            {deleteRecordButton}
        </Space>
    </Form>;
});