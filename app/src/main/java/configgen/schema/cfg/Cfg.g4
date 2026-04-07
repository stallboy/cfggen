grammar Cfg;

// ======================================================
// Parser Rules (语法规则) - 自顶向下组织
// ======================================================

// 1. Root Rule
schema
    : schema_ele* suffix_comment* EOF
    ;

schema_ele
    : struct_decl
    | interface_decl
    | table_decl
    | enum_decl
    ;

// 2. High-level Structures (Struct, Interface, Table)
struct_decl
    : leading_comment* STRUCT ns_ident metadata
      LC_COMMENT (field_decl | foreign_decl)*
      suffix_comment* RC
    ;

interface_decl
    : leading_comment* INTERFACE ns_ident metadata
      LC_COMMENT struct_decl+
      suffix_comment* RC
    ;

table_decl
    : leading_comment* TABLE ns_ident key metadata
      LC_COMMENT (field_decl | foreign_decl | key_decl)+
      suffix_comment* RC
    ;

enum_decl
    : leading_comment* ENUM ns_ident metadata
      LC_COMMENT ( enum_value_empty+ | enum_value_assigned+ )?
      suffix_comment* RC
    ;
// 3. Members & Fields (字段与定义)
field_decl
    : leading_comment* identifier COLON type_ ref? metadata SEMI_COMMENT
    ;

foreign_decl
    : leading_comment* REF identifier COLON key ref metadata SEMI_COMMENT
    ;

key_decl
    : leading_comment* key SEMI_COMMENT
    ;

enum_value_empty
    : leading_comment* identifier SEMI_COMMENT
    ;

// 已赋值的枚举字段
enum_value_assigned
    : leading_comment* identifier EQ enum_number SEMI_COMMENT
    ;

// 4. Types System (类型系统)
type_
    : TLIST '<' type_ele '>'              # TypeList
    | TMAP '<' type_ele ',' type_ele '>'  # TypeMap
    | type_ele                            # TypeBasic
    ;

type_ele
    : TBASE
    | ns_ident
    ;

// 5. References & Keys (引用与键)
ref
    : (REF | LISTREF) ns_ident key?
    ;

key
    : '[' identifier (',' identifier)* ']'
    ;

// 6. Metadata & Attributes (元数据与属性)
metadata
    : ( LP ident_with_opt_single_value ( COMMA ident_with_opt_single_value )* RP )?
    ;

ident_with_opt_single_value
    : identifier (EQ single_value)?
    | minus_ident
    ;

minus_ident
    : MINUS identifier
    ;

single_value
    : INTEGER_CONSTANT
    | HEX_INTEGER_CONSTANT
    | FLOAT_CONSTANT
    | STRING_CONSTANT
    | BOOL_CONSTANT
    ;

enum_number
    : INTEGER_CONSTANT
    | HEX_INTEGER_CONSTANT
    ;

// 7. Common Utilities (通用工具)
ns_ident
    : identifier ( DOT identifier )* ;

identifier
    : IDENT
    | STRUCT
    | INTERFACE
    | TABLE
    | ENUM
    | TLIST
    | TMAP
    | TBASE
    ;

// 命名标签规则，用于区分声明前注释和后缀注释
leading_comment
    : COMMENT
    ;

suffix_comment
    : COMMENT
    ;


// ======================================================
// Lexer Rules (词法规则) - 优先级非常重要
// ======================================================

// 1. Keywords (关键字) - 必须放在 IDENT 之前
STRUCT      : 'struct';
INTERFACE   : 'interface';
TABLE       : 'table';
ENUM        : 'enum';
TLIST       : 'list';
TMAP        : 'map';

// 基础类型关键字
TBASE
    : 'bool'
    | 'int'
    | 'long'
    | 'float'
    | 'str'
    | 'text'
    ;

// 2. Operators & Symbols (运算符与符号)
REF         : '->';
LISTREF     : '=>';
EQ          : '=';
LP          : '(';
RP          : ')';
LB          : '[';
RB          : ']';
RC          : '}';
DOT         : '.';
COMMA       : ',';
COLON       : ':';
PLUS        : '+';
MINUS       : '-';

// { 和 ; 及其可选的同行注释（必须与符号在同一行）
LC_COMMENT  : '{' [ \t]* ('//' ~[\r\n]*)?
            ;
SEMI_COMMENT: ';' [ \t]* ('//' ~[\r\n]*)?
            ;

// 3. Boolean Literals (布尔字面量)
BOOL_CONSTANT
    : 'true'
    | 'false'
    ;

// 4. Numeric Literals (数字字面量)
// 将 Fragment 放在使用它们的规则附近或文件底部
FLOAT_CONSTANT
    : (PLUS|MINUS)? FLOATLIT
    ;

HEX_INTEGER_CONSTANT
    : [-+]? '0' [xX] HEXADECIMAL_DIGIT+
    ;

INTEGER_CONSTANT
    : [-+]? DECIMAL_DIGIT+
    ;

// 5. String Literals (字符串字面量)
STRING_CONSTANT
    : '\'' SCHAR_SEQUENCE? '\''
    ;

// 6. Identifiers (标识符) - 放在关键字之后，避免关键字被识别为变量名
IDENT
    : [a-zA-Z_] [a-zA-Z0-9_]* ;

// 7. Comments & Whitespace (注释与空白)
// 如果你需要把注释保留在语法树中给 Parser 使用，不要加 -> skip
COMMENT
    : '//' ~[\r\n]* ;

WS
    : [ \t\r\n] -> skip
    ;


// ======================================================
// Fragments (词法片段) - 辅助 Lexer 规则
// ======================================================

fragment FLOATLIT
    : ( DECIMALS DOT DECIMALS? EXPONENT?
      | DECIMALS EXPONENT
      | DOT DECIMALS EXPONENT?
      )
    ;

fragment DECIMALS
    : DECIMAL_DIGIT+
    ;

fragment EXPONENT
    : ('e' | 'E') (PLUS|MINUS)? DECIMALS
    ;

fragment DECIMAL_DIGIT
    : [0-9]
    ;

fragment HEXADECIMAL_DIGIT
    : [0-9a-fA-F]
    ;

fragment SCHAR_SEQUENCE
    : SCHAR+
    ;

fragment SCHAR
    : ~['\\\r\n]
    | ESCAPE_SEQUENCE
    ;

fragment ESCAPE_SEQUENCE
    : SIMPLE_ESCAPE_SEQUENCE
    | HEXADECIMAL_ESCAPE_SEQUENCE
    | UNICODE_ESCAPE_SEQUENCE
    ;

fragment SIMPLE_ESCAPE_SEQUENCE
    : '\\' ['"?bfnrtv\\/]
    ;

fragment HEXADECIMAL_ESCAPE_SEQUENCE
    : '\\x' HEXADECIMAL_DIGIT+
    ;

fragment UNICODE_ESCAPE_SEQUENCE
    : '\\u' HEXADECIMAL_DIGIT+
    ;