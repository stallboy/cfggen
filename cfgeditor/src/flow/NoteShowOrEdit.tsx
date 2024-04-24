import {ChangeEvent, CSSProperties, memo, useCallback, useState} from "react";
import {App, Button, Flex} from "antd";
import TextArea from "antd/es/input/TextArea";
import {BookOutlined} from "@ant-design/icons";
import {useTranslation} from "react-i18next";
import {useMutation} from "@tanstack/react-query";
import {updateNote} from "../routes/api.ts";
import {store} from "../routes/setting/store.ts";
import {NoteEditResult, notesToMap} from "../routes/record/noteModel.ts";
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
        <p>{note} {<Button style={noteButtonStyle}
                           icon={bookIcon}
                           onClick={onEditClick}/>}</p>
    </div>
});


export const NoteEdit = memo(function NoteEdit({id, note, setIsEdit}: {
    id: string;
    note: string;
    setIsEdit: (ie: boolean) => void;
}) {
    const {server} = store;
    const {t} = useTranslation();
    const [newNote, setNewNote] = useState<string>(note);
    const {notification} = App.useApp();

    const {isPending, mutate} = useMutation<NoteEditResult, Error, string>({
        mutationFn: (newNote: string) => updateNote(server, id, newNote),

        onError: (error, variables) => {
            notification.error({
                message: `updateNote  ${id} ${variables} err: ${error.toString()}`,
                placement: 'topRight', duration: 4
            });
            setIsEdit(false);

        },
        onSuccess: (editResult, variables) => {
            const {resultCode, notes} = editResult;
            if (resultCode == 'updateOk' || resultCode == 'addOk' || resultCode == 'deleteOk') {
                notification.info({
                    message: `updateNote  ${resultCode} ${id} ${variables}`,
                    placement: 'topRight',
                    duration: 3
                });
                queryClient.setQueryData(['notes'], notesToMap(notes));
            } else {
                notification.warning({
                    message: `updateNote ${resultCode} ${id} ${variables}`,
                    placement: 'topRight',
                    duration: 4
                });
            }
            setIsEdit(false);
        },
    });

    function onCancelClick() {
        setIsEdit(false);
    }

    function onSubmitClick() {
        mutate(newNote);
    }

    function onNoteChange(e: ChangeEvent<HTMLTextAreaElement>) {
        const value = e.target.value;
        setNewNote(value);
    }

    return <Flex vertical style={{backgroundColor: "yellow", borderRadius: '8px'}}>
        <TextArea className='nodrag' placeholder='note' autoSize={{minRows: 2, maxRows: 10}}
                  value={newNote}
                  onChange={onNoteChange}/>
        <Flex justify={'flex-end'} gap={'small'}>
            <Button onClick={onCancelClick}>{t('cancelUpdateNote')}</Button>
            <Button type='primary' loading={isPending} onClick={onSubmitClick}>{t('updateNote')}</Button>
        </Flex>
    </Flex>


});

