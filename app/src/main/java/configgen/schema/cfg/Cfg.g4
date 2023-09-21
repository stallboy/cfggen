grammar Cfg ;

// Parser rules

schema : schema_ele* EOF ;

schema_ele: struct_decl | interface_decl | table_decl ;

struct_decl : 'struct' ns_ident metadata COMMENT?  LC  field_decl* foreign_decl*  RC ;

interface_decl : 'interface' ns_ident metadata COMMENT? LC struct_decl+ RC ;

table_decl : 'table' ns_ident key metadata COMMENT? LC key_decl* field_decl* foreign_decl*  RC ;

field_decl : identifier COLON type_ ( ref )? metadata SEMI COMMENT? ;

foreign_decl: '->' identifier COLON key ref metadata SEMI COMMENT? ;

key_decl : key SEMI ;

ref:  ('->' | '=>') ns_ident key? ;

key: '[' identifier (',' identifier)* ']' ;

COMMENT: '//' ~[\r\n]* ;

type_ : 'list<' type_ele '>' |  'map<' type_ele ','  type_ele '>' | type_ele;

type_ele : BASE_TYPE_NAME | ns_ident;

BASE_TYPE_NAME : 'bool' | 'int' | 'long' | 'float' | 'str' | 'res' | 'text' ;

metadata : ( LP commasep_ident_with_opt_single_value RP )? ;

commasep_ident_with_opt_single_value : ident_with_opt_single_value ( COMMA ident_with_opt_single_value )* ;

ident_with_opt_single_value : identifier ( COLON single_value )? ;

single_value : INTEGER_CONSTANT | HEX_INTEGER_CONSTANT | FLOAT_CONSTANT | STRING_CONSTANT ;

ns_ident : identifier ( DOT identifier )* ;

identifier: IDENT ;


// Lexer rules

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
    :   '\'' SCHAR_SEQUENCE? '\''
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

// fixed original grammar: allow line comments
//COMMENT : '//' ~[\r\n]* ;

WS : [ \t\r\n] -> skip ;