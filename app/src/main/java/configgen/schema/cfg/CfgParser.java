// Generated from D:/work/mygithub/cfggen/app/src/main/java/configgen/schema/cfg/Cfg.g4 by ANTLR 4.13.2
package configgen.schema.cfg;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class CfgParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.2", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, STRUCT=3, INTERFACE=4, TABLE=5, ENUM=6, TLIST=7, TMAP=8, 
		TBASE=9, REF=10, LISTREF=11, EQ=12, LP=13, RP=14, LB=15, RB=16, RC=17, 
		DOT=18, COMMA=19, COLON=20, PLUS=21, MINUS=22, LC_COMMENT=23, SEMI_COMMENT=24, 
		BOOL_CONSTANT=25, FLOAT_CONSTANT=26, HEX_INTEGER_CONSTANT=27, INTEGER_CONSTANT=28, 
		STRING_CONSTANT=29, IDENT=30, COMMENT=31, WS=32;
	public static final int
		RULE_schema = 0, RULE_schema_ele = 1, RULE_struct_decl = 2, RULE_interface_decl = 3, 
		RULE_table_decl = 4, RULE_enum_decl = 5, RULE_field_decl = 6, RULE_foreign_decl = 7, 
		RULE_key_decl = 8, RULE_enum_value = 9, RULE_type_ = 10, RULE_type_ele = 11, 
		RULE_ref = 12, RULE_key = 13, RULE_metadata = 14, RULE_ident_with_opt_single_value = 15, 
		RULE_minus_ident = 16, RULE_single_value = 17, RULE_ns_ident = 18, RULE_identifier = 19, 
		RULE_leading_comment = 20, RULE_suffix_comment = 21;
	private static String[] makeRuleNames() {
		return new String[] {
			"schema", "schema_ele", "struct_decl", "interface_decl", "table_decl", 
			"enum_decl", "field_decl", "foreign_decl", "key_decl", "enum_value", 
			"type_", "type_ele", "ref", "key", "metadata", "ident_with_opt_single_value", 
			"minus_ident", "single_value", "ns_ident", "identifier", "leading_comment", 
			"suffix_comment"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'<'", "'>'", "'struct'", "'interface'", "'table'", "'enum'", "'list'", 
			"'map'", null, "'->'", "'=>'", "'='", "'('", "')'", "'['", "']'", "'}'", 
			"'.'", "','", "':'", "'+'", "'-'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, "STRUCT", "INTERFACE", "TABLE", "ENUM", "TLIST", "TMAP", 
			"TBASE", "REF", "LISTREF", "EQ", "LP", "RP", "LB", "RB", "RC", "DOT", 
			"COMMA", "COLON", "PLUS", "MINUS", "LC_COMMENT", "SEMI_COMMENT", "BOOL_CONSTANT", 
			"FLOAT_CONSTANT", "HEX_INTEGER_CONSTANT", "INTEGER_CONSTANT", "STRING_CONSTANT", 
			"IDENT", "COMMENT", "WS"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "Cfg.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public CfgParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@SuppressWarnings("CheckReturnValue")
	public static class SchemaContext extends ParserRuleContext {
		public TerminalNode EOF() { return getToken(CfgParser.EOF, 0); }
		public List<Schema_eleContext> schema_ele() {
			return getRuleContexts(Schema_eleContext.class);
		}
		public Schema_eleContext schema_ele(int i) {
			return getRuleContext(Schema_eleContext.class,i);
		}
		public List<Suffix_commentContext> suffix_comment() {
			return getRuleContexts(Suffix_commentContext.class);
		}
		public Suffix_commentContext suffix_comment(int i) {
			return getRuleContext(Suffix_commentContext.class,i);
		}
		public SchemaContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_schema; }
	}

	public final SchemaContext schema() throws RecognitionException {
		SchemaContext _localctx = new SchemaContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_schema);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(47);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,0,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(44);
					schema_ele();
					}
					} 
				}
				setState(49);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,0,_ctx);
			}
			setState(53);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(50);
				suffix_comment();
				}
				}
				setState(55);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(56);
			match(EOF);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Schema_eleContext extends ParserRuleContext {
		public Struct_declContext struct_decl() {
			return getRuleContext(Struct_declContext.class,0);
		}
		public Interface_declContext interface_decl() {
			return getRuleContext(Interface_declContext.class,0);
		}
		public Table_declContext table_decl() {
			return getRuleContext(Table_declContext.class,0);
		}
		public Enum_declContext enum_decl() {
			return getRuleContext(Enum_declContext.class,0);
		}
		public Schema_eleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_schema_ele; }
	}

	public final Schema_eleContext schema_ele() throws RecognitionException {
		Schema_eleContext _localctx = new Schema_eleContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_schema_ele);
		try {
			setState(62);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,2,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(58);
				struct_decl();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(59);
				interface_decl();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(60);
				table_decl();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(61);
				enum_decl();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Struct_declContext extends ParserRuleContext {
		public TerminalNode STRUCT() { return getToken(CfgParser.STRUCT, 0); }
		public Ns_identContext ns_ident() {
			return getRuleContext(Ns_identContext.class,0);
		}
		public MetadataContext metadata() {
			return getRuleContext(MetadataContext.class,0);
		}
		public TerminalNode LC_COMMENT() { return getToken(CfgParser.LC_COMMENT, 0); }
		public TerminalNode RC() { return getToken(CfgParser.RC, 0); }
		public List<Leading_commentContext> leading_comment() {
			return getRuleContexts(Leading_commentContext.class);
		}
		public Leading_commentContext leading_comment(int i) {
			return getRuleContext(Leading_commentContext.class,i);
		}
		public List<Field_declContext> field_decl() {
			return getRuleContexts(Field_declContext.class);
		}
		public Field_declContext field_decl(int i) {
			return getRuleContext(Field_declContext.class,i);
		}
		public List<Foreign_declContext> foreign_decl() {
			return getRuleContexts(Foreign_declContext.class);
		}
		public Foreign_declContext foreign_decl(int i) {
			return getRuleContext(Foreign_declContext.class,i);
		}
		public List<Suffix_commentContext> suffix_comment() {
			return getRuleContexts(Suffix_commentContext.class);
		}
		public Suffix_commentContext suffix_comment(int i) {
			return getRuleContext(Suffix_commentContext.class,i);
		}
		public Struct_declContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_struct_decl; }
	}

	public final Struct_declContext struct_decl() throws RecognitionException {
		Struct_declContext _localctx = new Struct_declContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_struct_decl);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(67);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(64);
				leading_comment();
				}
				}
				setState(69);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(70);
			match(STRUCT);
			setState(71);
			ns_ident();
			setState(72);
			metadata();
			setState(73);
			match(LC_COMMENT);
			setState(78);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,5,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					setState(76);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,4,_ctx) ) {
					case 1:
						{
						setState(74);
						field_decl();
						}
						break;
					case 2:
						{
						setState(75);
						foreign_decl();
						}
						break;
					}
					} 
				}
				setState(80);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,5,_ctx);
			}
			setState(84);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(81);
				suffix_comment();
				}
				}
				setState(86);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(87);
			match(RC);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Interface_declContext extends ParserRuleContext {
		public TerminalNode INTERFACE() { return getToken(CfgParser.INTERFACE, 0); }
		public Ns_identContext ns_ident() {
			return getRuleContext(Ns_identContext.class,0);
		}
		public MetadataContext metadata() {
			return getRuleContext(MetadataContext.class,0);
		}
		public TerminalNode LC_COMMENT() { return getToken(CfgParser.LC_COMMENT, 0); }
		public TerminalNode RC() { return getToken(CfgParser.RC, 0); }
		public List<Leading_commentContext> leading_comment() {
			return getRuleContexts(Leading_commentContext.class);
		}
		public Leading_commentContext leading_comment(int i) {
			return getRuleContext(Leading_commentContext.class,i);
		}
		public List<Struct_declContext> struct_decl() {
			return getRuleContexts(Struct_declContext.class);
		}
		public Struct_declContext struct_decl(int i) {
			return getRuleContext(Struct_declContext.class,i);
		}
		public List<Suffix_commentContext> suffix_comment() {
			return getRuleContexts(Suffix_commentContext.class);
		}
		public Suffix_commentContext suffix_comment(int i) {
			return getRuleContext(Suffix_commentContext.class,i);
		}
		public Interface_declContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_interface_decl; }
	}

	public final Interface_declContext interface_decl() throws RecognitionException {
		Interface_declContext _localctx = new Interface_declContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_interface_decl);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(92);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(89);
				leading_comment();
				}
				}
				setState(94);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(95);
			match(INTERFACE);
			setState(96);
			ns_ident();
			setState(97);
			metadata();
			setState(98);
			match(LC_COMMENT);
			setState(100); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(99);
					struct_decl();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(102); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,8,_ctx);
			} while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER );
			setState(107);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(104);
				suffix_comment();
				}
				}
				setState(109);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(110);
			match(RC);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Table_declContext extends ParserRuleContext {
		public TerminalNode TABLE() { return getToken(CfgParser.TABLE, 0); }
		public Ns_identContext ns_ident() {
			return getRuleContext(Ns_identContext.class,0);
		}
		public KeyContext key() {
			return getRuleContext(KeyContext.class,0);
		}
		public MetadataContext metadata() {
			return getRuleContext(MetadataContext.class,0);
		}
		public TerminalNode LC_COMMENT() { return getToken(CfgParser.LC_COMMENT, 0); }
		public TerminalNode RC() { return getToken(CfgParser.RC, 0); }
		public List<Leading_commentContext> leading_comment() {
			return getRuleContexts(Leading_commentContext.class);
		}
		public Leading_commentContext leading_comment(int i) {
			return getRuleContext(Leading_commentContext.class,i);
		}
		public List<Field_declContext> field_decl() {
			return getRuleContexts(Field_declContext.class);
		}
		public Field_declContext field_decl(int i) {
			return getRuleContext(Field_declContext.class,i);
		}
		public List<Foreign_declContext> foreign_decl() {
			return getRuleContexts(Foreign_declContext.class);
		}
		public Foreign_declContext foreign_decl(int i) {
			return getRuleContext(Foreign_declContext.class,i);
		}
		public List<Key_declContext> key_decl() {
			return getRuleContexts(Key_declContext.class);
		}
		public Key_declContext key_decl(int i) {
			return getRuleContext(Key_declContext.class,i);
		}
		public List<Suffix_commentContext> suffix_comment() {
			return getRuleContexts(Suffix_commentContext.class);
		}
		public Suffix_commentContext suffix_comment(int i) {
			return getRuleContext(Suffix_commentContext.class,i);
		}
		public Table_declContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_table_decl; }
	}

	public final Table_declContext table_decl() throws RecognitionException {
		Table_declContext _localctx = new Table_declContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_table_decl);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(115);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(112);
				leading_comment();
				}
				}
				setState(117);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(118);
			match(TABLE);
			setState(119);
			ns_ident();
			setState(120);
			key();
			setState(121);
			metadata();
			setState(122);
			match(LC_COMMENT);
			setState(126); 
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					setState(126);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
					case 1:
						{
						setState(123);
						field_decl();
						}
						break;
					case 2:
						{
						setState(124);
						foreign_decl();
						}
						break;
					case 3:
						{
						setState(125);
						key_decl();
						}
						break;
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(128); 
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,12,_ctx);
			} while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER );
			setState(133);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(130);
				suffix_comment();
				}
				}
				setState(135);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(136);
			match(RC);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Enum_declContext extends ParserRuleContext {
		public TerminalNode ENUM() { return getToken(CfgParser.ENUM, 0); }
		public Ns_identContext ns_ident() {
			return getRuleContext(Ns_identContext.class,0);
		}
		public MetadataContext metadata() {
			return getRuleContext(MetadataContext.class,0);
		}
		public TerminalNode LC_COMMENT() { return getToken(CfgParser.LC_COMMENT, 0); }
		public TerminalNode RC() { return getToken(CfgParser.RC, 0); }
		public List<Leading_commentContext> leading_comment() {
			return getRuleContexts(Leading_commentContext.class);
		}
		public Leading_commentContext leading_comment(int i) {
			return getRuleContext(Leading_commentContext.class,i);
		}
		public List<Enum_valueContext> enum_value() {
			return getRuleContexts(Enum_valueContext.class);
		}
		public Enum_valueContext enum_value(int i) {
			return getRuleContext(Enum_valueContext.class,i);
		}
		public List<Suffix_commentContext> suffix_comment() {
			return getRuleContexts(Suffix_commentContext.class);
		}
		public Suffix_commentContext suffix_comment(int i) {
			return getRuleContext(Suffix_commentContext.class,i);
		}
		public Enum_declContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enum_decl; }
	}

	public final Enum_declContext enum_decl() throws RecognitionException {
		Enum_declContext _localctx = new Enum_declContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_enum_decl);
		int _la;
		try {
			int _alt;
			enterOuterAlt(_localctx, 1);
			{
			setState(141);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(138);
				leading_comment();
				}
				}
				setState(143);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(144);
			match(ENUM);
			setState(145);
			ns_ident();
			setState(146);
			metadata();
			setState(147);
			match(LC_COMMENT);
			setState(151);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,15,_ctx);
			while ( _alt!=2 && _alt!= ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(148);
					enum_value();
					}
					} 
				}
				setState(153);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,15,_ctx);
			}
			setState(157);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(154);
				suffix_comment();
				}
				}
				setState(159);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(160);
			match(RC);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Field_declContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode COLON() { return getToken(CfgParser.COLON, 0); }
		public Type_Context type_() {
			return getRuleContext(Type_Context.class,0);
		}
		public MetadataContext metadata() {
			return getRuleContext(MetadataContext.class,0);
		}
		public TerminalNode SEMI_COMMENT() { return getToken(CfgParser.SEMI_COMMENT, 0); }
		public List<Leading_commentContext> leading_comment() {
			return getRuleContexts(Leading_commentContext.class);
		}
		public Leading_commentContext leading_comment(int i) {
			return getRuleContext(Leading_commentContext.class,i);
		}
		public RefContext ref() {
			return getRuleContext(RefContext.class,0);
		}
		public Field_declContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_field_decl; }
	}

	public final Field_declContext field_decl() throws RecognitionException {
		Field_declContext _localctx = new Field_declContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_field_decl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(165);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(162);
				leading_comment();
				}
				}
				setState(167);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(168);
			identifier();
			setState(169);
			match(COLON);
			setState(170);
			type_();
			setState(172);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==REF || _la==LISTREF) {
				{
				setState(171);
				ref();
				}
			}

			setState(174);
			metadata();
			setState(175);
			match(SEMI_COMMENT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Foreign_declContext extends ParserRuleContext {
		public TerminalNode REF() { return getToken(CfgParser.REF, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode COLON() { return getToken(CfgParser.COLON, 0); }
		public KeyContext key() {
			return getRuleContext(KeyContext.class,0);
		}
		public RefContext ref() {
			return getRuleContext(RefContext.class,0);
		}
		public MetadataContext metadata() {
			return getRuleContext(MetadataContext.class,0);
		}
		public TerminalNode SEMI_COMMENT() { return getToken(CfgParser.SEMI_COMMENT, 0); }
		public List<Leading_commentContext> leading_comment() {
			return getRuleContexts(Leading_commentContext.class);
		}
		public Leading_commentContext leading_comment(int i) {
			return getRuleContext(Leading_commentContext.class,i);
		}
		public Foreign_declContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_foreign_decl; }
	}

	public final Foreign_declContext foreign_decl() throws RecognitionException {
		Foreign_declContext _localctx = new Foreign_declContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_foreign_decl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(180);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(177);
				leading_comment();
				}
				}
				setState(182);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(183);
			match(REF);
			setState(184);
			identifier();
			setState(185);
			match(COLON);
			setState(186);
			key();
			setState(187);
			ref();
			setState(188);
			metadata();
			setState(189);
			match(SEMI_COMMENT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Key_declContext extends ParserRuleContext {
		public KeyContext key() {
			return getRuleContext(KeyContext.class,0);
		}
		public TerminalNode SEMI_COMMENT() { return getToken(CfgParser.SEMI_COMMENT, 0); }
		public List<Leading_commentContext> leading_comment() {
			return getRuleContexts(Leading_commentContext.class);
		}
		public Leading_commentContext leading_comment(int i) {
			return getRuleContext(Leading_commentContext.class,i);
		}
		public Key_declContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_key_decl; }
	}

	public final Key_declContext key_decl() throws RecognitionException {
		Key_declContext _localctx = new Key_declContext(_ctx, getState());
		enterRule(_localctx, 16, RULE_key_decl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(194);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(191);
				leading_comment();
				}
				}
				setState(196);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(197);
			key();
			setState(198);
			match(SEMI_COMMENT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Enum_valueContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode SEMI_COMMENT() { return getToken(CfgParser.SEMI_COMMENT, 0); }
		public List<Leading_commentContext> leading_comment() {
			return getRuleContexts(Leading_commentContext.class);
		}
		public Leading_commentContext leading_comment(int i) {
			return getRuleContext(Leading_commentContext.class,i);
		}
		public Enum_valueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enum_value; }
	}

	public final Enum_valueContext enum_value() throws RecognitionException {
		Enum_valueContext _localctx = new Enum_valueContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_enum_value);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(203);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(200);
				leading_comment();
				}
				}
				setState(205);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(206);
			identifier();
			setState(207);
			match(SEMI_COMMENT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Type_Context extends ParserRuleContext {
		public Type_Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type_; }
	 
		public Type_Context() { }
		public void copyFrom(Type_Context ctx) {
			super.copyFrom(ctx);
		}
	}
	@SuppressWarnings("CheckReturnValue")
	public static class TypeBasicContext extends Type_Context {
		public Type_eleContext type_ele() {
			return getRuleContext(Type_eleContext.class,0);
		}
		public TypeBasicContext(Type_Context ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class TypeMapContext extends Type_Context {
		public TerminalNode TMAP() { return getToken(CfgParser.TMAP, 0); }
		public List<Type_eleContext> type_ele() {
			return getRuleContexts(Type_eleContext.class);
		}
		public Type_eleContext type_ele(int i) {
			return getRuleContext(Type_eleContext.class,i);
		}
		public TerminalNode COMMA() { return getToken(CfgParser.COMMA, 0); }
		public TypeMapContext(Type_Context ctx) { copyFrom(ctx); }
	}
	@SuppressWarnings("CheckReturnValue")
	public static class TypeListContext extends Type_Context {
		public TerminalNode TLIST() { return getToken(CfgParser.TLIST, 0); }
		public Type_eleContext type_ele() {
			return getRuleContext(Type_eleContext.class,0);
		}
		public TypeListContext(Type_Context ctx) { copyFrom(ctx); }
	}

	public final Type_Context type_() throws RecognitionException {
		Type_Context _localctx = new Type_Context(_ctx, getState());
		enterRule(_localctx, 20, RULE_type_);
		try {
			setState(222);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,22,_ctx) ) {
			case 1:
				_localctx = new TypeListContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(209);
				match(TLIST);
				setState(210);
				match(T__0);
				setState(211);
				type_ele();
				setState(212);
				match(T__1);
				}
				break;
			case 2:
				_localctx = new TypeMapContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(214);
				match(TMAP);
				setState(215);
				match(T__0);
				setState(216);
				type_ele();
				setState(217);
				match(COMMA);
				setState(218);
				type_ele();
				setState(219);
				match(T__1);
				}
				break;
			case 3:
				_localctx = new TypeBasicContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(221);
				type_ele();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Type_eleContext extends ParserRuleContext {
		public TerminalNode TBASE() { return getToken(CfgParser.TBASE, 0); }
		public Ns_identContext ns_ident() {
			return getRuleContext(Ns_identContext.class,0);
		}
		public Type_eleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type_ele; }
	}

	public final Type_eleContext type_ele() throws RecognitionException {
		Type_eleContext _localctx = new Type_eleContext(_ctx, getState());
		enterRule(_localctx, 22, RULE_type_ele);
		try {
			setState(226);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,23,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(224);
				match(TBASE);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(225);
				ns_ident();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class RefContext extends ParserRuleContext {
		public Ns_identContext ns_ident() {
			return getRuleContext(Ns_identContext.class,0);
		}
		public TerminalNode REF() { return getToken(CfgParser.REF, 0); }
		public TerminalNode LISTREF() { return getToken(CfgParser.LISTREF, 0); }
		public KeyContext key() {
			return getRuleContext(KeyContext.class,0);
		}
		public RefContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ref; }
	}

	public final RefContext ref() throws RecognitionException {
		RefContext _localctx = new RefContext(_ctx, getState());
		enterRule(_localctx, 24, RULE_ref);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(228);
			_la = _input.LA(1);
			if ( !(_la==REF || _la==LISTREF) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(229);
			ns_ident();
			setState(231);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LB) {
				{
				setState(230);
				key();
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class KeyContext extends ParserRuleContext {
		public TerminalNode LB() { return getToken(CfgParser.LB, 0); }
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public TerminalNode RB() { return getToken(CfgParser.RB, 0); }
		public List<TerminalNode> COMMA() { return getTokens(CfgParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CfgParser.COMMA, i);
		}
		public KeyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_key; }
	}

	public final KeyContext key() throws RecognitionException {
		KeyContext _localctx = new KeyContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_key);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(233);
			match(LB);
			setState(234);
			identifier();
			setState(239);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(235);
				match(COMMA);
				setState(236);
				identifier();
				}
				}
				setState(241);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(242);
			match(RB);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class MetadataContext extends ParserRuleContext {
		public TerminalNode LP() { return getToken(CfgParser.LP, 0); }
		public List<Ident_with_opt_single_valueContext> ident_with_opt_single_value() {
			return getRuleContexts(Ident_with_opt_single_valueContext.class);
		}
		public Ident_with_opt_single_valueContext ident_with_opt_single_value(int i) {
			return getRuleContext(Ident_with_opt_single_valueContext.class,i);
		}
		public TerminalNode RP() { return getToken(CfgParser.RP, 0); }
		public List<TerminalNode> COMMA() { return getTokens(CfgParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CfgParser.COMMA, i);
		}
		public MetadataContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_metadata; }
	}

	public final MetadataContext metadata() throws RecognitionException {
		MetadataContext _localctx = new MetadataContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_metadata);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(255);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LP) {
				{
				setState(244);
				match(LP);
				setState(245);
				ident_with_opt_single_value();
				setState(250);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(246);
					match(COMMA);
					setState(247);
					ident_with_opt_single_value();
					}
					}
					setState(252);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(253);
				match(RP);
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Ident_with_opt_single_valueContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode EQ() { return getToken(CfgParser.EQ, 0); }
		public Single_valueContext single_value() {
			return getRuleContext(Single_valueContext.class,0);
		}
		public Minus_identContext minus_ident() {
			return getRuleContext(Minus_identContext.class,0);
		}
		public Ident_with_opt_single_valueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ident_with_opt_single_value; }
	}

	public final Ident_with_opt_single_valueContext ident_with_opt_single_value() throws RecognitionException {
		Ident_with_opt_single_valueContext _localctx = new Ident_with_opt_single_valueContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_ident_with_opt_single_value);
		int _la;
		try {
			setState(263);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case STRUCT:
			case INTERFACE:
			case TABLE:
			case ENUM:
			case TLIST:
			case TMAP:
			case TBASE:
			case IDENT:
				enterOuterAlt(_localctx, 1);
				{
				setState(257);
				identifier();
				setState(260);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EQ) {
					{
					setState(258);
					match(EQ);
					setState(259);
					single_value();
					}
				}

				}
				break;
			case MINUS:
				enterOuterAlt(_localctx, 2);
				{
				setState(262);
				minus_ident();
				}
				break;
			default:
				throw new NoViableAltException(this);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Minus_identContext extends ParserRuleContext {
		public TerminalNode MINUS() { return getToken(CfgParser.MINUS, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public Minus_identContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_minus_ident; }
	}

	public final Minus_identContext minus_ident() throws RecognitionException {
		Minus_identContext _localctx = new Minus_identContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_minus_ident);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(265);
			match(MINUS);
			setState(266);
			identifier();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Single_valueContext extends ParserRuleContext {
		public TerminalNode INTEGER_CONSTANT() { return getToken(CfgParser.INTEGER_CONSTANT, 0); }
		public TerminalNode HEX_INTEGER_CONSTANT() { return getToken(CfgParser.HEX_INTEGER_CONSTANT, 0); }
		public TerminalNode FLOAT_CONSTANT() { return getToken(CfgParser.FLOAT_CONSTANT, 0); }
		public TerminalNode STRING_CONSTANT() { return getToken(CfgParser.STRING_CONSTANT, 0); }
		public TerminalNode BOOL_CONSTANT() { return getToken(CfgParser.BOOL_CONSTANT, 0); }
		public Single_valueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_single_value; }
	}

	public final Single_valueContext single_value() throws RecognitionException {
		Single_valueContext _localctx = new Single_valueContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_single_value);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(268);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 1040187392L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Ns_identContext extends ParserRuleContext {
		public List<IdentifierContext> identifier() {
			return getRuleContexts(IdentifierContext.class);
		}
		public IdentifierContext identifier(int i) {
			return getRuleContext(IdentifierContext.class,i);
		}
		public List<TerminalNode> DOT() { return getTokens(CfgParser.DOT); }
		public TerminalNode DOT(int i) {
			return getToken(CfgParser.DOT, i);
		}
		public Ns_identContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ns_ident; }
	}

	public final Ns_identContext ns_ident() throws RecognitionException {
		Ns_identContext _localctx = new Ns_identContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_ns_ident);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(270);
			identifier();
			setState(275);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOT) {
				{
				{
				setState(271);
				match(DOT);
				setState(272);
				identifier();
				}
				}
				setState(277);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class IdentifierContext extends ParserRuleContext {
		public TerminalNode IDENT() { return getToken(CfgParser.IDENT, 0); }
		public TerminalNode STRUCT() { return getToken(CfgParser.STRUCT, 0); }
		public TerminalNode INTERFACE() { return getToken(CfgParser.INTERFACE, 0); }
		public TerminalNode TABLE() { return getToken(CfgParser.TABLE, 0); }
		public TerminalNode ENUM() { return getToken(CfgParser.ENUM, 0); }
		public TerminalNode TLIST() { return getToken(CfgParser.TLIST, 0); }
		public TerminalNode TMAP() { return getToken(CfgParser.TMAP, 0); }
		public TerminalNode TBASE() { return getToken(CfgParser.TBASE, 0); }
		public IdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identifier; }
	}

	public final IdentifierContext identifier() throws RecognitionException {
		IdentifierContext _localctx = new IdentifierContext(_ctx, getState());
		enterRule(_localctx, 38, RULE_identifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(278);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 1073742840L) != 0)) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Leading_commentContext extends ParserRuleContext {
		public TerminalNode COMMENT() { return getToken(CfgParser.COMMENT, 0); }
		public Leading_commentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_leading_comment; }
	}

	public final Leading_commentContext leading_comment() throws RecognitionException {
		Leading_commentContext _localctx = new Leading_commentContext(_ctx, getState());
		enterRule(_localctx, 40, RULE_leading_comment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(280);
			match(COMMENT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	@SuppressWarnings("CheckReturnValue")
	public static class Suffix_commentContext extends ParserRuleContext {
		public TerminalNode COMMENT() { return getToken(CfgParser.COMMENT, 0); }
		public Suffix_commentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_suffix_comment; }
	}

	public final Suffix_commentContext suffix_comment() throws RecognitionException {
		Suffix_commentContext _localctx = new Suffix_commentContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_suffix_comment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(282);
			match(COMMENT);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\u0004\u0001 \u011d\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002"+
		"\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f"+
		"\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007\u0012"+
		"\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007\u0015"+
		"\u0001\u0000\u0005\u0000.\b\u0000\n\u0000\f\u00001\t\u0000\u0001\u0000"+
		"\u0005\u00004\b\u0000\n\u0000\f\u00007\t\u0000\u0001\u0000\u0001\u0000"+
		"\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0003\u0001?\b\u0001"+
		"\u0001\u0002\u0005\u0002B\b\u0002\n\u0002\f\u0002E\t\u0002\u0001\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0005\u0002"+
		"M\b\u0002\n\u0002\f\u0002P\t\u0002\u0001\u0002\u0005\u0002S\b\u0002\n"+
		"\u0002\f\u0002V\t\u0002\u0001\u0002\u0001\u0002\u0001\u0003\u0005\u0003"+
		"[\b\u0003\n\u0003\f\u0003^\t\u0003\u0001\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0004\u0003e\b\u0003\u000b\u0003\f\u0003f\u0001"+
		"\u0003\u0005\u0003j\b\u0003\n\u0003\f\u0003m\t\u0003\u0001\u0003\u0001"+
		"\u0003\u0001\u0004\u0005\u0004r\b\u0004\n\u0004\f\u0004u\t\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0004\u0004\u007f\b\u0004\u000b\u0004\f\u0004\u0080"+
		"\u0001\u0004\u0005\u0004\u0084\b\u0004\n\u0004\f\u0004\u0087\t\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0005\u0005\u0005\u008c\b\u0005\n\u0005\f\u0005"+
		"\u008f\t\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0005\u0005\u0096\b\u0005\n\u0005\f\u0005\u0099\t\u0005\u0001\u0005\u0005"+
		"\u0005\u009c\b\u0005\n\u0005\f\u0005\u009f\t\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0006\u0005\u0006\u00a4\b\u0006\n\u0006\f\u0006\u00a7\t\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0003\u0006\u00ad\b\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0001\u0007\u0005\u0007\u00b3\b\u0007\n"+
		"\u0007\f\u0007\u00b6\t\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\b\u0005\b"+
		"\u00c1\b\b\n\b\f\b\u00c4\t\b\u0001\b\u0001\b\u0001\b\u0001\t\u0005\t\u00ca"+
		"\b\t\n\t\f\t\u00cd\t\t\u0001\t\u0001\t\u0001\t\u0001\n\u0001\n\u0001\n"+
		"\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001"+
		"\n\u0001\n\u0003\n\u00df\b\n\u0001\u000b\u0001\u000b\u0003\u000b\u00e3"+
		"\b\u000b\u0001\f\u0001\f\u0001\f\u0003\f\u00e8\b\f\u0001\r\u0001\r\u0001"+
		"\r\u0001\r\u0005\r\u00ee\b\r\n\r\f\r\u00f1\t\r\u0001\r\u0001\r\u0001\u000e"+
		"\u0001\u000e\u0001\u000e\u0001\u000e\u0005\u000e\u00f9\b\u000e\n\u000e"+
		"\f\u000e\u00fc\t\u000e\u0001\u000e\u0001\u000e\u0003\u000e\u0100\b\u000e"+
		"\u0001\u000f\u0001\u000f\u0001\u000f\u0003\u000f\u0105\b\u000f\u0001\u000f"+
		"\u0003\u000f\u0108\b\u000f\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0011"+
		"\u0001\u0011\u0001\u0012\u0001\u0012\u0001\u0012\u0005\u0012\u0112\b\u0012"+
		"\n\u0012\f\u0012\u0115\t\u0012\u0001\u0013\u0001\u0013\u0001\u0014\u0001"+
		"\u0014\u0001\u0015\u0001\u0015\u0001\u0015\u0000\u0000\u0016\u0000\u0002"+
		"\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e"+
		" \"$&(*\u0000\u0003\u0001\u0000\n\u000b\u0001\u0000\u0019\u001d\u0002"+
		"\u0000\u0003\t\u001e\u001e\u0129\u0000/\u0001\u0000\u0000\u0000\u0002"+
		">\u0001\u0000\u0000\u0000\u0004C\u0001\u0000\u0000\u0000\u0006\\\u0001"+
		"\u0000\u0000\u0000\bs\u0001\u0000\u0000\u0000\n\u008d\u0001\u0000\u0000"+
		"\u0000\f\u00a5\u0001\u0000\u0000\u0000\u000e\u00b4\u0001\u0000\u0000\u0000"+
		"\u0010\u00c2\u0001\u0000\u0000\u0000\u0012\u00cb\u0001\u0000\u0000\u0000"+
		"\u0014\u00de\u0001\u0000\u0000\u0000\u0016\u00e2\u0001\u0000\u0000\u0000"+
		"\u0018\u00e4\u0001\u0000\u0000\u0000\u001a\u00e9\u0001\u0000\u0000\u0000"+
		"\u001c\u00ff\u0001\u0000\u0000\u0000\u001e\u0107\u0001\u0000\u0000\u0000"+
		" \u0109\u0001\u0000\u0000\u0000\"\u010c\u0001\u0000\u0000\u0000$\u010e"+
		"\u0001\u0000\u0000\u0000&\u0116\u0001\u0000\u0000\u0000(\u0118\u0001\u0000"+
		"\u0000\u0000*\u011a\u0001\u0000\u0000\u0000,.\u0003\u0002\u0001\u0000"+
		"-,\u0001\u0000\u0000\u0000.1\u0001\u0000\u0000\u0000/-\u0001\u0000\u0000"+
		"\u0000/0\u0001\u0000\u0000\u000005\u0001\u0000\u0000\u00001/\u0001\u0000"+
		"\u0000\u000024\u0003*\u0015\u000032\u0001\u0000\u0000\u000047\u0001\u0000"+
		"\u0000\u000053\u0001\u0000\u0000\u000056\u0001\u0000\u0000\u000068\u0001"+
		"\u0000\u0000\u000075\u0001\u0000\u0000\u000089\u0005\u0000\u0000\u0001"+
		"9\u0001\u0001\u0000\u0000\u0000:?\u0003\u0004\u0002\u0000;?\u0003\u0006"+
		"\u0003\u0000<?\u0003\b\u0004\u0000=?\u0003\n\u0005\u0000>:\u0001\u0000"+
		"\u0000\u0000>;\u0001\u0000\u0000\u0000><\u0001\u0000\u0000\u0000>=\u0001"+
		"\u0000\u0000\u0000?\u0003\u0001\u0000\u0000\u0000@B\u0003(\u0014\u0000"+
		"A@\u0001\u0000\u0000\u0000BE\u0001\u0000\u0000\u0000CA\u0001\u0000\u0000"+
		"\u0000CD\u0001\u0000\u0000\u0000DF\u0001\u0000\u0000\u0000EC\u0001\u0000"+
		"\u0000\u0000FG\u0005\u0003\u0000\u0000GH\u0003$\u0012\u0000HI\u0003\u001c"+
		"\u000e\u0000IN\u0005\u0017\u0000\u0000JM\u0003\f\u0006\u0000KM\u0003\u000e"+
		"\u0007\u0000LJ\u0001\u0000\u0000\u0000LK\u0001\u0000\u0000\u0000MP\u0001"+
		"\u0000\u0000\u0000NL\u0001\u0000\u0000\u0000NO\u0001\u0000\u0000\u0000"+
		"OT\u0001\u0000\u0000\u0000PN\u0001\u0000\u0000\u0000QS\u0003*\u0015\u0000"+
		"RQ\u0001\u0000\u0000\u0000SV\u0001\u0000\u0000\u0000TR\u0001\u0000\u0000"+
		"\u0000TU\u0001\u0000\u0000\u0000UW\u0001\u0000\u0000\u0000VT\u0001\u0000"+
		"\u0000\u0000WX\u0005\u0011\u0000\u0000X\u0005\u0001\u0000\u0000\u0000"+
		"Y[\u0003(\u0014\u0000ZY\u0001\u0000\u0000\u0000[^\u0001\u0000\u0000\u0000"+
		"\\Z\u0001\u0000\u0000\u0000\\]\u0001\u0000\u0000\u0000]_\u0001\u0000\u0000"+
		"\u0000^\\\u0001\u0000\u0000\u0000_`\u0005\u0004\u0000\u0000`a\u0003$\u0012"+
		"\u0000ab\u0003\u001c\u000e\u0000bd\u0005\u0017\u0000\u0000ce\u0003\u0004"+
		"\u0002\u0000dc\u0001\u0000\u0000\u0000ef\u0001\u0000\u0000\u0000fd\u0001"+
		"\u0000\u0000\u0000fg\u0001\u0000\u0000\u0000gk\u0001\u0000\u0000\u0000"+
		"hj\u0003*\u0015\u0000ih\u0001\u0000\u0000\u0000jm\u0001\u0000\u0000\u0000"+
		"ki\u0001\u0000\u0000\u0000kl\u0001\u0000\u0000\u0000ln\u0001\u0000\u0000"+
		"\u0000mk\u0001\u0000\u0000\u0000no\u0005\u0011\u0000\u0000o\u0007\u0001"+
		"\u0000\u0000\u0000pr\u0003(\u0014\u0000qp\u0001\u0000\u0000\u0000ru\u0001"+
		"\u0000\u0000\u0000sq\u0001\u0000\u0000\u0000st\u0001\u0000\u0000\u0000"+
		"tv\u0001\u0000\u0000\u0000us\u0001\u0000\u0000\u0000vw\u0005\u0005\u0000"+
		"\u0000wx\u0003$\u0012\u0000xy\u0003\u001a\r\u0000yz\u0003\u001c\u000e"+
		"\u0000z~\u0005\u0017\u0000\u0000{\u007f\u0003\f\u0006\u0000|\u007f\u0003"+
		"\u000e\u0007\u0000}\u007f\u0003\u0010\b\u0000~{\u0001\u0000\u0000\u0000"+
		"~|\u0001\u0000\u0000\u0000~}\u0001\u0000\u0000\u0000\u007f\u0080\u0001"+
		"\u0000\u0000\u0000\u0080~\u0001\u0000\u0000\u0000\u0080\u0081\u0001\u0000"+
		"\u0000\u0000\u0081\u0085\u0001\u0000\u0000\u0000\u0082\u0084\u0003*\u0015"+
		"\u0000\u0083\u0082\u0001\u0000\u0000\u0000\u0084\u0087\u0001\u0000\u0000"+
		"\u0000\u0085\u0083\u0001\u0000\u0000\u0000\u0085\u0086\u0001\u0000\u0000"+
		"\u0000\u0086\u0088\u0001\u0000\u0000\u0000\u0087\u0085\u0001\u0000\u0000"+
		"\u0000\u0088\u0089\u0005\u0011\u0000\u0000\u0089\t\u0001\u0000\u0000\u0000"+
		"\u008a\u008c\u0003(\u0014\u0000\u008b\u008a\u0001\u0000\u0000\u0000\u008c"+
		"\u008f\u0001\u0000\u0000\u0000\u008d\u008b\u0001\u0000\u0000\u0000\u008d"+
		"\u008e\u0001\u0000\u0000\u0000\u008e\u0090\u0001\u0000\u0000\u0000\u008f"+
		"\u008d\u0001\u0000\u0000\u0000\u0090\u0091\u0005\u0006\u0000\u0000\u0091"+
		"\u0092\u0003$\u0012\u0000\u0092\u0093\u0003\u001c\u000e\u0000\u0093\u0097"+
		"\u0005\u0017\u0000\u0000\u0094\u0096\u0003\u0012\t\u0000\u0095\u0094\u0001"+
		"\u0000\u0000\u0000\u0096\u0099\u0001\u0000\u0000\u0000\u0097\u0095\u0001"+
		"\u0000\u0000\u0000\u0097\u0098\u0001\u0000\u0000\u0000\u0098\u009d\u0001"+
		"\u0000\u0000\u0000\u0099\u0097\u0001\u0000\u0000\u0000\u009a\u009c\u0003"+
		"*\u0015\u0000\u009b\u009a\u0001\u0000\u0000\u0000\u009c\u009f\u0001\u0000"+
		"\u0000\u0000\u009d\u009b\u0001\u0000\u0000\u0000\u009d\u009e\u0001\u0000"+
		"\u0000\u0000\u009e\u00a0\u0001\u0000\u0000\u0000\u009f\u009d\u0001\u0000"+
		"\u0000\u0000\u00a0\u00a1\u0005\u0011\u0000\u0000\u00a1\u000b\u0001\u0000"+
		"\u0000\u0000\u00a2\u00a4\u0003(\u0014\u0000\u00a3\u00a2\u0001\u0000\u0000"+
		"\u0000\u00a4\u00a7\u0001\u0000\u0000\u0000\u00a5\u00a3\u0001\u0000\u0000"+
		"\u0000\u00a5\u00a6\u0001\u0000\u0000\u0000\u00a6\u00a8\u0001\u0000\u0000"+
		"\u0000\u00a7\u00a5\u0001\u0000\u0000\u0000\u00a8\u00a9\u0003&\u0013\u0000"+
		"\u00a9\u00aa\u0005\u0014\u0000\u0000\u00aa\u00ac\u0003\u0014\n\u0000\u00ab"+
		"\u00ad\u0003\u0018\f\u0000\u00ac\u00ab\u0001\u0000\u0000\u0000\u00ac\u00ad"+
		"\u0001\u0000\u0000\u0000\u00ad\u00ae\u0001\u0000\u0000\u0000\u00ae\u00af"+
		"\u0003\u001c\u000e\u0000\u00af\u00b0\u0005\u0018\u0000\u0000\u00b0\r\u0001"+
		"\u0000\u0000\u0000\u00b1\u00b3\u0003(\u0014\u0000\u00b2\u00b1\u0001\u0000"+
		"\u0000\u0000\u00b3\u00b6\u0001\u0000\u0000\u0000\u00b4\u00b2\u0001\u0000"+
		"\u0000\u0000\u00b4\u00b5\u0001\u0000\u0000\u0000\u00b5\u00b7\u0001\u0000"+
		"\u0000\u0000\u00b6\u00b4\u0001\u0000\u0000\u0000\u00b7\u00b8\u0005\n\u0000"+
		"\u0000\u00b8\u00b9\u0003&\u0013\u0000\u00b9\u00ba\u0005\u0014\u0000\u0000"+
		"\u00ba\u00bb\u0003\u001a\r\u0000\u00bb\u00bc\u0003\u0018\f\u0000\u00bc"+
		"\u00bd\u0003\u001c\u000e\u0000\u00bd\u00be\u0005\u0018\u0000\u0000\u00be"+
		"\u000f\u0001\u0000\u0000\u0000\u00bf\u00c1\u0003(\u0014\u0000\u00c0\u00bf"+
		"\u0001\u0000\u0000\u0000\u00c1\u00c4\u0001\u0000\u0000\u0000\u00c2\u00c0"+
		"\u0001\u0000\u0000\u0000\u00c2\u00c3\u0001\u0000\u0000\u0000\u00c3\u00c5"+
		"\u0001\u0000\u0000\u0000\u00c4\u00c2\u0001\u0000\u0000\u0000\u00c5\u00c6"+
		"\u0003\u001a\r\u0000\u00c6\u00c7\u0005\u0018\u0000\u0000\u00c7\u0011\u0001"+
		"\u0000\u0000\u0000\u00c8\u00ca\u0003(\u0014\u0000\u00c9\u00c8\u0001\u0000"+
		"\u0000\u0000\u00ca\u00cd\u0001\u0000\u0000\u0000\u00cb\u00c9\u0001\u0000"+
		"\u0000\u0000\u00cb\u00cc\u0001\u0000\u0000\u0000\u00cc\u00ce\u0001\u0000"+
		"\u0000\u0000\u00cd\u00cb\u0001\u0000\u0000\u0000\u00ce\u00cf\u0003&\u0013"+
		"\u0000\u00cf\u00d0\u0005\u0018\u0000\u0000\u00d0\u0013\u0001\u0000\u0000"+
		"\u0000\u00d1\u00d2\u0005\u0007\u0000\u0000\u00d2\u00d3\u0005\u0001\u0000"+
		"\u0000\u00d3\u00d4\u0003\u0016\u000b\u0000\u00d4\u00d5\u0005\u0002\u0000"+
		"\u0000\u00d5\u00df\u0001\u0000\u0000\u0000\u00d6\u00d7\u0005\b\u0000\u0000"+
		"\u00d7\u00d8\u0005\u0001\u0000\u0000\u00d8\u00d9\u0003\u0016\u000b\u0000"+
		"\u00d9\u00da\u0005\u0013\u0000\u0000\u00da\u00db\u0003\u0016\u000b\u0000"+
		"\u00db\u00dc\u0005\u0002\u0000\u0000\u00dc\u00df\u0001\u0000\u0000\u0000"+
		"\u00dd\u00df\u0003\u0016\u000b\u0000\u00de\u00d1\u0001\u0000\u0000\u0000"+
		"\u00de\u00d6\u0001\u0000\u0000\u0000\u00de\u00dd\u0001\u0000\u0000\u0000"+
		"\u00df\u0015\u0001\u0000\u0000\u0000\u00e0\u00e3\u0005\t\u0000\u0000\u00e1"+
		"\u00e3\u0003$\u0012\u0000\u00e2\u00e0\u0001\u0000\u0000\u0000\u00e2\u00e1"+
		"\u0001\u0000\u0000\u0000\u00e3\u0017\u0001\u0000\u0000\u0000\u00e4\u00e5"+
		"\u0007\u0000\u0000\u0000\u00e5\u00e7\u0003$\u0012\u0000\u00e6\u00e8\u0003"+
		"\u001a\r\u0000\u00e7\u00e6\u0001\u0000\u0000\u0000\u00e7\u00e8\u0001\u0000"+
		"\u0000\u0000\u00e8\u0019\u0001\u0000\u0000\u0000\u00e9\u00ea\u0005\u000f"+
		"\u0000\u0000\u00ea\u00ef\u0003&\u0013\u0000\u00eb\u00ec\u0005\u0013\u0000"+
		"\u0000\u00ec\u00ee\u0003&\u0013\u0000\u00ed\u00eb\u0001\u0000\u0000\u0000"+
		"\u00ee\u00f1\u0001\u0000\u0000\u0000\u00ef\u00ed\u0001\u0000\u0000\u0000"+
		"\u00ef\u00f0\u0001\u0000\u0000\u0000\u00f0\u00f2\u0001\u0000\u0000\u0000"+
		"\u00f1\u00ef\u0001\u0000\u0000\u0000\u00f2\u00f3\u0005\u0010\u0000\u0000"+
		"\u00f3\u001b\u0001\u0000\u0000\u0000\u00f4\u00f5\u0005\r\u0000\u0000\u00f5"+
		"\u00fa\u0003\u001e\u000f\u0000\u00f6\u00f7\u0005\u0013\u0000\u0000\u00f7"+
		"\u00f9\u0003\u001e\u000f\u0000\u00f8\u00f6\u0001\u0000\u0000\u0000\u00f9"+
		"\u00fc\u0001\u0000\u0000\u0000\u00fa\u00f8\u0001\u0000\u0000\u0000\u00fa"+
		"\u00fb\u0001\u0000\u0000\u0000\u00fb\u00fd\u0001\u0000\u0000\u0000\u00fc"+
		"\u00fa\u0001\u0000\u0000\u0000\u00fd\u00fe\u0005\u000e\u0000\u0000\u00fe"+
		"\u0100\u0001\u0000\u0000\u0000\u00ff\u00f4\u0001\u0000\u0000\u0000\u00ff"+
		"\u0100\u0001\u0000\u0000\u0000\u0100\u001d\u0001\u0000\u0000\u0000\u0101"+
		"\u0104\u0003&\u0013\u0000\u0102\u0103\u0005\f\u0000\u0000\u0103\u0105"+
		"\u0003\"\u0011\u0000\u0104\u0102\u0001\u0000\u0000\u0000\u0104\u0105\u0001"+
		"\u0000\u0000\u0000\u0105\u0108\u0001\u0000\u0000\u0000\u0106\u0108\u0003"+
		" \u0010\u0000\u0107\u0101\u0001\u0000\u0000\u0000\u0107\u0106\u0001\u0000"+
		"\u0000\u0000\u0108\u001f\u0001\u0000\u0000\u0000\u0109\u010a\u0005\u0016"+
		"\u0000\u0000\u010a\u010b\u0003&\u0013\u0000\u010b!\u0001\u0000\u0000\u0000"+
		"\u010c\u010d\u0007\u0001\u0000\u0000\u010d#\u0001\u0000\u0000\u0000\u010e"+
		"\u0113\u0003&\u0013\u0000\u010f\u0110\u0005\u0012\u0000\u0000\u0110\u0112"+
		"\u0003&\u0013\u0000\u0111\u010f\u0001\u0000\u0000\u0000\u0112\u0115\u0001"+
		"\u0000\u0000\u0000\u0113\u0111\u0001\u0000\u0000\u0000\u0113\u0114\u0001"+
		"\u0000\u0000\u0000\u0114%\u0001\u0000\u0000\u0000\u0115\u0113\u0001\u0000"+
		"\u0000\u0000\u0116\u0117\u0007\u0002\u0000\u0000\u0117\'\u0001\u0000\u0000"+
		"\u0000\u0118\u0119\u0005\u001f\u0000\u0000\u0119)\u0001\u0000\u0000\u0000"+
		"\u011a\u011b\u0005\u001f\u0000\u0000\u011b+\u0001\u0000\u0000\u0000\u001f"+
		"/5>CLNT\\fks~\u0080\u0085\u008d\u0097\u009d\u00a5\u00ac\u00b4\u00c2\u00cb"+
		"\u00de\u00e2\u00e7\u00ef\u00fa\u00ff\u0104\u0107\u0113";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}