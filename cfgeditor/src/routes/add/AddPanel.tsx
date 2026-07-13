import {memo, useState} from "react";
import {Flex, Result, Segmented} from "antd";
import {CodeOutlined, RobotOutlined} from "@ant-design/icons";
import {useTranslation} from "react-i18next";
import {Schema} from "@/domain/schema";
import {Chat} from "./Chat.tsx";
import {AddJson} from "./AddJson.tsx";
import {useIsCurTableEditable} from "./useEditable.ts";

/**
 * 「添加数据」面板：AI 生成(Chat) 与 JSON 导入(AddJson) 两种手段并列，Segmented 切换。
 * 两者同意图（给当前表写入一条记录），聚合在此，不再一个塞 Setting、一个塞 dragPanel。
 * dragPanel='add' 渲染本组件；作为独立侧栏面板，与 Finder/Setting 平级。
 */
export const AddPanel = memo(function AddPanel({schema}: { schema: Schema | undefined }) {
    const {t} = useTranslation();
    const editable = useIsCurTableEditable(schema);
    const [mode, setMode] = useState<'ai' | 'json'>('ai');

    if (!editable) return <Result title={t('notEditable')}/>;

    return (
        <Flex vertical gap="small" style={{height: '100%'}}>
            <Segmented block value={mode} options={[
                {icon: <RobotOutlined/>, label: t('aiGenerate'), value: 'ai'},
                {icon: <CodeOutlined/>, label: t('jsonImport'), value: 'json'},
            ]} onChange={(v) => setMode(v as 'ai' | 'json')}/>
            {/* 两侧常驻、用 display 隐藏非活动侧，避免切换卸载 Chat 丢失 useXChat 对话 */}
            <div style={{flex: 1, minHeight: 0}}>
                <div style={{height: '100%', display: mode === 'ai' ? 'flex' : 'none', flexDirection: 'column'}}>
                    <Chat schema={schema}/>
                </div>
                <div style={{height: '100%', display: mode === 'json' ? 'block' : 'none', overflow: 'auto'}}>
                    <AddJson/>
                </div>
            </div>
        </Flex>
    );
});
