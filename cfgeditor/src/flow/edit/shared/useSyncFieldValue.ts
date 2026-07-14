import {useEffect} from "react";
import type {FormInstance} from "antd";

/**
 * 同步外部 field.value 到 antd Form 字段（Form.Item 的 initialValue 仅在字段首次注册时生效，
 * 父级以 key={field.name} 复用实例时新 initialValue 被忽略，故命令式同步）。
 *
 * 依赖 field 引用（entityMap 每次重算都是新对象）而非仅 field.value——保证 undo/redo 后 effect 必跑、
 * 重新评估是否需要同步。但只在 antd Form 内部值与 field.value 不一致时才 setFieldValue：
 *
 * - 值类 undo 是必须 set 的场景：值类编辑不重算 entityMap（性能契约1），field.value 快照停在旧值、
 *   而 antd Form 内部已被用户输入改到新值；undo 让 editingObject 回旧值、entityMap 重算出
 *   field.value=旧值，此时 getFieldValue(新值) !== field.value(旧值) → set，同步成功。
 * - 一致的字段跳过：entityMap 重算时多数字段的 value 并未变化（如不相关的结构编辑）。若也 set，
 *   @rc-component/form 的 setField 分支对路径匹配字段是无条件 forceUpdate（不比较值，Field.js:239），
 *   会让这些字段多一次额外重渲。getFieldValue 比较把它挡掉——结构编辑只重渲真正变化的字段，
 *   不会"所有 form item 全刷"。
 *
 * setFieldValue by design 不触发 onValuesChange，不会回流污染 coalescing 栈。
 */
export function useSyncFieldValue(form: FormInstance, field: { name: string; value: unknown }) {
    useEffect(() => {
        if (form.getFieldValue(field.name) !== field.value) {
            form.setFieldValue(field.name, field.value);
        }
    }, [form, field]);
}
