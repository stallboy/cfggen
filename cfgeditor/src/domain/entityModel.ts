import {BriefDescription, JSONObject} from "@/api/recordModel";
import {PrimitiveType} from "@/api/schemaModel";
import {ResInfo} from "@/domain/resInfo";
import * as React from "react";

// ============================================================================
// 基础字段类型
// ============================================================================

/**
 * 字段基础属性（所有字段的共同属性）
 */
export interface FieldBase {
    name: string;
    comment?: string;
    handleIn?: boolean;   // 用于渲染输入连接点
    handleOut?: boolean;  // 用于渲染输出连接点
}

/**
 * 显示字段（只读模式）
 * 用于 Table/Record 浏览视图
 */
export interface DisplayField extends FieldBase {
    key: string;
    value: string;
}

// ============================================================================
// 值类型定义
// ============================================================================

/**
 * 原始值类型
 */
export type PrimitiveValue = string | number | boolean;

// PrimitiveType 已下沉到 @/api/schemaModel（后端类型系统权威），此处经顶部 import 复用。

// ============================================================================
// 编辑字段类型定义
// ============================================================================

/**
 * 原始类型编辑字段
 */
export interface PrimitiveEditField extends FieldBase {
    type: 'primitive';
    eleType: PrimitiveType;
    value: PrimitiveValue;
    autoCompleteOptions?: EntityEditFieldOptions;
}

/**
 * 数组原始类型编辑字段
 */
export interface ArrayPrimitiveEditField extends FieldBase {
    type: 'arrayOfPrimitive';
    eleType: PrimitiveType;
    value: PrimitiveValue[];
    autoCompleteOptions?: EntityEditFieldOptions;
}

/**
 * 内嵌字段数据（包含所有相关信息）
 */
export interface EmbeddedFieldData {
    /** 内嵌的字段列表 */
    fields: Array<{
        value: PrimitiveValue;
        type: PrimitiveType;
        name: string;
        comment?: string;
    }>;
    /** 节点的备注（来自 entity.note） */
    note?: string;
    /** interface 的实现名称（非 defaultImpl 时显示） */
    implName?: string;
    /** 内嵌字段的完整路径 */
    embeddedFieldChain?: (string | number)[];
}

/**
 * 结构体引用编辑字段
 */
export interface StructRefEditField extends FieldBase {
    type: 'structRef';
    eleType: string;
    value: string;  // 占位符，如 '<>'

    /** 内嵌字段数据（包含 fields, note, implName, embeddedFieldChain） */
    embeddedField?: EmbeddedFieldData;
}

/**
 * list/map 折叠状态（挂在 funcAdd 字段上）。
 * fold 状态持久化在父对象的 `$fold_<fieldName>` 键上（数组本身挂不了属性），
 * 与对象级 `$fold` 同约定：随数据提交、undo/redo 自动恢复。
 */
export interface ListFoldData {
    /** 当前是否已折叠 */
    folded: boolean;
    /** 元素数（折叠态摘要行显示用） */
    itemCount: number;
    /** 折叠/展开回调：fold=true 写 `$fold_<fieldName>`，false 删键 */
    onUpdateListFold: (fold: boolean, position: EntityPosition) => void;
}

/**
 * 函数添加编辑字段（用于向数组中添加结构体）
 */
export interface FuncAddEditField extends FieldBase { // arrayOfStructural
    type: 'funcAdd';
    eleType: string;
    value: FuncAddType;
    /** list/map 折叠信息：所有会 spawn 子节点的 list/map 字段都有（折叠入口 + 折叠态摘要行） */
    listFold?: ListFoldData;
}

/**
 * 接口编辑字段
 */
export interface InterfaceEditField extends FieldBase {
    type: 'interface';
    eleType: string;
    value: string;  // 实现名称
    autoCompleteOptions: EntityEditFieldOptions;
    implFields: EntityEditField[];
    interfaceOnChangeImpl: (impl: string, position: EntityPosition) => void;
}

/**
 * 提交函数编辑字段
 */
export interface FuncSubmitEditField extends FieldBase {
    type: 'funcSubmit';
    eleType: 'bool';  // 固定值
    value: FuncSubmitType;
}

/**
 * 编辑字段联合类型
 * 使用判别联合类型（Discriminated Union）确保类型安全
 */
export type EntityEditField =
    | PrimitiveEditField
    | ArrayPrimitiveEditField
    | StructRefEditField
    | FuncAddEditField
    | InterfaceEditField
    | FuncSubmitEditField;


// ============================================================================
// 函数类型定义
// ============================================================================

/**
 * 通用函数类型
 */
export type FuncType = () => void;

/**
 * 添加函数类型
 */
export type FuncAddType = (position: EntityPosition) => void;

/**
 * 提交函数类型
 */
export interface FuncSubmitType {
    funcSubmit: FuncType;
    funcClear: FuncType;
}

// ============================================================================
// 自动完成选项类型
// ============================================================================

/**
 * 自动完成选项
 */
export interface EntityEditFieldOption {
    value: string | number;
    label?: React.ReactNode;
    labelstr: string;
    title: string;
}

/**
 * 自动完成选项集合
 */
