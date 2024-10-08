export interface PromptRequest {
    role: string;
    table: string;
    examples: AIExample[];
    explains: string[];
}

export interface AIExample {
    id: string;
    description: string;
}

export interface PromptResult {
    resultCode: PromptResultCode;
    prompt: string;
    answer: string;
}

export type PromptResultCode =
    'ok'
    | 'tableNotSet'
    | 'tableNotFound'
    | 'tableNotJson'
    | 'exampleIdParseErr'
    | 'exampleIdNotFound'
    | 'exampleDescriptionEmpty';
