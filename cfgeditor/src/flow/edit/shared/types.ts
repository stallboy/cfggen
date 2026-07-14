import type {EntityEditField, EntityEditFieldOption} from "@/domain/entityModel";

// Select/AutoComplete 的过滤器配置类型（CustomAutoComplete 与 fieldUtils.getFilter 共用）。
export interface FilterOption {
    filterSort?: (a: EntityEditFieldOption, b: EntityEditFieldOption) => number;
    showSearch?: boolean;
    filterOption?: (input: string, option?: EntityEditFieldOption) => boolean;
}

// Primitive/ArrayOfPrimitive/FuncSubmit 三类字段项组件共用的 props。
export interface PrimitiveFormItemProps {
    field: EntityEditField;
    bgColor?: string;
}
