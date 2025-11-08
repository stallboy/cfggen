/**
 * TextMate Grammar Scope Mappings
 * Defines how TextMate scopes map to semantic token types for two-layer highlighting
 */

export interface TextMateScopeMapping {
    keywords: string;
    strings: string;
    numbers: string;
    comments: string;
    operators: string;
    punctuation: string;
    definitions: string;
    variables: string;
    customTypes: string;
}

/**
 * TextMate scope definitions for CFG language
 * These scopes are used in the TextMate grammar file
 */
export const TEXTMATE_SCOPES: TextMateScopeMapping = {
    keywords: 'keyword.control.cfg',
    strings: 'string.quoted.double.cfg',
    numbers: 'constant.numeric.cfg',
    comments: 'comment.line.double-slash.cfg',
    operators: 'keyword.operator.cfg',
    punctuation: 'punctuation.cfg',
    definitions: 'meta.definition.cfg',
    variables: 'variable.name.cfg',
    customTypes: 'entity.name.type.custom.cfg'
};

/**
 * Maps TextMate scopes to semantic token types
 * This bridge allows the semantic layer to understand TextMate scopes
 */
export const SCOPE_TO_SEMANTIC_MAP: Record<string, number> = {
    [TEXTMATE_SCOPES.keywords]: 0,           // structureDefinition
    [TEXTMATE_SCOPES.definitions]: 0,        // structureDefinition
    [TEXTMATE_SCOPES.customTypes]: 1,        // typeIdentifier
    [TEXTMATE_SCOPES.variables]: 2,          // fieldName
    [TEXTMATE_SCOPES.operators]: 3,          // foreignKey
    [TEXTMATE_SCOPES.comments]: 4,           // comment
    [TEXTMATE_SCOPES.keywords]: 5,           // metadata (for control keywords)
    [TEXTMATE_SCOPES.numbers]: 6,            // primaryKey (when appropriate)
    [TEXTMATE_SCOPES.strings]: 7             // uniqueKey (when appropriate)
};

/**
 * Get semantic token type from TextMate scope
 */
export function getSemanticTypeFromScope(scope: string): number {
    return SCOPE_TO_SEMANTIC_MAP[scope] ?? -1;
}

/**
 * Check if a scope is a TextMate scope
 */
export function isTextMateScope(scope: string): boolean {
    return scope.startsWith('source.cfg');
}
