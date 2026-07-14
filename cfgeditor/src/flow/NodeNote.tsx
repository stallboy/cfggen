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
    // noteDrafting：用户点了"添加 note"后置 true，用于让"空编辑器"显示出来。
    //
    // 背景：edit 态的 note 编辑器显示条件与"清空即消失"规则有内在冲突——
    //   - 点击添加后要让空编辑器出现（用户要往里输）；
    //   - 但用户把内容清空时又要让编辑器消失（隐式取消，原产品规则）。
    //   两者 tmpNote.note 都是 ""，纯数据无法区分。原代码靠预填字面量 "note："（非空）让点击添加后显示，
    //   清空（删到 length 0）即消失——但 "note：" 会被当真实内容提交进 json。
    //   现用 noteDrafting 标志区分这两种"空"：点击添加=drafting true（空也显示）；onChange 收到空串=drafting false（消失）。
    //   不预填任何脏内容，靠 placeholder 提示。
    const [noteDrafting, setNoteDrafting] = useState<boolean>(false);

    const onEditNote = useCallback(() => {
        setIsEditNote(true);
    }, []);

    // 编辑态点"添加 note"：置 drafting，tmpNote 留空（placeholder 提示），编辑器据此显示。
    const onEditNoteClickInEdit = useCallback(() => {
        setNoteDrafting(true);
        setTmpNote({note: "", entity});
    }, [entity]);

    const updateNoteInEdit = useCallback((noteValue: string) => {
        if (edit?.editOnUpdateNote) {
            edit.editOnUpdateNote(noteValue);
        }
        setTmpNote({note: noteValue, entity});
        if (noteValue === "") {
            // 清空 = 取消草拟：drafting 落 false，编辑器按"清空即消失"规则隐去（恢复原产品行为）。
            setNoteDrafting(false);
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
        // edit 态：编辑器正在显示（drafting 或有内容）或已有 json note → 隐藏"添加"按钮。
        const editingTmp = !!(tmpNote && tmpNote.entity === entity);
        const editingActive = editingTmp && (noteDrafting || tmpNote!.note.length > 0);
        const hasNote = !!(note && note.length > 0);

        if (edit && !editingActive && !hasNote) {
            return <Tooltip title={t('addNote')}>
                <Button style={iconButtonStyle} icon={bookIcon} aria-label={t('addNote')} onClick={onEditNoteClickInEdit} />
            </Tooltip>;
        }

        return null;
    }, [notes, id, isEditNote, note, edit, tmpNote, entity, label, noteDrafting, t, onEditNote, onEditNoteClickInEdit]);

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
            // 显示条件：tmpNote 已设（编辑中）→ drafting 或有内容才显示（清空即消失）；
            //          否则 → 有 json note 才显示（直接编辑已有 note）。
            // tmpNote.entity 匹配校验避免切换 record 后 key 相同时 note 误用。
            const editingTmp = !!(tmpNote && tmpNote.entity === entity);
            const showNote = editingTmp ? tmpNote!.note : (note ?? '');
            const showEditor = editingTmp
                ? (noteDrafting || tmpNote!.note.length > 0)
                : !!(note && note.length > 0);
            if (showEditor) {
                return <NoteEditInner note={showNote} updateNoteInEdit={updateNoteInEdit} />;
            }
        } else if (note) {
            return <NoteShowInner note={note} />;
        }

        return null;
    }, [label, edit, note, notes, id, isEditNote, tmpNote, entity, noteDrafting, updateNoteInEdit]);

    return {noteBlock, editNoteButton};
}
