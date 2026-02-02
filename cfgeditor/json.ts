export interface NodeShowType {
    recordLayout: NodePlacementStrategyType;
    editLayout: NodePlacementStrategyType;
    refLayout: NodePlacementStrategyType;

    tableLayout: NodePlacementStrategyType;
    tableRefLayout: NodePlacementStrategyType;

    nodeColorsByValue: KeywordColor[];
    nodeColorsByLabel: KeywordColor[];
    fieldColorsByName: KeywordColor[];

    refTableHides: string[];
    refIsShowCopyable: boolean;
    refShowDescription: ShowDescriptionType;
    refContainEnum: boolean;

    // Flow Visualization Properties
    nodeWidth: number;           // Default: 240px
    editNodeWidth: number;       // Default: 280px
    nodeColor: string;
    nodeRefColor: string;
    nodeRef2Color: string;
    nodeRefInColor: string;
    edgeColor: string;           // Default: '#0898b5'
    edgeStrokeWidth: number;     // Default: 3
    editFoldColor:      string;
    mrtreeSpacing: number;       // Default: 100
    layeredSpacing: number;      // Default: 60
    layeredNodeSpacing: number;  // Default: 80
}

export interface KeywordColor {
    keyword: string;
    color: string;
}

export type ShowDescriptionType = 'show' | 'showFallbackValue' | 'showValue' | 'none';
export type NodePlacementStrategyType = 'SIMPLE' | 'LINEAR_SEGMENTS' | 'BRANDES_KOEPF' | "mrtree";

export interface FixedRefPage {
    label: string; // 显示
    table: string;
    id: string;
    refIn: boolean;
    refOutDepth: number;
    maxNode: number;
    nodeShow: NodeShowType;
}

export interface FixedUnrefPage {
    label: string;
    table: string;
    refOutDepth: number;
    maxNode: number;
    nodeShow: NodeShowType;
}

export type FixedPage = FixedRefPage | FixedUnrefPage;

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

export interface ThemeConfig {
    themeFile?: string;
}