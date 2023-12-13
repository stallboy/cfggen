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
    string: string;
    max: number;
    items: SearchResultItem[];
}