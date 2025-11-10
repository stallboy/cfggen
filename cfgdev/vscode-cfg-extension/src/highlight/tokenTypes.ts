/**
 * Shared token types for semantic highlighting
 * These types must match the definitions in package.json
 */

// Token type names for VSCode semantic tokens legend
export const TOKEN_TYPE_NAMES: string[] = [
    'structureDefinition',  // 0: struct/interface/table names
    'typeIdentifier',       // 1: custom types (non-basic)
    'foreignKey',           // 2: foreign key references
    'metadata',             // 3: metadata keywords
    'primaryKey',           // 4: primary key fields
    'uniqueKey'             // 5: unique key fields
];

// Token type constants for internal use
export const TOKEN_TYPES = {
    STRUCTURE_DEFINITION: 0,  // struct/interface/table names
    TYPE_IDENTIFIER: 1,       // custom types and generic types
    FOREIGN_KEY: 2,           // foreign key references (->xxx)
    METADATA: 3,              // metadata keywords
    PRIMARY_KEY: 4,           // primary key field names
    UNIQUE_KEY: 5             // unique key field names
} as const;

export type TokenType = keyof typeof TOKEN_TYPES;