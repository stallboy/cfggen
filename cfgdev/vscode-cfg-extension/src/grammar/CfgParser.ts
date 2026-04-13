
import * as antlr from "antlr4ng";
import { Token } from "antlr4ng";

import { CfgVisitor } from "./CfgVisitor.js";

// for running tests with parameters, TODO: discuss strategy for typed parameters in CI
// eslint-disable-next-line no-unused-vars
type int = number;


export class CfgParser extends antlr.Parser {
    public static readonly T__0 = 1;
    public static readonly T__1 = 2;
    public static readonly STRUCT = 3;
    public static readonly INTERFACE = 4;
    public static readonly TABLE = 5;
    public static readonly ENUM = 6;
    public static readonly TLIST = 7;
    public static readonly TMAP = 8;
    public static readonly TBASE = 9;
    public static readonly REF = 10;
    public static readonly LISTREF = 11;
    public static readonly EQ = 12;
    public static readonly LP = 13;
    public static readonly RP = 14;
    public static readonly LB = 15;
    public static readonly RB = 16;
    public static readonly RC = 17;
    public static readonly DOT = 18;
    public static readonly COMMA = 19;
    public static readonly COLON = 20;
    public static readonly PLUS = 21;
    public static readonly MINUS = 22;
    public static readonly LC_COMMENT = 23;
    public static readonly SEMI_COMMENT = 24;
    public static readonly BOOL_CONSTANT = 25;
    public static readonly FLOAT_CONSTANT = 26;
    public static readonly HEX_INTEGER_CONSTANT = 27;
    public static readonly INTEGER_CONSTANT = 28;
    public static readonly STRING_CONSTANT = 29;
    public static readonly IDENT = 30;
    public static readonly COMMENT = 31;
    public static readonly WS = 32;
    public static readonly RULE_schema = 0;
    public static readonly RULE_schema_ele = 1;
    public static readonly RULE_struct_decl = 2;
    public static readonly RULE_interface_decl = 3;
    public static readonly RULE_table_decl = 4;
    public static readonly RULE_enum_decl = 5;
    public static readonly RULE_field_decl = 6;
    public static readonly RULE_foreign_decl = 7;
    public static readonly RULE_key_decl = 8;
    public static readonly RULE_enum_value_empty = 9;
    public static readonly RULE_enum_value_assigned = 10;
    public static readonly RULE_type_ = 11;
    public static readonly RULE_type_ele = 12;
    public static readonly RULE_ref = 13;
    public static readonly RULE_key = 14;
    public static readonly RULE_metadata = 15;
    public static readonly RULE_ident_with_opt_single_value = 16;
    public static readonly RULE_minus_ident = 17;
    public static readonly RULE_single_value = 18;
    public static readonly RULE_enum_number = 19;
    public static readonly RULE_ns_ident = 20;
    public static readonly RULE_identifier = 21;
    public static readonly RULE_leading_comment = 22;
    public static readonly RULE_suffix_comment = 23;

    public static readonly literalNames = [
        null, "'<'", "'>'", "'struct'", "'interface'", "'table'", "'enum'", 
        "'list'", "'map'", null, "'->'", "'=>'", "'='", "'('", "')'", "'['", 
        "']'", "'}'", "'.'", "','", "':'", "'+'", "'-'"
    ];

    public static readonly symbolicNames = [
        null, null, null, "STRUCT", "INTERFACE", "TABLE", "ENUM", "TLIST", 
        "TMAP", "TBASE", "REF", "LISTREF", "EQ", "LP", "RP", "LB", "RB", 
        "RC", "DOT", "COMMA", "COLON", "PLUS", "MINUS", "LC_COMMENT", "SEMI_COMMENT", 
        "BOOL_CONSTANT", "FLOAT_CONSTANT", "HEX_INTEGER_CONSTANT", "INTEGER_CONSTANT", 
        "STRING_CONSTANT", "IDENT", "COMMENT", "WS"
    ];
    public static readonly ruleNames = [
        "schema", "schema_ele", "struct_decl", "interface_decl", "table_decl", 
        "enum_decl", "field_decl", "foreign_decl", "key_decl", "enum_value_empty", 
        "enum_value_assigned", "type_", "type_ele", "ref", "key", "metadata", 
        "ident_with_opt_single_value", "minus_ident", "single_value", "enum_number", 
        "ns_ident", "identifier", "leading_comment", "suffix_comment",
    ];

    public get grammarFileName(): string { return "Cfg.g4"; }
    public get literalNames(): (string | null)[] { return CfgParser.literalNames; }
    public get symbolicNames(): (string | null)[] { return CfgParser.symbolicNames; }
    public get ruleNames(): string[] { return CfgParser.ruleNames; }
    public get serializedATN(): number[] { return CfgParser._serializedATN; }

    protected createFailedPredicateException(predicate?: string, message?: string): antlr.FailedPredicateException {
        return new antlr.FailedPredicateException(this, predicate, message);
    }

