import {useCallback, useMemo, useState} from "react";
import type {ReactNode} from "react";
import {Button} from "antd";
import {BookOutlined} from "@ant-design/icons";
import {Entity, EntityEdit} from "@/domain/entityModel";
import {mayHaveResOrNote} from "@/domain/entityPredicates";
import {NoteEdit, NoteEditInner, NoteShow, NoteShowInner} from "./NoteShowOrEdit.tsx";

const bookIcon = <BookOutlined />;
const iconButtonStyle = { borderWidth: 0, backgroundColor: 'transparent' };

interface TempNote {
    note: string;
    entity: Entity;
}

interface UseNodeNoteArgs {
    id: string;
    entity: Entity;
    edit?: EntityEdit;
    note?: string;
    notes?: Map<string, string>;
    label: string;
}

// note 双模式封装（来自原 FlowNode 的 editNoteButton/noteShowOrEdit 两个大 useMemo + isEditNote/tmpNote 状态）。
// 产物分两块 DOM：editNoteButton 渲染于 title 栏（无 note 时的"添加"触发器），noteBlock 渲染于节点顶部（note 内容区），
// 二者互斥显示但共享 isEditNote/tmpNote 状态，故用 hook 收口——FlowNode 调一次拿到两块 ReactNode，无需为共享状态提升到父级。
//
// 只读/card 态：NoteShow/NoteEdit 走 useMutation → updateNote API；无 note 时由 editNoteButton 触发编辑。
// 编辑态：NoteEditInner + 本地 tmpNote，走 edit.editOnUpdateNote（不触网，先写 tmpNote，提交时再落 json）。
export function useNodeNote({id, entity, edit, note, notes, label}: UseNodeNoteArgs): {
    noteBlock: ReactNode;
    editNoteButton: ReactNode;
} {
    const [isEditNote, setIsEditNote] = useState<boolean>(false);
    const [tmpNote, setTmpNote] = useState<TempNote | undefined>();

    const onEditNote = useCallback(() => {
        setIsEditNote(true);
    }, []);

    // 编辑态下设置 note：先写 tmpNote（本地，不会直接网络请求去 update json）。
    // 同时记录 tmpNote 时的 entity，因为切换不同 record 时可能 key 相同，此时 note 不能误用。
    const onEditNoteClickInEdit = useCallback(() => {
        setTmpNote({note: "note：", entity});
    }, [entity]);

    const updateNoteInEdit = useCallback((noteValue: string) => {
        if (edit?.editOnUpdateNote) {
            edit.editOnUpdateNote(noteValue);
            setTmpNote({note: noteValue, entity});
        }
    }, [edit, entity]);

    const editNoteButton = useMemo(() => {
        if (mayHaveResOrNote(label) && !edit) {
            const recordNote = notes?.get(id) ?? '';
            if (!((recordNote.length > 0) || isEditNote) && !note) {
                return <Button style={iconButtonStyle} icon={bookIcon} onClick={onEditNote} />;
            }
        }
        const hasExistingNote =
            (tmpNote && tmpNote.entity === entity && tmpNote.note.length > 0) ||
            (note && note.length > 0);

        if (edit && !hasExistingNote) {
            return <Button style={iconButtonStyle} icon={bookIcon} onClick={onEditNoteClickInEdit} />;
        }

        return null;
    }, [notes, id, isEditNote, note, edit, tmpNote, entity, label, onEditNote, onEditNoteClickInEdit]);

    const noteBlock = useMemo(() => {
        if (mayHaveResOrNote(label)) {
            const recordNote = notes?.get(id) ?? '';
            if ((recordNote.length > 0) || isEditNote) {
                if (isEditNote) {
                    return <NoteEdit id={id} note={recordNote} setIsEdit={setIsEditNote} />;
                } else {
                    return <NoteShow note={recordNote} setIsEdit={setIsEditNote} />;
                }
            } else if (note) {
                return <NoteShowInner note={note} />;
            }
        }

        if (edit) {
            let showNote;
            if (tmpNote && tmpNote.entity === entity) { // 有设置过就用它了
                if (tmpNote.note.length > 0) {
                    showNote = tmpNote.note;
                } // else {} 这样允许设置为空后不显示，虽然此时json里有note
            } else if (note && note.length > 0) {
                showNote = note;
            }
            if (showNote) {
                return <NoteEditInner note={showNote} updateNoteInEdit={updateNoteInEdit} />;
            }
        } else if (note) {
            return <NoteShowInner note={note} />;
        }

        return null;
    }, [label, edit, note, notes, id, isEditNote, tmpNote, entity, updateNoteInEdit]);

    return {noteBlock, editNoteButton};
}
