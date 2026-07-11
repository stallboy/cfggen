import {Entity, EntityEditField, isReadOnlyEntity, isEditableEntity, isCardEntity} from "@/domain/entityModel";
import type {NodeShowType} from "@/domain/storageJson";
import {ResInfo} from "@/domain/resInfo";
import {mayHaveResOrNote} from "@/domain/entityPredicates";
import {getDsLenAndDesc} from "./getDsLenAndDesc.ts";
import {getNodeWidth} from "./dimensions.ts";


// 在一次又一次尝试了等待node准备好，直接用node的computed理的width，height后，增加这一个异步，太容易有闪烁和被代码绕晕了。
// 放弃放弃，还是预先估算好。
//
// 高度估算魔数：均对应 antd 组件/本仓样式的实测 DOM 尺寸，喂给 ELK 当不可压缩边界框。
// ELK 不测 DOM，喂的尺寸必须与真实渲染一致，否则节点 overlap/留异常间隙——这正是这些魔数精度的意义。
// calcWidthHeight.test.ts 锁住算术，改任一常量须同步更新测试。

const NODE_BASE_H = 40;   // 标题栏 + 外层 padding 基础高度（所有节点共用起点）

// readonly（EntityProperties：antd List size='small'）
const FIELD_ROW_H = 41;   // 单个 List.Item 实测行高

// card（EntityCard：antd Card / Descriptions）
const CARD_BASE_H = 48;   // Card 容器基础
const CARD_TITLE_H = 32;  // 有 title 时 +
const CARD_DS_H = 38;     // 单条 description（Descriptions.Item）行高
const DESC_ROW_H = 22;    // desc 多行文本单行高
const IMAGE_H = 200;      // 卡片封面图高度
const DESC_CHAR_WIDTH = 8; // card desc 单个等宽位约宽 px（charsPerRow = nodeWidth / DESC_CHAR_WIDTH，240/8=30）

// editable（EntityForm：antd Form）
const EDIT_BASE_H = 20;                  // 表单 padding 基础
const EDIT_ROW_H = 40;                   // 单个 Form.Item 行高
const EDIT_FOLD_H = 16;                  // fold 折叠态额外高度
const EDIT_ARRAY_EXTRA_PER_ITEM = 8;     // arrayOfPrimitive 每项 extra
const EDIT_TEXT_ROW_H = 22;              // 长文本 primitive 每行高
const EDIT_TEXT_WRAP_COLS = 10;          // 长文本每行字符数（估算换行）
const EDIT_TEXT_EXTRA_BASE = 10;         // 长文本 extra 基础（行数>1 才计入 extra）
const EDIT_TEXT_MAX_ROWS = 10;           // 长文本最大估算行数

// notes（note TextArea）
const NOTE_ROW_H = 22;     // note 单行高
const NOTE_WRAP_COLS = 15; // note 每行字符数（估算换行）
const NOTE_MAX_ROWS = 10;  // note 最大估算行数
const NOTE_MIN_ROWS = 2;   // note 最小估算行数（即使很短也预留）
const NOTE_PADDING_H = 22; // note 区域额外 padding

export function calcWidthHeight(entity: Entity, nodeShow?: NodeShowType, notes?: Map<string, string>) {
    const {id, label} = entity;
    const width = getNodeWidth(entity, nodeShow);
    let height = NODE_BASE_H;

    if (isReadOnlyEntity(entity)) {
        height += FIELD_ROW_H * entity.fields.length;

    } else if (isCardEntity(entity)) {
        const brief = entity.brief;
        height += CARD_BASE_H + (brief.title ? CARD_TITLE_H : 0);
        const [showDsLen, desc] = getDsLenAndDesc(brief, nodeShow);
        height += showDsLen * CARD_DS_H;
        if (desc) {
            height += DESC_ROW_H * simpleStrRowCount(desc, Math.floor(width / DESC_CHAR_WIDTH));
        }
        if (findFirstImage(entity.assets)) {
            height += IMAGE_H;
        }

    } else if (isEditableEntity(entity)) {
        const edit = entity.edit;
        const [cnt, extra] = calcEditFieldsCntAndExtra(edit.fields)
        height += EDIT_BASE_H + EDIT_ROW_H * cnt + extra;
        if (edit.fold) {
            height += EDIT_FOLD_H;
        }
    }

    if (notes && mayHaveResOrNote(label)) {
        const note = notes.get(id);
        if (note) {
            let row = note.length / NOTE_WRAP_COLS;
            if (row > NOTE_MAX_ROWS) {
                row = NOTE_MAX_ROWS;
            }
            if (row < NOTE_MIN_ROWS) {
                row = NOTE_MIN_ROWS;
            }
            height += row * NOTE_ROW_H + NOTE_PADDING_H;
        }
    }

    return [width, height];

}

