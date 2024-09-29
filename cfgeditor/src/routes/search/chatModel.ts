export interface PromptRequest {
    role: string;
    table: string;
    examples: AIExample[];
}

export interface AIExample {
    id: string;
    description: string;
}

export interface PromptResult {
    resultCode: PromptResultCode;
    prompt: string;
}

export type PromptResultCode =
    'ok'
    | 'tableNotSet'
    | 'tableNotFound'
    | 'tableNotJson'
    | 'exampleIdParseErr'
    | 'exampleIdNotFound'
    | 'exampleDescriptionEmpty';