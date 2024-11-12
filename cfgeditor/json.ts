export interface NodeShowType {
    recordLayout: NodePlacementStrategyType;
    editLayout: NodePlacementStrategyType;
    refLayout: NodePlacementStrategyType;

    tableLayout: NodePlacementStrategyType;
    tableRefLayout: NodePlacementStrategyType;

    nodeColorsByValue: KeywordColor[];
    nodeColorsByLabel: KeywordColor[];
    fieldColorsByName: KeywordColor[];
    editFoldColor: string;

    refTableHides: string[];
    refIsShowCopyable: boolean;
    refShowDescription: ShowDescriptionType;
    refContainEnum: boolean;
}

export interface KeywordColor {
    keyword: string;
    color: string;
}

export type ShowDescriptionType = 'show' | 'showFallbackValue' | 'showValue' | 'none';
export type NodePlacementStrategyType = 'SIMPLE' | 'LINEAR_SEGMENTS' | 'BRANDES_KOEPF' | "mrtree";

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
