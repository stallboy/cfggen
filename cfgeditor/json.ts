export interface NodeShowType {
    showHead: ShowHeadType;
    showDescription: ShowDescriptionType;
    containEnum: boolean;
    nodePlacementStrategy: NodePlacementStrategyType;
    keywordColors: KeywordColor[];
    tableHideAndColors: TableHideAndColor[];

    fieldColors: KeywordColor[];
}

export interface KeywordColor {
    keyword: string;
    color: string;
}

export interface TableHideAndColor {
    keyword: string;
    hide: boolean;
    color: string;
}

export type ShowHeadType = 'show' | 'showCopyable';
export type ShowDescriptionType = 'show' | 'showFallbackValue' | 'showValue' | 'none';
export type NodePlacementStrategyType = 'SIMPLE' | 'LINEAR_SEGMENTS' | 'BRANDES_KOEPF' | "mrtree";;

export interface FixedPage {
    label: string; // 显示
    table: string;
    id: string;
    refIn: boolean;
    refOutDepth: number;
    maxNode: number;
    nodeShow: NodeShowType;
}

export interface FixedPagesConf {
    pages: FixedPage[];
}

export interface ResDir {
    dir: string;
    txtAsSrt?: boolean;
    lang?: string;
}

export interface TauriConf {
    resDirs: ResDir[];
    assetDir: string;
    assetRefTable: string;
}

export interface AIConf {
    baseUrl: string;
    apiKey: string;
    model: string;
}
