
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
    public static readonly TLIST = 6;
    public static readonly TMAP = 7;
    public static readonly TBASE = 8;
    public static readonly REF = 9;
    public static readonly LISTREF = 10;
    public static readonly COMMENT = 11;
    public static readonly IDENT = 12;
    public static readonly SEMI = 13;
    public static readonly EQ = 14;
    public static readonly LP = 15;
    public static readonly RP = 16;
    public static readonly LB = 17;
    public static readonly RB = 18;
    public static readonly LC = 19;
    public static readonly RC = 20;
    public static readonly DOT = 21;
    public static readonly COMMA = 22;
    public static readonly COLON = 23;
    public static readonly PLUS = 24;
    public static readonly MINUS = 25;
    public static readonly STRING_CONSTANT = 26;
    public static readonly INTEGER_CONSTANT = 27;
    public static readonly HEX_INTEGER_CONSTANT = 28;
    public static readonly FLOAT_CONSTANT = 29;
    public static readonly WS = 30;
    public static readonly RULE_schema = 0;
    public static readonly RULE_schema_ele = 1;
    public static readonly RULE_struct_decl = 2;
    public static readonly RULE_interface_decl = 3;
    public static readonly RULE_table_decl = 4;
    public static readonly RULE_field_decl = 5;
    public static readonly RULE_foreign_decl = 6;
    public static readonly RULE_type_ = 7;
    public static readonly RULE_type_ele = 8;
    public static readonly RULE_ref = 9;
    public static readonly RULE_key_decl = 10;
    public static readonly RULE_key = 11;
    public static readonly RULE_metadata = 12;
    public static readonly RULE_ident_with_opt_single_value = 13;
    public static readonly RULE_minus_ident = 14;
    public static readonly RULE_single_value = 15;
    public static readonly RULE_ns_ident = 16;
    public static readonly RULE_identifier = 17;
    public static readonly RULE_keywords = 18;

    public static readonly literalNames = [
        null, "'<'", "'>'", "'struct'", "'interface'", "'table'", "'list'", 
        "'map'", null, "'->'", "'=>'", null, null, "';'", "'='", "'('", 
        "')'", "'['", "']'", "'{'", "'}'", "'.'", "','", "':'", "'+'", "'-'"
    ];

    public static readonly symbolicNames = [
        null, null, null, "STRUCT", "INTERFACE", "TABLE", "TLIST", "TMAP", 
        "TBASE", "REF", "LISTREF", "COMMENT", "IDENT", "SEMI", "EQ", "LP", 
        "RP", "LB", "RB", "LC", "RC", "DOT", "COMMA", "COLON", "PLUS", "MINUS", 
        "STRING_CONSTANT", "INTEGER_CONSTANT", "HEX_INTEGER_CONSTANT", "FLOAT_CONSTANT", 
        "WS"
    ];
    public static readonly ruleNames = [
        "schema", "schema_ele", "struct_decl", "interface_decl", "table_decl", 
        "field_decl", "foreign_decl", "type_", "type_ele", "ref", "key_decl", 
        "key", "metadata", "ident_with_opt_single_value", "minus_ident", 
        "single_value", "ns_ident", "identifier", "keywords",
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
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 41;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while ((((_la) & ~0x1F) === 0 && ((1 << _la) & 56) !== 0)) {
                {
                {
                this.state = 38;
                this.schema_ele();
                }
                }
                this.state = 43;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 44;
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
            this.state = 49;
            this.errorHandler.sync(this);
            switch (this.tokenStream.LA(1)) {
            case CfgParser.STRUCT:
                this.enterOuterAlt(localContext, 1);
                {
                this.state = 46;
                this.struct_decl();
                }
                break;
            case CfgParser.INTERFACE:
                this.enterOuterAlt(localContext, 2);
                {
                this.state = 47;
                this.interface_decl();
                }
                break;
            case CfgParser.TABLE:
                this.enterOuterAlt(localContext, 3);
                {
                this.state = 48;
                this.table_decl();
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
    public struct_decl(): Struct_declContext {
        let localContext = new Struct_declContext(this.context, this.state);
        this.enterRule(localContext, 4, CfgParser.RULE_struct_decl);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 51;
            this.match(CfgParser.STRUCT);
            this.state = 52;
            this.ns_ident();
            this.state = 53;
            this.metadata();
            this.state = 54;
            this.match(CfgParser.LC);
            this.state = 56;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            if (_la === 11) {
                {
                this.state = 55;
                this.match(CfgParser.COMMENT);
                }
            }

            this.state = 61;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while ((((_la) & ~0x1F) === 0 && ((1 << _la) & 4600) !== 0)) {
                {
                {
                this.state = 58;
                this.field_decl();
                }
                }
                this.state = 63;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 67;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 9) {
                {
                {
                this.state = 64;
                this.foreign_decl();
                }
                }
                this.state = 69;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 70;
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
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 72;
            this.match(CfgParser.INTERFACE);
            this.state = 73;
            this.ns_ident();
            this.state = 74;
            this.metadata();
            this.state = 75;
            this.match(CfgParser.LC);
            this.state = 77;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            if (_la === 11) {
                {
                this.state = 76;
                this.match(CfgParser.COMMENT);
                }
            }

            this.state = 80;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            do {
                {
                {
                this.state = 79;
                this.struct_decl();
                }
                }
                this.state = 82;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            } while (_la === 3);
            this.state = 84;
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
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 86;
            this.match(CfgParser.TABLE);
            this.state = 87;
            this.ns_ident();
            this.state = 88;
            this.key();
            this.state = 89;
            this.metadata();
            this.state = 90;
            this.match(CfgParser.LC);
            this.state = 92;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            if (_la === 11) {
                {
                this.state = 91;
                this.match(CfgParser.COMMENT);
                }
            }

            this.state = 97;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 17) {
                {
                {
                this.state = 94;
                this.key_decl();
                }
                }
                this.state = 99;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 103;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while ((((_la) & ~0x1F) === 0 && ((1 << _la) & 4600) !== 0)) {
                {
                {
                this.state = 100;
                this.field_decl();
                }
                }
                this.state = 105;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 109;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 9) {
                {
                {
                this.state = 106;
                this.foreign_decl();
                }
                }
                this.state = 111;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 112;
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
        this.enterRule(localContext, 10, CfgParser.RULE_field_decl);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 114;
            this.identifier();
            this.state = 115;
            this.match(CfgParser.COLON);
            this.state = 116;
            this.type_();
            this.state = 118;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            if (_la === 9 || _la === 10) {
                {
                this.state = 117;
                this.ref();
                }
            }

            this.state = 120;
            this.metadata();
            this.state = 121;
            this.match(CfgParser.SEMI);
            this.state = 123;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            if (_la === 11) {
                {
                this.state = 122;
                this.match(CfgParser.COMMENT);
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
    public foreign_decl(): Foreign_declContext {
        let localContext = new Foreign_declContext(this.context, this.state);
        this.enterRule(localContext, 12, CfgParser.RULE_foreign_decl);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 125;
            this.match(CfgParser.REF);
            this.state = 126;
            this.identifier();
            this.state = 127;
            this.match(CfgParser.COLON);
            this.state = 128;
            this.key();
            this.state = 129;
            this.ref();
            this.state = 130;
            this.metadata();
            this.state = 131;
            this.match(CfgParser.SEMI);
            this.state = 133;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            if (_la === 11) {
                {
                this.state = 132;
                this.match(CfgParser.COMMENT);
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
    public type_(): Type_Context {
        let localContext = new Type_Context(this.context, this.state);
        this.enterRule(localContext, 14, CfgParser.RULE_type_);
        try {
            this.state = 148;
            this.errorHandler.sync(this);
            switch (this.interpreter.adaptivePredict(this.tokenStream, 14, this.context) ) {
            case 1:
                this.enterOuterAlt(localContext, 1);
                {
                this.state = 135;
                this.match(CfgParser.TLIST);
                this.state = 136;
                this.match(CfgParser.T__0);
                this.state = 137;
                this.type_ele();
                this.state = 138;
                this.match(CfgParser.T__1);
                }
                break;
            case 2:
                this.enterOuterAlt(localContext, 2);
                {
                this.state = 140;
                this.match(CfgParser.TMAP);
                this.state = 141;
                this.match(CfgParser.T__0);
                this.state = 142;
                this.type_ele();
                this.state = 143;
                this.match(CfgParser.COMMA);
                this.state = 144;
                this.type_ele();
                this.state = 145;
                this.match(CfgParser.T__1);
                }
                break;
            case 3:
                this.enterOuterAlt(localContext, 3);
                {
                this.state = 147;
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
        this.enterRule(localContext, 16, CfgParser.RULE_type_ele);
        try {
            this.state = 152;
            this.errorHandler.sync(this);
            switch (this.interpreter.adaptivePredict(this.tokenStream, 15, this.context) ) {
            case 1:
                this.enterOuterAlt(localContext, 1);
                {
                this.state = 150;
                this.match(CfgParser.TBASE);
                }
                break;
            case 2:
                this.enterOuterAlt(localContext, 2);
                {
                this.state = 151;
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
        this.enterRule(localContext, 18, CfgParser.RULE_ref);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 154;
            _la = this.tokenStream.LA(1);
            if(!(_la === 9 || _la === 10)) {
            this.errorHandler.recoverInline(this);
            }
            else {
                this.errorHandler.reportMatch(this);
                this.consume();
            }
            this.state = 155;
            this.ns_ident();
            this.state = 157;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            if (_la === 17) {
                {
                this.state = 156;
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
    public key_decl(): Key_declContext {
        let localContext = new Key_declContext(this.context, this.state);
        this.enterRule(localContext, 20, CfgParser.RULE_key_decl);
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 159;
            this.key();
            this.state = 160;
            this.match(CfgParser.SEMI);
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
        this.enterRule(localContext, 22, CfgParser.RULE_key);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 162;
            this.match(CfgParser.LB);
            this.state = 163;
            this.identifier();
            this.state = 168;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 22) {
                {
                {
                this.state = 164;
                this.match(CfgParser.COMMA);
                this.state = 165;
                this.identifier();
                }
                }
                this.state = 170;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 171;
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
        this.enterRule(localContext, 24, CfgParser.RULE_metadata);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 184;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            if (_la === 15) {
                {
                this.state = 173;
                this.match(CfgParser.LP);
                this.state = 174;
                this.ident_with_opt_single_value();
                this.state = 179;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
                while (_la === 22) {
                    {
                    {
                    this.state = 175;
                    this.match(CfgParser.COMMA);
                    this.state = 176;
                    this.ident_with_opt_single_value();
                    }
                    }
                    this.state = 181;
                    this.errorHandler.sync(this);
                    _la = this.tokenStream.LA(1);
                }
                this.state = 182;
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
        this.enterRule(localContext, 26, CfgParser.RULE_ident_with_opt_single_value);
        let _la: number;
        try {
            this.state = 192;
            this.errorHandler.sync(this);
            switch (this.tokenStream.LA(1)) {
            case CfgParser.STRUCT:
            case CfgParser.INTERFACE:
            case CfgParser.TABLE:
            case CfgParser.TLIST:
            case CfgParser.TMAP:
            case CfgParser.TBASE:
            case CfgParser.IDENT:
                this.enterOuterAlt(localContext, 1);
                {
                this.state = 186;
                this.identifier();
                this.state = 189;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
                if (_la === 14) {
                    {
                    this.state = 187;
                    this.match(CfgParser.EQ);
                    this.state = 188;
                    this.single_value();
                    }
                }

                }
                break;
            case CfgParser.MINUS:
                this.enterOuterAlt(localContext, 2);
                {
                this.state = 191;
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
        this.enterRule(localContext, 28, CfgParser.RULE_minus_ident);
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 194;
            this.match(CfgParser.MINUS);
            this.state = 195;
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
        this.enterRule(localContext, 30, CfgParser.RULE_single_value);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 197;
            _la = this.tokenStream.LA(1);
            if(!((((_la) & ~0x1F) === 0 && ((1 << _la) & 1006632960) !== 0))) {
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
        this.enterRule(localContext, 32, CfgParser.RULE_ns_ident);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 199;
            this.identifier();
            this.state = 204;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 21) {
                {
                {
                this.state = 200;
                this.match(CfgParser.DOT);
                this.state = 201;
                this.identifier();
                }
                }
                this.state = 206;
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
        this.enterRule(localContext, 34, CfgParser.RULE_identifier);
        try {
            this.state = 209;
            this.errorHandler.sync(this);
            switch (this.tokenStream.LA(1)) {
            case CfgParser.IDENT:
                this.enterOuterAlt(localContext, 1);
                {
                this.state = 207;
                this.match(CfgParser.IDENT);
                }
                break;
            case CfgParser.STRUCT:
            case CfgParser.INTERFACE:
            case CfgParser.TABLE:
            case CfgParser.TLIST:
            case CfgParser.TMAP:
            case CfgParser.TBASE:
                this.enterOuterAlt(localContext, 2);
                {
                this.state = 208;
                this.keywords();
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
    public keywords(): KeywordsContext {
        let localContext = new KeywordsContext(this.context, this.state);
        this.enterRule(localContext, 36, CfgParser.RULE_keywords);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 211;
            _la = this.tokenStream.LA(1);
            if(!((((_la) & ~0x1F) === 0 && ((1 << _la) & 504) !== 0))) {
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

    public static readonly _serializedATN: number[] = [
        4,1,30,214,2,0,7,0,2,1,7,1,2,2,7,2,2,3,7,3,2,4,7,4,2,5,7,5,2,6,7,
        6,2,7,7,7,2,8,7,8,2,9,7,9,2,10,7,10,2,11,7,11,2,12,7,12,2,13,7,13,
        2,14,7,14,2,15,7,15,2,16,7,16,2,17,7,17,2,18,7,18,1,0,5,0,40,8,0,
        10,0,12,0,43,9,0,1,0,1,0,1,1,1,1,1,1,3,1,50,8,1,1,2,1,2,1,2,1,2,
        1,2,3,2,57,8,2,1,2,5,2,60,8,2,10,2,12,2,63,9,2,1,2,5,2,66,8,2,10,
        2,12,2,69,9,2,1,2,1,2,1,3,1,3,1,3,1,3,1,3,3,3,78,8,3,1,3,4,3,81,
        8,3,11,3,12,3,82,1,3,1,3,1,4,1,4,1,4,1,4,1,4,1,4,3,4,93,8,4,1,4,
        5,4,96,8,4,10,4,12,4,99,9,4,1,4,5,4,102,8,4,10,4,12,4,105,9,4,1,
        4,5,4,108,8,4,10,4,12,4,111,9,4,1,4,1,4,1,5,1,5,1,5,1,5,3,5,119,
        8,5,1,5,1,5,1,5,3,5,124,8,5,1,6,1,6,1,6,1,6,1,6,1,6,1,6,1,6,3,6,
        134,8,6,1,7,1,7,1,7,1,7,1,7,1,7,1,7,1,7,1,7,1,7,1,7,1,7,1,7,3,7,
        149,8,7,1,8,1,8,3,8,153,8,8,1,9,1,9,1,9,3,9,158,8,9,1,10,1,10,1,
        10,1,11,1,11,1,11,1,11,5,11,167,8,11,10,11,12,11,170,9,11,1,11,1,
        11,1,12,1,12,1,12,1,12,5,12,178,8,12,10,12,12,12,181,9,12,1,12,1,
        12,3,12,185,8,12,1,13,1,13,1,13,3,13,190,8,13,1,13,3,13,193,8,13,
        1,14,1,14,1,14,1,15,1,15,1,16,1,16,1,16,5,16,203,8,16,10,16,12,16,
        206,9,16,1,17,1,17,3,17,210,8,17,1,18,1,18,1,18,0,0,19,0,2,4,6,8,
        10,12,14,16,18,20,22,24,26,28,30,32,34,36,0,3,1,0,9,10,1,0,26,29,
        1,0,3,8,220,0,41,1,0,0,0,2,49,1,0,0,0,4,51,1,0,0,0,6,72,1,0,0,0,
        8,86,1,0,0,0,10,114,1,0,0,0,12,125,1,0,0,0,14,148,1,0,0,0,16,152,
        1,0,0,0,18,154,1,0,0,0,20,159,1,0,0,0,22,162,1,0,0,0,24,184,1,0,
        0,0,26,192,1,0,0,0,28,194,1,0,0,0,30,197,1,0,0,0,32,199,1,0,0,0,
        34,209,1,0,0,0,36,211,1,0,0,0,38,40,3,2,1,0,39,38,1,0,0,0,40,43,
        1,0,0,0,41,39,1,0,0,0,41,42,1,0,0,0,42,44,1,0,0,0,43,41,1,0,0,0,
        44,45,5,0,0,1,45,1,1,0,0,0,46,50,3,4,2,0,47,50,3,6,3,0,48,50,3,8,
        4,0,49,46,1,0,0,0,49,47,1,0,0,0,49,48,1,0,0,0,50,3,1,0,0,0,51,52,
        5,3,0,0,52,53,3,32,16,0,53,54,3,24,12,0,54,56,5,19,0,0,55,57,5,11,
        0,0,56,55,1,0,0,0,56,57,1,0,0,0,57,61,1,0,0,0,58,60,3,10,5,0,59,
        58,1,0,0,0,60,63,1,0,0,0,61,59,1,0,0,0,61,62,1,0,0,0,62,67,1,0,0,
        0,63,61,1,0,0,0,64,66,3,12,6,0,65,64,1,0,0,0,66,69,1,0,0,0,67,65,
        1,0,0,0,67,68,1,0,0,0,68,70,1,0,0,0,69,67,1,0,0,0,70,71,5,20,0,0,
        71,5,1,0,0,0,72,73,5,4,0,0,73,74,3,32,16,0,74,75,3,24,12,0,75,77,
        5,19,0,0,76,78,5,11,0,0,77,76,1,0,0,0,77,78,1,0,0,0,78,80,1,0,0,
        0,79,81,3,4,2,0,80,79,1,0,0,0,81,82,1,0,0,0,82,80,1,0,0,0,82,83,
        1,0,0,0,83,84,1,0,0,0,84,85,5,20,0,0,85,7,1,0,0,0,86,87,5,5,0,0,
        87,88,3,32,16,0,88,89,3,22,11,0,89,90,3,24,12,0,90,92,5,19,0,0,91,
        93,5,11,0,0,92,91,1,0,0,0,92,93,1,0,0,0,93,97,1,0,0,0,94,96,3,20,
        10,0,95,94,1,0,0,0,96,99,1,0,0,0,97,95,1,0,0,0,97,98,1,0,0,0,98,
        103,1,0,0,0,99,97,1,0,0,0,100,102,3,10,5,0,101,100,1,0,0,0,102,105,
        1,0,0,0,103,101,1,0,0,0,103,104,1,0,0,0,104,109,1,0,0,0,105,103,
        1,0,0,0,106,108,3,12,6,0,107,106,1,0,0,0,108,111,1,0,0,0,109,107,
        1,0,0,0,109,110,1,0,0,0,110,112,1,0,0,0,111,109,1,0,0,0,112,113,
        5,20,0,0,113,9,1,0,0,0,114,115,3,34,17,0,115,116,5,23,0,0,116,118,
        3,14,7,0,117,119,3,18,9,0,118,117,1,0,0,0,118,119,1,0,0,0,119,120,
        1,0,0,0,120,121,3,24,12,0,121,123,5,13,0,0,122,124,5,11,0,0,123,
        122,1,0,0,0,123,124,1,0,0,0,124,11,1,0,0,0,125,126,5,9,0,0,126,127,
        3,34,17,0,127,128,5,23,0,0,128,129,3,22,11,0,129,130,3,18,9,0,130,
        131,3,24,12,0,131,133,5,13,0,0,132,134,5,11,0,0,133,132,1,0,0,0,
        133,134,1,0,0,0,134,13,1,0,0,0,135,136,5,6,0,0,136,137,5,1,0,0,137,
        138,3,16,8,0,138,139,5,2,0,0,139,149,1,0,0,0,140,141,5,7,0,0,141,
        142,5,1,0,0,142,143,3,16,8,0,143,144,5,22,0,0,144,145,3,16,8,0,145,
        146,5,2,0,0,146,149,1,0,0,0,147,149,3,16,8,0,148,135,1,0,0,0,148,
        140,1,0,0,0,148,147,1,0,0,0,149,15,1,0,0,0,150,153,5,8,0,0,151,153,
        3,32,16,0,152,150,1,0,0,0,152,151,1,0,0,0,153,17,1,0,0,0,154,155,
        7,0,0,0,155,157,3,32,16,0,156,158,3,22,11,0,157,156,1,0,0,0,157,
        158,1,0,0,0,158,19,1,0,0,0,159,160,3,22,11,0,160,161,5,13,0,0,161,
        21,1,0,0,0,162,163,5,17,0,0,163,168,3,34,17,0,164,165,5,22,0,0,165,
        167,3,34,17,0,166,164,1,0,0,0,167,170,1,0,0,0,168,166,1,0,0,0,168,
        169,1,0,0,0,169,171,1,0,0,0,170,168,1,0,0,0,171,172,5,18,0,0,172,
        23,1,0,0,0,173,174,5,15,0,0,174,179,3,26,13,0,175,176,5,22,0,0,176,
        178,3,26,13,0,177,175,1,0,0,0,178,181,1,0,0,0,179,177,1,0,0,0,179,
        180,1,0,0,0,180,182,1,0,0,0,181,179,1,0,0,0,182,183,5,16,0,0,183,
        185,1,0,0,0,184,173,1,0,0,0,184,185,1,0,0,0,185,25,1,0,0,0,186,189,
        3,34,17,0,187,188,5,14,0,0,188,190,3,30,15,0,189,187,1,0,0,0,189,
        190,1,0,0,0,190,193,1,0,0,0,191,193,3,28,14,0,192,186,1,0,0,0,192,
        191,1,0,0,0,193,27,1,0,0,0,194,195,5,25,0,0,195,196,3,34,17,0,196,
        29,1,0,0,0,197,198,7,1,0,0,198,31,1,0,0,0,199,204,3,34,17,0,200,
        201,5,21,0,0,201,203,3,34,17,0,202,200,1,0,0,0,203,206,1,0,0,0,204,
        202,1,0,0,0,204,205,1,0,0,0,205,33,1,0,0,0,206,204,1,0,0,0,207,210,
        5,12,0,0,208,210,3,36,18,0,209,207,1,0,0,0,209,208,1,0,0,0,210,35,
        1,0,0,0,211,212,7,2,0,0,212,37,1,0,0,0,24,41,49,56,61,67,77,82,92,
        97,103,109,118,123,133,148,152,157,168,179,184,189,192,204,209
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
    public LC(): antlr.TerminalNode {
        return this.getToken(CfgParser.LC, 0)!;
    }
    public RC(): antlr.TerminalNode {
        return this.getToken(CfgParser.RC, 0)!;
    }
    public COMMENT(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.COMMENT, 0);
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
    public LC(): antlr.TerminalNode {
        return this.getToken(CfgParser.LC, 0)!;
    }
    public RC(): antlr.TerminalNode {
        return this.getToken(CfgParser.RC, 0)!;
    }
    public COMMENT(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.COMMENT, 0);
    }
    public struct_decl(): Struct_declContext[];
    public struct_decl(i: number): Struct_declContext | null;
    public struct_decl(i?: number): Struct_declContext[] | Struct_declContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Struct_declContext);
        }

        return this.getRuleContext(i, Struct_declContext);
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
    public LC(): antlr.TerminalNode {
        return this.getToken(CfgParser.LC, 0)!;
    }
    public RC(): antlr.TerminalNode {
        return this.getToken(CfgParser.RC, 0)!;
    }
    public COMMENT(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.COMMENT, 0);
    }
    public key_decl(): Key_declContext[];
    public key_decl(i: number): Key_declContext | null;
    public key_decl(i?: number): Key_declContext[] | Key_declContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Key_declContext);
        }

        return this.getRuleContext(i, Key_declContext);
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
    public SEMI(): antlr.TerminalNode {
        return this.getToken(CfgParser.SEMI, 0)!;
    }
    public ref(): RefContext | null {
        return this.getRuleContext(0, RefContext);
    }
    public COMMENT(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.COMMENT, 0);
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
    public SEMI(): antlr.TerminalNode {
        return this.getToken(CfgParser.SEMI, 0)!;
    }
    public COMMENT(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.COMMENT, 0);
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


export class Type_Context extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public TLIST(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.TLIST, 0);
    }
    public type_ele(): Type_eleContext[];
    public type_ele(i: number): Type_eleContext | null;
    public type_ele(i?: number): Type_eleContext[] | Type_eleContext | null {
        if (i === undefined) {
            return this.getRuleContexts(Type_eleContext);
        }

        return this.getRuleContext(i, Type_eleContext);
    }
    public TMAP(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.TMAP, 0);
    }
    public COMMA(): antlr.TerminalNode | null {
        return this.getToken(CfgParser.COMMA, 0);
    }
    public override get ruleIndex(): number {
        return CfgParser.RULE_type_;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitType_) {
            return visitor.visitType_(this);
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


export class Key_declContext extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public key(): KeyContext {
        return this.getRuleContext(0, KeyContext)!;
    }
    public SEMI(): antlr.TerminalNode {
        return this.getToken(CfgParser.SEMI, 0)!;
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
    public keywords(): KeywordsContext | null {
        return this.getRuleContext(0, KeywordsContext);
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


export class KeywordsContext extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
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
        return CfgParser.RULE_keywords;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitKeywords) {
            return visitor.visitKeywords(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}
