import {memo, RefObject, useCallback} from "react";
import {useNavigate} from "react-router-dom";
import {useTranslation} from "react-i18next";
import {App, Button, Divider, Form, Input, InputNumber, Radio, Space} from "antd";
import {
    setImageSizeScale,
    useMyStore,
    invalidateAllQueries, setServer,
} from "../../store/store.ts";
import {CloseOutlined} from "@ant-design/icons";
import {Schema} from "../table/schemaUtil.tsx";
import {STable} from "../table/schemaModel.ts";
import {useMutation} from "@tanstack/react-query";
import {RecordEditResult} from "../record/recordModel.ts";
import {deleteRecord} from "../api.ts";
import {toBlob} from "html-to-image";
import {saveAs} from "file-saver";
import {OpFixPages} from "./OpFixPages.tsx";
import {PageType, navTo, useLocationData} from "../../store/store.ts";
import {getCurrentWebviewWindow} from "@tauri-apps/api/webviewWindow";
import {KeyShortCut} from "./KeyShortcut.tsx";

export async function toggleFullScreen() {
    const appWindow = getCurrentWebviewWindow()
    const isFullScreen = await appWindow.isFullscreen();
    await appWindow.setFullscreen(!isFullScreen);
}


export const Operations = memo(function Operations({schema, curTable, flowRef}: {
    schema: Schema | undefined;
    curTable: STable | null;
    flowRef: RefObject<HTMLDivElement>;
}) {
    const {t} = useTranslation();
    const {server, imageSizeScale} = useMyStore();

    const {curTableId, curId, curPage} = useLocationData();
    const {notification} = App.useApp();
    const navigate = useNavigate();

    const deleteRecordMutation = useMutation<RecordEditResult, Error>({
        mutationFn: () => deleteRecord(server, curTableId, curId),

        onError: (error) => {
            notification.error({
                title: `deleteRecord ${curTableId}/${curId} err: ${error.message}`,
                placement: 'topRight',
                duration: 4
            });
        },
        onSuccess: (editResult) => {
            if (editResult.resultCode == 'deleteOk') {
                // console.log(editResult);
                notification.info({
                    title: `deleteRecord ${curTableId}/${curId} ${editResult.resultCode}`,
                    placement: 'topRight',
                    duration: 3
                });
                invalidateAllQueries();
            } else {
                notification.warning({
                    title: `deleteRecord ${curTableId}/${curId}  ${editResult.resultCode}`,
                    placement: 'topRight',
                    duration: 4
                });
            }
        },
    });

    const onToPng = useCallback(function () {
        const {current} = flowRef;
        if (current === null) {
            return
        }

        const w = current.offsetWidth * imageSizeScale;
        const h = current.offsetHeight * imageSizeScale;

        toBlob(current, {
            cacheBust: true, canvasWidth: w, canvasHeight: h, pixelRatio: 1,
            filter: ({classList}: HTMLElement) => {

                return (!classList) ||
                    (!classList.contains('react-flow__attribution') &&
                        !classList.contains('react-flow__controls') &&
                        !classList.contains('react-flow__background'));
            }
        }).then((blob) => {
            if (blob) {
                let fn;
                if (curPage.startsWith("table")) {
                    fn = `${curPage}_${curTableId}.png`;
                } else {
                    fn = `${curPage}_${curTableId}_${curId}.png`;
                }
                saveAs(blob, fn);
                notification.info({title: "save png to " + fn, duration: 3});
            }
        }).catch((err) => {
            notification.error({title: "save png failed: limit the max node count", duration: 3});
            console.log(err)
        })
    }, [flowRef, imageSizeScale, curPage, curTableId, notification]);

    const options = [
        {label: t('table'), value: 'table'},
        {label: t('tableRef'), value: 'tableRef'},
        {label: t('record'), value: 'record'},
        {label: t('recordRef'), value: 'recordRef'}
    ];

    const onChangeCurPage = useCallback((page: PageType) => {
        navigate(navTo(page, curTableId, curId));
    }, [curTableId, curId, navigate]);


    return <>
        <Form layout={'vertical'} initialValues={{server}}>
            <Form.Item label={t('curServer')}>
                {server}
            </Form.Item>
            <Form.Item name='server' label={t('newServer')}>
                <Input.Search enterButton={t('connect')} onSearch={(value: string) => setServer(value)}/>
            </Form.Item>
        </Form>

        <Divider/>
        <OpFixPages schema={schema} curTable={curTable}/>


        <Radio.Group optionType="button"
                     value={curPage}
                     options={options}
                     onChange={(e) => onChangeCurPage(e.target.value)}/>
        <Divider/>

        <Form layout={'vertical'} initialValues={{imageSizeScale}}>
            <Form.Item name='imageSizeScale' label={t('imageSizeScale')}>
                <Space>
                    <InputNumber min={1} max={256} onChange={setImageSizeScale}/>
                    <Button type="primary" onClick={onToPng}>
                        {t('toPng')}
                    </Button>
                </Space>
            </Form.Item>


            {(schema && curTable && schema.isEditable && curTable.isEditable) &&
                <Form.Item>
                    <Divider/>
                    <Button type="primary" danger
                            onClick={() => deleteRecordMutation.mutate()}>
                        <CloseOutlined/>{t('deleteCurRecord')}
                    </Button>
                </Form.Item>
            }
        </Form>
        <Divider/>
        <Button onClick={toggleFullScreen}> {t('toggleFullScreen')}</Button>
        <Divider/>

        <KeyShortCut/>
    </>;
});