    public constructor(input: antlr.TokenStream) {
        super(input);
        this.interpreter = new antlr.ParserATNSimulator(this, CfgParser._ATN, CfgParser.decisionsToDFA, new antlr.PredictionContextCache());
    }
    public schema(): SchemaContext {
        let localContext = new SchemaContext(this.context, this.state);
        this.enterRule(localContext, 0, CfgParser.RULE_schema);
        let _la: number;
        try {
            let alternative: number;
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 51;
            this.errorHandler.sync(this);
            alternative = this.interpreter.adaptivePredict(this.tokenStream, 0, this.context);
            while (alternative !== 2 && alternative !== antlr.ATN.INVALID_ALT_NUMBER) {
                if (alternative === 1) {
                    {
                    {
                    this.state = 48;
                    this.schema_ele();
                    }
                    }
                }
                this.state = 53;
                this.errorHandler.sync(this);
                alternative = this.interpreter.adaptivePredict(this.tokenStream, 0, this.context);
            }
            this.state = 57;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 31) {
                {
                {
                this.state = 54;
                this.suffix_comment();
                }
                }
                this.state = 59;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 60;
            this.match(CfgParser.EOF);
            }
        }
        catch (re) {
            if (re instanceof antlr.RecognitionException) {
                this.errorHandler.reportError(this, re);
                this.errorHandler.recover(this, re);
            } else {
                throw re;
            }
        }
        finally {
            this.exitRule();
        }
        return localContext;
    }
    public schema_ele(): Schema_eleContext {
        let localContext = new Schema_eleContext(this.context, this.state);
        this.enterRule(localContext, 2, CfgParser.RULE_schema_ele);
        try {
            this.state = 66;
            this.errorHandler.sync(this);
            switch (this.interpreter.adaptivePredict(this.tokenStream, 2, this.context) ) {
            case 1:
                this.enterOuterAlt(localContext, 1);
                {
                this.state = 62;
                this.struct_decl();
                }
                break;
            case 2:
                this.enterOuterAlt(localContext, 2);
                {
                this.state = 63;
                this.interface_decl();
                }
                break;
            case 3:
                this.enterOuterAlt(localContext, 3);
                {
                this.state = 64;
                this.table_decl();
                }
                break;
            case 4:
                this.enterOuterAlt(localContext, 4);
                {
                this.state = 65;
                this.enum_decl();
                }
                break;
            }
        }
        catch (re) {
            if (re instanceof antlr.RecognitionException) {
                this.errorHandler.reportError(this, re);
                this.errorHandler.recover(this, re);
            } else {
                throw re;
            }
        }
        finally {
            this.exitRule();
        }
        return localContext;
    }
    public struct_decl(): Struct_declContext {
        let localContext = new Struct_declContext(this.context, this.state);
        this.enterRule(localContext, 4, CfgParser.RULE_struct_decl);
        let _la: number;
        try {
            let alternative: number;
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 71;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 31) {
                {
                {
                this.state = 68;
                this.leading_comment();
                }
                }
                this.state = 73;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 74;
            this.match(CfgParser.STRUCT);
            this.state = 75;
            this.ns_ident();
            this.state = 76;
            this.metadata();
            this.state = 77;
            this.match(CfgParser.LC_COMMENT);
            this.state = 82;
            this.errorHandler.sync(this);
            alternative = this.interpreter.adaptivePredict(this.tokenStream, 5, this.context);
            while (alternative !== 2 && alternative !== antlr.ATN.INVALID_ALT_NUMBER) {
                if (alternative === 1) {
                    {
                    this.state = 80;
                    this.errorHandler.sync(this);
                    switch (this.interpreter.adaptivePredict(this.tokenStream, 4, this.context) ) {
                    case 1:
                        {
                        this.state = 78;
                        this.field_decl();
                        }
                        break;
                    case 2:
                        {
                        this.state = 79;
                        this.foreign_decl();
                        }
                        break;
                    }
                    }
                }
                this.state = 84;
                this.errorHandler.sync(this);
                alternative = this.interpreter.adaptivePredict(this.tokenStream, 5, this.context);
            }
            this.state = 88;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 31) {
                {
                {
                this.state = 85;
                this.suffix_comment();
                }
                }
                this.state = 90;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 91;
            this.match(CfgParser.RC);
            }
        }
        catch (re) {
            if (re instanceof antlr.RecognitionException) {
                this.errorHandler.reportError(this, re);
                this.errorHandler.recover(this, re);
            } else {
                throw re;
            }
        }
        finally {
            this.exitRule();
        }
        return localContext;
    }
    public interface_decl(): Interface_declContext {
        let localContext = new Interface_declContext(this.context, this.state);
        this.enterRule(localContext, 6, CfgParser.RULE_interface_decl);
        let _la: number;
        try {
            let alternative: number;
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 96;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 31) {
                {
                {
                this.state = 93;
                this.leading_comment();
                }
                }
                this.state = 98;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 99;
            this.match(CfgParser.INTERFACE);
            this.state = 100;
            this.ns_ident();
            this.state = 101;
            this.metadata();
            this.state = 102;
            this.match(CfgParser.LC_COMMENT);
            this.state = 104;
            this.errorHandler.sync(this);
            alternative = 1;
            do {
                switch (alternative) {
                case 1:
                    {
                    {
                    this.state = 103;
                    this.struct_decl();
                    }
                    }
                    break;
                default:
                    throw new antlr.NoViableAltException(this);
                }
                this.state = 106;
                this.errorHandler.sync(this);
                alternative = this.interpreter.adaptivePredict(this.tokenStream, 8, this.context);
            } while (alternative !== 2 && alternative !== antlr.ATN.INVALID_ALT_NUMBER);
            this.state = 111;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 31) {
                {
                {
                this.state = 108;
                this.suffix_comment();
                }
                }
                this.state = 113;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 114;
            this.match(CfgParser.RC);
            }
        }
        catch (re) {
            if (re instanceof antlr.RecognitionException) {
                this.errorHandler.reportError(this, re);
                this.errorHandler.recover(this, re);
            } else {
                throw re;
            }
        }
        finally {
            this.exitRule();
        }
        return localContext;
    }
    public table_decl(): Table_declContext {
        let localContext = new Table_declContext(this.context, this.state);
        this.enterRule(localContext, 8, CfgParser.RULE_table_decl);
        let _la: number;
        try {
            let alternative: number;
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 119;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 31) {
                {
                {
                this.state = 116;
                this.leading_comment();
                }
                }
                this.state = 121;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 122;
            this.match(CfgParser.TABLE);
            this.state = 123;
            this.ns_ident();
            this.state = 124;
            this.key();
            this.state = 125;
            this.metadata();
            this.state = 126;
            this.match(CfgParser.LC_COMMENT);
            this.state = 130;
            this.errorHandler.sync(this);
            alternative = 1;
            do {
                switch (alternative) {
                case 1:
                    {
                    this.state = 130;
                    this.errorHandler.sync(this);
                    switch (this.interpreter.adaptivePredict(this.tokenStream, 11, this.context) ) {
                    case 1:
                        {
                        this.state = 127;
                        this.field_decl();
                        }
                        break;
                    case 2:
                        {
                        this.state = 128;
                        this.foreign_decl();
                        }
                        break;
                    case 3:
                        {
                        this.state = 129;
                        this.key_decl();
                        }
                        break;
                    }
                    }
                    break;
                default:
                    throw new antlr.NoViableAltException(this);
                }
                this.state = 132;
                this.errorHandler.sync(this);
                alternative = this.interpreter.adaptivePredict(this.tokenStream, 12, this.context);
            } while (alternative !== 2 && alternative !== antlr.ATN.INVALID_ALT_NUMBER);
            this.state = 137;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 31) {
                {
                {
                this.state = 134;
                this.suffix_comment();
                }
                }
                this.state = 139;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 140;
            this.match(CfgParser.RC);
            }
        }
        catch (re) {
            if (re instanceof antlr.RecognitionException) {
                this.errorHandler.reportError(this, re);
                this.errorHandler.recover(this, re);
            } else {
                throw re;
            }
        }
        finally {
            this.exitRule();
        }
        return localContext;
    }
    public enum_decl(): Enum_declContext {
        let localContext = new Enum_declContext(this.context, this.state);
        this.enterRule(localContext, 10, CfgParser.RULE_enum_decl);
        let _la: number;
        try {
            let alternative: number;
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 145;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 31) {
                {
                {
                this.state = 142;
                this.leading_comment();
                }
                }
                this.state = 147;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 148;
            this.match(CfgParser.ENUM);
            this.state = 149;
            this.ns_ident();
            this.state = 150;
            this.metadata();
            this.state = 151;
            this.match(CfgParser.LC_COMMENT);
            this.state = 162;
            this.errorHandler.sync(this);
            switch (this.interpreter.adaptivePredict(this.tokenStream, 17, this.context) ) {
            case 1:
                {
                this.state = 153;
                this.errorHandler.sync(this);
                alternative = 1;
                do {
                    switch (alternative) {
                    case 1:
                        {
                        {
                        this.state = 152;
                        this.enum_value_empty();
                        }
                        }
                        break;
                    default:
                        throw new antlr.NoViableAltException(this);
                    }
                    this.state = 155;
                    this.errorHandler.sync(this);
                    alternative = this.interpreter.adaptivePredict(this.tokenStream, 15, this.context);
                } while (alternative !== 2 && alternative !== antlr.ATN.INVALID_ALT_NUMBER);
                }
                break;
            case 2:
                {
                this.state = 158;
                this.errorHandler.sync(this);
                alternative = 1;
                do {
                    switch (alternative) {
                    case 1:
                        {
                        {
                        this.state = 157;
                        this.enum_value_assigned();
                        }
                        }
                        break;
                    default:
                        throw new antlr.NoViableAltException(this);
                    }
                    this.state = 160;
                    this.errorHandler.sync(this);
                    alternative = this.interpreter.adaptivePredict(this.tokenStream, 16, this.context);
                } while (alternative !== 2 && alternative !== antlr.ATN.INVALID_ALT_NUMBER);
                }
                break;
            }
            this.state = 167;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 31) {
                {
                {
                this.state = 164;
                this.suffix_comment();
                }
                }
                this.state = 169;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 170;
            this.match(CfgParser.RC);
            }
        }
        catch (re) {
            if (re instanceof antlr.RecognitionException) {
                this.errorHandler.reportError(this, re);
                this.errorHandler.recover(this, re);
            } else {
                throw re;
            }
        }
        finally {
            this.exitRule();
        }
        return localContext;
    }
    public field_decl(): Field_declContext {
        let localContext = new Field_declContext(this.context, this.state);
        this.enterRule(localContext, 12, CfgParser.RULE_field_decl);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 175;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 31) {
                {
                {
                this.state = 172;
                this.leading_comment();
                }
                }
                this.state = 177;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 178;
            this.identifier();
            this.state = 179;
            this.match(CfgParser.COLON);
            this.state = 180;
            this.type_();
            this.state = 182;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            if (_la === 10 || _la === 11) {
                {
                this.state = 181;
                this.ref();
                }
            }

            this.state = 184;
            this.metadata();
            this.state = 185;
            this.match(CfgParser.SEMI_COMMENT);
            }
        }
        catch (re) {
            if (re instanceof antlr.RecognitionException) {
                this.errorHandler.reportError(this, re);
                this.errorHandler.recover(this, re);
            } else {
                throw re;
            }
        }
        finally {
            this.exitRule();
        }
        return localContext;
    }
    public foreign_decl(): Foreign_declContext {
        let localContext = new Foreign_declContext(this.context, this.state);
        this.enterRule(localContext, 14, CfgParser.RULE_foreign_decl);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 190;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 31) {
                {
                {
                this.state = 187;
                this.leading_comment();
                }
                }
                this.state = 192;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 193;
            this.match(CfgParser.REF);
            this.state = 194;
            this.identifier();
            this.state = 195;
            this.match(CfgParser.COLON);
            this.state = 196;
            this.key();
            this.state = 197;
            this.ref();
            this.state = 198;
            this.metadata();
            this.state = 199;
            this.match(CfgParser.SEMI_COMMENT);
            }
        }
        catch (re) {
            if (re instanceof antlr.RecognitionException) {
                this.errorHandler.reportError(this, re);
                this.errorHandler.recover(this, re);
            } else {
                throw re;
            }
        }
        finally {
            this.exitRule();
        }
        return localContext;
    }
    public key_decl(): Key_declContext {
        let localContext = new Key_declContext(this.context, this.state);
        this.enterRule(localContext, 16, CfgParser.RULE_key_decl);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 204;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 31) {
                {
                {
                this.state = 201;
                this.leading_comment();
                }
                }
                this.state = 206;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 207;
            this.key();
            this.state = 208;
            this.match(CfgParser.SEMI_COMMENT);
            }
        }
        catch (re) {
            if (re instanceof antlr.RecognitionException) {
                this.errorHandler.reportError(this, re);
                this.errorHandler.recover(this, re);
            } else {
                throw re;
            }
        }
        finally {
            this.exitRule();
        }
        return localContext;
    }
    public enum_value_empty(): Enum_value_emptyContext {
        let localContext = new Enum_value_emptyContext(this.context, this.state);
        this.enterRule(localContext, 18, CfgParser.RULE_enum_value_empty);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 213;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 31) {
                {
                {
                this.state = 210;
                this.leading_comment();
                }
                }
                this.state = 215;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 216;
            this.identifier();
            this.state = 217;
            this.match(CfgParser.SEMI_COMMENT);
            }
        }
        catch (re) {
            if (re instanceof antlr.RecognitionException) {
                this.errorHandler.reportError(this, re);
                this.errorHandler.recover(this, re);
            } else {
                throw re;
            }
        }
        finally {
            this.exitRule();
        }
        return localContext;
    }
    public enum_value_assigned(): Enum_value_assignedContext {
        let localContext = new Enum_value_assignedContext(this.context, this.state);
        this.enterRule(localContext, 20, CfgParser.RULE_enum_value_assigned);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 222;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 31) {
                {
                {
                this.state = 219;
                this.leading_comment();
                }
                }
                this.state = 224;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 225;
            this.identifier();
            this.state = 226;
            this.match(CfgParser.EQ);
            this.state = 227;
            this.enum_number();
            this.state = 228;
            this.match(CfgParser.SEMI_COMMENT);
            }
        }
        catch (re) {
            if (re instanceof antlr.RecognitionException) {
                this.errorHandler.reportError(this, re);
                this.errorHandler.recover(this, re);
            } else {
                throw re;
            }
        }
        finally {
            this.exitRule();
        }
        return localContext;
    }
    public type_(): Type_Context {
        let localContext = new Type_Context(this.context, this.state);
        this.enterRule(localContext, 22, CfgParser.RULE_type_);
        try {
            this.state = 243;
            this.errorHandler.sync(this);
            switch (this.interpreter.adaptivePredict(this.tokenStream, 25, this.context) ) {
            case 1:
                localContext = new TypeListContext(localContext);
                this.enterOuterAlt(localContext, 1);
                {
                this.state = 230;
                this.match(CfgParser.TLIST);
                this.state = 231;
                this.match(CfgParser.T__0);
                this.state = 232;
                this.type_ele();
                this.state = 233;
                this.match(CfgParser.T__1);
                }
                break;
            case 2:
                localContext = new TypeMapContext(localContext);
                this.enterOuterAlt(localContext, 2);
                {
                this.state = 235;
                this.match(CfgParser.TMAP);
                this.state = 236;
                this.match(CfgParser.T__0);
                this.state = 237;
                this.type_ele();
                this.state = 238;
                this.match(CfgParser.COMMA);
                this.state = 239;
                this.type_ele();
                this.state = 240;
                this.match(CfgParser.T__1);
                }
                break;
            case 3:
                localContext = new TypeBasicContext(localContext);
                this.enterOuterAlt(localContext, 3);
                {
                this.state = 242;
                this.type_ele();
                }
                break;
            }
        }
        catch (re) {
            if (re instanceof antlr.RecognitionException) {
                this.errorHandler.reportError(this, re);
                this.errorHandler.recover(this, re);
            } else {
                throw re;
            }
        }
        finally {
            this.exitRule();
        }
        return localContext;
    }
    public type_ele(): Type_eleContext {
        let localContext = new Type_eleContext(this.context, this.state);
        this.enterRule(localContext, 24, CfgParser.RULE_type_ele);
        try {
            this.state = 247;
            this.errorHandler.sync(this);
            switch (this.interpreter.adaptivePredict(this.tokenStream, 26, this.context) ) {
            case 1:
                this.enterOuterAlt(localContext, 1);
                {
                this.state = 245;
                this.match(CfgParser.TBASE);
                }
                break;
            case 2:
                this.enterOuterAlt(localContext, 2);
                {
                this.state = 246;
                this.ns_ident();
                }
                break;
            }
        }
        catch (re) {
            if (re instanceof antlr.RecognitionException) {
                this.errorHandler.reportError(this, re);
                this.errorHandler.recover(this, re);
            } else {
                throw re;
            }
        }
        finally {
            this.exitRule();
        }
        return localContext;
    }
    public ref(): RefContext {
        let localContext = new RefContext(this.context, this.state);
        this.enterRule(localContext, 26, CfgParser.RULE_ref);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 249;
            _la = this.tokenStream.LA(1);
            if(!(_la === 10 || _la === 11)) {
            this.errorHandler.recoverInline(this);
            }
            else {
                this.errorHandler.reportMatch(this);
                this.consume();
            }
            this.state = 250;
            this.ns_ident();
            this.state = 252;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            if (_la === 15) {
                {
                this.state = 251;
                this.key();
                }
            }

            }
        }
        catch (re) {
            if (re instanceof antlr.RecognitionException) {
                this.errorHandler.reportError(this, re);
                this.errorHandler.recover(this, re);
            } else {
                throw re;
            }
        }
        finally {
            this.exitRule();
        }
        return localContext;
    }
    public key(): KeyContext {
        let localContext = new KeyContext(this.context, this.state);
        this.enterRule(localContext, 28, CfgParser.RULE_key);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 254;
            this.match(CfgParser.LB);
            this.state = 255;
            this.identifier();
            this.state = 260;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 19) {
                {
                {
                this.state = 256;
                this.match(CfgParser.COMMA);
                this.state = 257;
                this.identifier();
                }
                }
                this.state = 262;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 263;
            this.match(CfgParser.RB);
            }
        }
        catch (re) {
            if (re instanceof antlr.RecognitionException) {
                this.errorHandler.reportError(this, re);
                this.errorHandler.recover(this, re);
            } else {
                throw re;
            }
        }
        finally {
            this.exitRule();
        }
        return localContext;
    }
    public metadata(): MetadataContext {
        let localContext = new MetadataContext(this.context, this.state);
        this.enterRule(localContext, 30, CfgParser.RULE_metadata);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 276;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            if (_la === 13) {
                {
                this.state = 265;
                this.match(CfgParser.LP);
                this.state = 266;
                this.ident_with_opt_single_value();
                this.state = 271;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
                while (_la === 19) {
                    {
                    {
                    this.state = 267;
                    this.match(CfgParser.COMMA);
                    this.state = 268;
                    this.ident_with_opt_single_value();
                    }
                    }
                    this.state = 273;
                    this.errorHandler.sync(this);
                    _la = this.tokenStream.LA(1);
                }
                this.state = 274;
                this.match(CfgParser.RP);
                }
            }

            }
        }
        catch (re) {
            if (re instanceof antlr.RecognitionException) {
                this.errorHandler.reportError(this, re);
                this.errorHandler.recover(this, re);
            } else {
                throw re;
            }
        }
        finally {
            this.exitRule();
        }
        return localContext;
    }
    public ident_with_opt_single_value(): Ident_with_opt_single_valueContext {
        let localContext = new Ident_with_opt_single_valueContext(this.context, this.state);
        this.enterRule(localContext, 32, CfgParser.RULE_ident_with_opt_single_value);
        let _la: number;
        try {
            this.state = 284;
            this.errorHandler.sync(this);
            switch (this.tokenStream.LA(1)) {
            case CfgParser.STRUCT:
            case CfgParser.INTERFACE:
            case CfgParser.TABLE:
            case CfgParser.ENUM:
            case CfgParser.TLIST:
            case CfgParser.TMAP:
            case CfgParser.TBASE:
            case CfgParser.IDENT:
                this.enterOuterAlt(localContext, 1);
                {
                this.state = 278;
                this.identifier();
                this.state = 281;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
                if (_la === 12) {
                    {
                    this.state = 279;
                    this.match(CfgParser.EQ);
                    this.state = 280;
                    this.single_value();
                    }
                }

                }
                break;
            case CfgParser.MINUS:
                this.enterOuterAlt(localContext, 2);
                {
                this.state = 283;
                this.minus_ident();
                }
                break;
            default:
                throw new antlr.NoViableAltException(this);
            }
        }
        catch (re) {
            if (re instanceof antlr.RecognitionException) {
                this.errorHandler.reportError(this, re);
                this.errorHandler.recover(this, re);
            } else {
                throw re;
            }
        }
        finally {
            this.exitRule();
        }
        return localContext;
    }
    public minus_ident(): Minus_identContext {
        let localContext = new Minus_identContext(this.context, this.state);
        this.enterRule(localContext, 34, CfgParser.RULE_minus_ident);
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 286;
            this.match(CfgParser.MINUS);
            this.state = 287;
            this.identifier();
            }
        }
        catch (re) {
            if (re instanceof antlr.RecognitionException) {
                this.errorHandler.reportError(this, re);
                this.errorHandler.recover(this, re);
            } else {
                throw re;
            }
        }
        finally {
            this.exitRule();
        }
        return localContext;
    }
    public single_value(): Single_valueContext {
        let localContext = new Single_valueContext(this.context, this.state);
        this.enterRule(localContext, 36, CfgParser.RULE_single_value);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 289;
            _la = this.tokenStream.LA(1);
            if(!((((_la) & ~0x1F) === 0 && ((1 << _la) & 1040187392) !== 0))) {
            this.errorHandler.recoverInline(this);
            }
            else {
                this.errorHandler.reportMatch(this);
                this.consume();
            }
            }
        }
        catch (re) {
            if (re instanceof antlr.RecognitionException) {
                this.errorHandler.reportError(this, re);
                this.errorHandler.recover(this, re);
            } else {
                throw re;
            }
        }
        finally {
            this.exitRule();
        }
        return localContext;
    }
    public enum_number(): Enum_numberContext {
        let localContext = new Enum_numberContext(this.context, this.state);
        this.enterRule(localContext, 38, CfgParser.RULE_enum_number);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 291;
            _la = this.tokenStream.LA(1);
            if(!(_la === 27 || _la === 28)) {
            this.errorHandler.recoverInline(this);
            }
            else {
                this.errorHandler.reportMatch(this);
                this.consume();
            }
            }
        }
        catch (re) {
            if (re instanceof antlr.RecognitionException) {
                this.errorHandler.reportError(this, re);
                this.errorHandler.recover(this, re);
            } else {
                throw re;
            }
        }
        finally {
            this.exitRule();
        }
        return localContext;
    }
    public ns_ident(): Ns_identContext {
        let localContext = new Ns_identContext(this.context, this.state);
        this.enterRule(localContext, 40, CfgParser.RULE_ns_ident);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 293;
            this.identifier();
            this.state = 298;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 18) {
                {
                {
                this.state = 294;
                this.match(CfgParser.DOT);
                this.state = 295;
                this.identifier();
                }
                }
                this.state = 300;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            }
        }
        catch (re) {
            if (re instanceof antlr.RecognitionException) {
                this.errorHandler.reportError(this, re);
                this.errorHandler.recover(this, re);
            } else {
                throw re;
            }
        }
        finally {
            this.exitRule();
        }
        return localContext;
    }
    public identifier(): IdentifierContext {
        let localContext = new IdentifierContext(this.context, this.state);
        this.enterRule(localContext, 42, CfgParser.RULE_identifier);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 301;
            _la = this.tokenStream.LA(1);
            if(!((((_la) & ~0x1F) === 0 && ((1 << _la) & 1073742840) !== 0))) {
            this.errorHandler.recoverInline(this);
            }
            else {
                this.errorHandler.reportMatch(this);
                this.consume();
            }
            }
        }
        catch (re) {
            if (re instanceof antlr.RecognitionException) {
                this.errorHandler.reportError(this, re);
                this.errorHandler.recover(this, re);
            } else {
                throw re;
            }
        }
        finally {
            this.exitRule();
        }
        return localContext;
    }
    public leading_comment(): Leading_commentContext {
        let localContext = new Leading_commentContext(this.context, this.state);
        this.enterRule(localContext, 44, CfgParser.RULE_leading_comment);
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 303;
            this.match(CfgParser.COMMENT);
            }
        }
        catch (re) {
            if (re instanceof antlr.RecognitionException) {
                this.errorHandler.reportError(this, re);
                this.errorHandler.recover(this, re);
            } else {
                throw re;
            }
        }
        finally {
            this.exitRule();
        }
        return localContext;
    }
    public suffix_comment(): Suffix_commentContext {
        let localContext = new Suffix_commentContext(this.context, this.state);
        this.enterRule(localContext, 46, CfgParser.RULE_suffix_comment);
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 305;
            this.match(CfgParser.COMMENT);
            }
        }
        catch (re) {
            if (re instanceof antlr.RecognitionException) {
                this.errorHandler.reportError(this, re);
                this.errorHandler.recover(this, re);
            } else {
                throw re;
            }
        }
        finally {
            this.exitRule();
        }
        return localContext;
    }

    public static readonly _serializedATN: number[] = [
        4,1,32,308,2,0,7,0,2,1,7,1,2,2,7,2,2,3,7,3,2,4,7,4,2,5,7,5,2,6,7,
        6,2,7,7,7,2,8,7,8,2,9,7,9,2,10,7,10,2,11,7,11,2,12,7,12,2,13,7,13,
        2,14,7,14,2,15,7,15,2,16,7,16,2,17,7,17,2,18,7,18,2,19,7,19,2,20,
        7,20,2,21,7,21,2,22,7,22,2,23,7,23,1,0,5,0,50,8,0,10,0,12,0,53,9,
        0,1,0,5,0,56,8,0,10,0,12,0,59,9,0,1,0,1,0,1,1,1,1,1,1,1,1,3,1,67,
        8,1,1,2,5,2,70,8,2,10,2,12,2,73,9,2,1,2,1,2,1,2,1,2,1,2,1,2,5,2,
        81,8,2,10,2,12,2,84,9,2,1,2,5,2,87,8,2,10,2,12,2,90,9,2,1,2,1,2,
        1,3,5,3,95,8,3,10,3,12,3,98,9,3,1,3,1,3,1,3,1,3,1,3,4,3,105,8,3,
        11,3,12,3,106,1,3,5,3,110,8,3,10,3,12,3,113,9,3,1,3,1,3,1,4,5,4,
        118,8,4,10,4,12,4,121,9,4,1,4,1,4,1,4,1,4,1,4,1,4,1,4,1,4,4,4,131,
        8,4,11,4,12,4,132,1,4,5,4,136,8,4,10,4,12,4,139,9,4,1,4,1,4,1,5,
        5,5,144,8,5,10,5,12,5,147,9,5,1,5,1,5,1,5,1,5,1,5,4,5,154,8,5,11,
        5,12,5,155,1,5,4,5,159,8,5,11,5,12,5,160,3,5,163,8,5,1,5,5,5,166,
        8,5,10,5,12,5,169,9,5,1,5,1,5,1,6,5,6,174,8,6,10,6,12,6,177,9,6,
        1,6,1,6,1,6,1,6,3,6,183,8,6,1,6,1,6,1,6,1,7,5,7,189,8,7,10,7,12,
        7,192,9,7,1,7,1,7,1,7,1,7,1,7,1,7,1,7,1,7,1,8,5,8,203,8,8,10,8,12,
        8,206,9,8,1,8,1,8,1,8,1,9,5,9,212,8,9,10,9,12,9,215,9,9,1,9,1,9,
        1,9,1,10,5,10,221,8,10,10,10,12,10,224,9,10,1,10,1,10,1,10,1,10,
        1,10,1,11,1,11,1,11,1,11,1,11,1,11,1,11,1,11,1,11,1,11,1,11,1,11,
        1,11,3,11,244,8,11,1,12,1,12,3,12,248,8,12,1,13,1,13,1,13,3,13,253,
        8,13,1,14,1,14,1,14,1,14,5,14,259,8,14,10,14,12,14,262,9,14,1,14,
        1,14,1,15,1,15,1,15,1,15,5,15,270,8,15,10,15,12,15,273,9,15,1,15,
        1,15,3,15,277,8,15,1,16,1,16,1,16,3,16,282,8,16,1,16,3,16,285,8,
        16,1,17,1,17,1,17,1,18,1,18,1,19,1,19,1,20,1,20,1,20,5,20,297,8,
        20,10,20,12,20,300,9,20,1,21,1,21,1,22,1,22,1,23,1,23,1,23,0,0,24,
        0,2,4,6,8,10,12,14,16,18,20,22,24,26,28,30,32,34,36,38,40,42,44,
        46,0,4,1,0,10,11,1,0,25,29,1,0,27,28,2,0,3,9,30,30,322,0,51,1,0,
        0,0,2,66,1,0,0,0,4,71,1,0,0,0,6,96,1,0,0,0,8,119,1,0,0,0,10,145,
        1,0,0,0,12,175,1,0,0,0,14,190,1,0,0,0,16,204,1,0,0,0,18,213,1,0,
        0,0,20,222,1,0,0,0,22,243,1,0,0,0,24,247,1,0,0,0,26,249,1,0,0,0,
        28,254,1,0,0,0,30,276,1,0,0,0,32,284,1,0,0,0,34,286,1,0,0,0,36,289,
        1,0,0,0,38,291,1,0,0,0,40,293,1,0,0,0,42,301,1,0,0,0,44,303,1,0,
        0,0,46,305,1,0,0,0,48,50,3,2,1,0,49,48,1,0,0,0,50,53,1,0,0,0,51,
        49,1,0,0,0,51,52,1,0,0,0,52,57,1,0,0,0,53,51,1,0,0,0,54,56,3,46,
        23,0,55,54,1,0,0,0,56,59,1,0,0,0,57,55,1,0,0,0,57,58,1,0,0,0,58,
        60,1,0,0,0,59,57,1,0,0,0,60,61,5,0,0,1,61,1,1,0,0,0,62,67,3,4,2,
        0,63,67,3,6,3,0,64,67,3,8,4,0,65,67,3,10,5,0,66,62,1,0,0,0,66,63,
        1,0,0,0,66,64,1,0,0,0,66,65,1,0,0,0,67,3,1,0,0,0,68,70,3,44,22,0,
        69,68,1,0,0,0,70,73,1,0,0,0,71,69,1,0,0,0,71,72,1,0,0,0,72,74,1,
        0,0,0,73,71,1,0,0,0,74,75,5,3,0,0,75,76,3,40,20,0,76,77,3,30,15,
        0,77,82,5,23,0,0,78,81,3,12,6,0,79,81,3,14,7,0,80,78,1,0,0,0,80,
        79,1,0,0,0,81,84,1,0,0,0,82,80,1,0,0,0,82,83,1,0,0,0,83,88,1,0,0,
        0,84,82,1,0,0,0,85,87,3,46,23,0,86,85,1,0,0,0,87,90,1,0,0,0,88,86,
        1,0,0,0,88,89,1,0,0,0,89,91,1,0,0,0,90,88,1,0,0,0,91,92,5,17,0,0,
        92,5,1,0,0,0,93,95,3,44,22,0,94,93,1,0,0,0,95,98,1,0,0,0,96,94,1,
        0,0,0,96,97,1,0,0,0,97,99,1,0,0,0,98,96,1,0,0,0,99,100,5,4,0,0,100,
        101,3,40,20,0,101,102,3,30,15,0,102,104,5,23,0,0,103,105,3,4,2,0,
        104,103,1,0,0,0,105,106,1,0,0,0,106,104,1,0,0,0,106,107,1,0,0,0,
        107,111,1,0,0,0,108,110,3,46,23,0,109,108,1,0,0,0,110,113,1,0,0,
        0,111,109,1,0,0,0,111,112,1,0,0,0,112,114,1,0,0,0,113,111,1,0,0,
        0,114,115,5,17,0,0,115,7,1,0,0,0,116,118,3,44,22,0,117,116,1,0,0,
        0,118,121,1,0,0,0,119,117,1,0,0,0,119,120,1,0,0,0,120,122,1,0,0,
        0,121,119,1,0,0,0,122,123,5,5,0,0,123,124,3,40,20,0,124,125,3,28,
        14,0,125,126,3,30,15,0,126,130,5,23,0,0,127,131,3,12,6,0,128,131,
        3,14,7,0,129,131,3,16,8,0,130,127,1,0,0,0,130,128,1,0,0,0,130,129,
        1,0,0,0,131,132,1,0,0,0,132,130,1,0,0,0,132,133,1,0,0,0,133,137,
        1,0,0,0,134,136,3,46,23,0,135,134,1,0,0,0,136,139,1,0,0,0,137,135,
        1,0,0,0,137,138,1,0,0,0,138,140,1,0,0,0,139,137,1,0,0,0,140,141,
        5,17,0,0,141,9,1,0,0,0,142,144,3,44,22,0,143,142,1,0,0,0,144,147,
        1,0,0,0,145,143,1,0,0,0,145,146,1,0,0,0,146,148,1,0,0,0,147,145,
        1,0,0,0,148,149,5,6,0,0,149,150,3,40,20,0,150,151,3,30,15,0,151,
        162,5,23,0,0,152,154,3,18,9,0,153,152,1,0,0,0,154,155,1,0,0,0,155,
        153,1,0,0,0,155,156,1,0,0,0,156,163,1,0,0,0,157,159,3,20,10,0,158,
        157,1,0,0,0,159,160,1,0,0,0,160,158,1,0,0,0,160,161,1,0,0,0,161,
        163,1,0,0,0,162,153,1,0,0,0,162,158,1,0,0,0,162,163,1,0,0,0,163,
        167,1,0,0,0,164,166,3,46,23,0,165,164,1,0,0,0,166,169,1,0,0,0,167,
        165,1,0,0,0,167,168,1,0,0,0,168,170,1,0,0,0,169,167,1,0,0,0,170,
        171,5,17,0,0,171,11,1,0,0,0,172,174,3,44,22,0,173,172,1,0,0,0,174,
        177,1,0,0,0,175,173,1,0,0,0,175,176,1,0,0,0,176,178,1,0,0,0,177,
        175,1,0,0,0,178,179,3,42,21,0,179,180,5,20,0,0,180,182,3,22,11,0,
        181,183,3,26,13,0,182,181,1,0,0,0,182,183,1,0,0,0,183,184,1,0,0,
        0,184,185,3,30,15,0,185,186,5,24,0,0,186,13,1,0,0,0,187,189,3,44,
        22,0,188,187,1,0,0,0,189,192,1,0,0,0,190,188,1,0,0,0,190,191,1,0,
        0,0,191,193,1,0,0,0,192,190,1,0,0,0,193,194,5,10,0,0,194,195,3,42,
        21,0,195,196,5,20,0,0,196,197,3,28,14,0,197,198,3,26,13,0,198,199,
        3,30,15,0,199,200,5,24,0,0,200,15,1,0,0,0,201,203,3,44,22,0,202,
        201,1,0,0,0,203,206,1,0,0,0,204,202,1,0,0,0,204,205,1,0,0,0,205,
        207,1,0,0,0,206,204,1,0,0,0,207,208,3,28,14,0,208,209,5,24,0,0,209,
        17,1,0,0,0,210,212,3,44,22,0,211,210,1,0,0,0,212,215,1,0,0,0,213,
        211,1,0,0,0,213,214,1,0,0,0,214,216,1,0,0,0,215,213,1,0,0,0,216,
        217,3,42,21,0,217,218,5,24,0,0,218,19,1,0,0,0,219,221,3,44,22,0,
        220,219,1,0,0,0,221,224,1,0,0,0,222,220,1,0,0,0,222,223,1,0,0,0,
        223,225,1,0,0,0,224,222,1,0,0,0,225,226,3,42,21,0,226,227,5,12,0,
        0,227,228,3,38,19,0,228,229,5,24,0,0,229,21,1,0,0,0,230,231,5,7,
        0,0,231,232,5,1,0,0,232,233,3,24,12,0,233,234,5,2,0,0,234,244,1,
        0,0,0,235,236,5,8,0,0,236,237,5,1,0,0,237,238,3,24,12,0,238,239,
        5,19,0,0,239,240,3,24,12,0,240,241,5,2,0,0,241,244,1,0,0,0,242,244,
        3,24,12,0,243,230,1,0,0,0,243,235,1,0,0,0,243,242,1,0,0,0,244,23,
        1,0,0,0,245,248,5,9,0,0,246,248,3,40,20,0,247,245,1,0,0,0,247,246,
        1,0,0,0,248,25,1,0,0,0,249,250,7,0,0,0,250,252,3,40,20,0,251,253,
        3,28,14,0,252,251,1,0,0,0,252,253,1,0,0,0,253,27,1,0,0,0,254,255,
        5,15,0,0,255,260,3,42,21,0,256,257,5,19,0,0,257,259,3,42,21,0,258,
        256,1,0,0,0,259,262,1,0,0,0,260,258,1,0,0,0,260,261,1,0,0,0,261,
        263,1,0,0,0,262,260,1,0,0,0,263,264,5,16,0,0,264,29,1,0,0,0,265,
        266,5,13,0,0,266,271,3,32,16,0,267,268,5,19,0,0,268,270,3,32,16,
        0,269,267,1,0,0,0,270,273,1,0,0,0,271,269,1,0,0,0,271,272,1,0,0,
        0,272,274,1,0,0,0,273,271,1,0,0,0,274,275,5,14,0,0,275,277,1,0,0,
        0,276,265,1,0,0,0,276,277,1,0,0,0,277,31,1,0,0,0,278,281,3,42,21,
        0,279,280,5,12,0,0,280,282,3,36,18,0,281,279,1,0,0,0,281,282,1,0,
        0,0,282,285,1,0,0,0,283,285,3,34,17,0,284,278,1,0,0,0,284,283,1,
        0,0,0,285,33,1,0,0,0,286,287,5,22,0,0,287,288,3,42,21,0,288,35,1,
        0,0,0,289,290,7,1,0,0,290,37,1,0,0,0,291,292,7,2,0,0,292,39,1,0,
        0,0,293,298,3,42,21,0,294,295,5,18,0,0,295,297,3,42,21,0,296,294,
        1,0,0,0,297,300,1,0,0,0,298,296,1,0,0,0,298,299,1,0,0,0,299,41,1,
        0,0,0,300,298,1,0,0,0,301,302,7,3,0,0,302,43,1,0,0,0,303,304,5,31,
        0,0,304,45,1,0,0,0,305,306,5,31,0,0,306,47,1,0,0,0,34,51,57,66,71,
        80,82,88,96,106,111,119,130,132,137,145,155,160,162,167,175,182,
        190,204,213,222,243,247,252,260,271,276,281,284,298
    ];

    private static __ATN: antlr.ATN;
    public static get _ATN(): antlr.ATN {
        if (!CfgParser.__ATN) {
            CfgParser.__ATN = new antlr.ATNDeserializer().deserialize(CfgParser._serializedATN);
        }

        return CfgParser.__ATN;
    }


    private static readonly vocabulary = new antlr.Vocabulary(CfgParser.literalNames, CfgParser.symbolicNames, []);

    public override get vocabulary(): antlr.Vocabulary {
        return CfgParser.vocabulary;
    }

    private static readonly decisionsToDFA = CfgParser._ATN.decisionToState.map( (ds: antlr.DecisionState, index: number) => new antlr.DFA(ds, index) );
}

