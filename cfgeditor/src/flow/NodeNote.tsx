import {useCallback, useMemo, useState} from "react";
import type {ReactNode} from "react";
import {Button, Tooltip} from "antd";
import {BookOutlined} from "@ant-design/icons";
import {useTranslation} from "react-i18next";
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
// 为何用 hook 返回 JSX（看似「hook 吐组件」的怪写法）而非组件：editNoteButton 是作为 prop 注入兄弟组件 <NodeTitle>
// （非直接渲染），noteBlock 才直接摆放——这种「产出一个值喂兄弟组件 + 一块 DOM 自己摆」的混合需求，hook 返回 {ReactNode}
// 比 render-prop 更精确（后者得把 <NodeTitle> 整体搬进 children 函数）。属 headless-hook 写法，有意为之，非反模式。
//
// 只读/card 态：NoteShow/NoteEdit 走 useMutation → updateNote API；无 note 时由 editNoteButton 触发编辑。
// 编辑态：NoteEditInner + 本地 tmpNote，走 edit.editOnUpdateNote（不触网，先写 tmpNote，提交时再落 json）。
export function useNodeNote({id, entity, edit, note, notes, label}: UseNodeNoteArgs): {
    noteBlock: ReactNode;
    editNoteButton: ReactNode;
} {
    const {t} = useTranslation();
    const [isEditNote, setIsEditNote] = useState<boolean>(false);
    const [tmpNote, setTmpNote] = useState<TempNote | undefined>();

    const onEditNote = useCallback(() => {
        setIsEditNote(true);
    }, []);

    // 编辑态下点"添加 note"：留空 tmpNote.note，由 NoteEditInner 的 placeholder 提示。
    // 原预填字面量 "note：" 会被当真实内容提交进 json（用户不清空时）。editingTmp（tmpNote set）
    // 作为"显示编辑器"的信号，取代原"note 长度>0 才显示"的 hack——内容空也能显示空编辑器。
    // 记录 tmpNote 时的 entity：切换不同 record 时 key 可能相同，note 不能误用。
    const onEditNoteClickInEdit = useCallback(() => {
        setTmpNote({note: "", entity});
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
                return <Tooltip title={t('addNote')}>
                    <Button style={iconButtonStyle} icon={bookIcon} aria-label={t('addNote')} onClick={onEditNote} />
                </Tooltip>;
            }
        }
        // edit 态：用户已点"添加"(tmpNote set) 或已有 note → 隐藏 book 图标（noteBlock 的编辑器接管）。
        const editingTmp = !!(tmpNote && tmpNote.entity === entity);
        const hasNote = !!(note && note.length > 0);

        if (edit && !editingTmp && !hasNote) {
            return <Tooltip title={t('addNote')}>
                <Button style={iconButtonStyle} icon={bookIcon} aria-label={t('addNote')} onClick={onEditNoteClickInEdit} />
            </Tooltip>;
        }

        return null;
    }, [notes, id, isEditNote, note, edit, tmpNote, entity, label, t, onEditNote, onEditNoteClickInEdit]);

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
            // editingTmp：用户点过"添加 note"（tmpNote set，内容即便为空也显示编辑器，靠 placeholder 提示）。
            // 无 tmpNote 但有 json note：直接进编辑器编辑该 note。
            const editingTmp = !!(tmpNote && tmpNote.entity === entity);
            let showNote: string | undefined;
            if (editingTmp) {
                showNote = tmpNote!.note;
            } else if (note && note.length > 0) {
                showNote = note;
            }
            if (editingTmp || (note && note.length > 0)) {
                return <NoteEditInner note={showNote ?? ''} updateNoteInEdit={updateNoteInEdit} />;
            }
        } else if (note) {
            return <NoteShowInner note={note} />;
        }

        return null;
    }, [label, edit, note, notes, id, isEditNote, tmpNote, entity, updateNoteInEdit]);

    return {noteBlock, editNoteButton};
}
