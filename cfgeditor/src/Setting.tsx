import {Button, Divider, Form, Input, InputNumber, Select, Switch, Tabs} from "antd";
import {CloseOutlined, LeftOutlined, RightOutlined, SearchOutlined} from "@ant-design/icons";
import {KeywordColorSetting} from "./KeywordColorSetting.tsx";
import {useTranslation} from "react-i18next";
import {KeywordColor, NodePlacementStrategyType, ShowDescriptionType} from "./model/entityModel.ts";
import {DraggablePanelType, FixedPage, pageRecordRef} from "./CfgEditorApp.tsx";
import {Schema, STable} from "./model/schemaModel.ts";


export function Setting({
                            schema, curTableId, curId, curPage,
                            curTable, hasFix, onDeleteRecord,
                            server, onConnectServer,

                            maxImpl, setMaxImpl,
                            refIn, setRefIn, refOutDepth, setRefOutDepth, maxNode, setMaxNode,
                            recordRefIn, setRecordRefIn, recordRefOutDepth, setRecordRefOutDepth,
                            recordMaxNode, setRecordMaxNode,
                            searchMax, setSearchMax,
                            imageSizeScale, setImageSizeScale, onToPng,
                            setFix,
                            dragPanel, setDragPanel,
                            keywordColors, setKeywordColors,
                            showDescription, setShowDescription,
                            nodePlacementStrategy, setNodePlacementStrategy,
                            containEnum, setContainEnum,

                        }: {
    schema: Schema | null;
    curTableId: string;
    curId: string;
    curPage: string;
    curTable: STable | null;
    hasFix: boolean;
    onDeleteRecord: () => void;

    server: string;
    onConnectServer: (v: string) => void;

    maxImpl: number;
    setMaxImpl: (v: number) => void;
    refIn: boolean;
    setRefIn: (v: boolean) => void;
    refOutDepth: number;
    setRefOutDepth: (v: number) => void;
    maxNode: number;
    setMaxNode: (v: number) => void;
    recordRefIn: boolean;
    setRecordRefIn: (v: boolean) => void;
    recordRefOutDepth: number;
    setRecordRefOutDepth: (v: number) => void;
    recordMaxNode: number;
    setRecordMaxNode: (v: number) => void;
    searchMax: number;
    setSearchMax: (v: number) => void;
    imageSizeScale: number;
    setImageSizeScale: (v: number) => void;
    onToPng: () => void;
    setFix: (v: FixedPage | null) => void;
    dragPanel: DraggablePanelType;
    setDragPanel: (v: DraggablePanelType) => void;
    keywordColors: KeywordColor[],
    setKeywordColors: (v: KeywordColor[]) => void;
    showDescription: ShowDescriptionType;
    setShowDescription: (t: ShowDescriptionType) => void;
    nodePlacementStrategy: NodePlacementStrategyType;
    setNodePlacementStrategy: (t: NodePlacementStrategyType) => void;
    containEnum: boolean;
    setContainEnum: (t: boolean) => void;
}) {

    const {t} = useTranslation();

    function onChangeMaxImpl(value: number | null) {
        if (value) {
            setMaxImpl(value);
            localStorage.setItem('maxImpl', value.toString());
        }
    }

    function onChangeRefIn(checked: boolean) {
        setRefIn(checked);
        localStorage.setItem('refIn', checked ? 'true' : 'false');
    }

    function onChangeRefOutDepth(value: number | null) {
        if (value) {
            setRefOutDepth(value);
            localStorage.setItem('refOutDepth', value.toString());
        }
    }

    function onChangeMaxNode(value: number | null) {
        if (value) {
            setMaxNode(value);
            localStorage.setItem('maxNode', value.toString());
        }
    }

    function onChangeRecordRefIn(checked: boolean) {
        setRecordRefIn(checked);
        localStorage.setItem('recordRefIn', checked ? 'true' : 'false');
    }

    function onChangeRecordRefOutDepth(value: number | null) {
        if (value) {
            setRecordRefOutDepth(value);
            localStorage.setItem('recordRefOutDepth', value.toString());
        }
    }

    function onChangeRecordMaxNode(value: number | null) {
        if (value) {
            setRecordMaxNode(value);
            localStorage.setItem('recordMaxNode', value.toString());
        }
    }

    function onChangeContainEnum(checked: boolean) {
        setContainEnum(checked);
        localStorage.setItem('containEnum', checked ? 'true' : 'false');
    }

    function onChangeShowDescription(value: string) {
        setShowDescription(value as ShowDescriptionType);
        localStorage.setItem('showDescription', value);
    }

    function onChangeNodePlacementStrategy(value: string) {
        setNodePlacementStrategy(value as NodePlacementStrategyType);
        localStorage.setItem('nodePlacementStrategy', value);
    }

    function onChangeSearchMax(value: number | null) {
        if (value) {
            setSearchMax(value);
            localStorage.setItem('searchMax', value.toString());
        }
    }

    function onChangeImageSizeScale(value: number | null) {
        if (value) {
            setImageSizeScale(value);
            localStorage.setItem('imageSizeScale', value.toString());
        }
    }

    function onChangeDragePanel(value: string) {
        setDragPanel(value as DraggablePanelType);
        localStorage.setItem('dragPanel', value);
    }


    function onChangeKeywordColors(colors: KeywordColor[]) {
        setKeywordColors(colors);
        localStorage.setItem('keywordColors', JSON.stringify(colors));
    }


    let deleteRecordButton;
    if (schema && curTable && schema.isEditable && curTable.isEditable) {
        deleteRecordButton = <Form.Item wrapperCol={{offset: 10}}>
            <Button type="primary" danger onClick={onDeleteRecord}>
                <CloseOutlined/>{t('deleteCurRecord')}
            </Button>
        </Form.Item>
    }


    let addFixButton;
    let removeFixButton;
    if (schema && curTable && curPage == pageRecordRef) {
        function onAddFix() {
            let fp = new FixedPage(curTableId, curId, recordRefIn, recordRefOutDepth, recordMaxNode);
            setFix(fp);
            localStorage.setItem('fix', JSON.stringify(fp));
        }

        addFixButton = <Form.Item wrapperCol={{offset: 10}}>
            <Button type="primary" onClick={onAddFix}>
                {t('addFix')}
            </Button>
        </Form.Item>
    }
    if (hasFix) {
        function onRemoveFix() {
            setFix(null);
            localStorage.removeItem('fix');
        }

        removeFixButton = <Form.Item wrapperCol={{offset: 10}}>
            <Button type="primary" onClick={onRemoveFix}>
                {t('removeFix')}
            </Button>
        </Form.Item>
    }

    let tableSetting =
        <Form labelCol={{span: 10}} wrapperCol={{span: 14}} layout={'horizontal'}>
            <Form.Item label={t('implsShowCnt')} htmlFor='implsShowCount'>
                <InputNumber id='implsShowCount' value={maxImpl} min={1} max={500} onChange={onChangeMaxImpl}/>
            </Form.Item>

            <Form.Item label={t('refIn')}>
                <Switch id='refIn' checked={refIn} onChange={onChangeRefIn}/>
            </Form.Item>

            <Form.Item label={t('refOutDepth')}>
                <InputNumber id='refOutDepth' value={refOutDepth} min={1} max={500} onChange={onChangeRefOutDepth}/>
            </Form.Item>

            <Form.Item label={t('maxNode')}>
                <InputNumber id='maxNode' value={maxNode} min={1} max={500} onChange={onChangeMaxNode}/>
            </Form.Item>
        </Form>;

    let recordSetting = <>
        <Form labelCol={{span: 10}} wrapperCol={{span: 14}} layout={'horizontal'}>
            <Form.Item label={t('recordRefIn')}>
                <Switch id='recordRefIn' checked={recordRefIn} onChange={onChangeRecordRefIn}/>
            </Form.Item>

            <Form.Item label={t('recordRefOutDepth')}>
                <InputNumber id='recordRefOutDepth' value={recordRefOutDepth} min={1} max={500}
                             onChange={onChangeRecordRefOutDepth}/>
            </Form.Item>

            <Form.Item label={t('recordMaxNode')}>
                <InputNumber id='recordMaxNode' value={recordMaxNode} min={1} max={500}
                             onChange={onChangeRecordMaxNode}/>
            </Form.Item>

            <Form.Item label={t('containEnum')}>
                <Switch id='containEnum' checked={containEnum} onChange={onChangeContainEnum}/>
            </Form.Item>


            <Form.Item label={t('showDescription')}>
                <Select id='showDescription' value={showDescription} onChange={onChangeShowDescription} options={[
                    {label: t('show'), value: 'show'},
                    {label: t('showFallbackValue'), value: 'showFallbackValue'},
                    {label: t('showValue'), value: 'showValue'},
                    {label: t('none'), value: 'none'}]}/>
            </Form.Item>

            <Form.Item label={t('nodePlacementStrategy')}>
                <Select id='nodePlacementStrategy' value={nodePlacementStrategy}
                        onChange={onChangeNodePlacementStrategy} options={[
                    {label: t('LINEAR_SEGMENTS'), value: 'LINEAR_SEGMENTS'},
                    {label: t('SIMPLE'), value: 'SIMPLE'},
                    {label: t('BRANDES_KOEPF'), value: 'BRANDES_KOEPF'}]}/>
            </Form.Item>

            <Form.Item label={t('imageSizeScale')}>
                <InputNumber id='imageSizeScale' value={imageSizeScale} min={1} max={256}
                             onChange={onChangeImageSizeScale}/>
            </Form.Item>

            <Form.Item wrapperCol={{offset: 10}}>
                <Button type="primary" onClick={onToPng}>
                    {t('toPng')}
                </Button>
            </Form.Item>
        </Form>
        <Divider/>
        <KeywordColorSetting keywordColors={keywordColors} setKeywordColors={onChangeKeywordColors}/>
    </>;

    let otherSetting =
        <Form labelCol={{span: 10}} wrapperCol={{span: 14}} layout={'horizontal'}>
            <Form.Item label={t('searchMaxReturn')}>
                <InputNumber id='searchMaxReturn' value={searchMax} min={1} max={500} onChange={onChangeSearchMax}/>
            </Form.Item>

            <Form.Item label={t('dragPanel')}>
                <Select id='dragPanel' value={dragPanel} onChange={onChangeDragePanel} options={[
                    {label: t('recordRef'), value: 'recordRef'},
                    {label: t('fix'), value: 'fix'},
                    {label: t('none'), value: 'none'}]}/>
            </Form.Item>

            {addFixButton}
            {removeFixButton}

            <Form.Item label={t('curServer')}>
                {server}
            </Form.Item>
            <Form.Item label={t('newServer')}>
                <Input.Search id='newServer' enterButton={t('connect')}
                              onSearch={onConnectServer}/>
            </Form.Item>
            {deleteRecordButton}
        </Form>;


    let keySetting =
        <Form labelCol={{span: 10}} wrapperCol={{span: 14}} layout={'horizontal'}>
            <Form.Item label={<LeftOutlined/>}>
                alt+x
            </Form.Item>
            <Form.Item label={<RightOutlined/>}>
                alt+c
            </Form.Item>
            <Form.Item label={t('table')}>
                alt+1
            </Form.Item>
            <Form.Item label={t('tableRef')}>
                alt+2
            </Form.Item>
            <Form.Item label={t('record')}>
                alt+3
            </Form.Item>
            <Form.Item label={t('recordRef')}>
                alt+4
            </Form.Item>
            <Form.Item label={<SearchOutlined/>}>
                alt+q
            </Form.Item>
        </Form>;

    let items = [
        {key: 'tableSetting', label: t('tableSetting'), children: tableSetting,},
        {key: 'recordSetting', label: t('recordSetting'), children: recordSetting,},
        {key: 'otherSetting', label: t('otherSetting'), children: otherSetting,},
        {key: 'keySetting', label: t('keySetting'), children: keySetting,},
    ]


    return <Tabs items={items} tabPosition='left'/>;

}