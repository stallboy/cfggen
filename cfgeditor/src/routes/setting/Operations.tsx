import {memo, RefObject} from "react";
import {useTranslation} from "react-i18next";
import {App, Button, Divider, Form, Input, InputNumber, Radio} from "antd";
import {
    DragPanelType,
    setDragPanel,
    setFix,
    removeFix,
    setImageSizeScale, setServer,
    store,
    useLocationData, invalidateAllQueries
} from "./store.ts";
import {CloseOutlined} from "@ant-design/icons";
import {Schema} from "../table/schemaUtil.ts";
import {STable} from "../table/schemaModel.ts";
import {useMutation} from "@tanstack/react-query";
import {RecordEditResult} from "../record/recordModel.ts";
import {deleteRecord} from "../api.ts";
import {toBlob} from "html-to-image";
import {saveAs} from "file-saver";
import {formLayout} from "./TableSetting.tsx";


export const Operations = memo(function Operations({schema, curTable, flowRef}: {
    schema: Schema | undefined;
    curTable: STable | null;
    flowRef: RefObject<HTMLDivElement>;
}) {
    const {curPage} = useLocationData();
    const {t} = useTranslation();
    const {
        server, dragPanel,
        imageSizeScale, fix
    } = store;

    const {curTableId, curId} = useLocationData();
    const {notification} = App.useApp();

    const deleteRecordMutation = useMutation<RecordEditResult, Error>({
        mutationFn: () => deleteRecord(server, curTableId, curId),

        onError: (error, _variables, _context) => {
            notification.error({
                message: `deleteRecord ${curTableId}/${curId} err: ${error.message}`,
                placement: 'topRight',
                duration: 4
            });
        },
        onSuccess: (editResult, _variables, _context) => {
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

    function onToPng() {
        const {current} = flowRef;
        if (current === null) {
            return
        }

        let w = current.offsetWidth * imageSizeScale;
        let h = current.offsetHeight * imageSizeScale;

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

        {(schema && curTable && curPage == 'recordRef') &&
            <Form.Item wrapperCol={{offset: 6}}>
                <Button type="primary" onClick={() => setFix(curTableId, curId)}>
                    {t('addFix')}
                </Button>
            </Form.Item>
        }
        {fix &&
            <Form.Item wrapperCol={{offset: 6}}>
                <Button type="primary" onClick={removeFix}>
                    {t('removeFix')}
                </Button>
            </Form.Item>
        }


        <Form.Item label={t('curServer')}>
            {server}
        </Form.Item>
        <Form.Item name='server' label={t('newServer')}>
            <Input.Search enterButton={t('connect')} onSearch={(value: string) => setServer(value)}/>
        </Form.Item>

        <Divider/>

        {(schema && curTable && schema.isEditable && curTable.isEditable) &&
            <Button type="primary" danger onClick={() => deleteRecordMutation.mutate()}>
                <CloseOutlined/>{t('deleteCurRecord')}
            </Button>
        }

    </Form>;
});
