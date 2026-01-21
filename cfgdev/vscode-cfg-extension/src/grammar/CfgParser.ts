
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
    public static readonly EQ = 11;
    public static readonly LP = 12;
    public static readonly RP = 13;
    public static readonly LB = 14;
    public static readonly RB = 15;
    public static readonly RC = 16;
    public static readonly DOT = 17;
    public static readonly COMMA = 18;
    public static readonly COLON = 19;
    public static readonly PLUS = 20;
    public static readonly MINUS = 21;
    public static readonly LC_COMMENT = 22;
    public static readonly SEMI_COMMENT = 23;
    public static readonly BOOL_CONSTANT = 24;
    public static readonly FLOAT_CONSTANT = 25;
    public static readonly HEX_INTEGER_CONSTANT = 26;
    public static readonly INTEGER_CONSTANT = 27;
    public static readonly STRING_CONSTANT = 28;
    public static readonly IDENT = 29;
    public static readonly COMMENT = 30;
    public static readonly WS = 31;
    public static readonly RULE_schema = 0;
    public static readonly RULE_schema_ele = 1;
    public static readonly RULE_struct_decl = 2;
    public static readonly RULE_interface_decl = 3;
    public static readonly RULE_table_decl = 4;
    public static readonly RULE_field_decl = 5;
    public static readonly RULE_foreign_decl = 6;
    public static readonly RULE_key_decl = 7;
    public static readonly RULE_type_ = 8;
    public static readonly RULE_type_ele = 9;
    public static readonly RULE_ref = 10;
    public static readonly RULE_key = 11;
    public static readonly RULE_metadata = 12;
    public static readonly RULE_ident_with_opt_single_value = 13;
    public static readonly RULE_minus_ident = 14;
    public static readonly RULE_single_value = 15;
    public static readonly RULE_ns_ident = 16;
    public static readonly RULE_identifier = 17;
    public static readonly RULE_comment = 18;

    public static readonly literalNames = [
        null, "'<'", "'>'", "'struct'", "'interface'", "'table'", "'list'", 
        "'map'", null, "'->'", "'=>'", "'='", "'('", "')'", "'['", "']'", 
        "'}'", "'.'", "','", "':'", "'+'", "'-'"
    ];

    public static readonly symbolicNames = [
        null, null, null, "STRUCT", "INTERFACE", "TABLE", "TLIST", "TMAP", 
        "TBASE", "REF", "LISTREF", "EQ", "LP", "RP", "LB", "RB", "RC", "DOT", 
        "COMMA", "COLON", "PLUS", "MINUS", "LC_COMMENT", "SEMI_COMMENT", 
        "BOOL_CONSTANT", "FLOAT_CONSTANT", "HEX_INTEGER_CONSTANT", "INTEGER_CONSTANT", 
        "STRING_CONSTANT", "IDENT", "COMMENT", "WS"
    ];
    public static readonly ruleNames = [
        "schema", "schema_ele", "struct_decl", "interface_decl", "table_decl", 
        "field_decl", "foreign_decl", "key_decl", "type_", "type_ele", "ref", 
        "key", "metadata", "ident_with_opt_single_value", "minus_ident", 
        "single_value", "ns_ident", "identifier", "comment",
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
            while ((((_la) & ~0x1F) === 0 && ((1 << _la) & 1073741880) !== 0)) {
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
            switch (this.interpreter.adaptivePredict(this.tokenStream, 1, this.context) ) {
            case 1:
                this.enterOuterAlt(localContext, 1);
                {
                this.state = 46;
                this.struct_decl();
                }
                break;
            case 2:
                this.enterOuterAlt(localContext, 2);
                {
                this.state = 47;
                this.interface_decl();
                }
                break;
            case 3:
                this.enterOuterAlt(localContext, 3);
                {
                this.state = 48;
                this.table_decl();
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
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 54;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 30) {
                {
                {
                this.state = 51;
                this.comment();
                }
                }
                this.state = 56;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 57;
            this.match(CfgParser.STRUCT);
            this.state = 58;
            this.ns_ident();
            this.state = 59;
            this.metadata();
            this.state = 60;
            this.match(CfgParser.LC_COMMENT);
            this.state = 65;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while ((((_la) & ~0x1F) === 0 && ((1 << _la) & 1610613752) !== 0)) {
                {
                this.state = 63;
                this.errorHandler.sync(this);
                switch (this.interpreter.adaptivePredict(this.tokenStream, 3, this.context) ) {
                case 1:
                    {
                    this.state = 61;
                    this.field_decl();
                    }
                    break;
                case 2:
                    {
                    this.state = 62;
                    this.foreign_decl();
                    }
                    break;
                }
                }
                this.state = 67;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 68;
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
            this.state = 73;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 30) {
                {
                {
                this.state = 70;
                this.comment();
                }
                }
                this.state = 75;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 76;
            this.match(CfgParser.INTERFACE);
            this.state = 77;
            this.ns_ident();
            this.state = 78;
            this.metadata();
            this.state = 79;
            this.match(CfgParser.LC_COMMENT);
            this.state = 81;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            do {
                {
                {
                this.state = 80;
                this.struct_decl();
                }
                }
                this.state = 83;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            } while (_la === 3 || _la === 30);
            this.state = 85;
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
            this.state = 90;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 30) {
                {
                {
                this.state = 87;
                this.comment();
                }
                }
                this.state = 92;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 93;
            this.match(CfgParser.TABLE);
            this.state = 94;
            this.ns_ident();
            this.state = 95;
            this.key();
            this.state = 96;
            this.metadata();
            this.state = 97;
            this.match(CfgParser.LC_COMMENT);
            this.state = 101;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            do {
                {
                this.state = 101;
                this.errorHandler.sync(this);
                switch (this.interpreter.adaptivePredict(this.tokenStream, 8, this.context) ) {
                case 1:
                    {
                    this.state = 98;
                    this.field_decl();
                    }
                    break;
                case 2:
                    {
                    this.state = 99;
                    this.foreign_decl();
                    }
                    break;
                case 3:
                    {
                    this.state = 100;
                    this.key_decl();
                    }
                    break;
                }
                }
                this.state = 103;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            } while ((((_la) & ~0x1F) === 0 && ((1 << _la) & 1610630136) !== 0));
            this.state = 105;
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
            this.state = 110;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 30) {
                {
                {
                this.state = 107;
                this.comment();
                }
                }
                this.state = 112;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 113;
            this.identifier();
            this.state = 114;
            this.match(CfgParser.COLON);
            this.state = 115;
            this.type_();
            this.state = 117;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            if (_la === 9 || _la === 10) {
                {
                this.state = 116;
                this.ref();
                }
            }

            this.state = 119;
            this.metadata();
            this.state = 120;
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
        this.enterRule(localContext, 12, CfgParser.RULE_foreign_decl);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 125;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 30) {
                {
                {
                this.state = 122;
                this.comment();
                }
                }
                this.state = 127;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 128;
            this.match(CfgParser.REF);
            this.state = 129;
            this.identifier();
            this.state = 130;
            this.match(CfgParser.COLON);
            this.state = 131;
            this.key();
            this.state = 132;
            this.ref();
            this.state = 133;
            this.metadata();
            this.state = 134;
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
        this.enterRule(localContext, 14, CfgParser.RULE_key_decl);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 139;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 30) {
                {
                {
                this.state = 136;
                this.comment();
                }
                }
                this.state = 141;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 142;
            this.key();
            this.state = 143;
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
        this.enterRule(localContext, 16, CfgParser.RULE_type_);
        try {
            this.state = 158;
            this.errorHandler.sync(this);
            switch (this.interpreter.adaptivePredict(this.tokenStream, 14, this.context) ) {
            case 1:
                localContext = new TypeListContext(localContext);
                this.enterOuterAlt(localContext, 1);
                {
                this.state = 145;
                this.match(CfgParser.TLIST);
                this.state = 146;
                this.match(CfgParser.T__0);
                this.state = 147;
                this.type_ele();
                this.state = 148;
                this.match(CfgParser.T__1);
                }
                break;
            case 2:
                localContext = new TypeMapContext(localContext);
                this.enterOuterAlt(localContext, 2);
                {
                this.state = 150;
                this.match(CfgParser.TMAP);
                this.state = 151;
                this.match(CfgParser.T__0);
                this.state = 152;
                this.type_ele();
                this.state = 153;
                this.match(CfgParser.COMMA);
                this.state = 154;
                this.type_ele();
                this.state = 155;
                this.match(CfgParser.T__1);
                }
                break;
            case 3:
                localContext = new TypeBasicContext(localContext);
                this.enterOuterAlt(localContext, 3);
                {
                this.state = 157;
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
        this.enterRule(localContext, 18, CfgParser.RULE_type_ele);
        try {
            this.state = 162;
            this.errorHandler.sync(this);
            switch (this.interpreter.adaptivePredict(this.tokenStream, 15, this.context) ) {
            case 1:
                this.enterOuterAlt(localContext, 1);
                {
                this.state = 160;
                this.match(CfgParser.TBASE);
                }
                break;
            case 2:
                this.enterOuterAlt(localContext, 2);
                {
                this.state = 161;
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
        this.enterRule(localContext, 20, CfgParser.RULE_ref);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 164;
            _la = this.tokenStream.LA(1);
            if(!(_la === 9 || _la === 10)) {
            this.errorHandler.recoverInline(this);
            }
            else {
                this.errorHandler.reportMatch(this);
                this.consume();
            }
            this.state = 165;
            this.ns_ident();
            this.state = 167;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            if (_la === 14) {
                {
                this.state = 166;
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
        this.enterRule(localContext, 22, CfgParser.RULE_key);
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 169;
            this.match(CfgParser.LB);
            this.state = 170;
            this.identifier();
            this.state = 175;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 18) {
                {
                {
                this.state = 171;
                this.match(CfgParser.COMMA);
                this.state = 172;
                this.identifier();
                }
                }
                this.state = 177;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
            }
            this.state = 178;
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
            this.state = 191;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            if (_la === 12) {
                {
                this.state = 180;
                this.match(CfgParser.LP);
                this.state = 181;
                this.ident_with_opt_single_value();
                this.state = 186;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
                while (_la === 18) {
                    {
                    {
                    this.state = 182;
                    this.match(CfgParser.COMMA);
                    this.state = 183;
                    this.ident_with_opt_single_value();
                    }
                    }
                    this.state = 188;
                    this.errorHandler.sync(this);
                    _la = this.tokenStream.LA(1);
                }
                this.state = 189;
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
            this.state = 199;
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
                this.state = 193;
                this.identifier();
                this.state = 196;
                this.errorHandler.sync(this);
                _la = this.tokenStream.LA(1);
                if (_la === 11) {
                    {
                    this.state = 194;
                    this.match(CfgParser.EQ);
                    this.state = 195;
                    this.single_value();
                    }
                }

                }
                break;
            case CfgParser.MINUS:
                this.enterOuterAlt(localContext, 2);
                {
                this.state = 198;
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
            this.state = 201;
            this.match(CfgParser.MINUS);
            this.state = 202;
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
            this.state = 204;
            _la = this.tokenStream.LA(1);
            if(!((((_la) & ~0x1F) === 0 && ((1 << _la) & 520093696) !== 0))) {
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
            this.state = 206;
            this.identifier();
            this.state = 211;
            this.errorHandler.sync(this);
            _la = this.tokenStream.LA(1);
            while (_la === 17) {
                {
                {
                this.state = 207;
                this.match(CfgParser.DOT);
                this.state = 208;
                this.identifier();
                }
                }
                this.state = 213;
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
        let _la: number;
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 214;
            _la = this.tokenStream.LA(1);
            if(!((((_la) & ~0x1F) === 0 && ((1 << _la) & 536871416) !== 0))) {
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
    public comment(): CommentContext {
        let localContext = new CommentContext(this.context, this.state);
        this.enterRule(localContext, 36, CfgParser.RULE_comment);
        try {
            this.enterOuterAlt(localContext, 1);
            {
            this.state = 216;
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
        4,1,31,219,2,0,7,0,2,1,7,1,2,2,7,2,2,3,7,3,2,4,7,4,2,5,7,5,2,6,7,
        6,2,7,7,7,2,8,7,8,2,9,7,9,2,10,7,10,2,11,7,11,2,12,7,12,2,13,7,13,
        2,14,7,14,2,15,7,15,2,16,7,16,2,17,7,17,2,18,7,18,1,0,5,0,40,8,0,
        10,0,12,0,43,9,0,1,0,1,0,1,1,1,1,1,1,3,1,50,8,1,1,2,5,2,53,8,2,10,
        2,12,2,56,9,2,1,2,1,2,1,2,1,2,1,2,1,2,5,2,64,8,2,10,2,12,2,67,9,
        2,1,2,1,2,1,3,5,3,72,8,3,10,3,12,3,75,9,3,1,3,1,3,1,3,1,3,1,3,4,
        3,82,8,3,11,3,12,3,83,1,3,1,3,1,4,5,4,89,8,4,10,4,12,4,92,9,4,1,
        4,1,4,1,4,1,4,1,4,1,4,1,4,1,4,4,4,102,8,4,11,4,12,4,103,1,4,1,4,
        1,5,5,5,109,8,5,10,5,12,5,112,9,5,1,5,1,5,1,5,1,5,3,5,118,8,5,1,
        5,1,5,1,5,1,6,5,6,124,8,6,10,6,12,6,127,9,6,1,6,1,6,1,6,1,6,1,6,
        1,6,1,6,1,6,1,7,5,7,138,8,7,10,7,12,7,141,9,7,1,7,1,7,1,7,1,8,1,
        8,1,8,1,8,1,8,1,8,1,8,1,8,1,8,1,8,1,8,1,8,1,8,3,8,159,8,8,1,9,1,
        9,3,9,163,8,9,1,10,1,10,1,10,3,10,168,8,10,1,11,1,11,1,11,1,11,5,
        11,174,8,11,10,11,12,11,177,9,11,1,11,1,11,1,12,1,12,1,12,1,12,5,
        12,185,8,12,10,12,12,12,188,9,12,1,12,1,12,3,12,192,8,12,1,13,1,
        13,1,13,3,13,197,8,13,1,13,3,13,200,8,13,1,14,1,14,1,14,1,15,1,15,
        1,16,1,16,1,16,5,16,210,8,16,10,16,12,16,213,9,16,1,17,1,17,1,18,
        1,18,1,18,0,0,19,0,2,4,6,8,10,12,14,16,18,20,22,24,26,28,30,32,34,
        36,0,3,1,0,9,10,1,0,24,28,2,0,3,8,29,29,225,0,41,1,0,0,0,2,49,1,
        0,0,0,4,54,1,0,0,0,6,73,1,0,0,0,8,90,1,0,0,0,10,110,1,0,0,0,12,125,
        1,0,0,0,14,139,1,0,0,0,16,158,1,0,0,0,18,162,1,0,0,0,20,164,1,0,
        0,0,22,169,1,0,0,0,24,191,1,0,0,0,26,199,1,0,0,0,28,201,1,0,0,0,
        30,204,1,0,0,0,32,206,1,0,0,0,34,214,1,0,0,0,36,216,1,0,0,0,38,40,
        3,2,1,0,39,38,1,0,0,0,40,43,1,0,0,0,41,39,1,0,0,0,41,42,1,0,0,0,
        42,44,1,0,0,0,43,41,1,0,0,0,44,45,5,0,0,1,45,1,1,0,0,0,46,50,3,4,
        2,0,47,50,3,6,3,0,48,50,3,8,4,0,49,46,1,0,0,0,49,47,1,0,0,0,49,48,
        1,0,0,0,50,3,1,0,0,0,51,53,3,36,18,0,52,51,1,0,0,0,53,56,1,0,0,0,
        54,52,1,0,0,0,54,55,1,0,0,0,55,57,1,0,0,0,56,54,1,0,0,0,57,58,5,
        3,0,0,58,59,3,32,16,0,59,60,3,24,12,0,60,65,5,22,0,0,61,64,3,10,
        5,0,62,64,3,12,6,0,63,61,1,0,0,0,63,62,1,0,0,0,64,67,1,0,0,0,65,
        63,1,0,0,0,65,66,1,0,0,0,66,68,1,0,0,0,67,65,1,0,0,0,68,69,5,16,
        0,0,69,5,1,0,0,0,70,72,3,36,18,0,71,70,1,0,0,0,72,75,1,0,0,0,73,
        71,1,0,0,0,73,74,1,0,0,0,74,76,1,0,0,0,75,73,1,0,0,0,76,77,5,4,0,
        0,77,78,3,32,16,0,78,79,3,24,12,0,79,81,5,22,0,0,80,82,3,4,2,0,81,
        80,1,0,0,0,82,83,1,0,0,0,83,81,1,0,0,0,83,84,1,0,0,0,84,85,1,0,0,
        0,85,86,5,16,0,0,86,7,1,0,0,0,87,89,3,36,18,0,88,87,1,0,0,0,89,92,
        1,0,0,0,90,88,1,0,0,0,90,91,1,0,0,0,91,93,1,0,0,0,92,90,1,0,0,0,
        93,94,5,5,0,0,94,95,3,32,16,0,95,96,3,22,11,0,96,97,3,24,12,0,97,
        101,5,22,0,0,98,102,3,10,5,0,99,102,3,12,6,0,100,102,3,14,7,0,101,
        98,1,0,0,0,101,99,1,0,0,0,101,100,1,0,0,0,102,103,1,0,0,0,103,101,
        1,0,0,0,103,104,1,0,0,0,104,105,1,0,0,0,105,106,5,16,0,0,106,9,1,
        0,0,0,107,109,3,36,18,0,108,107,1,0,0,0,109,112,1,0,0,0,110,108,
        1,0,0,0,110,111,1,0,0,0,111,113,1,0,0,0,112,110,1,0,0,0,113,114,
        3,34,17,0,114,115,5,19,0,0,115,117,3,16,8,0,116,118,3,20,10,0,117,
        116,1,0,0,0,117,118,1,0,0,0,118,119,1,0,0,0,119,120,3,24,12,0,120,
        121,5,23,0,0,121,11,1,0,0,0,122,124,3,36,18,0,123,122,1,0,0,0,124,
        127,1,0,0,0,125,123,1,0,0,0,125,126,1,0,0,0,126,128,1,0,0,0,127,
        125,1,0,0,0,128,129,5,9,0,0,129,130,3,34,17,0,130,131,5,19,0,0,131,
        132,3,22,11,0,132,133,3,20,10,0,133,134,3,24,12,0,134,135,5,23,0,
        0,135,13,1,0,0,0,136,138,3,36,18,0,137,136,1,0,0,0,138,141,1,0,0,
        0,139,137,1,0,0,0,139,140,1,0,0,0,140,142,1,0,0,0,141,139,1,0,0,
        0,142,143,3,22,11,0,143,144,5,23,0,0,144,15,1,0,0,0,145,146,5,6,
        0,0,146,147,5,1,0,0,147,148,3,18,9,0,148,149,5,2,0,0,149,159,1,0,
        0,0,150,151,5,7,0,0,151,152,5,1,0,0,152,153,3,18,9,0,153,154,5,18,
        0,0,154,155,3,18,9,0,155,156,5,2,0,0,156,159,1,0,0,0,157,159,3,18,
        9,0,158,145,1,0,0,0,158,150,1,0,0,0,158,157,1,0,0,0,159,17,1,0,0,
        0,160,163,5,8,0,0,161,163,3,32,16,0,162,160,1,0,0,0,162,161,1,0,
        0,0,163,19,1,0,0,0,164,165,7,0,0,0,165,167,3,32,16,0,166,168,3,22,
        11,0,167,166,1,0,0,0,167,168,1,0,0,0,168,21,1,0,0,0,169,170,5,14,
        0,0,170,175,3,34,17,0,171,172,5,18,0,0,172,174,3,34,17,0,173,171,
        1,0,0,0,174,177,1,0,0,0,175,173,1,0,0,0,175,176,1,0,0,0,176,178,
        1,0,0,0,177,175,1,0,0,0,178,179,5,15,0,0,179,23,1,0,0,0,180,181,
        5,12,0,0,181,186,3,26,13,0,182,183,5,18,0,0,183,185,3,26,13,0,184,
        182,1,0,0,0,185,188,1,0,0,0,186,184,1,0,0,0,186,187,1,0,0,0,187,
        189,1,0,0,0,188,186,1,0,0,0,189,190,5,13,0,0,190,192,1,0,0,0,191,
        180,1,0,0,0,191,192,1,0,0,0,192,25,1,0,0,0,193,196,3,34,17,0,194,
        195,5,11,0,0,195,197,3,30,15,0,196,194,1,0,0,0,196,197,1,0,0,0,197,
        200,1,0,0,0,198,200,3,28,14,0,199,193,1,0,0,0,199,198,1,0,0,0,200,
        27,1,0,0,0,201,202,5,21,0,0,202,203,3,34,17,0,203,29,1,0,0,0,204,
        205,7,1,0,0,205,31,1,0,0,0,206,211,3,34,17,0,207,208,5,17,0,0,208,
        210,3,34,17,0,209,207,1,0,0,0,210,213,1,0,0,0,211,209,1,0,0,0,211,
        212,1,0,0,0,212,33,1,0,0,0,213,211,1,0,0,0,214,215,7,2,0,0,215,35,
        1,0,0,0,216,217,5,30,0,0,217,37,1,0,0,0,23,41,49,54,63,65,73,83,
        90,101,103,110,117,125,139,158,162,167,175,186,191,196,199,211
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
    public LC_COMMENT(): antlr.TerminalNode {
        return this.getToken(CfgParser.LC_COMMENT, 0)!;
    }
    public RC(): antlr.TerminalNode {
        return this.getToken(CfgParser.RC, 0)!;
    }
    public comment(): CommentContext[];
    public comment(i: number): CommentContext | null;
    public comment(i?: number): CommentContext[] | CommentContext | null {
        if (i === undefined) {
            return this.getRuleContexts(CommentContext);
        }

        return this.getRuleContext(i, CommentContext);
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
    public LC_COMMENT(): antlr.TerminalNode {
        return this.getToken(CfgParser.LC_COMMENT, 0)!;
    }
    public RC(): antlr.TerminalNode {
        return this.getToken(CfgParser.RC, 0)!;
    }
    public comment(): CommentContext[];
    public comment(i: number): CommentContext | null;
    public comment(i?: number): CommentContext[] | CommentContext | null {
        if (i === undefined) {
            return this.getRuleContexts(CommentContext);
        }

        return this.getRuleContext(i, CommentContext);
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
    public LC_COMMENT(): antlr.TerminalNode {
        return this.getToken(CfgParser.LC_COMMENT, 0)!;
    }
    public RC(): antlr.TerminalNode {
        return this.getToken(CfgParser.RC, 0)!;
    }
    public comment(): CommentContext[];
    public comment(i: number): CommentContext | null;
    public comment(i?: number): CommentContext[] | CommentContext | null {
        if (i === undefined) {
            return this.getRuleContexts(CommentContext);
        }

        return this.getRuleContext(i, CommentContext);
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
    public SEMI_COMMENT(): antlr.TerminalNode {
        return this.getToken(CfgParser.SEMI_COMMENT, 0)!;
    }
    public comment(): CommentContext[];
    public comment(i: number): CommentContext | null;
    public comment(i?: number): CommentContext[] | CommentContext | null {
        if (i === undefined) {
            return this.getRuleContexts(CommentContext);
        }

        return this.getRuleContext(i, CommentContext);
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
    public comment(): CommentContext[];
    public comment(i: number): CommentContext | null;
    public comment(i?: number): CommentContext[] | CommentContext | null {
        if (i === undefined) {
            return this.getRuleContexts(CommentContext);
        }

        return this.getRuleContext(i, CommentContext);
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
    public comment(): CommentContext[];
    public comment(i: number): CommentContext | null;
    public comment(i?: number): CommentContext[] | CommentContext | null {
        if (i === undefined) {
            return this.getRuleContexts(CommentContext);
        }

        return this.getRuleContext(i, CommentContext);
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


export class CommentContext extends antlr.ParserRuleContext {
    public constructor(parent: antlr.ParserRuleContext | null, invokingState: number) {
        super(parent, invokingState);
    }
    public COMMENT(): antlr.TerminalNode {
        return this.getToken(CfgParser.COMMENT, 0)!;
    }
    public override get ruleIndex(): number {
        return CfgParser.RULE_comment;
    }
    public override accept<Result>(visitor: CfgVisitor<Result>): Result | null {
        if (visitor.visitComment) {
            return visitor.visitComment(this);
        } else {
            return visitor.visitChildren(this);
        }
    }
}
