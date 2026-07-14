import {memo, RefObject, useCallback} from "react";
import {useNavigate} from "react-router";
import {useTranslation} from "react-i18next";
import {App, Button, Divider, Form, InputNumber, Popconfirm, Radio, Space} from "antd";
import {
    setImageSizeScale,
    useMyStore,
} from "@/store/store";
import {invalidateAllQueries} from "@/app/queryClient";
import {CloseOutlined} from "@ant-design/icons";
import {Schema} from "@/domain/schema";
import {STable} from "@/api/schemaModel";
import {useMutation} from "@tanstack/react-query";
import {RecordEditResult} from "@/api/recordModel";
import {deleteRecord} from "@/api/api";
import {toBlob} from "html-to-image";
import {saveAs} from "file-saver";
import {PageType, navTo, useLocationData} from "@/store/store";
import {KeyShortCut} from "./KeyShortcut.tsx";
import {toggleFullScreen} from "@/utils/windowUtils";


export const ToolsSetting = memo(function ToolsSetting({schema, curTable, flowRef}: {
    schema: Schema | undefined;
    curTable: STable | null;
    flowRef: RefObject<HTMLDivElement | null>;
}) {
    const {t} = useTranslation();
    const {server, imageSizeScale} = useMyStore();

    const {curPage, curTableId, curId} = useLocationData();
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
                // record/recordRef 是记录级视图，文件名带 curId；table/tableRef/recordUnref 是表级视图，只带表名
                const isRecordLevel = curPage === 'record' || curPage === 'recordRef';
                const fn = isRecordLevel
                    ? `${curPage}_${curTableId}_${curId}.png`
                    : `${curPage}_${curTableId}.png`;
                saveAs(blob, fn);
                notification.info({title: "save png to " + fn, duration: 3});
            }
        }).catch(() => {
            notification.error({title: "save png failed: limit the max node count", duration: 3});
        })
        // eslint-disable-next-line react-hooks/exhaustive-deps -- flowRef 在 body 中经解构使用（const {current} = flowRef），oxlint exhaustive-deps 未追踪解构引用而误报
    }, [flowRef, imageSizeScale, curPage, notification, curTableId, curId]);

    const options = [
        {label: t('table'), value: 'table'},
        {label: t('tableRef'), value: 'tableRef'},
        {label: t('record'), value: 'record'},
        {label: t('recordRef'), value: 'recordRef'},
        {label: t('unreferenced'), value: 'recordUnref'}
    ];

    const onChangeCurPage = useCallback((page: PageType) => {
        // recordUnref 路由为 recordUnref/:table/*（带 id 段），统一带 curId：切到 unref 再切回 record/table 等不丢上下文
        navigate(navTo(page, curTableId, curId));
    }, [curTableId, curId, navigate]);


    return <>
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
        </Form>

        {schema && curTable && schema.isEditable &&
            <Popconfirm title={t('deleteCurRecord')}
                        okText={t('delete')}
                        cancelText={t('cancel')}
                        okButtonProps={{danger: true}}
                        onConfirm={() => deleteRecordMutation.mutate()}>
                <Button type="primary" danger>
                    <CloseOutlined/>{t('deleteCurRecord')}
                </Button>
            </Popconfirm>
        }

        <Divider/>
        <Button onClick={toggleFullScreen}> {t('toggleFullScreen')}</Button>
        <Divider/>

        <KeyShortCut/>
    </>;
});
