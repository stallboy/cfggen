import type {ArrayPrimitiveEditField, EntityEditFieldOption, FuncSubmitEditField, PrimitiveEditField} from "@/domain/entityModel";

export const FORM_LAYOUT = {
    labelCol: {xs: {span: 24}, sm: {span: 6}},
    wrapperCol: {xs: {span: 24}, sm: {span: 18}},
};

export const FORM_ITEM_LAYOUT_WITHOUT_LABEL = {
    wrapperCol: {
        xs: {span: 24, offset: 0},
        sm: {span: 18, offset: 6},
    },
};

export const AUTO_COMPLETE_ITEM_STYLE = {style: {width: 170}};

// Select/AutoComplete 的过滤器配置类型（CustomAutoComplete 与 fieldUtils.getFilter 共用）。
export interface FilterOption {
    filterSort?: (a: EntityEditFieldOption, b: EntityEditFieldOption) => number;
    showSearch?: boolean;
    filterOption?: (input: string, option?: EntityEditFieldOption) => boolean;
}

// Select 过滤器配置
export const FILTER_EMPTY: FilterOption = {};

export const FILTER_SEARCH: FilterOption = {
    showSearch: true,
    filterOption: (input: string, option?: EntityEditFieldOption) => {
        if (!option) return false;
        return option.labelstr.toLowerCase().includes(input.toLowerCase());
    },
};

// 主题配置（FORM_THEME）已上提到 FlowGraph 单实例 ConfigProvider（原每节点一个，N=45 时 mount 开销可观），见 FlowGraph.tsx。
export const FORM_STYLE = {backgroundColor: "white", borderRadius: 15, padding: 10};


// 各字段项组件的 props：field 用具体字段类型（而非 EntityEditField 联合），
// 这样组件内访问 value/autoCompleteOptions 不依赖「联合所有成员都有该属性」的巧合。
export interface PrimitiveFormItemProps {
    field: PrimitiveEditField;
    bgColor?: string;
}

export interface ArrayPrimitiveFormItemProps {
    field: ArrayPrimitiveEditField;
    bgColor?: string;
}

export interface FuncSubmitFormItemProps {
    field: FuncSubmitEditField;
}
