
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
    public static readonly SCOPE = 21;
    public static readonly AT = 22;
    public static readonly PLUS = 23;
    public static readonly MINUS = 24;
    public static readonly LC_COMMENT = 25;
    public static readonly SEMI_COMMENT = 26;
    public static readonly BOOL_CONSTANT = 27;
    public static readonly FLOAT_CONSTANT = 28;
    public static readonly HEX_INTEGER_CONSTANT = 29;
    public static readonly INTEGER_CONSTANT = 30;
    public static readonly STRING_CONSTANT = 31;
    public static readonly IDENT = 32;
    public static readonly COMMENT = 33;
    public static readonly WS = 34;
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
    public static readonly RULE_source_values = 14;
    public static readonly RULE_source_value = 15;
    public static readonly RULE_key = 16;
    public static readonly RULE_metadata = 17;
    public static readonly RULE_ident_with_opt_single_value = 18;
    public static readonly RULE_minus_ident = 19;
    public static readonly RULE_single_value = 20;
    public static readonly RULE_enum_number = 21;
    public static readonly RULE_ns_ident = 22;
    public static readonly RULE_identifier = 23;
    public static readonly RULE_leading_comment = 24;
    public static readonly RULE_suffix_comment = 25;

    public static readonly literalNames = [
        null, "'<'", "'>'", "'struct'", "'interface'", "'table'", "'enum'",
        "'list'", "'map'", null, "'->'", "'=>'", "'='", "'('", "')'", "'['",
        "']'", "'}'", "'.'", "','", "':'", "'::'", "'@'", "'+'", "'-'"
    ];

    public static readonly symbolicNames = [
        null, null, null, "STRUCT", "INTERFACE", "TABLE", "ENUM", "TLIST",
        "TMAP", "TBASE", "REF", "LISTREF", "EQ", "LP", "RP", "LB", "RB",
        "RC", "DOT", "COMMA", "COLON", "SCOPE", "AT", "PLUS", "MINUS", "LC_COMMENT",
        "SEMI_COMMENT", "BOOL_CONSTANT", "FLOAT_CONSTANT", "HEX_INTEGER_CONSTANT",
        "INTEGER_CONSTANT", "STRING_CONSTANT", "IDENT", "COMMENT", "WS"
    ];
    public static readonly ruleNames = [
        "schema", "schema_ele", "struct_decl", "interface_decl", "table_decl",
        "enum_decl", "field_decl", "foreign_decl", "key_decl", "enum_value_empty",
        "enum_value_assigned", "type_", "type_ele", "ref", "source_values",
        "source_value", "key", "metadata", "ident_with_opt_single_value",
        "minus_ident", "single_value", "enum_number", "ns_ident", "identifier",
        "leading_comment", "suffix_comment",
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
            this.state = 55;
            this.errorHandler.sync(this);
            alternative = this.interpreter.adaptivePredict(this.tokenStream, 0, this.context);
            while (alternative !== 2 && alternative !== antlr.ATN.INVALID_ALT_NUMBER) {
                if (alternative === 1) {
                    {
                    {
                    this.state = 52;
                    this.schema_ele();
                    }
                    }
                }
                this.state = 57;
                this.errorHandler.sync(this);
                alternative = this.interpreter.adaptivePredict(this.tokenStream, 0, this.context);
            }
            this.state = 61;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 33) {
                {
                {
                this.state = 58;
                this.suffix_comment();
                }
                }
                this.state = 63;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 64;
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
            this.state = 70;
            this.errorHandler.sync(this);
            switch (this.interpreter.adaptivePredict(this.tokenStream, 2, this.context) ) {
            case 1:
                this.enterOuterAlt(localContext, 1);
                {
                this.state = 66;
                this.struct_decl();
                }
                break;
            case 2:
                this.enterOuterAlt(localContext, 2);
                {
                this.state = 67;
                this.interface_decl();
                }
                break;
            case 3:
                this.enterOuterAlt(localContext, 3);
                {
                this.state = 68;
                this.table_decl();
                }
                break;
            case 4:
                this.enterOuterAlt(localContext, 4);
                {
                this.state = 69;
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
            this.state = 75;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 33) {
                {
                {
                this.state = 72;
                this.leading_comment();
                }
                }
                this.state = 77;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 78;
            this.match(CfgParser.STRUCT);
            this.state = 79;
            this.ns_ident();
            this.state = 80;
            this.metadata();
            this.state = 81;
            this.match(CfgParser.LC_COMMENT);
            this.state = 86;
            this.errorHandler.sync(this);
            alternative = this.interpreter.adaptivePredict(this.tokenStream, 5, this.context);
            while (alternative !== 2 && alternative !== antlr.ATN.INVALID_ALT_NUMBER) {
                if (alternative === 1) {
                    {
                    this.state = 84;
                    this.errorHandler.sync(this);
                    switch (this.interpreter.adaptivePredict(this.tokenStream, 4, this.context) ) {
                    case 1:
                        {
                        this.state = 82;
                        this.field_decl();
                        }
                        break;
                    case 2:
                        {
                        this.state = 83;
                        this.foreign_decl();
                        }
                        break;
                    }
                    }
                }
                this.state = 88;
                this.errorHandler.sync(this);
                alternative = this.interpreter.adaptivePredict(this.tokenStream, 5, this.context);
            }
            this.state = 92;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 33) {
                {
                {
                this.state = 89;
                this.suffix_comment();
                }
                }
                this.state = 94;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 95;
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
            this.state = 100;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 33) {
                {
                {
                this.state = 97;
                this.leading_comment();
                }
                }
                this.state = 102;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 103;
            this.match(CfgParser.INTERFACE);
            this.state = 104;
            this.ns_ident();
            this.state = 105;
            this.metadata();
            this.state = 106;
            this.match(CfgParser.LC_COMMENT);
            this.state = 108;
            this.errorHandler.sync(this);
            alternative = 1;
            do {
                switch (alternative) {
                case 1:
                    {
                    {
                    this.state = 107;
                    this.struct_decl();
                    }
                    }
                    break;
                default:
                    throw new antlr.NoViableAltException(this);
                }
                this.state = 110;
                this.errorHandler.sync(this);
                alternative = this.interpreter.adaptivePredict(this.tokenStream, 8, this.context);
            } while (alternative !== 2 && alternative !== antlr.ATN.INVALID_ALT_NUMBER);
            this.state = 115;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 33) {
                {
                {
                this.state = 112;
                this.suffix_comment();
                }
                }
                this.state = 117;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 118;
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
            this.state = 123;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 33) {
                {
                {
                this.state = 120;
                this.leading_comment();
                }
                }
                this.state = 125;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 126;
            this.match(CfgParser.TABLE);
            this.state = 127;
            this.ns_ident();
            this.state = 128;
            this.key();
            this.state = 129;
            this.metadata();
            this.state = 130;
            this.match(CfgParser.LC_COMMENT);
            this.state = 134;
            this.errorHandler.sync(this);
            alternative = 1;
            do {
                switch (alternative) {
                case 1:
                    {
                    this.state = 134;
                    this.errorHandler.sync(this);
                    switch (this.interpreter.adaptivePredict(this.tokenStream, 11, this.context) ) {
                    case 1:
                        {
                        this.state = 131;
                        this.field_decl();
                        }
                        break;
                    case 2:
                        {
                        this.state = 132;
                        this.foreign_decl();
                        }
                        break;
                    case 3:
                        {
                        this.state = 133;
                        this.key_decl();
                        }
                        break;
                    }
                    }
                    break;
                default:
                    throw new antlr.NoViableAltException(this);
                }
                this.state = 136;
                this.errorHandler.sync(this);
                alternative = this.interpreter.adaptivePredict(this.tokenStream, 12, this.context);
            } while (alternative !== 2 && alternative !== antlr.ATN.INVALID_ALT_NUMBER);
            this.state = 141;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 33) {
                {
                {
                this.state = 138;
                this.suffix_comment();
                }
                }
                this.state = 143;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 144;
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
            this.state = 149;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 33) {
                {
                {
                this.state = 146;
                this.leading_comment();
                }
                }
                this.state = 151;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 152;
            this.match(CfgParser.ENUM);
            this.state = 153;
            this.ns_ident();
            this.state = 154;
            this.metadata();
            this.state = 155;
            this.match(CfgParser.LC_COMMENT);
            this.state = 166;
            this.errorHandler.sync(this);
            switch (this.interpreter.adaptivePredict(this.tokenStream, 17, this.context) ) {
            case 1:
                {
                this.state = 157;
                this.errorHandler.sync(this);
                alternative = 1;
                do {
                    switch (alternative) {
                    case 1:
                        {
                        {
                        this.state = 156;
                        this.enum_value_empty();
                        }
                        }
                        break;
                    default:
                        throw new antlr.NoViableAltException(this);
                    }
                    this.state = 159;
                    this.errorHandler.sync(this);
                    alternative = this.interpreter.adaptivePredict(this.tokenStream, 15, this.context);
                } while (alternative !== 2 && alternative !== antlr.ATN.INVALID_ALT_NUMBER);
                }
                break;
            case 2:
                {
                this.state = 162;
                this.errorHandler.sync(this);
                alternative = 1;
                do {
                    switch (alternative) {
                    case 1:
                        {
                        {
                        this.state = 161;
                        this.enum_value_assigned();
                        }
                        }
                        break;
                    default:
                        throw new antlr.NoViableAltException(this);
                    }
                    this.state = 164;
                    this.errorHandler.sync(this);
                    alternative = this.interpreter.adaptivePredict(this.tokenStream, 16, this.context);
                } while (alternative !== 2 && alternative !== antlr.ATN.INVALID_ALT_NUMBER);
                }
                break;
            }
            this.state = 171;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 33) {
                {
                {
                this.state = 168;
                this.suffix_comment();
                }
                }
                this.state = 173;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 174;
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
            this.state = 179;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 33) {
                {
                {
                this.state = 176;
                this.leading_comment();
                }
                }
                this.state = 181;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 182;
            this.identifier();
            this.state = 183;
            this.match(CfgParser.COLON);
            this.state = 184;
            this.type_();
            this.state = 186;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            if (_la === 10 || _la === 11) {
                {
                this.state = 185;
                this.ref();
                }
            }

            this.state = 188;
            this.metadata();
            this.state = 189;
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
            this.state = 194;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 33) {
                {
                {
                this.state = 191;
                this.leading_comment();
                }
                }
                this.state = 196;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 197;
            this.match(CfgParser.REF);
            this.state = 198;
            this.identifier();
            this.state = 199;
            this.match(CfgParser.COLON);
            this.state = 200;
            this.source_values();
            this.state = 201;
            this.ref();
            this.state = 202;
            this.metadata();
            this.state = 203;
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
            this.state = 208;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 33) {
                {
                {
                this.state = 205;
                this.leading_comment();
                }
                }
                this.state = 210;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 211;
            this.key();
            this.state = 212;
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
            this.state = 217;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 33) {
                {
                {
                this.state = 214;
                this.leading_comment();
                }
                }
                this.state = 219;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 220;
            this.identifier();
            this.state = 221;
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
            this.state = 226;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 33) {
                {
                {
                this.state = 223;
                this.leading_comment();
                }
                }
                this.state = 228;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 229;
            this.identifier();
            this.state = 230;
            this.match(CfgParser.EQ);
            this.state = 231;
            this.enum_number();
            this.state = 232;
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
            this.state = 247;
            this.errorHandler.sync(this);
            switch (this.interpreter.adaptivePredict(this.tokenStream, 25, this.context) ) {
            case 1:
                localContext = new TypeListContext(localContext);
                this.enterOuterAlt(localContext, 1);
                {
                this.state = 234;
                this.match(CfgParser.TLIST);
                this.state = 235;
                this.match(CfgParser.T__0);
                this.state = 236;
                this.type_ele();
                this.state = 237;
                this.match(CfgParser.T__1);
                }
                break;
            case 2:
                localContext = new TypeMapContext(localContext);
                this.enterOuterAlt(localContext, 2);
                {
                this.state = 239;
                this.match(CfgParser.TMAP);
                this.state = 240;
                this.match(CfgParser.T__0);
                this.state = 241;
                this.type_ele();
                this.state = 242;
                this.match(CfgParser.COMMA);
                this.state = 243;
                this.type_ele();
                this.state = 244;
                this.match(CfgParser.T__1);
                }
                break;
            case 3:
                localContext = new TypeBasicContext(localContext);
                this.enterOuterAlt(localContext, 3);
                {
                this.state = 246;
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
            this.state = 251;
            this.errorHandler.sync(this);
            switch (this.interpreter.adaptivePredict(this.tokenStream, 26, this.context) ) {
            case 1:
                this.enterOuterAlt(localContext, 1);
                {
                this.state = 249;
                this.match(CfgParser.TBASE);
                }
                break;
            case 2:
                this.enterOuterAlt(localContext, 2);
                {
                this.state = 250;
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
            this.state = 276;
            this.errorHandler.sync(this);
            switch (this.tokenStream.LA(1)) {
            case CfgParser.REF:
                this.enterOuterAlt(localContext, 1);
                {
                this.state = 253;
                this.match(CfgParser.REF);
                this.state = 269;
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
                    {
                    this.state = 254;
                    this.ns_ident();
                    this.state = 256;
                    this.errorHandler.sync(this);
                    _la = this.tokenStream.LA(1);
                    if (_la === 15) {
                        {
                        this.state = 255;
                        this.key();
                        }
                    }

                    this.state = 260;
                    this.errorHandler.sync(this);
                    _la = this.tokenStream.LA(1);
                    if (_la === 21) {
                        {
                        this.state = 258;
                        this.match(CfgParser.SCOPE);
                        this.state = 259;
                        this.ns_ident();
                        }
                    }

                    }
                    break;
                case CfgParser.SCOPE:
                    {
                    this.state = 262;
                    this.match(CfgParser.SCOPE);
                    this.state = 263;
                    this.ns_ident();
                    }
                    break;
                case CfgParser.AT:
                    {
                    this.state = 264;
                    this.match(CfgParser.AT);
                    this.state = 265;
                    this.ns_ident();
                    this.state = 266;
                    this.match(CfgParser.SCOPE);
                    this.state = 267;
                    this.ns_ident();
                    }
                    break;
                default:
                    throw new antlr.NoViableAltException(this);
                }
                }
                break;
            case CfgParser.LISTREF:
                this.enterOuterAlt(localContext, 2);
                {
                this.state = 271;
                this.match(CfgParser.LISTREF);
                this.state = 272;
                this.ns_ident();
                this.state = 274;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
                if (_la === 15) {
                    {
                    this.state = 273;
                    this.key();
                    }
                }

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
    public source_values(): Source_valuesContext {
        let localContext = new Source_valuesContext(this.context, this.state);
        this.enterRule(localContext, 28, CfgParser.RULE_source_values);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 278;
            this.match(CfgParser.LB);
            this.state = 287;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            if (((((_la - 3)) & ~0x1F) === 0 && ((1 << (_la - 3)) & 537657471) !== 0)) {
                {
                this.state = 279;
                this.source_value();
                this.state = 284;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
                while (_la === 19) {
                    {
                    {
                    this.state = 280;
                    this.match(CfgParser.COMMA);
                    this.state = 281;
                    this.source_value();
                    }
                    }
                    this.state = 286;
                    this.errorHandler.sync(this);
                    _la = this.tokenStream.LA(1);
                }
                }
            }

            this.state = 289;
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
    public source_value(): Source_valueContext {
        let localContext = new Source_valueContext(this.context, this.state);
        this.enterRule(localContext, 30, CfgParser.RULE_source_value);
        try {
            this.state = 299;
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
                this.state = 291;
                this.ns_ident();
                }
                break;
            case CfgParser.SCOPE:
                this.enterOuterAlt(localContext, 2);
                {
                this.state = 292;
                this.match(CfgParser.SCOPE);
                this.state = 293;
                this.ns_ident();
                }
                break;
            case CfgParser.AT:
                this.enterOuterAlt(localContext, 3);
                {
                this.state = 294;
                this.match(CfgParser.AT);
                this.state = 295;
                this.ns_ident();
                this.state = 296;
                this.match(CfgParser.SCOPE);
                this.state = 297;
                this.ns_ident();
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
    public key(): KeyContext {
        let localContext = new KeyContext(this.context, this.state);
        this.enterRule(localContext, 32, CfgParser.RULE_key);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 301;
            this.match(CfgParser.LB);
            this.state = 302;
            this.identifier();
            this.state = 307;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 19) {
                {
                {
                this.state = 303;
                this.match(CfgParser.COMMA);
                this.state = 304;
                this.identifier();
                }
                }
                this.state = 309;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 310;
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
        this.enterRule(localContext, 34, CfgParser.RULE_metadata);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 323;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            if (_la === 13) {
                {
                this.state = 312;
                this.match(CfgParser.LP);
                this.state = 313;
                this.ident_with_opt_single_value();
                this.state = 318;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
                while (_la === 19) {
                    {
                    {
                    this.state = 314;
                    this.match(CfgParser.COMMA);
                    this.state = 315;
                    this.ident_with_opt_single_value();
                    }
                    }
                    this.state = 320;
                    this.errorHandler.sync(this);
                    _la = this.tokenStream.LA(1);
                }
                this.state = 321;
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
        this.enterRule(localContext, 36, CfgParser.RULE_ident_with_opt_single_value);
        let _la: number;
        try {
            this.state = 331;
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
                this.state = 325;
                this.identifier();
                this.state = 328;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
                if (_la === 12) {
                    {
                    this.state = 326;
                    this.match(CfgParser.EQ);
                    this.state = 327;
                    this.single_value();
                    }
                }

                }
                break;
            case CfgParser.MINUS:
                this.enterOuterAlt(localContext, 2);
                {
                this.state = 330;
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
        this.enterRule(localContext, 38, CfgParser.RULE_minus_ident);
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 333;
            this.match(CfgParser.MINUS);
            this.state = 334;
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
        this.enterRule(localContext, 40, CfgParser.RULE_single_value);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 336;
            _la = this.tokenStream.LA(1);
            if(!((((_la) & ~0x1F) === 0 && ((1 << _la) & 4160749568) !== 0))) {
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
        this.enterRule(localContext, 42, CfgParser.RULE_enum_number);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 338;
            _la = this.tokenStream.LA(1);
            if(!(_la === 29 || _la === 30)) {
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
        this.enterRule(localContext, 44, CfgParser.RULE_ns_ident);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 340;
            this.identifier();
            this.state = 345;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 18) {
                {
                {
                this.state = 341;
                this.match(CfgParser.DOT);
                this.state = 342;
                this.identifier();
                }
                }
                this.state = 347;
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
        this.enterRule(localContext, 46, CfgParser.RULE_identifier);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 348;
            _la = this.tokenStream.LA(1);
            if(!(((((_la - 3)) & ~0x1F) === 0 && ((1 << (_la - 3)) & 536871039) !== 0))) {
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
        this.enterRule(localContext, 48, CfgParser.RULE_leading_comment);
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 350;
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
        this.enterRule(localContext, 50, CfgParser.RULE_suffix_comment);
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 352;
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
        4,1,34,355,2,0,7,0,2,1,7,1,2,2,7,2,2,3,7,3,2,4,7,4,2,5,7,5,2,6,7,
        6,2,7,7,7,2,8,7,8,2,9,7,9,2,10,7,10,2,11,7,11,2,12,7,12,2,13,7,13,
        2,14,7,14,2,15,7,15,2,16,7,16,2,17,7,17,2,18,7,18,2,19,7,19,2,20,
        7,20,2,21,7,21,2,22,7,22,2,23,7,23,2,24,7,24,2,25,7,25,1,0,5,0,54,
        8,0,10,0,12,0,57,9,0,1,0,5,0,60,8,0,10,0,12,0,63,9,0,1,0,1,0,1,1,
        1,1,1,1,1,1,3,1,71,8,1,1,2,5,2,74,8,2,10,2,12,2,77,9,2,1,2,1,2,1,
        2,1,2,1,2,1,2,5,2,85,8,2,10,2,12,2,88,9,2,1,2,5,2,91,8,2,10,2,12,
        2,94,9,2,1,2,1,2,1,3,5,3,99,8,3,10,3,12,3,102,9,3,1,3,1,3,1,3,1,
        3,1,3,4,3,109,8,3,11,3,12,3,110,1,3,5,3,114,8,3,10,3,12,3,117,9,
        3,1,3,1,3,1,4,5,4,122,8,4,10,4,12,4,125,9,4,1,4,1,4,1,4,1,4,1,4,
        1,4,1,4,1,4,4,4,135,8,4,11,4,12,4,136,1,4,5,4,140,8,4,10,4,12,4,
        143,9,4,1,4,1,4,1,5,5,5,148,8,5,10,5,12,5,151,9,5,1,5,1,5,1,5,1,
        5,1,5,4,5,158,8,5,11,5,12,5,159,1,5,4,5,163,8,5,11,5,12,5,164,3,
        5,167,8,5,1,5,5,5,170,8,5,10,5,12,5,173,9,5,1,5,1,5,1,6,5,6,178,
        8,6,10,6,12,6,181,9,6,1,6,1,6,1,6,1,6,3,6,187,8,6,1,6,1,6,1,6,1,
        7,5,7,193,8,7,10,7,12,7,196,9,7,1,7,1,7,1,7,1,7,1,7,1,7,1,7,1,7,
        1,8,5,8,207,8,8,10,8,12,8,210,9,8,1,8,1,8,1,8,1,9,5,9,216,8,9,10,
        9,12,9,219,9,9,1,9,1,9,1,9,1,10,5,10,225,8,10,10,10,12,10,228,9,
        10,1,10,1,10,1,10,1,10,1,10,1,11,1,11,1,11,1,11,1,11,1,11,1,11,1,
        11,1,11,1,11,1,11,1,11,1,11,3,11,248,8,11,1,12,1,12,3,12,252,8,12,
        1,13,1,13,1,13,3,13,257,8,13,1,13,1,13,3,13,261,8,13,1,13,1,13,1,
        13,1,13,1,13,1,13,1,13,3,13,270,8,13,1,13,1,13,1,13,3,13,275,8,13,
        3,13,277,8,13,1,14,1,14,1,14,1,14,5,14,283,8,14,10,14,12,14,286,
        9,14,3,14,288,8,14,1,14,1,14,1,15,1,15,1,15,1,15,1,15,1,15,1,15,
        1,15,3,15,300,8,15,1,16,1,16,1,16,1,16,5,16,306,8,16,10,16,12,16,
        309,9,16,1,16,1,16,1,17,1,17,1,17,1,17,5,17,317,8,17,10,17,12,17,
        320,9,17,1,17,1,17,3,17,324,8,17,1,18,1,18,1,18,3,18,329,8,18,1,
        18,3,18,332,8,18,1,19,1,19,1,19,1,20,1,20,1,21,1,21,1,22,1,22,1,
        22,5,22,344,8,22,10,22,12,22,347,9,22,1,23,1,23,1,24,1,24,1,25,1,
        25,1,25,0,0,26,0,2,4,6,8,10,12,14,16,18,20,22,24,26,28,30,32,34,
        36,38,40,42,44,46,48,50,0,3,1,0,27,31,1,0,29,30,2,0,3,9,32,32,376,
        0,55,1,0,0,0,2,70,1,0,0,0,4,75,1,0,0,0,6,100,1,0,0,0,8,123,1,0,0,
        0,10,149,1,0,0,0,12,179,1,0,0,0,14,194,1,0,0,0,16,208,1,0,0,0,18,
        217,1,0,0,0,20,226,1,0,0,0,22,247,1,0,0,0,24,251,1,0,0,0,26,276,
        1,0,0,0,28,278,1,0,0,0,30,299,1,0,0,0,32,301,1,0,0,0,34,323,1,0,
        0,0,36,331,1,0,0,0,38,333,1,0,0,0,40,336,1,0,0,0,42,338,1,0,0,0,
        44,340,1,0,0,0,46,348,1,0,0,0,48,350,1,0,0,0,50,352,1,0,0,0,52,54,
        3,2,1,0,53,52,1,0,0,0,54,57,1,0,0,0,55,53,1,0,0,0,55,56,1,0,0,0,
        56,61,1,0,0,0,57,55,1,0,0,0,58,60,3,50,25,0,59,58,1,0,0,0,60,63,
        1,0,0,0,61,59,1,0,0,0,61,62,1,0,0,0,62,64,1,0,0,0,63,61,1,0,0,0,
        64,65,5,0,0,1,65,1,1,0,0,0,66,71,3,4,2,0,67,71,3,6,3,0,68,71,3,8,
        4,0,69,71,3,10,5,0,70,66,1,0,0,0,70,67,1,0,0,0,70,68,1,0,0,0,70,
        69,1,0,0,0,71,3,1,0,0,0,72,74,3,48,24,0,73,72,1,0,0,0,74,77,1,0,
        0,0,75,73,1,0,0,0,75,76,1,0,0,0,76,78,1,0,0,0,77,75,1,0,0,0,78,79,
        5,3,0,0,79,80,3,44,22,0,80,81,3,34,17,0,81,86,5,25,0,0,82,85,3,12,
        6,0,83,85,3,14,7,0,84,82,1,0,0,0,84,83,1,0,0,0,85,88,1,0,0,0,86,
        84,1,0,0,0,86,87,1,0,0,0,87,92,1,0,0,0,88,86,1,0,0,0,89,91,3,50,
        25,0,90,89,1,0,0,0,91,94,1,0,0,0,92,90,1,0,0,0,92,93,1,0,0,0,93,
        95,1,0,0,0,94,92,1,0,0,0,95,96,5,17,0,0,96,5,1,0,0,0,97,99,3,48,
        24,0,98,97,1,0,0,0,99,102,1,0,0,0,100,98,1,0,0,0,100,101,1,0,0,0,
        101,103,1,0,0,0,102,100,1,0,0,0,103,104,5,4,0,0,104,105,3,44,22,
        0,105,106,3,34,17,0,106,108,5,25,0,0,107,109,3,4,2,0,108,107,1,0,
        0,0,109,110,1,0,0,0,110,108,1,0,0,0,110,111,1,0,0,0,111,115,1,0,
        0,0,112,114,3,50,25,0,113,112,1,0,0,0,114,117,1,0,0,0,115,113,1,
        0,0,0,115,116,1,0,0,0,116,118,1,0,0,0,117,115,1,0,0,0,118,119,5,
        17,0,0,119,7,1,0,0,0,120,122,3,48,24,0,121,120,1,0,0,0,122,125,1,
        0,0,0,123,121,1,0,0,0,123,124,1,0,0,0,124,126,1,0,0,0,125,123,1,
        0,0,0,126,127,5,5,0,0,127,128,3,44,22,0,128,129,3,32,16,0,129,130,
        3,34,17,0,130,134,5,25,0,0,131,135,3,12,6,0,132,135,3,14,7,0,133,
        135,3,16,8,0,134,131,1,0,0,0,134,132,1,0,0,0,134,133,1,0,0,0,135,
        136,1,0,0,0,136,134,1,0,0,0,136,137,1,0,0,0,137,141,1,0,0,0,138,
        140,3,50,25,0,139,138,1,0,0,0,140,143,1,0,0,0,141,139,1,0,0,0,141,
        142,1,0,0,0,142,144,1,0,0,0,143,141,1,0,0,0,144,145,5,17,0,0,145,
        9,1,0,0,0,146,148,3,48,24,0,147,146,1,0,0,0,148,151,1,0,0,0,149,
        147,1,0,0,0,149,150,1,0,0,0,150,152,1,0,0,0,151,149,1,0,0,0,152,
        153,5,6,0,0,153,154,3,44,22,0,154,155,3,34,17,0,155,166,5,25,0,0,
        156,158,3,18,9,0,157,156,1,0,0,0,158,159,1,0,0,0,159,157,1,0,0,0,
        159,160,1,0,0,0,160,167,1,0,0,0,161,163,3,20,10,0,162,161,1,0,0,
        0,163,164,1,0,0,0,164,162,1,0,0,0,164,165,1,0,0,0,165,167,1,0,0,
        0,166,157,1,0,0,0,166,162,1,0,0,0,166,167,1,0,0,0,167,171,1,0,0,
        0,168,170,3,50,25,0,169,168,1,0,0,0,170,173,1,0,0,0,171,169,1,0,
        0,0,171,172,1,0,0,0,172,174,1,0,0,0,173,171,1,0,0,0,174,175,5,17,
        0,0,175,11,1,0,0,0,176,178,3,48,24,0,177,176,1,0,0,0,178,181,1,0,
        0,0,179,177,1,0,0,0,179,180,1,0,0,0,180,182,1,0,0,0,181,179,1,0,
        0,0,182,183,3,46,23,0,183,184,5,20,0,0,184,186,3,22,11,0,185,187,
        3,26,13,0,186,185,1,0,0,0,186,187,1,0,0,0,187,188,1,0,0,0,188,189,
        3,34,17,0,189,190,5,26,0,0,190,13,1,0,0,0,191,193,3,48,24,0,192,
        191,1,0,0,0,193,196,1,0,0,0,194,192,1,0,0,0,194,195,1,0,0,0,195,
        197,1,0,0,0,196,194,1,0,0,0,197,198,5,10,0,0,198,199,3,46,23,0,199,
        200,5,20,0,0,200,201,3,28,14,0,201,202,3,26,13,0,202,203,3,34,17,
        0,203,204,5,26,0,0,204,15,1,0,0,0,205,207,3,48,24,0,206,205,1,0,
        0,0,207,210,1,0,0,0,208,206,1,0,0,0,208,209,1,0,0,0,209,211,1,0,
        0,0,210,208,1,0,0,0,211,212,3,32,16,0,212,213,5,26,0,0,213,17,1,
        0,0,0,214,216,3,48,24,0,215,214,1,0,0,0,216,219,1,0,0,0,217,215,
        1,0,0,0,217,218,1,0,0,0,218,220,1,0,0,0,219,217,1,0,0,0,220,221,
        3,46,23,0,221,222,5,26,0,0,222,19,1,0,0,0,223,225,3,48,24,0,224,
        223,1,0,0,0,225,228,1,0,0,0,226,224,1,0,0,0,226,227,1,0,0,0,227,
        229,1,0,0,0,228,226,1,0,0,0,229,230,3,46,23,0,230,231,5,12,0,0,231,
        232,3,42,21,0,232,233,5,26,0,0,233,21,1,0,0,0,234,235,5,7,0,0,235,
        236,5,1,0,0,236,237,3,24,12,0,237,238,5,2,0,0,238,248,1,0,0,0,239,
        240,5,8,0,0,240,241,5,1,0,0,241,242,3,24,12,0,242,243,5,19,0,0,243,
        244,3,24,12,0,244,245,5,2,0,0,245,248,1,0,0,0,246,248,3,24,12,0,
        247,234,1,0,0,0,247,239,1,0,0,0,247,246,1,0,0,0,248,23,1,0,0,0,249,
        252,5,9,0,0,250,252,3,44,22,0,251,249,1,0,0,0,251,250,1,0,0,0,252,
        25,1,0,0,0,253,269,5,10,0,0,254,256,3,44,22,0,255,257,3,32,16,0,
        256,255,1,0,0,0,256,257,1,0,0,0,257,260,1,0,0,0,258,259,5,21,0,0,
        259,261,3,44,22,0,260,258,1,0,0,0,260,261,1,0,0,0,261,270,1,0,0,
        0,262,263,5,21,0,0,263,270,3,44,22,0,264,265,5,22,0,0,265,266,3,
        44,22,0,266,267,5,21,0,0,267,268,3,44,22,0,268,270,1,0,0,0,269,254,
        1,0,0,0,269,262,1,0,0,0,269,264,1,0,0,0,270,277,1,0,0,0,271,272,
        5,11,0,0,272,274,3,44,22,0,273,275,3,32,16,0,274,273,1,0,0,0,274,
        275,1,0,0,0,275,277,1,0,0,0,276,253,1,0,0,0,276,271,1,0,0,0,277,
        27,1,0,0,0,278,287,5,15,0,0,279,284,3,30,15,0,280,281,5,19,0,0,281,
        283,3,30,15,0,282,280,1,0,0,0,283,286,1,0,0,0,284,282,1,0,0,0,284,
        285,1,0,0,0,285,288,1,0,0,0,286,284,1,0,0,0,287,279,1,0,0,0,287,
        288,1,0,0,0,288,289,1,0,0,0,289,290,5,16,0,0,290,29,1,0,0,0,291,
        300,3,44,22,0,292,293,5,21,0,0,293,300,3,44,22,0,294,295,5,22,0,
        0,295,296,3,44,22,0,296,297,5,21,0,0,297,298,3,44,22,0,298,300,1,
        0,0,0,299,291,1,0,0,0,299,292,1,0,0,0,299,294,1,0,0,0,300,31,1,0,
        0,0,301,302,5,15,0,0,302,307,3,46,23,0,303,304,5,19,0,0,304,306,
        3,46,23,0,305,303,1,0,0,0,306,309,1,0,0,0,307,305,1,0,0,0,307,308,
        1,0,0,0,308,310,1,0,0,0,309,307,1,0,0,0,310,311,5,16,0,0,311,33,
        1,0,0,0,312,313,5,13,0,0,313,318,3,36,18,0,314,315,5,19,0,0,315,
        317,3,36,18,0,316,314,1,0,0,0,317,320,1,0,0,0,318,316,1,0,0,0,318,
        319,1,0,0,0,319,321,1,0,0,0,320,318,1,0,0,0,321,322,5,14,0,0,322,
        324,1,0,0,0,323,312,1,0,0,0,323,324,1,0,0,0,324,35,1,0,0,0,325,328,
        3,46,23,0,326,327,5,12,0,0,327,329,3,40,20,0,328,326,1,0,0,0,328,
        329,1,0,0,0,329,332,1,0,0,0,330,332,3,38,19,0,331,325,1,0,0,0,331,
        330,1,0,0,0,332,37,1,0,0,0,333,334,5,24,0,0,334,335,3,46,23,0,335,
        39,1,0,0,0,336,337,7,0,0,0,337,41,1,0,0,0,338,339,7,1,0,0,339,43,
        1,0,0,0,340,345,3,46,23,0,341,342,5,18,0,0,342,344,3,46,23,0,343,
        341,1,0,0,0,344,347,1,0,0,0,345,343,1,0,0,0,345,346,1,0,0,0,346,
        45,1,0,0,0,347,345,1,0,0,0,348,349,7,2,0,0,349,47,1,0,0,0,350,351,
        5,33,0,0,351,49,1,0,0,0,352,353,5,33,0,0,353,51,1,0,0,0,41,55,61,
        70,75,84,86,92,100,110,115,123,134,136,141,149,159,164,166,171,179,
        186,194,208,217,226,247,251,256,260,269,274,276,284,287,299,307,
        318,323,328,331,345
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
    public source_values(): Source_valuesContext {
        return this.getRuleContext(0, Source_valuesContext)!;
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
    public REF(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.REF, 0);
    }
    public ns_ident(): Ns_identContext[];
    public ns_ident(i: number): Ns_identContext | null;
    public ns_ident(i?: number): Ns_identContext[] | Ns_identContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Ns_identContext);
        }

        return this.getRuleContext(i, Ns_identContext);
    }
    public SCOPE(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.SCOPE, 0);
    }
    public AT(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.AT, 0);
    }
    public key(): KeyContext | null {
        return this.getRuleContext(0, KeyContext);
    }
    public LISTREF(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.LISTREF, 0);
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


export class Source_valuesContext extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public LB(): antlr.TerminalNode {
        return this.getToken(CfgParser.LB, 0)!;
    }
    public RB(): antlr.TerminalNode {
        return this.getToken(CfgParser.RB, 0)!;
    }
    public source_value(): Source_valueContext[];
    public source_value(i: number): Source_valueContext | null;
    public source_value(i?: number): Source_valueContext[] | Source_valueContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Source_valueContext);
        }

        return this.getRuleContext(i, Source_valueContext);
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
        return CfgParser.RULE_source_values;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitSource_values) {
            return visitor.visitSource_values(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}


export class Source_valueContext extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public ns_ident(): Ns_identContext[];
    public ns_ident(i: number): Ns_identContext | null;
    public ns_ident(i?: number): Ns_identContext[] | Ns_identContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Ns_identContext);
        }

        return this.getRuleContext(i, Ns_identContext);
    }
    public SCOPE(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.SCOPE, 0);
    }
    public AT(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.AT, 0);
    }
    public override get ruleIndex(): number {
        return CfgParser.RULE_source_value;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitSource_value) {
            return visitor.visitSource_value(this);
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
