// 实体谓词：集中那些"靠 label 文本形态判断 entity 类别"的隐式约定，
// 避免散落多处的魔法判定（无注释）在重构中被误改。

/**
 * 该 label 是否可能挂载资源(res)或笔记(note)。
 *
 * 约定：record 类实体的 label 形如 `表名_记录ID`（含下划线）——只有这类"记录实体"
 * 才会在 resMap 里查到资源、在 notes Map 里查到笔记。表结构(table)等实体的 label
 * 不含 '_'，不参与资源/笔记查找。
 *
 * 调用点：FlowNode（是否渲染 note 编辑入口/资源按钮）、calcWidthHeight（是否计 note 行高）、
 * findAllResInfos（是否查 resMap）。统一在此判定，避免 4 处复制 `label.includes('_')`。
 */
export function mayHaveResOrNote(label: string): boolean {
    return label.includes('_');
}
