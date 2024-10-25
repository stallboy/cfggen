export interface PromptResult {
    resultCode: PromptResultCode;
    prompt: string; // user
    init: string;   // assistant
}

export type PromptResultCode =
    'ok'
    | 'AICfgNotSet'
    | 'tableNotSet'
    | 'tableNotFound'
    | 'promptFileNotFound';
