import {ChangeEvent, CSSProperties, memo, useCallback, useState} from "react";
import {App, Button, Flex, Input} from "antd";
import {BookOutlined} from "@ant-design/icons";
import {useTranslation} from "react-i18next";
import {useMutation} from "@tanstack/react-query";
import {updateNote} from "@/api/api";
import {useMyStore} from "@/store/store";
import {NoteEditResult, notesToMap} from "@/api/noteModel";
import {queryClient} from "@/queryClient";


const noteButtonStyle: CSSProperties = {float: 'right', borderWidth: 0, backgroundColor: 'transparent'};
const bookIcon = <BookOutlined/>;
const noteStyle: CSSProperties = {backgroundColor: "yellow", borderRadius: '8px'}
const TEXT_AREA_STYLE: CSSProperties = {backgroundColor: "yellow"};

export const NoteShow = memo(function NoteShow({note, setIsEdit}: {
    note: string;
    setIsEdit: (ie: boolean) => void;
}) {
    const onEditClick = useCallback(() => {
        setIsEdit(true);
    }, [setIsEdit]);

    return <div style={noteStyle}>
        {note} <Button style={noteButtonStyle}
                       icon={bookIcon}
                       onClick={onEditClick}/>
    </div>
});


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

    const onNoteChange = useCallback((e: ChangeEvent<HTMLTextAreaElement>) => {
        const value = e.target.value;
        setNewNote(value);
    }, [setNewNote]);

    return <Flex vertical style={noteStyle}>
        <Input.TextArea className='nodrag' placeholder='note'
                  rows={1}
                  style={TEXT_AREA_STYLE}
                  value={newNote}
                  onChange={onNoteChange}/>
        <Flex justify={'flex-end'} gap={'small'}>
            <Button onClick={onCancelClick}>{t('cancelUpdateNote')}</Button>
            <Button type='primary' loading={isPending} onClick={onSubmitClick}>{t('updateNote')}</Button>
        </Flex>
    </Flex>


});

export const NoteShowInner = memo(function NoteShowInner({note}: {
    note: string;
}) {
    return <div style={noteStyle}>
        {note}
    </div>
});

export const NoteEditInner = memo(function NoteEditInner({note, updateNoteInEdit}: {
    note: string;
    updateNoteInEdit: (note: string) => void;
}) {
    const onNoteChange = useCallback((e: ChangeEvent<HTMLTextAreaElement>) => {
        const value = e.target.value;
        updateNoteInEdit(value);
    }, [updateNoteInEdit]);

    return <Flex vertical style={noteStyle}>
        <Input.TextArea className='nodrag' placeholder='note'
                  rows={1}
                  style={TEXT_AREA_STYLE}
                  value={note}
                  onChange={onNoteChange}/>
    </Flex>
});

