import {BriefDescription, JSONObject} from "../routes/record/recordModel.ts";
import {NodeShowType} from "../store/storageJson.ts";
import {ResInfo} from "../res/resInfo.ts";
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

/**
 * 原始数据类型
 */
export type PrimitiveType = 'bool' | 'int' | 'long' | 'float' | 'str' | 'text';

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
 * 函数添加编辑字段（用于向数组中添加结构体）
 */
export interface FuncAddEditField extends FieldBase { // arrayOfStructural
    type: 'funcAdd';
    eleType: string;
    value: FuncAddType;
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
    editOnUpdateValues: (values: Record<string, unknown>) => void;
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
    sharedSetting?: EntitySharedSetting;
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
// Entity Graph
// ============================================================================

/**
 * 实体图
 */
export interface EntityGraph {
    entityMap: Map<string, Entity>;
    sharedSetting?: EntitySharedSetting;
}

// ============================================================================
// Entity Shared Setting
// ============================================================================

/**
 * 实体共享设置
 */
export interface EntitySharedSetting {
    notes?: Map<string, string>;
    query?: string;
    nodeShow?: NodeShowType;
}
