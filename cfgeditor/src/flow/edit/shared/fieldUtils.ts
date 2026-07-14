import type {CSSProperties} from "react";
import {EntityEditField, EntityEditFieldOptions, PrimitiveValue} from "@/domain/entityModel";
import {isNumberType} from "@/api/schemaModel";
import {FILTER_EMPTY, FILTER_SEARCH} from "./constants.ts";
import type {FilterOption} from "./types.ts";

// 类型守卫：primitive / arrayOfPrimitive / interface 三类字段可携带 autoCompleteOptions。
export function hasAutoCompleteOptions(
    field: EntityEditField
): field is EntityEditField & { autoCompleteOptions: EntityEditFieldOptions } {
    return field.type === "primitive" || field.type === "arrayOfPrimitive" || field.type === "interface";
}

export function getFilter(useSearch: boolean): FilterOption {
    return useSearch ? FILTER_SEARCH : FILTER_EMPTY;
}

export function getDefaultPrimitiveValue(field: EntityEditField): PrimitiveValue {
    const {eleType} = field;

    // 有自动完成选项时使用第一个选项值
    if (hasAutoCompleteOptions(field) && field.autoCompleteOptions?.options.length) {
        return field.autoCompleteOptions.options[0].value as PrimitiveValue;
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
    if (hasAutoCompleteOptions(field) && field.autoCompleteOptions?.options.length) {
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
