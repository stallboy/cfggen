import {ChangeEvent, CSSProperties, memo, useCallback, useState} from "react";
import {App, Button, Flex, Input} from "antd";
import {BookOutlined, DeleteOutlined} from "@ant-design/icons";
import {useTranslation} from "react-i18next";
import {useMutation} from "@tanstack/react-query";
import {updateNote} from "@/api/api";
import {useMyStore} from "@/store/store";
import {NoteEditResult, notesToMap} from "@/api/noteModel";
import {queryClient} from "@/app/queryClient";
import {estimateNoteRows, NOTE_ROW_H} from "./layout/calcWidthHeight.ts";

// ── note 展示/编辑组件：按「是否内嵌 EntityForm 编辑表单」分两套 ──
//
// 独立态（只读 / card 视图，自带网络请求）：
//   NoteShow       展示 note + 编辑按钮，点击切到 NoteEdit
//   NoteEdit       自带 useMutation，提交即 updateNote API 落库 + 刷新 ['notes']
//
// Inner 态（内嵌 EntityForm 编辑表单，不触网）：
//   NoteShowInner  纯展示，无按钮
//   NoteEditInner  onChange → updateNoteInEdit 写入编辑会话，随表单 alt+s 提交
//
// 记法：「Inner」后缀 = 嵌在编辑表单里、不直接落库、随 EntityEdit 会话提交；
//       无 Inner = 独立卡片态、自己发网络请求。
//       调度见 useNodeNote（NodeNote.tsx）：edit 分支用 Inner 两件，非 edit 用 NoteShow / NoteEdit。

const noteButtonStyle: CSSProperties = {float: 'right', borderWidth: 0, backgroundColor: 'transparent'};
const bookIcon = <BookOutlined/>;

// note 是"便利贴"语义，底色刻意用饱和暖黄显眼（区别于节点底色，提醒用户这里有批注）。
// 原纯 yellow #FFFF00 过刺眼、colorWarningBg #fffbe6 又太淡失去提醒作用——取中间的 #ffe066（明亮金黄）。
// 固定色不随主题：暗色模式下黄底黑字的便利贴同样合理且醒目。不加 border 以免改变 note 高度估算
// （estimateNoteRows/NOTE_ROW_H 按当前无 border 渲染校准）。
const NOTE_BG = '#ffe066';
const NOTE_STYLE: CSSProperties = {backgroundColor: NOTE_BG, borderRadius: 8, whiteSpace: 'pre-wrap'};
const NOTE_TEXT_AREA_STYLE: CSSProperties = {backgroundColor: NOTE_BG};
// whiteSpace: pre-wrap 让 div（NoteShow/NoteShowInner）尊重 note 里的 \n 换行——默认 normal 会把 \n 当空白折叠，
// 与 TextArea（NoteEdit/NoteEditInner 按 \n 换行）表现不一致，含换行的 note 在 div 里行数错乱、与 estimateNoteRows
// 预留高度对不上。pre-wrap 仅影响文本内联布局；NoteEdit/NoteEditInner 的 Flex 容器也用此样式作背景，TextArea 不受影响。

export const NoteShow = memo(function NoteShow({note, setIsEdit}: {
    note: string;
    setIsEdit: (ie: boolean) => void;
}) {
    const {t} = useTranslation();
    const onEditClick = useCallback(() => {
        setIsEdit(true);
    }, [setIsEdit]);

    return <div style={{...NOTE_STYLE, minHeight: estimateNoteRows(note) * NOTE_ROW_H}}>
        {note} <Button style={noteButtonStyle}
                    icon={bookIcon}
                    aria-label={t('editNote')}
                    onClick={onEditClick}/>
    </div>
});