export class SchemaContext extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public EOF(): antlr.TerminalNode {
        return this.getToken(CfgParser.EOF, 0)!;
    }
    public schema_ele(): Schema_eleContext[];
    public schema_ele(i: number): Schema_eleContext | null;
    public schema_ele(i?: number): Schema_eleContext[] | Schema_eleContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Schema_eleContext);
        }

        return this.getRuleContext(i, Schema_eleContext);
    }
    public suffix_comment(): Suffix_commentContext[];
    public suffix_comment(i: number): Suffix_commentContext | null;
    public suffix_comment(i?: number): Suffix_commentContext[] | Suffix_commentContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Suffix_commentContext);
        }

        return this.getRuleContext(i, Suffix_commentContext);
    }
    public override get ruleIndex(): number {
        return CfgParser.RULE_schema;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitSchema) {
            return visitor.visitSchema(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}


export class Schema_eleContext extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public struct_decl(): Struct_declContext | null {
        return this.getRuleContext(0, Struct_declContext);
    }
    public interface_decl(): Interface_declContext | null {
        return this.getRuleContext(0, Interface_declContext);
    }
    public table_decl(): Table_declContext | null {
        return this.getRuleContext(0, Table_declContext);
    }
    public enum_decl(): Enum_declContext | null {
        return this.getRuleContext(0, Enum_declContext);
    }
    public override get ruleIndex(): number {
        return CfgParser.RULE_schema_ele;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitSchema_ele) {
            return visitor.visitSchema_ele(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}


export class Struct_declContext extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public STRUCT(): antlr.TerminalNode {
        return this.getToken(CfgParser.STRUCT, 0)!;
    }
    public ns_ident(): Ns_identContext {
        return this.getRuleContext(0, Ns_identContext)!;
    }
    public metadata(): MetadataContext {
        return this.getRuleContext(0, MetadataContext)!;
    }
    public LC_COMMENT(): antlr.TerminalNode {
        return this.getToken(CfgParser.LC_COMMENT, 0)!;
    }
    public RC(): antlr.TerminalNode {
        return this.getToken(CfgParser.RC, 0)!;
    }
    public leading_comment(): Leading_commentContext[];
    public leading_comment(i: number): Leading_commentContext | null;
    public leading_comment(i?: number): Leading_commentContext[] | Leading_commentContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Leading_commentContext);
        }

        return this.getRuleContext(i, Leading_commentContext);
    }
    public field_decl(): Field_declContext[];
    public field_decl(i: number): Field_declContext | null;
    public field_decl(i?: number): Field_declContext[] | Field_declContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Field_declContext);
        }

        return this.getRuleContext(i, Field_declContext);
    }
    public foreign_decl(): Foreign_declContext[];
    public foreign_decl(i: number): Foreign_declContext | null;
    public foreign_decl(i?: number): Foreign_declContext[] | Foreign_declContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Foreign_declContext);
        }

        return this.getRuleContext(i, Foreign_declContext);
    }
    public suffix_comment(): Suffix_commentContext[];
    public suffix_comment(i: number): Suffix_commentContext | null;
    public suffix_comment(i?: number): Suffix_commentContext[] | Suffix_commentContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Suffix_commentContext);
        }

        return this.getRuleContext(i, Suffix_commentContext);
    }
    public override get ruleIndex(): number {
        return CfgParser.RULE_struct_decl;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitStruct_decl) {
            return visitor.visitStruct_decl(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}


export class Interface_declContext extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public INTERFACE(): antlr.TerminalNode {
        return this.getToken(CfgParser.INTERFACE, 0)!;
    }
    public ns_ident(): Ns_identContext {
        return this.getRuleContext(0, Ns_identContext)!;
    }
    public metadata(): MetadataContext {
        return this.getRuleContext(0, MetadataContext)!;
    }
    public LC_COMMENT(): antlr.TerminalNode {
        return this.getToken(CfgParser.LC_COMMENT, 0)!;
    }
    public RC(): antlr.TerminalNode {
        return this.getToken(CfgParser.RC, 0)!;
    }
    public leading_comment(): Leading_commentContext[];
    public leading_comment(i: number): Leading_commentContext | null;
    public leading_comment(i?: number): Leading_commentContext[] | Leading_commentContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Leading_commentContext);
        }

        return this.getRuleContext(i, Leading_commentContext);
    }
    public struct_decl(): Struct_declContext[];
    public struct_decl(i: number): Struct_declContext | null;
    public struct_decl(i?: number): Struct_declContext[] | Struct_declContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Struct_declContext);
        }

        return this.getRuleContext(i, Struct_declContext);
    }
    public suffix_comment(): Suffix_commentContext[];
    public suffix_comment(i: number): Suffix_commentContext | null;
    public suffix_comment(i?: number): Suffix_commentContext[] | Suffix_commentContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Suffix_commentContext);
        }

        return this.getRuleContext(i, Suffix_commentContext);
    }
    public override get ruleIndex(): number {
        return CfgParser.RULE_interface_decl;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitInterface_decl) {
            return visitor.visitInterface_decl(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}


export class Table_declContext extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public TABLE(): antlr.TerminalNode {
        return this.getToken(CfgParser.TABLE, 0)!;
    }
    public ns_ident(): Ns_identContext {
        return this.getRuleContext(0, Ns_identContext)!;
    }
    public key(): KeyContext {
        return this.getRuleContext(0, KeyContext)!;
    }
    public metadata(): MetadataContext {
        return this.getRuleContext(0, MetadataContext)!;
    }
    public LC_COMMENT(): antlr.TerminalNode {
        return this.getToken(CfgParser.LC_COMMENT, 0)!;
    }
    public RC(): antlr.TerminalNode {
        return this.getToken(CfgParser.RC, 0)!;
    }
    public leading_comment(): Leading_commentContext[];
    public leading_comment(i: number): Leading_commentContext | null;
    public leading_comment(i?: number): Leading_commentContext[] | Leading_commentContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Leading_commentContext);
        }

        return this.getRuleContext(i, Leading_commentContext);
    }
    public field_decl(): Field_declContext[];
    public field_decl(i: number): Field_declContext | null;
    public field_decl(i?: number): Field_declContext[] | Field_declContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Field_declContext);
        }

        return this.getRuleContext(i, Field_declContext);
    }
    public foreign_decl(): Foreign_declContext[];
    public foreign_decl(i: number): Foreign_declContext | null;
    public foreign_decl(i?: number): Foreign_declContext[] | Foreign_declContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Foreign_declContext);
        }

        return this.getRuleContext(i, Foreign_declContext);
    }
    public key_decl(): Key_declContext[];
    public key_decl(i: number): Key_declContext | null;
    public key_decl(i?: number): Key_declContext[] | Key_declContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Key_declContext);
        }

        return this.getRuleContext(i, Key_declContext);
    }
    public suffix_comment(): Suffix_commentContext[];
    public suffix_comment(i: number): Suffix_commentContext | null;
    public suffix_comment(i?: number): Suffix_commentContext[] | Suffix_commentContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Suffix_commentContext);
        }

        return this.getRuleContext(i, Suffix_commentContext);
    }
    public override get ruleIndex(): number {
        return CfgParser.RULE_table_decl;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitTable_decl) {
            return visitor.visitTable_decl(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}


export class Enum_declContext extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public ENUM(): antlr.TerminalNode {
        return this.getToken(CfgParser.ENUM, 0)!;
    }
    public ns_ident(): Ns_identContext {
        return this.getRuleContext(0, Ns_identContext)!;
    }
    public metadata(): MetadataContext {
        return this.getRuleContext(0, MetadataContext)!;
    }
    public LC_COMMENT(): antlr.TerminalNode {
        return this.getToken(CfgParser.LC_COMMENT, 0)!;
    }
    public RC(): antlr.TerminalNode {
        return this.getToken(CfgParser.RC, 0)!;
    }
    public leading_comment(): Leading_commentContext[];
    public leading_comment(i: number): Leading_commentContext | null;
    public leading_comment(i?: number): Leading_commentContext[] | Leading_commentContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Leading_commentContext);
        }

        return this.getRuleContext(i, Leading_commentContext);
    }
    public suffix_comment(): Suffix_commentContext[];
    public suffix_comment(i: number): Suffix_commentContext | null;
    public suffix_comment(i?: number): Suffix_commentContext[] | Suffix_commentContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Suffix_commentContext);
        }

        return this.getRuleContext(i, Suffix_commentContext);
    }
    public enum_value_empty(): Enum_value_emptyContext[];
    public enum_value_empty(i: number): Enum_value_emptyContext | null;
    public enum_value_empty(i?: number): Enum_value_emptyContext[] | Enum_value_emptyContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Enum_value_emptyContext);
        }

        return this.getRuleContext(i, Enum_value_emptyContext);
    }
    public enum_value_assigned(): Enum_value_assignedContext[];
    public enum_value_assigned(i: number): Enum_value_assignedContext | null;
    public enum_value_assigned(i?: number): Enum_value_assignedContext[] | Enum_value_assignedContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Enum_value_assignedContext);
        }

        return this.getRuleContext(i, Enum_value_assignedContext);
    }
    public override get ruleIndex(): number {
        return CfgParser.RULE_enum_decl;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitEnum_decl) {
            return visitor.visitEnum_decl(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}


export class Field_declContext extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public identifier(): IdentifierContext {
        return this.getRuleContext(0, IdentifierContext)!;
    }
    public COLON(): antlr.TerminalNode {
        return this.getToken(CfgParser.COLON, 0)!;
    }
    public type_(): Type_Context {
        return this.getRuleContext(0, Type_Context)!;
    }
    public metadata(): MetadataContext {
        return this.getRuleContext(0, MetadataContext)!;
    }
    public SEMI_COMMENT(): antlr.TerminalNode {
        return this.getToken(CfgParser.SEMI_COMMENT, 0)!;
    }
    public leading_comment(): Leading_commentContext[];
    public leading_comment(i: number): Leading_commentContext | null;
    public leading_comment(i?: number): Leading_commentContext[] | Leading_commentContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Leading_commentContext);
        }

        return this.getRuleContext(i, Leading_commentContext);
    }
    public ref(): RefContext | null {
        return this.getRuleContext(0, RefContext);
    }
    public override get ruleIndex(): number {
        return CfgParser.RULE_field_decl;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitField_decl) {
            return visitor.visitField_decl(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}


export class Foreign_declContext extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public REF(): antlr.TerminalNode {
        return this.getToken(CfgParser.REF, 0)!;
    }
    public identifier(): IdentifierContext {
        return this.getRuleContext(0, IdentifierContext)!;
    }
    public COLON(): antlr.TerminalNode {
        return this.getToken(CfgParser.COLON, 0)!;
    }
    public key(): KeyContext {
        return this.getRuleContext(0, KeyContext)!;
    }
    public ref(): RefContext {
        return this.getRuleContext(0, RefContext)!;
    }
    public metadata(): MetadataContext {
        return this.getRuleContext(0, MetadataContext)!;
    }
    public SEMI_COMMENT(): antlr.TerminalNode {
        return this.getToken(CfgParser.SEMI_COMMENT, 0)!;
    }
    public leading_comment(): Leading_commentContext[];
    public leading_comment(i: number): Leading_commentContext | null;
    public leading_comment(i?: number): Leading_commentContext[] | Leading_commentContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Leading_commentContext);
        }

        return this.getRuleContext(i, Leading_commentContext);
    }
    public override get ruleIndex(): number {
        return CfgParser.RULE_foreign_decl;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitForeign_decl) {
            return visitor.visitForeign_decl(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}


export class Key_declContext extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public key(): KeyContext {
        return this.getRuleContext(0, KeyContext)!;
    }
    public SEMI_COMMENT(): antlr.TerminalNode {
        return this.getToken(CfgParser.SEMI_COMMENT, 0)!;
    }
    public leading_comment(): Leading_commentContext[];
    public leading_comment(i: number): Leading_commentContext | null;
    public leading_comment(i?: number): Leading_commentContext[] | Leading_commentContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Leading_commentContext);
        }

        return this.getRuleContext(i, Leading_commentContext);
    }
    public override get ruleIndex(): number {
        return CfgParser.RULE_key_decl;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitKey_decl) {
            return visitor.visitKey_decl(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}


export class Enum_value_emptyContext extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public identifier(): IdentifierContext {
        return this.getRuleContext(0, IdentifierContext)!;
    }
    public SEMI_COMMENT(): antlr.TerminalNode {
        return this.getToken(CfgParser.SEMI_COMMENT, 0)!;
    }
    public leading_comment(): Leading_commentContext[];
    public leading_comment(i: number): Leading_commentContext | null;
    public leading_comment(i?: number): Leading_commentContext[] | Leading_commentContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Leading_commentContext);
        }

        return this.getRuleContext(i, Leading_commentContext);
    }
    public override get ruleIndex(): number {
        return CfgParser.RULE_enum_value_empty;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitEnum_value_empty) {
            return visitor.visitEnum_value_empty(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}


export class Enum_value_assignedContext extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public identifier(): IdentifierContext {
        return this.getRuleContext(0, IdentifierContext)!;
    }
    public EQ(): antlr.TerminalNode {
        return this.getToken(CfgParser.EQ, 0)!;
    }
    public enum_number(): Enum_numberContext {
        return this.getRuleContext(0, Enum_numberContext)!;
    }
    public SEMI_COMMENT(): antlr.TerminalNode {
        return this.getToken(CfgParser.SEMI_COMMENT, 0)!;
    }
    public leading_comment(): Leading_commentContext[];
    public leading_comment(i: number): Leading_commentContext | null;
    public leading_comment(i?: number): Leading_commentContext[] | Leading_commentContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Leading_commentContext);
        }

        return this.getRuleContext(i, Leading_commentContext);
    }
    public override get ruleIndex(): number {
        return CfgParser.RULE_enum_value_assigned;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitEnum_value_assigned) {
            return visitor.visitEnum_value_assigned(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}


export class Type_Context extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public override get ruleIndex(): number {
        return CfgParser.RULE_type_;
    }
    public override copyFrom(ctx: Type_Context): void {
        super.copyFrom(ctx);
    }
}
export class TypeListContext extends Type_Context {
    public constructor(ctx: Type_Context) {
        super(ctx.parent, ctx.invokingState);
        super.copyFrom(ctx);
    }
    public TLIST(): antlr.TerminalNode {
        return this.getToken(CfgParser.TLIST, 0)!;
    }
    public type_ele(): Type_eleContext {
        return this.getRuleContext(0, Type_eleContext)!;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitTypeList) {
            return visitor.visitTypeList(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}
export class TypeMapContext extends Type_Context {
    public constructor(ctx: Type_Context) {
        super(ctx.parent, ctx.invokingState);
        super.copyFrom(ctx);
    }
    public TMAP(): antlr.TerminalNode {
        return this.getToken(CfgParser.TMAP, 0)!;
    }
    public type_ele(): Type_eleContext[];
    public type_ele(i: number): Type_eleContext | null;
    public type_ele(i?: number): Type_eleContext[] | Type_eleContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Type_eleContext);
        }

