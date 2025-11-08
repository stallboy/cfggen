// Core types
export {
    ConfigFile,
    Definition,
    TextRange,
    SymbolTable,
    ParseError,
    DefinitionType,
    Reference
} from './configFile';

// Struct types
export {
    StructDefinition,
    StructMetadataExtension,
    ForeignKey as StructForeignKey
} from './structDefinition';

// Interface types
export {
    InterfaceDefinition,
    InterfaceMetadataExtension
} from './interfaceDefinition';

// Table types
export {
    TableDefinition,
    TableMetadataExtension,
    ForeignKey as TableForeignKey
} from './tableDefinition';

// Field types
export {
    FieldDefinition,
    FieldType,
    BaseType,
    ListType,
    MapType,
    CustomType,
    FieldMetadata
} from './fieldDefinition';

// Foreign key types
export {
    ForeignKeyDefinition,
    ReferenceTarget
} from './foreignKeyDefinition';

// Metadata types
export {
    Metadata,
    Literal,
    IntegerLiteral,
    HexIntegerLiteral,
    FloatLiteral,
    StringLiteral
} from './metadataDefinition';
