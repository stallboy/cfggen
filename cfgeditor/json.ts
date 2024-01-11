export interface NodeShowType {
    showHead: ShowHeadType;
    showDescription: ShowDescriptionType;
    containEnum: boolean;
    nodePlacementStrategy: NodePlacementStrategyType;
    keywordColors: KeywordColor[];
    tableColors: KeywordColor[];
}

export interface KeywordColor {
    keyword: string;
    color: string;
}

export type ShowHeadType = 'show' | 'showCopyable';
export type ShowDescriptionType = 'show' | 'showFallbackValue' | 'showValue' | 'none';
export type NodePlacementStrategyType = 'SIMPLE' | 'LINEAR_SEGMENTS' | 'BRANDES_KOEPF';


export interface FixedPage {
    table: string;
    id: string;
    refIn: boolean;
    refOutDepth: number;
    maxNode: number;
    nodeShow: NodeShowType;
}