export interface EntityEditFieldOptions {
    options: EntityEditFieldOption[];
    isValueInteger: boolean;
    isEnum: boolean;
}

// ============================================================================
// EntityEdit 接口
// ============================================================================


/**
 * EntityEdit 接口
 * 重命名：editFields -> fields
 * 保持向后兼容：保留 editOnUpdateValues 等旧属性名
 */
export interface EntityEdit {
    fields: EntityEditField[];
    editOnUpdateValues: (changed: Record<string, unknown>, allValues: Record<string, unknown>) => void;
    editOnUpdateNote: (note?: string) => void;
    editOnUpdateFold: (fold: boolean, position: EntityPosition, embeddedFieldChain?: (string | number)[]) => void;
    editOnDelete?: (position: EntityPosition) => void;
    editOnMoveUp?: (position: EntityPosition) => void;
    editOnMoveDown?: (position: EntityPosition) => void;
    editFieldChain?: (string | number)[];
    editObj?: JSONObject;
    editAllowObjType?: string;
    fold?: boolean;
    hasChild: boolean;
    canBeEmbedded?: boolean;  // 标识是否可以被内嵌
}

// ============================================================================
// Entity Position
// ============================================================================

/**
 * 实体位置
 */
export interface EntityPosition {
    id: string;
    x: number;
    y: number;
}

// ============================================================================
// Entity Brief（卡片显示）
// ============================================================================

/**
 * 实体简要信息（用于卡片显示）
 */
export interface EntityBrief {
    title?: string;
    descriptions?: BriefDescription[];
    value: string;
}

// ============================================================================
// Entity Edge Types
// ============================================================================

/**
 * 实体源边
 */
export interface EntitySourceEdge {
    sourceHandle: string;
    target: string;
    targetHandle: string;
    type: EntityEdgeType;
    label?: string;
}

/**
 * 实体边类型
 */
export enum EntityEdgeType {
    Normal,
    Ref = 1,
}

/**
 * 实体类型
 */
export enum EntityType {
    Normal,
    Ref,
    Ref2,
    RefIn,
}

// ============================================================================
// Entity 基础接口
// ============================================================================

/**
 * 实体基础属性（所有 Entity 共享）
 */
export interface EntityBase {
    id: string;
    label: string;
    sourceEdges: EntitySourceEdge[];
    entityType?: EntityType;
    note?: string;
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    userData?: any;
    assets?: ResInfo[];
    handleIn?: boolean;
    handleOut?: boolean;
}

// ============================================================================
// Entity 联合类型（判别联合）
// ============================================================================

/**
 * 只读显示型 Entity
 * 用于 Table/Record 浏览视图
 */
export interface ReadOnlyEntity extends EntityBase {
    type: 'readonly';
    fields: DisplayField[];
}

/**
 * 编辑型 Entity
 * 用于 Record 编辑视图
 */
export interface EditableEntity extends EntityBase {
    type: 'editable';
    edit: EntityEdit;
}

/**
 * 卡片型 Entity
 * 用于简短显示
 */
export interface CardEntity extends EntityBase {
    type: 'card';
    brief: EntityBrief;
}

/**
 * 统一的 Entity 联合类型
 * 使用判别属性 'type' 区分不同类型
 */
export type Entity = ReadOnlyEntity | EditableEntity | CardEntity;

// ============================================================================
// 类型守卫函数
// ============================================================================

/**
 * 判断是否为 ReadOnlyEntity
 */
export function isReadOnlyEntity(entity: Entity): entity is ReadOnlyEntity {
    return entity.type === 'readonly';
}

/**
 * 判断是否为 EditableEntity
 */
export function isEditableEntity(entity: Entity): entity is EditableEntity {
    return entity.type === 'editable';
}

/**
 * 判断是否为 CardEntity
 */
export function isCardEntity(entity: Entity): entity is CardEntity {
    return entity.type === 'card';
}


// ============================================================================
// FitView / EditingObjectRes —— 编辑态 ↔ 视图层契约
// 抽离到此，消除 flow/useEntityToGraph → routes/record/editingObject 的反向依赖
// ============================================================================

export enum EFitView {
    FitFull = 'FitFull',
    FitId = 'FitId',
    /** undo/redo 用：以 fitViewToIdPosition.id 为锚点，relayout 后让该节点屏幕坐标不变。锚点 = 被撤销操作的
     *  视觉焦点（delete 取父节点）。anchorOld 由 flow 层从「上一帧布局」读——不用 position.x/y，那是「当初操作
     *  发起时」的坐标，期间可能发生别的编辑，已过时。
     *  与 FitId 区别：anchorOld 来源不同（prevMap vs position.x/y）；与 NoChange 区别：NoChange 无论如何不动，KeepStable 布局变就补偿、不变则不动。 */
    KeepStable = 'KeepStable',
    /** 不跳视口：只读/固定页保持当前视口；值类 undo（布局不变）亦用此（不调 fitView/setViewport）。 */
    NoChange = 'NoChange',
}

export type EditingObjectRes = {
    fitView: EFitView;
    fitViewToIdPosition?: EntityPosition;
    isEdited: boolean;
}