function calcEditFieldsCntAndExtra(editFields: EntityEditField[]) {
    let cnt = 0;
    let extra = 0;
    for (const editField of editFields) {
        switch (editField.type) {
            case "arrayOfPrimitive": {
                const len = (editField.value as never[]).length
                cnt += len + 1;
                extra += len * EDIT_ARRAY_EXTRA_PER_ITEM
                break;
            }

            case "interface":
                cnt++;
                if (editField.implFields) {
                    const [implCnt, implExtra] = calcEditFieldsCntAndExtra(editField.implFields)
                    cnt += implCnt
                    extra += implExtra
                }
                break;
            case 'primitive':
                if (editField.eleType == 'text' || editField.eleType == 'str') {
                    let row = (editField.value as string).length / EDIT_TEXT_WRAP_COLS;
                    if (row > EDIT_TEXT_MAX_ROWS) {
                        row = EDIT_TEXT_MAX_ROWS;
                    }
                    if (row > 1) {
                        extra += row * EDIT_TEXT_ROW_H + EDIT_TEXT_EXTRA_BASE;
                    } else {
                        cnt++;
                    }
                } else {
                    cnt++;
                }
                break;
            default:
                cnt++;
                break;
        }
    }
    return [cnt, extra]
}


// card desc 占用显示行数估算。
//
// 旧实现用 charCodeAt + `code > 255 ? 2 : 1` + 固定 30 字符换行，三个缺陷：
//   (a) 代理对（emoji）两个 code unit 都 > 255 → 单个 emoji 被计成宽度 4；
//   (b) Latin Extended/Cyrillic 等 > 255 的窄字符被误计为 2；
//   (c) 固定 30 与 nodeWidth 解耦。
//
// 现：按字形(grapheme)/码点迭代 + East Asian Width 表（CJK/全角=2，其余=1）+ charsPerRow 从 nodeWidth 派生。
// 优先 Intl.Segmenter（精确处理 ZWJ emoji 族）；worker 不支持时降级 for...of（仍正确处理代理对，避免 (a)）。
// 模块加载即构造 Segmenter 失败也不抛（try/catch → null → 走码点降级），不会拖垮 layout worker。
const segmenter: Intl.Segmenter | null = (() => {
    try {
        return new Intl.Segmenter('en', {granularity: 'grapheme'});
    } catch {
        return null;
    }
})();

// East Asian Width 宽字符判定（这些码点占 2 个等宽位）。覆盖 CJK 统一/扩展、假名、谚文、全角等。
function isWideCode(code: number): boolean {
    return (
        (code >= 0x1100 && code <= 0x115F) ||   // Hangul Jamo
        (code >= 0x2E80 && code <= 0x303E) ||   // CJK Radicals / Kangxi / CJK 标点
        (code >= 0x3040 && code <= 0x33BF) ||   // Hiragana / Katakana / CJK Symbols
        (code >= 0x3400 && code <= 0x4DBF) ||   // CJK Unified Extension A
        (code >= 0x4E00 && code <= 0x9FFF) ||   // CJK Unified Ideographs
        (code >= 0xA000 && code <= 0xA4CF) ||   // Yi
        (code >= 0xAC00 && code <= 0xD7A3) ||   // Hangul Syllables
        (code >= 0xF900 && code <= 0xFAFF) ||   // CJK Compat Ideographs
        (code >= 0xFE30 && code <= 0xFE4F) ||   // CJK Compat Forms
        (code >= 0xFF00 && code <= 0xFF60) ||   // Fullwidth Forms
        (code >= 0xFFE0 && code <= 0xFFE6) ||   // Fullwidth Signs
        (code >= 0x1F300 && code <= 0x1FAFF) || // Emoji / 符号（宽）
        (code >= 0x20000 && code <= 0x2FFFD) || // CJK Extension B-F
        (code >= 0x30000 && code <= 0x3FFFD)    // CJK Extension G+
    );
}

export function simpleStrRowCount(str: string, charsPerRow = 30): number {
    let len = 0;
    let row = 0;
    const consume = (g: string) => {
        // 用 includes('\n') 而非 === '\n'：Intl.Segmenter 按 UAX#29 GB3 把 \r\n 合并成 1 个
        // grapheme（segment 串为 '\r\n'），=== 比对会漏掉 Windows 换行 → 多行 CJK desc 行数低估。
        if (g.includes('\n')) {
            row++;
            len = 0;
            return;
        }
        len += isWideCode(g.codePointAt(0) ?? 0) ? 2 : 1;
        if (len >= charsPerRow) {
            row++;
            len = 0;
        }
    };
    if (segmenter) {
        for (const {segment} of segmenter.segment(str)) consume(segment);
    } else {
        for (const ch of str) consume(ch);  // 码点迭代：代理对算 1 个码点（emoji 不再被计成 4）
    }
    return row;
}

export function findFirstImage(assets: ResInfo[] | undefined): string | undefined {
    if (assets) {
        for (const r of assets) {
            if (r.type == 'image') {
                return r.path;
            }
        }
    }
}
