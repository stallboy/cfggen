import {ChangeEvent, CSSProperties, memo, useCallback, useState} from "react";
import {App, Button, Flex} from "antd";
import TextArea from "antd/es/input/TextArea";
import {BookOutlined} from "@ant-design/icons";
import {useTranslation} from "react-i18next";
import {useMutation} from "@tanstack/react-query";
import {updateNote} from "../api/api.ts";
import {useMyStore} from "../store/store.ts";
import {NoteEditResult, notesToMap} from "../api/noteModel.ts";
import {queryClient} from "../main.tsx";


const noteButtonStyle: CSSProperties = {float: 'right', borderWidth: 0, backgroundColor: 'transparent'};
const bookIcon = <BookOutlined/>;
const noteStyle: CSSProperties = {backgroundColor: "yellow", borderRadius: '8px'}

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
            setIsEdit(false);

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
            } else {
                notification.warning({
                    title: `updateNote ${resultCode} ${id} ${variables}`,
                    placement: 'topRight',
                    duration: 4
                });
            }
            setIsEdit(false);
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
        <TextArea className='nodrag' placeholder='note'
                  autoSize={autoSize}
                  style={textAreaStyle}
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

const textAreaStyle = {backgroundColor: "yellow"};
const autoSize = {minRows: 1, maxRows: 10};

export const NoteEditInner = memo(function NoteEditInner({note, updateNoteInEdit}: {
    note: string;
    updateNoteInEdit: (note: string) => void;
}) {
    const onNoteChange = useCallback((e: ChangeEvent<HTMLTextAreaElement>) => {
        const value = e.target.value;
        updateNoteInEdit(value);
    }, [updateNoteInEdit]);

    return <Flex vertical style={noteStyle}>
        <TextArea className='nodrag' placeholder='note'
                  autoSize={autoSize}
                  style={textAreaStyle}
                  value={note}
                  onChange={onNoteChange}/>
    </Flex>
});