        return this.getRuleContext(i, Type_eleContext);
    }
    public COMMA(): antlr.TerminalNode {
        return this.getToken(CfgParser.COMMA, 0)!;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitTypeMap) {
            return visitor.visitTypeMap(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}
export class TypeBasicContext extends Type_Context {
    public constructor(ctx: Type_Context) {
        super(ctx.parent, ctx.invokingState);
        super.copyFrom(ctx);
    }
    public type_ele(): Type_eleContext {
        return this.getRuleContext(0, Type_eleContext)!;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitTypeBasic) {
            return visitor.visitTypeBasic(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}


export class Type_eleContext extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public TBASE(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.TBASE, 0);
    }
    public ns_ident(): Ns_identContext | null {
        return this.getRuleContext(0, Ns_identContext);
    }
    public override get ruleIndex(): number {
        return CfgParser.RULE_type_ele;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitType_ele) {
            return visitor.visitType_ele(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}


export class RefContext extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public ns_ident(): Ns_identContext {
        return this.getRuleContext(0, Ns_identContext)!;
    }
    public REF(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.REF, 0);
    }
    public LISTREF(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.LISTREF, 0);
    }
    public key(): KeyContext | null {
        return this.getRuleContext(0, KeyContext);
    }
    public override get ruleIndex(): number {
        return CfgParser.RULE_ref;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitRef) {
            return visitor.visitRef(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}


export class KeyContext extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public LB(): antlr.TerminalNode {
        return this.getToken(CfgParser.LB, 0)!;
    }
    public identifier(): IdentifierContext[];
    public identifier(i: number): IdentifierContext | null;
    public identifier(i?: number): IdentifierContext[] | IdentifierContext | null {
        if (i === undefined) {
            return this.getRuleContexts(IdentifierContext);
        }

        return this.getRuleContext(i, IdentifierContext);
    }
    public RB(): antlr.TerminalNode {
        return this.getToken(CfgParser.RB, 0)!;
    }
    public COMMA(): antlr.TerminalNode[];
    public COMMA(i: number): antlr.TerminalNode | null;
    public COMMA(i?: number): antlr.TerminalNode | null | antlr.TerminalNode[] {
    	if (i === undefined) {
    		return this.getTokens(CfgParser.COMMA);
    	} else {
    		return this.getToken(CfgParser.COMMA, i);
    	}
    }
    public override get ruleIndex(): number {
        return CfgParser.RULE_key;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitKey) {
            return visitor.visitKey(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}


export class MetadataContext extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public LP(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.LP, 0);
    }
    public ident_with_opt_single_value(): Ident_with_opt_single_valueContext[];
    public ident_with_opt_single_value(i: number): Ident_with_opt_single_valueContext | null;
    public ident_with_opt_single_value(i?: number): Ident_with_opt_single_valueContext[] | Ident_with_opt_single_valueContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Ident_with_opt_single_valueContext);
        }

        return this.getRuleContext(i, Ident_with_opt_single_valueContext);
    }
    public RP(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.RP, 0);
    }
    public COMMA(): antlr.TerminalNode[];
    public COMMA(i: number): antlr.TerminalNode | null;
    public COMMA(i?: number): antlr.TerminalNode | null | antlr.TerminalNode[] {
    	if (i === undefined) {
    		return this.getTokens(CfgParser.COMMA);
    	} else {
    		return this.getToken(CfgParser.COMMA, i);
    	}
    }
    public override get ruleIndex(): number {
        return CfgParser.RULE_metadata;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitMetadata) {
            return visitor.visitMetadata(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}


export class Ident_with_opt_single_valueContext extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public identifier(): IdentifierContext | null {
        return this.getRuleContext(0, IdentifierContext);
    }
    public EQ(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.EQ, 0);
    }
    public single_value(): Single_valueContext | null {
        return this.getRuleContext(0, Single_valueContext);
    }
    public minus_ident(): Minus_identContext | null {
        return this.getRuleContext(0, Minus_identContext);
    }
    public override get ruleIndex(): number {
        return CfgParser.RULE_ident_with_opt_single_value;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitIdent_with_opt_single_value) {
            return visitor.visitIdent_with_opt_single_value(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}


export class Minus_identContext extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public MINUS(): antlr.TerminalNode {
        return this.getToken(CfgParser.MINUS, 0)!;
    }
    public identifier(): IdentifierContext {
        return this.getRuleContext(0, IdentifierContext)!;
    }
    public override get ruleIndex(): number {
        return CfgParser.RULE_minus_ident;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitMinus_ident) {
            return visitor.visitMinus_ident(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}


export class Single_valueContext extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public INTEGER_CONSTANT(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.INTEGER_CONSTANT, 0);
    }
    public HEX_INTEGER_CONSTANT(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.HEX_INTEGER_CONSTANT, 0);
    }
    public FLOAT_CONSTANT(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.FLOAT_CONSTANT, 0);
    }
    public STRING_CONSTANT(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.STRING_CONSTANT, 0);
    }
    public BOOL_CONSTANT(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.BOOL_CONSTANT, 0);
    }
    public override get ruleIndex(): number {
        return CfgParser.RULE_single_value;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitSingle_value) {
            return visitor.visitSingle_value(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}


export class Enum_numberContext extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public INTEGER_CONSTANT(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.INTEGER_CONSTANT, 0);
    }
    public HEX_INTEGER_CONSTANT(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.HEX_INTEGER_CONSTANT, 0);
    }
    public override get ruleIndex(): number {
        return CfgParser.RULE_enum_number;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitEnum_number) {
            return visitor.visitEnum_number(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}


export class Ns_identContext extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public identifier(): IdentifierContext[];
    public identifier(i: number): IdentifierContext | null;
    public identifier(i?: number): IdentifierContext[] | IdentifierContext | null {
        if (i === undefined) {
            return this.getRuleContexts(IdentifierContext);
        }

        return this.getRuleContext(i, IdentifierContext);
    }
    public DOT(): antlr.TerminalNode[];
    public DOT(i: number): antlr.TerminalNode | null;
    public DOT(i?: number): antlr.TerminalNode | null | antlr.TerminalNode[] {
    	if (i === undefined) {
    		return this.getTokens(CfgParser.DOT);
    	} else {
    		return this.getToken(CfgParser.DOT, i);
    	}
    }
    public override get ruleIndex(): number {
        return CfgParser.RULE_ns_ident;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitNs_ident) {
            return visitor.visitNs_ident(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}


export class IdentifierContext extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public IDENT(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.IDENT, 0);
    }
    public STRUCT(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.STRUCT, 0);
    }
    public INTERFACE(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.INTERFACE, 0);
    }
    public TABLE(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.TABLE, 0);
    }
    public ENUM(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.ENUM, 0);
    }
    public TLIST(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.TLIST, 0);
    }
    public TMAP(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.TMAP, 0);
    }
    public TBASE(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.TBASE, 0);
    }
    public override get ruleIndex(): number {
        return CfgParser.RULE_identifier;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitIdentifier) {
            return visitor.visitIdentifier(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}


export class Leading_commentContext extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public COMMENT(): antlr.TerminalNode {
        return this.getToken(CfgParser.COMMENT, 0)!;
    }
    public override get ruleIndex(): number {
        return CfgParser.RULE_leading_comment;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitLeading_comment) {
            return visitor.visitLeading_comment(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}


export class Suffix_commentContext extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public COMMENT(): antlr.TerminalNode {
        return this.getToken(CfgParser.COMMENT, 0)!;
    }
    public override get ruleIndex(): number {
        return CfgParser.RULE_suffix_comment;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitSuffix_comment) {
            return visitor.visitSuffix_comment(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}
