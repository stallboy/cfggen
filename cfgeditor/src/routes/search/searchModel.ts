export type SearchResultCode =
    'ok'
    | 'qNotSet'

export interface SearchResultItem {
    table: string;
    pk: string; // id
    fieldChain: string;
    value: string;
}

export interface SearchResult {
    resultCode: SearchResultCode;
    q: string;
    max: number;
    items: SearchResultItem[];
}