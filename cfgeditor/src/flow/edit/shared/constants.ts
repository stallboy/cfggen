import type {EntityEditFieldOption} from "@/domain/entityModel";

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

// Select 过滤器配置
export const FILTER_EMPTY = {};

export const FILTER_SEARCH = {
    showSearch: true,
    filterOption: (input: string, option?: EntityEditFieldOption) => {
        if (!option) return false;
        return option.labelstr.toLowerCase().includes(input.toLowerCase());
    },
};

// 主题配置（FORM_THEME）已上提到 FlowGraph 单实例 ConfigProvider（原每节点一个，N=45 时 mount 开销可观），见 FlowGraph.tsx。
export const FORM_STYLE = {backgroundColor: "white", borderRadius: 15, padding: 10};