// 独立态编辑器（只读 / card 视图用）：自带 useMutation，提交即 updateNote API 落库 + 刷新 ['notes'] 缓存。
// 仅真成功才 setIsEdit(false) 关闭（失败保留 newNote 便于重试）。嵌入版见 NoteEditInner。
export const NoteEdit = memo(function NoteEdit({id, note, setIsEdit}: {
    id: string;
    note: string;
    setIsEdit: (ie: boolean) => void;
}) {
    const {server} = useMyStore();
    const {t} = useTranslation();
    const [newNote, setNewNote] = useState<string>(note);
    const {notification} = App.useApp();

    const {isPending, mutate} = useMutation<NoteEditResult, Error, string>({
        mutationFn: (newNote: string) => updateNote(server, id, newNote),

        onError: (error, variables) => {
            notification.error({
                title: `updateNote  ${id} ${variables} err: ${error.toString()}`,
                placement: 'topRight', duration: 4
            });
            // 提交失败时保持编辑框打开，保留用户已输入的 newNote 便于重试
            // （原 setIsEdit(false) 会卸载 NoteEdit、销毁 newNote 本地状态）
        },
        onSuccess: (editResult, variables) => {
            const {resultCode, notes} = editResult;
            if (resultCode == 'updateOk' || resultCode == 'addOk' || resultCode == 'deleteOk') {
                notification.info({
                    title: `updateNote  ${resultCode} ${id} ${variables}`,
                    placement: 'topRight',
                    duration: 3
                });
                queryClient.setQueryData(['notes'], notesToMap(notes));
                setIsEdit(false);   // 仅真成功才关闭编辑器
            } else {
                notification.warning({
                    title: `updateNote ${resultCode} ${id} ${variables}`,
                    placement: 'topRight',
                    duration: 4
                });
                // 业务失败（storeErr/keyNotSet/keyNotFoundOnDelete，HTTP 200 带非 OK resultCode）
                // 保留编辑框与 newNote，便于用户改后重试——与 onError 的设计意图一致。
            }
        },
    });

    const onCancelClick = useCallback(() => {
        setIsEdit(false);
    }, [setIsEdit]);

    const onSubmitClick = useCallback(() => {
        mutate(newNote);
    }, [mutate, newNote]);

    // 删除 note：提交空串 → updateNote 走 'deleteOk' 分支（onSuccess 关闭编辑器 + 刷新 ['notes']）。
    // 仅已有 note 时显示该按钮（新增态 recordNote 为空，Cancel 已足够）。
    const onDeleteClick = useCallback(() => {
        mutate("");
    }, [mutate]);

    const onNoteChange = useCallback((e: ChangeEvent<HTMLTextAreaElement>) => {
        const value = e.target.value;
        setNewNote(value);
    }, [setNewNote]);

    return <Flex vertical style={NOTE_STYLE}>
        <Input.TextArea className='nodrag' placeholder={t('notePlaceholder')} autoFocus
                  rows={estimateNoteRows(note)}
                  style={NOTE_TEXT_AREA_STYLE}
                  value={newNote}
                  onChange={onNoteChange}/>
        <Flex justify={'flex-end'} gap={'small'}>
            <Button onClick={onCancelClick}>{t('cancelUpdateNote')}</Button>
            {note.length > 0 &&
                <Button danger icon={<DeleteOutlined/>} onClick={onDeleteClick}>{t('nodeDelete')}</Button>}
            <Button type='primary' loading={isPending} onClick={onSubmitClick}>{t('updateNote')}</Button>
        </Flex>
    </Flex>


});

export const NoteShowInner = memo(function NoteShowInner({note}: {
    note: string;
}) {
    return <div style={{...NOTE_STYLE, minHeight: estimateNoteRows(note) * NOTE_ROW_H}}>
        {note}
    </div>
});

// 嵌入态编辑器（内嵌 EntityForm 编辑表单）：不触网，onChange → updateNoteInEdit 写入编辑会话(EntityEdit)，
// note 随整个表单 alt+s 一起提交；无提交/取消按钮（提交由外层会话统一）。独立版见 NoteEdit。
export const NoteEditInner = memo(function NoteEditInner({note, updateNoteInEdit}: {
    note: string;
    updateNoteInEdit: (note: string) => void;
}) {
    const {t} = useTranslation();
    // rows 在 mount 时按初始 note 算一次，之后固定——符合 §6 "textarea 固定 rows、不随输入动态伸缩"。
    // note 是变化的 tmpNote，若 rows 随之动态重算，编辑长 note 时 textarea 会持续长高、节点 DOM
    // 超出 ELK 估算高度而 overlap 相邻节点。value 仍受控随输入变化，但高度固定（超出滚动）。
    // 不加 autoFocus：编辑态下多个已有 note 的节点会各自挂一份 NoteEditInner，autoFocus 会让
    // 最后挂载的那个抢焦点 + scrollIntoView，把焦点从用户要编辑的表单字段夺走。autoFocus 只留给
    // 单实例的 NoteEdit（点击触发、一次只一个）。placeholder 仍走 i18n。
    const [rows] = useState(() => estimateNoteRows(note));
    const onNoteChange = useCallback((e: ChangeEvent<HTMLTextAreaElement>) => {
        const value = e.target.value;
        updateNoteInEdit(value);
    }, [updateNoteInEdit]);

    return <Flex vertical style={NOTE_STYLE}>
        <Input.TextArea className='nodrag' placeholder={t('notePlaceholder')}
                  rows={rows}
                  style={NOTE_TEXT_AREA_STYLE}
                  value={note}
                  onChange={onNoteChange}/>
    </Flex>
});

