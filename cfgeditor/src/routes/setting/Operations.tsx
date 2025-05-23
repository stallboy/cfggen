import {memo, RefObject, useCallback} from "react";
import {useTranslation} from "react-i18next";
import {App, Button, Divider, Form, InputNumber} from "antd";
import {
    setImageSizeScale,
    useMyStore,
    useLocationData,
    invalidateAllQueries,
} from "./store.ts";
import {CloseOutlined} from "@ant-design/icons";
import {Schema} from "../table/schemaUtil.tsx";
import {STable} from "../table/schemaModel.ts";
import {useMutation} from "@tanstack/react-query";
import {RecordEditResult} from "../record/recordModel.ts";
import {deleteRecord} from "../api.ts";
import {toBlob} from "html-to-image";
import {saveAs} from "file-saver";
import {formItemLayoutWithOutLabel, formLayout} from "./BasicSetting.tsx";
import {OpFixPages} from "./OpFixPages.tsx";


export const Operations = memo(function Operations({schema, curTable, flowRef}: {
    schema: Schema | undefined;
    curTable: STable | null;
    flowRef: RefObject<HTMLDivElement>;
}) {
    const {curPage} = useLocationData();
    const {t} = useTranslation();
    const {server, imageSizeScale} = useMyStore();

    const {curTableId, curId} = useLocationData();
    const {notification} = App.useApp();

    const deleteRecordMutation = useMutation<RecordEditResult, Error>({
        mutationFn: () => deleteRecord(server, curTableId, curId),

        onError: (error) => {
            notification.error({
                message: `deleteRecord ${curTableId}/${curId} err: ${error.message}`,
                placement: 'topRight',
                duration: 4
            });
        },
        onSuccess: (editResult) => {
            if (editResult.resultCode == 'deleteOk') {
                // console.log(editResult);
                notification.info({
                    message: `deleteRecord ${curTableId}/${curId} ${editResult.resultCode}`,
                    placement: 'topRight',
                    duration: 3
                });
                invalidateAllQueries();
            } else {
                notification.warning({
                    message: `deleteRecord ${curTableId}/${curId}  ${editResult.resultCode}`,
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
                notification.info({message: "save png to " + fn, duration: 3});
            }
        }).catch((err) => {
            notification.error({message: "save png failed: limit the max node count", duration: 3});
            console.log(err)
        })
    }, [flowRef, imageSizeScale, curPage, curTableId, notification]);


    return <>
        <OpFixPages schema={schema} curTable={curTable}/>
        <Form {...formLayout} layout={'horizontal'} initialValues={{imageSizeScale}}>
            <Form.Item name='imageSizeScale' label={t('imageSizeScale')}>
                <InputNumber min={1} max={256} onChange={setImageSizeScale}/>
            </Form.Item>

            <Form.Item {...formItemLayoutWithOutLabel}>
                <Button type="primary" onClick={onToPng}>
                    {t('toPng')}
                </Button>
            </Form.Item>

            <Divider/>

            {(schema && curTable && schema.isEditable && curTable.isEditable) &&
                <Form.Item {...formItemLayoutWithOutLabel}>
                    <Button type="primary" danger
                            onClick={() => deleteRecordMutation.mutate()}>
                        <CloseOutlined/>{t('deleteCurRecord')}
                    </Button>
                </Form.Item>
            }

        </Form>

    </>;
});
