grammar Cfg ;

// Parser rules

schema :  (  struct_decl | interface_decl | table_decl )* EOF ;

struct_decl : STRUCT ns_ident metadata LC ( field_decl )* RC ;

interface_decl : INTERFACE ns_ident metadata LC ( field_decl )* RC ;

table_decl : TABLE ns_ident metadata LC ( field_decl )* RC ;


enum_decl : ENUM identifier ( COLON type_ )? metadata LC commasep_enumval_decl RC ;

union_decl : UNION identifier metadata LC commasep_unionval_with_opt_alias RC ;

root_decl : ROOT_TYPE identifier SEMI ;

field_decl : identifier COLON type_ ( EQ scalar )? metadata SEMI ;

rpc_decl : RPC_SERVICE identifier LC rpc_method+ RC ;

rpc_method : identifier LP identifier RP COLON identifier metadata SEMI ;

type_ : LB type_ ( COLON integer_const )? RB | BASE_TYPE_NAME | ns_ident ;

enumval_decl : ns_ident ( EQ integer_const )? ;

commasep_enumval_decl : enumval_decl ( COMMA enumval_decl )* COMMA? ;

unionval_with_opt_alias : ns_ident ( COLON ns_ident )? ( EQ integer_const )? ;

commasep_unionval_with_opt_alias : unionval_with_opt_alias ( COMMA unionval_with_opt_alias )* COMMA? ;

ident_with_opt_single_value : identifier ( COLON single_value )? ;

commasep_ident_with_opt_single_value : ident_with_opt_single_value ( COMMA ident_with_opt_single_value )* ;

metadata : ( LP commasep_ident_with_opt_single_value RP )? ;

scalar : INTEGER_CONSTANT | HEX_INTEGER_CONSTANT | FLOAT_CONSTANT | identifier ;

object_ : LC commasep_ident_with_value RC ;

ident_with_value : identifier COLON value ;

commasep_ident_with_value : ident_with_value ( COMMA ident_with_value )* COMMA? ;

single_value : scalar | STRING_CONSTANT ;

value : single_value | object_ | LB commasep_value RB ;

commasep_value : value( COMMA value )* COMMA? ;

file_extension_decl : FILE_EXTENSION STRING_CONSTANT SEMI ;

file_identifier_decl : FILE_IDENTIFIER STRING_CONSTANT SEMI ;

ns_ident : identifier ( DOT identifier )* ;

integer_const :  INTEGER_CONSTANT | HEX_INTEGER_CONSTANT ;

identifier: IDENT | keywords;

keywords
  : ATTRIBUTE
  | ENUM
  | FILE_EXTENSION
  | FILE_IDENTIFIER
  | INCLUDE
  | NATIVE_INCLUDE
  | NAMESPACE
  | ROOT_TYPE
  | RPC_SERVICE
  | STRUCT
  | TABLE
  | UNION
  ;

// Lexer rules

// keywords
STRUCT:     'struct';
INTERFACE:  'interface';
TABLE:      'table';

PACK:   'pack';
SEP:    'sep';
FIX:    'fix';
BLOCK:  'block';


// symbols
SEMI: ';';
EQ: '=';
LP: '(';
RP: ')';
LB: '[';
RB: ']';
LC: '{';
RC: '}';
DOT: '.';
COMMA: ',';
COLON: ':';
PLUS: '+';
MINUS: '-';

fragment
DECIMAL_DIGIT
    :   [0-9]
    ;

fragment
HEXADECIMAL_DIGIT
    :   [0-9a-fA-F]
    ;

fragment
ESCAPE_SEQUENCE
    :   SIMPLE_ESCAPE_SEQUENCE
    |   HEXADECIMAL_ESCAPE_SEQUENCE
    |	UNICODE_ESCAPE_SEQUENCE
    ;

fragment
SIMPLE_ESCAPE_SEQUENCE
    :   '\\' ['"?bfnrtv\\/]
    ;

fragment
HEXADECIMAL_ESCAPE_SEQUENCE
    :   '\\x' HEXADECIMAL_DIGIT+
    ;

fragment
UNICODE_ESCAPE_SEQUENCE
    :   '\\u' HEXADECIMAL_DIGIT+
    ;

STRING_CONSTANT
    :   '"' SCHAR_SEQUENCE? '"'
    ;

fragment
SCHAR_SEQUENCE
    :   SCHAR+
    ;

fragment
SCHAR
    :   ~["\\\r\n]
    |   ESCAPE_SEQUENCE
    ;

BASE_TYPE_NAME : 'bool' | 'byte' | 'ubyte' | 'short' | 'ushort' | 'int' | 'uint' | 'float' | 'long' | 'ulong' | 'double' | 'int8' | 'uint8' | 'int16' | 'uint16' | 'int32' | 'uint32' | 'int64' | 'uint64' | 'float32' | 'float64' | 'string' ;

INTEGER_CONSTANT : [-+]? DECIMAL_DIGIT+ | 'true' | 'false' ;

IDENT : [a-zA-Z_] [a-zA-Z0-9_]* ;

HEX_INTEGER_CONSTANT : [-+]? '0' [xX] HEXADECIMAL_DIGIT+ ;

FLOAT_CONSTANT : (PLUS|MINUS)? FLOATLIT ;

// Floating-point literals

fragment
FLOATLIT
    :   (   DECIMALS DOT DECIMALS? EXPONENT?
        |   DECIMALS EXPONENT
        |   DOT DECIMALS EXPONENT?
        )
    |   'inf'
    |   'nan'
    ;

fragment
DECIMALS
    :   DECIMAL_DIGIT+
    ;

fragment
EXPONENT
    :   ('e' | 'E') (PLUS|MINUS)? DECIMALS
    ;

BLOCK_COMMENT:	'/*' .*? '*/' -> channel(HIDDEN);

// fixed original grammar: allow line comments
COMMENT : '//' ~[\r\n]* -> channel(HIDDEN);

WS : [ \t\r\n] -> skip ;