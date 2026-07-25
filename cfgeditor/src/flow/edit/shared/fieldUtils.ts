import type {CSSProperties} from "react";
import {EntityEditField, EntityEditFieldOptions, PrimitiveValue} from "@/domain/entityModel";
import {isNumberType} from "@/api/schemaModel";
import {FILTER_EMPTY, FILTER_SEARCH, FilterOption} from "./constants.ts";

// 类型守卫：primitive / arrayOfPrimitive / interface 三类字段可携带 autoCompleteOptions。
export function hasAutoCompleteOptions(
    field: EntityEditField
): field is EntityEditField & { autoCompleteOptions: EntityEditFieldOptions } {
    return field.type === "primitive" || field.type === "arrayOfPrimitive" || field.type === "interface";
}

// autoComplete 选项的统一谓词：类型收窄 + 选项非空（options.length > 0），命中返回选项集合，否则 undefined。
// 各消费方（默认值/展开判断/控件选择/数组项样式）共用同一判定口径，勿再各自展开 hasAutoCompleteOptions + length 判断。
export function getAutoCompleteOptions(field: EntityEditField): EntityEditFieldOptions | undefined {
    if (hasAutoCompleteOptions(field) && field.autoCompleteOptions && field.autoCompleteOptions.options.length > 0) {
        return field.autoCompleteOptions;
    }
    return undefined;
}

export function getFilter(useSearch: boolean): FilterOption {
    return useSearch ? FILTER_SEARCH : FILTER_EMPTY;
}

export function getDefaultPrimitiveValue(field: EntityEditField): PrimitiveValue {
    const {eleType} = field;

    // 有自动完成选项时使用第一个选项值
    const autoCompleteOptions = getAutoCompleteOptions(field);
    if (autoCompleteOptions) {
        return autoCompleteOptions.options[0].value as PrimitiveValue;
    }

    // 根据类型返回默认值
    switch (eleType) {
        case "bool":
            return false;
        case "int":
        case "long":
        case "float":
            return 0;
        default:
            return "";
    }
}

export function isArrayPrimitiveBoolOrNumber(field: EntityEditField): boolean {
    // 有自动完成选项时不展开
    if (getAutoCompleteOptions(field)) {
        return false;
    } else if (field.eleType == 'bool') {
        return true;
    } else {
        return isNumberType(field.eleType);
    }
}

// 字段项背景色样式：bgColor 缺省（值类无高亮）时返回空对象，否则铺色。Primitive/ArrayOfPrimitive 共用。
// 纯函数（无 hook 依赖）。调用方若需引用稳定（喂给 useMemo deps），应依赖 bgColor 而非本函数返回值——后者每次新对象。
export function fieldItemStyle(bgColor?: string): CSSProperties {
    return bgColor === undefined ? {} : {backgroundColor: bgColor};
}
