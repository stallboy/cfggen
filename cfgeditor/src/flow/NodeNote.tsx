import {useCallback, useMemo, useState} from "react";
import type {ReactNode} from "react";
import {Button} from "antd";
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

    // 有效 note：tmpNote 已为当前 entity 编辑过（含清空）→ 用 tmpNote；否则才用 json note。
    // 用它统一"编辑器是否显示"与"添加按钮是否显示"——两者互为非，避免清空已有 note 后
    // note prop（json，提交前 stale）仍非空导致添加按钮不出现。
    // tmpNote.entity 匹配校验避免切换 record 后 key 相同时 note 误用。
    const editingTmp = !!(tmpNote && tmpNote.entity === entity);
    const effectiveNote = editingTmp ? tmpNote!.note : (note ?? '');
    // 编辑器显示 = 正在草拟新 note（点添加后 drafting，内容可空）或有效 note 非空。
    const noteEditorVisible = (editingTmp && noteDrafting) || effectiveNote.length > 0;

    const editNoteButton = useMemo(() => {
        if (mayHaveResOrNote(label) && !edit) {
            const recordNote = notes?.get(id) ?? '';
            if (!((recordNote.length > 0) || isEditNote) && !note) {
                return <Button style={iconButtonStyle} icon={bookIcon} aria-label={t('addNote')} onClick={onEditNote} />;
            }
        }
        // edit 态：编辑器不可见（无草拟、有效 note 空）→ 显示"添加"按钮（与 noteBlock 互为非）。
        if (edit && !noteEditorVisible) {
            return <Button style={iconButtonStyle} icon={bookIcon} aria-label={t('addNote')} onClick={onEditNoteClickInEdit} />;
        }

        return null;
    }, [notes, id, isEditNote, note, edit, label, noteEditorVisible, t, onEditNote, onEditNoteClickInEdit]);

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
            // 编辑器显示条件与 editNoteButton 互为非（共用 noteEditorVisible/effectiveNote）：
            // 草拟中（drafting，内容可空）或有效 note 非空才显示；清空即隐去。
            if (noteEditorVisible) {
                return <NoteEditInner note={effectiveNote} updateNoteInEdit={updateNoteInEdit} />;
            }
        } else if (note) {
            return <NoteShowInner note={note} />;
        }

        return null;
    }, [label, edit, note, notes, id, isEditNote, noteEditorVisible, effectiveNote, updateNoteInEdit]);

    return {noteBlock, editNoteButton};
}
