// Generated from Cfg.g4 by ANTLR 4.13.2
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
		DOT=18, COMMA=19, COLON=20, SCOPE=21, AT=22, PLUS=23, MINUS=24, LC_COMMENT=25,
		SEMI_COMMENT=26, BOOL_CONSTANT=27, FLOAT_CONSTANT=28, HEX_INTEGER_CONSTANT=29,
		INTEGER_CONSTANT=30, STRING_CONSTANT=31, IDENT=32, COMMENT=33, WS=34;
	public static final int
		RULE_schema = 0, RULE_schema_ele = 1, RULE_struct_decl = 2, RULE_interface_decl = 3,
		RULE_table_decl = 4, RULE_enum_decl = 5, RULE_field_decl = 6, RULE_foreign_decl = 7,
		RULE_key_decl = 8, RULE_enum_value_empty = 9, RULE_enum_value_assigned = 10,
		RULE_type_ = 11, RULE_type_ele = 12, RULE_ref = 13, RULE_source_values = 14,
		RULE_source_value = 15, RULE_key = 16, RULE_metadata = 17, RULE_ident_with_opt_single_value = 18,
		RULE_minus_ident = 19, RULE_single_value = 20, RULE_enum_number = 21,
		RULE_ns_ident = 22, RULE_identifier = 23, RULE_leading_comment = 24, RULE_suffix_comment = 25;
	private static String[] makeRuleNames() {
		return new String[] {
			"schema", "schema_ele", "struct_decl", "interface_decl", "table_decl",
			"enum_decl", "field_decl", "foreign_decl", "key_decl", "enum_value_empty",
			"enum_value_assigned", "type_", "type_ele", "ref", "source_values", "source_value",
			"key", "metadata", "ident_with_opt_single_value", "minus_ident", "single_value",
			"enum_number", "ns_ident", "identifier", "leading_comment", "suffix_comment"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'<'", "'>'", "'struct'", "'interface'", "'table'", "'enum'", "'list'",
			"'map'", null, "'->'", "'=>'", "'='", "'('", "')'", "'['", "']'", "'}'",
			"'.'", "','", "':'", "'::'", "'@'", "'+'", "'-'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, "STRUCT", "INTERFACE", "TABLE", "ENUM", "TLIST", "TMAP",
			"TBASE", "REF", "LISTREF", "EQ", "LP", "RP", "LB", "RB", "RC", "DOT",
			"COMMA", "COLON", "SCOPE", "AT", "PLUS", "MINUS", "LC_COMMENT", "SEMI_COMMENT",
			"BOOL_CONSTANT", "FLOAT_CONSTANT", "HEX_INTEGER_CONSTANT", "INTEGER_CONSTANT",
			"STRING_CONSTANT", "IDENT", "COMMENT", "WS"
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
			setState(55);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,0,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					{
					setState(52);
					schema_ele();
					}
					}
				}
				setState(57);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,0,_ctx);
			}
			setState(61);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(58);
				suffix_comment();
				}
				}
				setState(63);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(64);
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
			setState(70);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,2,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(66);
				struct_decl();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(67);
				interface_decl();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(68);
				table_decl();
				}
				break;
			case 4:
				enterOuterAlt(_localctx, 4);
				{
				setState(69);
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
			setState(75);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(72);
				leading_comment();
				}
				}
				setState(77);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(78);
			match(STRUCT);
			setState(79);
			ns_ident();
			setState(80);
			metadata();
			setState(81);
			match(LC_COMMENT);
			setState(86);
			_errHandler.sync(this);
			_alt = getInterpreter().adaptivePredict(_input,5,_ctx);
			while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
				if ( _alt==1 ) {
					{
					setState(84);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,4,_ctx) ) {
					case 1:
						{
						setState(82);
						field_decl();
						}
						break;
					case 2:
						{
						setState(83);
						foreign_decl();
						}
						break;
					}
					}
				}
				setState(88);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,5,_ctx);
			}
			setState(92);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(89);
				suffix_comment();
				}
				}
				setState(94);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(95);
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
			setState(100);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(97);
				leading_comment();
				}
				}
				setState(102);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(103);
			match(INTERFACE);
			setState(104);
			ns_ident();
			setState(105);
			metadata();
			setState(106);
			match(LC_COMMENT);
			setState(108);
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					{
					setState(107);
					struct_decl();
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(110);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,8,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
			setState(115);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(112);
				suffix_comment();
				}
				}
				setState(117);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(118);
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
			setState(123);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(120);
				leading_comment();
				}
				}
				setState(125);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(126);
			match(TABLE);
			setState(127);
			ns_ident();
			setState(128);
			key();
			setState(129);
			metadata();
			setState(130);
			match(LC_COMMENT);
			setState(134);
			_errHandler.sync(this);
			_alt = 1;
			do {
				switch (_alt) {
				case 1:
					{
					setState(134);
					_errHandler.sync(this);
					switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
					case 1:
						{
						setState(131);
						field_decl();
						}
						break;
					case 2:
						{
						setState(132);
						foreign_decl();
						}
						break;
					case 3:
						{
						setState(133);
						key_decl();
						}
						break;
					}
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				setState(136);
				_errHandler.sync(this);
				_alt = getInterpreter().adaptivePredict(_input,12,_ctx);
			} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
			setState(141);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(138);
				suffix_comment();
				}
				}
				setState(143);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(144);
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
		public List<Suffix_commentContext> suffix_comment() {
			return getRuleContexts(Suffix_commentContext.class);
		}
		public Suffix_commentContext suffix_comment(int i) {
			return getRuleContext(Suffix_commentContext.class,i);
		}
		public List<Enum_value_emptyContext> enum_value_empty() {
			return getRuleContexts(Enum_value_emptyContext.class);
		}
		public Enum_value_emptyContext enum_value_empty(int i) {
			return getRuleContext(Enum_value_emptyContext.class,i);
		}
		public List<Enum_value_assignedContext> enum_value_assigned() {
			return getRuleContexts(Enum_value_assignedContext.class);
		}
		public Enum_value_assignedContext enum_value_assigned(int i) {
			return getRuleContext(Enum_value_assignedContext.class,i);
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
			setState(149);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(146);
				leading_comment();
				}
				}
				setState(151);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(152);
			match(ENUM);
			setState(153);
			ns_ident();
			setState(154);
			metadata();
			setState(155);
			match(LC_COMMENT);
			setState(166);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,17,_ctx) ) {
			case 1:
				{
				setState(157);
				_errHandler.sync(this);
				_alt = 1;
				do {
					switch (_alt) {
					case 1:
						{
						{
						setState(156);
						enum_value_empty();
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					setState(159);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,15,_ctx);
				} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
				}
				break;
			case 2:
				{
				setState(162);
				_errHandler.sync(this);
				_alt = 1;
				do {
					switch (_alt) {
					case 1:
						{
						{
						setState(161);
						enum_value_assigned();
						}
						}
						break;
					default:
						throw new NoViableAltException(this);
					}
					setState(164);
					_errHandler.sync(this);
					_alt = getInterpreter().adaptivePredict(_input,16,_ctx);
				} while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER );
				}
				break;
			}
			setState(171);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(168);
				suffix_comment();
				}
				}
				setState(173);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(174);
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
			setState(179);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(176);
				leading_comment();
				}
				}
				setState(181);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(182);
			identifier();
			setState(183);
			match(COLON);
			setState(184);
			type_();
			setState(186);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==REF || _la==LISTREF) {
				{
				setState(185);
				ref();
				}
			}

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
	public static class Foreign_declContext extends ParserRuleContext {
		public TerminalNode REF() { return getToken(CfgParser.REF, 0); }
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode COLON() { return getToken(CfgParser.COLON, 0); }
		public Source_valuesContext source_values() {
			return getRuleContext(Source_valuesContext.class,0);
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
			match(REF);
			setState(198);
			identifier();
			setState(199);
			match(COLON);
			setState(200);
			source_values();
			setState(201);
			ref();
			setState(202);
			metadata();
			setState(203);
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
			setState(208);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(205);
				leading_comment();
				}
				}
				setState(210);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(211);
			key();
			setState(212);
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
	public static class Enum_value_emptyContext extends ParserRuleContext {
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
		public Enum_value_emptyContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enum_value_empty; }
	}

	public final Enum_value_emptyContext enum_value_empty() throws RecognitionException {
		Enum_value_emptyContext _localctx = new Enum_value_emptyContext(_ctx, getState());
		enterRule(_localctx, 18, RULE_enum_value_empty);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(217);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(214);
				leading_comment();
				}
				}
				setState(219);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(220);
			identifier();
			setState(221);
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
	public static class Enum_value_assignedContext extends ParserRuleContext {
		public IdentifierContext identifier() {
			return getRuleContext(IdentifierContext.class,0);
		}
		public TerminalNode EQ() { return getToken(CfgParser.EQ, 0); }
		public Enum_numberContext enum_number() {
			return getRuleContext(Enum_numberContext.class,0);
		}
		public TerminalNode SEMI_COMMENT() { return getToken(CfgParser.SEMI_COMMENT, 0); }
		public List<Leading_commentContext> leading_comment() {
			return getRuleContexts(Leading_commentContext.class);
		}
		public Leading_commentContext leading_comment(int i) {
			return getRuleContext(Leading_commentContext.class,i);
		}
		public Enum_value_assignedContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enum_value_assigned; }
	}

	public final Enum_value_assignedContext enum_value_assigned() throws RecognitionException {
		Enum_value_assignedContext _localctx = new Enum_value_assignedContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_enum_value_assigned);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(226);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(223);
				leading_comment();
				}
				}
				setState(228);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(229);
			identifier();
			setState(230);
			match(EQ);
			setState(231);
			enum_number();
			setState(232);
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
		enterRule(_localctx, 22, RULE_type_);
		try {
			setState(247);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,25,_ctx) ) {
			case 1:
				_localctx = new TypeListContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(234);
				match(TLIST);
				setState(235);
				match(T__0);
				setState(236);
				type_ele();
				setState(237);
				match(T__1);
				}
				break;
			case 2:
				_localctx = new TypeMapContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(239);
				match(TMAP);
				setState(240);
				match(T__0);
				setState(241);
				type_ele();
				setState(242);
				match(COMMA);
				setState(243);
				type_ele();
				setState(244);
				match(T__1);
				}
				break;
			case 3:
				_localctx = new TypeBasicContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(246);
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
		enterRule(_localctx, 24, RULE_type_ele);
		try {
			setState(251);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,26,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(249);
				match(TBASE);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(250);
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
		public TerminalNode REF() { return getToken(CfgParser.REF, 0); }
		public List<Ns_identContext> ns_ident() {
			return getRuleContexts(Ns_identContext.class);
		}
		public Ns_identContext ns_ident(int i) {
			return getRuleContext(Ns_identContext.class,i);
		}
		public TerminalNode SCOPE() { return getToken(CfgParser.SCOPE, 0); }
		public TerminalNode AT() { return getToken(CfgParser.AT, 0); }
		public KeyContext key() {
			return getRuleContext(KeyContext.class,0);
		}
		public TerminalNode LISTREF() { return getToken(CfgParser.LISTREF, 0); }
		public RefContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ref; }
	}

	public final RefContext ref() throws RecognitionException {
		RefContext _localctx = new RefContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_ref);
		int _la;
		try {
			setState(276);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case REF:
				enterOuterAlt(_localctx, 1);
				{
				setState(253);
				match(REF);
				setState(269);
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
					{
					setState(254);
					ns_ident();
					setState(256);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==LB) {
						{
						setState(255);
						key();
						}
					}

					setState(260);
					_errHandler.sync(this);
					_la = _input.LA(1);
					if (_la==SCOPE) {
						{
						setState(258);
						match(SCOPE);
						setState(259);
						ns_ident();
						}
					}

					}
					break;
				case SCOPE:
					{
					setState(262);
					match(SCOPE);
					setState(263);
					ns_ident();
					}
					break;
				case AT:
					{
					setState(264);
					match(AT);
					setState(265);
					ns_ident();
					setState(266);
					match(SCOPE);
					setState(267);
					ns_ident();
					}
					break;
				default:
					throw new NoViableAltException(this);
				}
				}
				break;
			case LISTREF:
				enterOuterAlt(_localctx, 2);
				{
				setState(271);
				match(LISTREF);
				setState(272);
				ns_ident();
				setState(274);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LB) {
					{
					setState(273);
					key();
					}
				}

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
	public static class Source_valuesContext extends ParserRuleContext {
		public TerminalNode LB() { return getToken(CfgParser.LB, 0); }
		public TerminalNode RB() { return getToken(CfgParser.RB, 0); }
		public List<Source_valueContext> source_value() {
			return getRuleContexts(Source_valueContext.class);
		}
		public Source_valueContext source_value(int i) {
			return getRuleContext(Source_valueContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(CfgParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(CfgParser.COMMA, i);
		}
		public Source_valuesContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_source_values; }
	}

	public final Source_valuesContext source_values() throws RecognitionException {
		Source_valuesContext _localctx = new Source_valuesContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_source_values);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(278);
			match(LB);
			setState(287);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & 4301259768L) != 0)) {
				{
				setState(279);
				source_value();
				setState(284);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(280);
					match(COMMA);
					setState(281);
					source_value();
					}
					}
					setState(286);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(289);
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
	public static class Source_valueContext extends ParserRuleContext {
		public List<Ns_identContext> ns_ident() {
			return getRuleContexts(Ns_identContext.class);
		}
		public Ns_identContext ns_ident(int i) {
			return getRuleContext(Ns_identContext.class,i);
		}
		public TerminalNode SCOPE() { return getToken(CfgParser.SCOPE, 0); }
		public TerminalNode AT() { return getToken(CfgParser.AT, 0); }
		public Source_valueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_source_value; }
	}

	public final Source_valueContext source_value() throws RecognitionException {
		Source_valueContext _localctx = new Source_valueContext(_ctx, getState());
		enterRule(_localctx, 30, RULE_source_value);
		try {
			setState(299);
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
				setState(291);
				ns_ident();
				}
				break;
			case SCOPE:
				enterOuterAlt(_localctx, 2);
				{
				setState(292);
				match(SCOPE);
				setState(293);
				ns_ident();
				}
				break;
			case AT:
				enterOuterAlt(_localctx, 3);
				{
				setState(294);
				match(AT);
				setState(295);
				ns_ident();
				setState(296);
				match(SCOPE);
				setState(297);
				ns_ident();
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
		enterRule(_localctx, 32, RULE_key);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(301);
			match(LB);
			setState(302);
			identifier();
			setState(307);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(303);
				match(COMMA);
				setState(304);
				identifier();
				}
				}
				setState(309);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(310);
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
		enterRule(_localctx, 34, RULE_metadata);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(323);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LP) {
				{
				setState(312);
				match(LP);
				setState(313);
				ident_with_opt_single_value();
				setState(318);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(314);
					match(COMMA);
					setState(315);
					ident_with_opt_single_value();
					}
					}
					setState(320);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(321);
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
		enterRule(_localctx, 36, RULE_ident_with_opt_single_value);
		int _la;
		try {
			setState(331);
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
				setState(325);
				identifier();
				setState(328);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EQ) {
					{
					setState(326);
					match(EQ);
					setState(327);
					single_value();
					}
				}

				}
				break;
			case MINUS:
				enterOuterAlt(_localctx, 2);
				{
				setState(330);
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
		enterRule(_localctx, 38, RULE_minus_ident);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(333);
			match(MINUS);
			setState(334);
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
		enterRule(_localctx, 40, RULE_single_value);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(336);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 4160749568L) != 0)) ) {
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
	public static class Enum_numberContext extends ParserRuleContext {
		public TerminalNode INTEGER_CONSTANT() { return getToken(CfgParser.INTEGER_CONSTANT, 0); }
		public TerminalNode HEX_INTEGER_CONSTANT() { return getToken(CfgParser.HEX_INTEGER_CONSTANT, 0); }
		public Enum_numberContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_enum_number; }
	}

	public final Enum_numberContext enum_number() throws RecognitionException {
		Enum_numberContext _localctx = new Enum_numberContext(_ctx, getState());
		enterRule(_localctx, 42, RULE_enum_number);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(338);
			_la = _input.LA(1);
			if ( !(_la==HEX_INTEGER_CONSTANT || _la==INTEGER_CONSTANT) ) {
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
		enterRule(_localctx, 44, RULE_ns_ident);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(340);
			identifier();
			setState(345);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOT) {
				{
				{
				setState(341);
				match(DOT);
				setState(342);
				identifier();
				}
				}
				setState(347);
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
		enterRule(_localctx, 46, RULE_identifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(348);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 4294968312L) != 0)) ) {
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
		enterRule(_localctx, 48, RULE_leading_comment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(350);
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
		enterRule(_localctx, 50, RULE_suffix_comment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(352);
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
		"\u0004\u0001\"\u0163\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001\u0002"+
		"\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004\u0002"+
		"\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007\u0002"+
		"\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b\u0002"+
		"\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007\u000f"+
		"\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007\u0012"+
		"\u0002\u0013\u0007\u0013\u0002\u0014\u0007\u0014\u0002\u0015\u0007\u0015"+
		"\u0002\u0016\u0007\u0016\u0002\u0017\u0007\u0017\u0002\u0018\u0007\u0018"+
		"\u0002\u0019\u0007\u0019\u0001\u0000\u0005\u00006\b\u0000\n\u0000\f\u0000"+
		"9\t\u0000\u0001\u0000\u0005\u0000<\b\u0000\n\u0000\f\u0000?\t\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0001\u0003"+
		"\u0001G\b\u0001\u0001\u0002\u0005\u0002J\b\u0002\n\u0002\f\u0002M\t\u0002"+
		"\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002"+
		"\u0005\u0002U\b\u0002\n\u0002\f\u0002X\t\u0002\u0001\u0002\u0005\u0002"+
		"[\b\u0002\n\u0002\f\u0002^\t\u0002\u0001\u0002\u0001\u0002\u0001\u0003"+
		"\u0005\u0003c\b\u0003\n\u0003\f\u0003f\t\u0003\u0001\u0003\u0001\u0003"+
		"\u0001\u0003\u0001\u0003\u0001\u0003\u0004\u0003m\b\u0003\u000b\u0003"+
		"\f\u0003n\u0001\u0003\u0005\u0003r\b\u0003\n\u0003\f\u0003u\t\u0003\u0001"+
		"\u0003\u0001\u0003\u0001\u0004\u0005\u0004z\b\u0004\n\u0004\f\u0004}\t"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0004\u0004\u0087\b\u0004\u000b\u0004\f"+
		"\u0004\u0088\u0001\u0004\u0005\u0004\u008c\b\u0004\n\u0004\f\u0004\u008f"+
		"\t\u0004\u0001\u0004\u0001\u0004\u0001\u0005\u0005\u0005\u0094\b\u0005"+
		"\n\u0005\f\u0005\u0097\t\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0001"+
		"\u0005\u0001\u0005\u0004\u0005\u009e\b\u0005\u000b\u0005\f\u0005\u009f"+
		"\u0001\u0005\u0004\u0005\u00a3\b\u0005\u000b\u0005\f\u0005\u00a4\u0003"+
		"\u0005\u00a7\b\u0005\u0001\u0005\u0005\u0005\u00aa\b\u0005\n\u0005\f\u0005"+
		"\u00ad\t\u0005\u0001\u0005\u0001\u0005\u0001\u0006\u0005\u0006\u00b2\b"+
		"\u0006\n\u0006\f\u0006\u00b5\t\u0006\u0001\u0006\u0001\u0006\u0001\u0006"+
		"\u0001\u0006\u0003\u0006\u00bb\b\u0006\u0001\u0006\u0001\u0006\u0001\u0006"+
		"\u0001\u0007\u0005\u0007\u00c1\b\u0007\n\u0007\f\u0007\u00c4\t\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001"+
		"\u0007\u0001\u0007\u0001\b\u0005\b\u00cf\b\b\n\b\f\b\u00d2\t\b\u0001\b"+
		"\u0001\b\u0001\b\u0001\t\u0005\t\u00d8\b\t\n\t\f\t\u00db\t\t\u0001\t\u0001"+
		"\t\u0001\t\u0001\n\u0005\n\u00e1\b\n\n\n\f\n\u00e4\t\n\u0001\n\u0001\n"+
		"\u0001\n\u0001\n\u0001\n\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b"+
		"\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b"+
		"\u0001\u000b\u0001\u000b\u0001\u000b\u0003\u000b\u00f8\b\u000b\u0001\f"+
		"\u0001\f\u0003\f\u00fc\b\f\u0001\r\u0001\r\u0001\r\u0003\r\u0101\b\r\u0001"+
		"\r\u0001\r\u0003\r\u0105\b\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001\r\u0001"+
		"\r\u0001\r\u0003\r\u010e\b\r\u0001\r\u0001\r\u0001\r\u0003\r\u0113\b\r"+
		"\u0003\r\u0115\b\r\u0001\u000e\u0001\u000e\u0001\u000e\u0001\u000e\u0005"+
		"\u000e\u011b\b\u000e\n\u000e\f\u000e\u011e\t\u000e\u0003\u000e\u0120\b"+
		"\u000e\u0001\u000e\u0001\u000e\u0001\u000f\u0001\u000f\u0001\u000f\u0001"+
		"\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0001\u000f\u0003\u000f\u012c"+
		"\b\u000f\u0001\u0010\u0001\u0010\u0001\u0010\u0001\u0010\u0005\u0010\u0132"+
		"\b\u0010\n\u0010\f\u0010\u0135\t\u0010\u0001\u0010\u0001\u0010\u0001\u0011"+
		"\u0001\u0011\u0001\u0011\u0001\u0011\u0005\u0011\u013d\b\u0011\n\u0011"+
		"\f\u0011\u0140\t\u0011\u0001\u0011\u0001\u0011\u0003\u0011\u0144\b\u0011"+
		"\u0001\u0012\u0001\u0012\u0001\u0012\u0003\u0012\u0149\b\u0012\u0001\u0012"+
		"\u0003\u0012\u014c\b\u0012\u0001\u0013\u0001\u0013\u0001\u0013\u0001\u0014"+
		"\u0001\u0014\u0001\u0015\u0001\u0015\u0001\u0016\u0001\u0016\u0001\u0016"+
		"\u0005\u0016\u0158\b\u0016\n\u0016\f\u0016\u015b\t\u0016\u0001\u0017\u0001"+
		"\u0017\u0001\u0018\u0001\u0018\u0001\u0019\u0001\u0019\u0001\u0019\u0000"+
		"\u0000\u001a\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016"+
		"\u0018\u001a\u001c\u001e \"$&(*,.02\u0000\u0003\u0001\u0000\u001b\u001f"+
		"\u0001\u0000\u001d\u001e\u0002\u0000\u0003\t  \u0178\u00007\u0001\u0000"+
		"\u0000\u0000\u0002F\u0001\u0000\u0000\u0000\u0004K\u0001\u0000\u0000\u0000"+
		"\u0006d\u0001\u0000\u0000\u0000\b{\u0001\u0000\u0000\u0000\n\u0095\u0001"+
		"\u0000\u0000\u0000\f\u00b3\u0001\u0000\u0000\u0000\u000e\u00c2\u0001\u0000"+
		"\u0000\u0000\u0010\u00d0\u0001\u0000\u0000\u0000\u0012\u00d9\u0001\u0000"+
		"\u0000\u0000\u0014\u00e2\u0001\u0000\u0000\u0000\u0016\u00f7\u0001\u0000"+
		"\u0000\u0000\u0018\u00fb\u0001\u0000\u0000\u0000\u001a\u0114\u0001\u0000"+
		"\u0000\u0000\u001c\u0116\u0001\u0000\u0000\u0000\u001e\u012b\u0001\u0000"+
		"\u0000\u0000 \u012d\u0001\u0000\u0000\u0000\"\u0143\u0001\u0000\u0000"+
		"\u0000$\u014b\u0001\u0000\u0000\u0000&\u014d\u0001\u0000\u0000\u0000("+
		"\u0150\u0001\u0000\u0000\u0000*\u0152\u0001\u0000\u0000\u0000,\u0154\u0001"+
		"\u0000\u0000\u0000.\u015c\u0001\u0000\u0000\u00000\u015e\u0001\u0000\u0000"+
		"\u00002\u0160\u0001\u0000\u0000\u000046\u0003\u0002\u0001\u000054\u0001"+
		"\u0000\u0000\u000069\u0001\u0000\u0000\u000075\u0001\u0000\u0000\u0000"+
		"78\u0001\u0000\u0000\u00008=\u0001\u0000\u0000\u000097\u0001\u0000\u0000"+
		"\u0000:<\u00032\u0019\u0000;:\u0001\u0000\u0000\u0000<?\u0001\u0000\u0000"+
		"\u0000=;\u0001\u0000\u0000\u0000=>\u0001\u0000\u0000\u0000>@\u0001\u0000"+
		"\u0000\u0000?=\u0001\u0000\u0000\u0000@A\u0005\u0000\u0000\u0001A\u0001"+
		"\u0001\u0000\u0000\u0000BG\u0003\u0004\u0002\u0000CG\u0003\u0006\u0003"+
		"\u0000DG\u0003\b\u0004\u0000EG\u0003\n\u0005\u0000FB\u0001\u0000\u0000"+
		"\u0000FC\u0001\u0000\u0000\u0000FD\u0001\u0000\u0000\u0000FE\u0001\u0000"+
		"\u0000\u0000G\u0003\u0001\u0000\u0000\u0000HJ\u00030\u0018\u0000IH\u0001"+
		"\u0000\u0000\u0000JM\u0001\u0000\u0000\u0000KI\u0001\u0000\u0000\u0000"+
		"KL\u0001\u0000\u0000\u0000LN\u0001\u0000\u0000\u0000MK\u0001\u0000\u0000"+
		"\u0000NO\u0005\u0003\u0000\u0000OP\u0003,\u0016\u0000PQ\u0003\"\u0011"+
		"\u0000QV\u0005\u0019\u0000\u0000RU\u0003\f\u0006\u0000SU\u0003\u000e\u0007"+
		"\u0000TR\u0001\u0000\u0000\u0000TS\u0001\u0000\u0000\u0000UX\u0001\u0000"+
		"\u0000\u0000VT\u0001\u0000\u0000\u0000VW\u0001\u0000\u0000\u0000W\\\u0001"+
		"\u0000\u0000\u0000XV\u0001\u0000\u0000\u0000Y[\u00032\u0019\u0000ZY\u0001"+
		"\u0000\u0000\u0000[^\u0001\u0000\u0000\u0000\\Z\u0001\u0000\u0000\u0000"+
		"\\]\u0001\u0000\u0000\u0000]_\u0001\u0000\u0000\u0000^\\\u0001\u0000\u0000"+
		"\u0000_`\u0005\u0011\u0000\u0000`\u0005\u0001\u0000\u0000\u0000ac\u0003"+
		"0\u0018\u0000ba\u0001\u0000\u0000\u0000cf\u0001\u0000\u0000\u0000db\u0001"+
		"\u0000\u0000\u0000de\u0001\u0000\u0000\u0000eg\u0001\u0000\u0000\u0000"+
		"fd\u0001\u0000\u0000\u0000gh\u0005\u0004\u0000\u0000hi\u0003,\u0016\u0000"+
		"ij\u0003\"\u0011\u0000jl\u0005\u0019\u0000\u0000km\u0003\u0004\u0002\u0000"+
		"lk\u0001\u0000\u0000\u0000mn\u0001\u0000\u0000\u0000nl\u0001\u0000\u0000"+
		"\u0000no\u0001\u0000\u0000\u0000os\u0001\u0000\u0000\u0000pr\u00032\u0019"+
		"\u0000qp\u0001\u0000\u0000\u0000ru\u0001\u0000\u0000\u0000sq\u0001\u0000"+
		"\u0000\u0000st\u0001\u0000\u0000\u0000tv\u0001\u0000\u0000\u0000us\u0001"+
		"\u0000\u0000\u0000vw\u0005\u0011\u0000\u0000w\u0007\u0001\u0000\u0000"+
		"\u0000xz\u00030\u0018\u0000yx\u0001\u0000\u0000\u0000z}\u0001\u0000\u0000"+
		"\u0000{y\u0001\u0000\u0000\u0000{|\u0001\u0000\u0000\u0000|~\u0001\u0000"+
		"\u0000\u0000}{\u0001\u0000\u0000\u0000~\u007f\u0005\u0005\u0000\u0000"+
		"\u007f\u0080\u0003,\u0016\u0000\u0080\u0081\u0003 \u0010\u0000\u0081\u0082"+
		"\u0003\"\u0011\u0000\u0082\u0086\u0005\u0019\u0000\u0000\u0083\u0087\u0003"+
		"\f\u0006\u0000\u0084\u0087\u0003\u000e\u0007\u0000\u0085\u0087\u0003\u0010"+
		"\b\u0000\u0086\u0083\u0001\u0000\u0000\u0000\u0086\u0084\u0001\u0000\u0000"+
		"\u0000\u0086\u0085\u0001\u0000\u0000\u0000\u0087\u0088\u0001\u0000\u0000"+
		"\u0000\u0088\u0086\u0001\u0000\u0000\u0000\u0088\u0089\u0001\u0000\u0000"+
		"\u0000\u0089\u008d\u0001\u0000\u0000\u0000\u008a\u008c\u00032\u0019\u0000"+
		"\u008b\u008a\u0001\u0000\u0000\u0000\u008c\u008f\u0001\u0000\u0000\u0000"+
		"\u008d\u008b\u0001\u0000\u0000\u0000\u008d\u008e\u0001\u0000\u0000\u0000"+
		"\u008e\u0090\u0001\u0000\u0000\u0000\u008f\u008d\u0001\u0000\u0000\u0000"+
		"\u0090\u0091\u0005\u0011\u0000\u0000\u0091\t\u0001\u0000\u0000\u0000\u0092"+
		"\u0094\u00030\u0018\u0000\u0093\u0092\u0001\u0000\u0000\u0000\u0094\u0097"+
		"\u0001\u0000\u0000\u0000\u0095\u0093\u0001\u0000\u0000\u0000\u0095\u0096"+
		"\u0001\u0000\u0000\u0000\u0096\u0098\u0001\u0000\u0000\u0000\u0097\u0095"+
		"\u0001\u0000\u0000\u0000\u0098\u0099\u0005\u0006\u0000\u0000\u0099\u009a"+
		"\u0003,\u0016\u0000\u009a\u009b\u0003\"\u0011\u0000\u009b\u00a6\u0005"+
		"\u0019\u0000\u0000\u009c\u009e\u0003\u0012\t\u0000\u009d\u009c\u0001\u0000"+
		"\u0000\u0000\u009e\u009f\u0001\u0000\u0000\u0000\u009f\u009d\u0001\u0000"+
		"\u0000\u0000\u009f\u00a0\u0001\u0000\u0000\u0000\u00a0\u00a7\u0001\u0000"+
		"\u0000\u0000\u00a1\u00a3\u0003\u0014\n\u0000\u00a2\u00a1\u0001\u0000\u0000"+
		"\u0000\u00a3\u00a4\u0001\u0000\u0000\u0000\u00a4\u00a2\u0001\u0000\u0000"+
		"\u0000\u00a4\u00a5\u0001\u0000\u0000\u0000\u00a5\u00a7\u0001\u0000\u0000"+
		"\u0000\u00a6\u009d\u0001\u0000\u0000\u0000\u00a6\u00a2\u0001\u0000\u0000"+
		"\u0000\u00a6\u00a7\u0001\u0000\u0000\u0000\u00a7\u00ab\u0001\u0000\u0000"+
		"\u0000\u00a8\u00aa\u00032\u0019\u0000\u00a9\u00a8\u0001\u0000\u0000\u0000"+
		"\u00aa\u00ad\u0001\u0000\u0000\u0000\u00ab\u00a9\u0001\u0000\u0000\u0000"+
		"\u00ab\u00ac\u0001\u0000\u0000\u0000\u00ac\u00ae\u0001\u0000\u0000\u0000"+
		"\u00ad\u00ab\u0001\u0000\u0000\u0000\u00ae\u00af\u0005\u0011\u0000\u0000"+
		"\u00af\u000b\u0001\u0000\u0000\u0000\u00b0\u00b2\u00030\u0018\u0000\u00b1"+
		"\u00b0\u0001\u0000\u0000\u0000\u00b2\u00b5\u0001\u0000\u0000\u0000\u00b3"+
		"\u00b1\u0001\u0000\u0000\u0000\u00b3\u00b4\u0001\u0000\u0000\u0000\u00b4"+
		"\u00b6\u0001\u0000\u0000\u0000\u00b5\u00b3\u0001\u0000\u0000\u0000\u00b6"+
		"\u00b7\u0003.\u0017\u0000\u00b7\u00b8\u0005\u0014\u0000\u0000\u00b8\u00ba"+
		"\u0003\u0016\u000b\u0000\u00b9\u00bb\u0003\u001a\r\u0000\u00ba\u00b9\u0001"+
		"\u0000\u0000\u0000\u00ba\u00bb\u0001\u0000\u0000\u0000\u00bb\u00bc\u0001"+
		"\u0000\u0000\u0000\u00bc\u00bd\u0003\"\u0011\u0000\u00bd\u00be\u0005\u001a"+
		"\u0000\u0000\u00be\r\u0001\u0000\u0000\u0000\u00bf\u00c1\u00030\u0018"+
		"\u0000\u00c0\u00bf\u0001\u0000\u0000\u0000\u00c1\u00c4\u0001\u0000\u0000"+
		"\u0000\u00c2\u00c0\u0001\u0000\u0000\u0000\u00c2\u00c3\u0001\u0000\u0000"+
		"\u0000\u00c3\u00c5\u0001\u0000\u0000\u0000\u00c4\u00c2\u0001\u0000\u0000"+
		"\u0000\u00c5\u00c6\u0005\n\u0000\u0000\u00c6\u00c7\u0003.\u0017\u0000"+
		"\u00c7\u00c8\u0005\u0014\u0000\u0000\u00c8\u00c9\u0003\u001c\u000e\u0000"+
		"\u00c9\u00ca\u0003\u001a\r\u0000\u00ca\u00cb\u0003\"\u0011\u0000\u00cb"+
		"\u00cc\u0005\u001a\u0000\u0000\u00cc\u000f\u0001\u0000\u0000\u0000\u00cd"+
		"\u00cf\u00030\u0018\u0000\u00ce\u00cd\u0001\u0000\u0000\u0000\u00cf\u00d2"+
		"\u0001\u0000\u0000\u0000\u00d0\u00ce\u0001\u0000\u0000\u0000\u00d0\u00d1"+
		"\u0001\u0000\u0000\u0000\u00d1\u00d3\u0001\u0000\u0000\u0000\u00d2\u00d0"+
		"\u0001\u0000\u0000\u0000\u00d3\u00d4\u0003 \u0010\u0000\u00d4\u00d5\u0005"+
		"\u001a\u0000\u0000\u00d5\u0011\u0001\u0000\u0000\u0000\u00d6\u00d8\u0003"+
		"0\u0018\u0000\u00d7\u00d6\u0001\u0000\u0000\u0000\u00d8\u00db\u0001\u0000"+
		"\u0000\u0000\u00d9\u00d7\u0001\u0000\u0000\u0000\u00d9\u00da\u0001\u0000"+
		"\u0000\u0000\u00da\u00dc\u0001\u0000\u0000\u0000\u00db\u00d9\u0001\u0000"+
		"\u0000\u0000\u00dc\u00dd\u0003.\u0017\u0000\u00dd\u00de\u0005\u001a\u0000"+
		"\u0000\u00de\u0013\u0001\u0000\u0000\u0000\u00df\u00e1\u00030\u0018\u0000"+
		"\u00e0\u00df\u0001\u0000\u0000\u0000\u00e1\u00e4\u0001\u0000\u0000\u0000"+
		"\u00e2\u00e0\u0001\u0000\u0000\u0000\u00e2\u00e3\u0001\u0000\u0000\u0000"+
		"\u00e3\u00e5\u0001\u0000\u0000\u0000\u00e4\u00e2\u0001\u0000\u0000\u0000"+
		"\u00e5\u00e6\u0003.\u0017\u0000\u00e6\u00e7\u0005\f\u0000\u0000\u00e7"+
		"\u00e8\u0003*\u0015\u0000\u00e8\u00e9\u0005\u001a\u0000\u0000\u00e9\u0015"+
		"\u0001\u0000\u0000\u0000\u00ea\u00eb\u0005\u0007\u0000\u0000\u00eb\u00ec"+
		"\u0005\u0001\u0000\u0000\u00ec\u00ed\u0003\u0018\f\u0000\u00ed\u00ee\u0005"+
		"\u0002\u0000\u0000\u00ee\u00f8\u0001\u0000\u0000\u0000\u00ef\u00f0\u0005"+
		"\b\u0000\u0000\u00f0\u00f1\u0005\u0001\u0000\u0000\u00f1\u00f2\u0003\u0018"+
		"\f\u0000\u00f2\u00f3\u0005\u0013\u0000\u0000\u00f3\u00f4\u0003\u0018\f"+
		"\u0000\u00f4\u00f5\u0005\u0002\u0000\u0000\u00f5\u00f8\u0001\u0000\u0000"+
		"\u0000\u00f6\u00f8\u0003\u0018\f\u0000\u00f7\u00ea\u0001\u0000\u0000\u0000"+
		"\u00f7\u00ef\u0001\u0000\u0000\u0000\u00f7\u00f6\u0001\u0000\u0000\u0000"+
		"\u00f8\u0017\u0001\u0000\u0000\u0000\u00f9\u00fc\u0005\t\u0000\u0000\u00fa"+
		"\u00fc\u0003,\u0016\u0000\u00fb\u00f9\u0001\u0000\u0000\u0000\u00fb\u00fa"+
		"\u0001\u0000\u0000\u0000\u00fc\u0019\u0001\u0000\u0000\u0000\u00fd\u010d"+
		"\u0005\n\u0000\u0000\u00fe\u0100\u0003,\u0016\u0000\u00ff\u0101\u0003"+
		" \u0010\u0000\u0100\u00ff\u0001\u0000\u0000\u0000\u0100\u0101\u0001\u0000"+
		"\u0000\u0000\u0101\u0104\u0001\u0000\u0000\u0000\u0102\u0103\u0005\u0015"+
		"\u0000\u0000\u0103\u0105\u0003,\u0016\u0000\u0104\u0102\u0001\u0000\u0000"+
		"\u0000\u0104\u0105\u0001\u0000\u0000\u0000\u0105\u010e\u0001\u0000\u0000"+
		"\u0000\u0106\u0107\u0005\u0015\u0000\u0000\u0107\u010e\u0003,\u0016\u0000"+
		"\u0108\u0109\u0005\u0016\u0000\u0000\u0109\u010a\u0003,\u0016\u0000\u010a"+
		"\u010b\u0005\u0015\u0000\u0000\u010b\u010c\u0003,\u0016\u0000\u010c\u010e"+
		"\u0001\u0000\u0000\u0000\u010d\u00fe\u0001\u0000\u0000\u0000\u010d\u0106"+
		"\u0001\u0000\u0000\u0000\u010d\u0108\u0001\u0000\u0000\u0000\u010e\u0115"+
		"\u0001\u0000\u0000\u0000\u010f\u0110\u0005\u000b\u0000\u0000\u0110\u0112"+
		"\u0003,\u0016\u0000\u0111\u0113\u0003 \u0010\u0000\u0112\u0111\u0001\u0000"+
		"\u0000\u0000\u0112\u0113\u0001\u0000\u0000\u0000\u0113\u0115\u0001\u0000"+
		"\u0000\u0000\u0114\u00fd\u0001\u0000\u0000\u0000\u0114\u010f\u0001\u0000"+
		"\u0000\u0000\u0115\u001b\u0001\u0000\u0000\u0000\u0116\u011f\u0005\u000f"+
		"\u0000\u0000\u0117\u011c\u0003\u001e\u000f\u0000\u0118\u0119\u0005\u0013"+
		"\u0000\u0000\u0119\u011b\u0003\u001e\u000f\u0000\u011a\u0118\u0001\u0000"+
		"\u0000\u0000\u011b\u011e\u0001\u0000\u0000\u0000\u011c\u011a\u0001\u0000"+
		"\u0000\u0000\u011c\u011d\u0001\u0000\u0000\u0000\u011d\u0120\u0001\u0000"+
		"\u0000\u0000\u011e\u011c\u0001\u0000\u0000\u0000\u011f\u0117\u0001\u0000"+
		"\u0000\u0000\u011f\u0120\u0001\u0000\u0000\u0000\u0120\u0121\u0001\u0000"+
		"\u0000\u0000\u0121\u0122\u0005\u0010\u0000\u0000\u0122\u001d\u0001\u0000"+
		"\u0000\u0000\u0123\u012c\u0003,\u0016\u0000\u0124\u0125\u0005\u0015\u0000"+
		"\u0000\u0125\u012c\u0003,\u0016\u0000\u0126\u0127\u0005\u0016\u0000\u0000"+
		"\u0127\u0128\u0003,\u0016\u0000\u0128\u0129\u0005\u0015\u0000\u0000\u0129"+
		"\u012a\u0003,\u0016\u0000\u012a\u012c\u0001\u0000\u0000\u0000\u012b\u0123"+
		"\u0001\u0000\u0000\u0000\u012b\u0124\u0001\u0000\u0000\u0000\u012b\u0126"+
		"\u0001\u0000\u0000\u0000\u012c\u001f\u0001\u0000\u0000\u0000\u012d\u012e"+
		"\u0005\u000f\u0000\u0000\u012e\u0133\u0003.\u0017\u0000\u012f\u0130\u0005"+
		"\u0013\u0000\u0000\u0130\u0132\u0003.\u0017\u0000\u0131\u012f\u0001\u0000"+
		"\u0000\u0000\u0132\u0135\u0001\u0000\u0000\u0000\u0133\u0131\u0001\u0000"+
		"\u0000\u0000\u0133\u0134\u0001\u0000\u0000\u0000\u0134\u0136\u0001\u0000"+
		"\u0000\u0000\u0135\u0133\u0001\u0000\u0000\u0000\u0136\u0137\u0005\u0010"+
		"\u0000\u0000\u0137!\u0001\u0000\u0000\u0000\u0138\u0139\u0005\r\u0000"+
		"\u0000\u0139\u013e\u0003$\u0012\u0000\u013a\u013b\u0005\u0013\u0000\u0000"+
		"\u013b\u013d\u0003$\u0012\u0000\u013c\u013a\u0001\u0000\u0000\u0000\u013d"+
		"\u0140\u0001\u0000\u0000\u0000\u013e\u013c\u0001\u0000\u0000\u0000\u013e"+
		"\u013f\u0001\u0000\u0000\u0000\u013f\u0141\u0001\u0000\u0000\u0000\u0140"+
		"\u013e\u0001\u0000\u0000\u0000\u0141\u0142\u0005\u000e\u0000\u0000\u0142"+
		"\u0144\u0001\u0000\u0000\u0000\u0143\u0138\u0001\u0000\u0000\u0000\u0143"+
		"\u0144\u0001\u0000\u0000\u0000\u0144#\u0001\u0000\u0000\u0000\u0145\u0148"+
		"\u0003.\u0017\u0000\u0146\u0147\u0005\f\u0000\u0000\u0147\u0149\u0003"+
		"(\u0014\u0000\u0148\u0146\u0001\u0000\u0000\u0000\u0148\u0149\u0001\u0000"+
		"\u0000\u0000\u0149\u014c\u0001\u0000\u0000\u0000\u014a\u014c\u0003&\u0013"+
		"\u0000\u014b\u0145\u0001\u0000\u0000\u0000\u014b\u014a\u0001\u0000\u0000"+
		"\u0000\u014c%\u0001\u0000\u0000\u0000\u014d\u014e\u0005\u0018\u0000\u0000"+
		"\u014e\u014f\u0003.\u0017\u0000\u014f\'\u0001\u0000\u0000\u0000\u0150"+
		"\u0151\u0007\u0000\u0000\u0000\u0151)\u0001\u0000\u0000\u0000\u0152\u0153"+
		"\u0007\u0001\u0000\u0000\u0153+\u0001\u0000\u0000\u0000\u0154\u0159\u0003"+
		".\u0017\u0000\u0155\u0156\u0005\u0012\u0000\u0000\u0156\u0158\u0003.\u0017"+
		"\u0000\u0157\u0155\u0001\u0000\u0000\u0000\u0158\u015b\u0001\u0000\u0000"+
		"\u0000\u0159\u0157\u0001\u0000\u0000\u0000\u0159\u015a\u0001\u0000\u0000"+
		"\u0000\u015a-\u0001\u0000\u0000\u0000\u015b\u0159\u0001\u0000\u0000\u0000"+
		"\u015c\u015d\u0007\u0002\u0000\u0000\u015d/\u0001\u0000\u0000\u0000\u015e"+
		"\u015f\u0005!\u0000\u0000\u015f1\u0001\u0000\u0000\u0000\u0160\u0161\u0005"+
		"!\u0000\u0000\u01613\u0001\u0000\u0000\u0000)7=FKTV\\dns{\u0086\u0088"+
		"\u008d\u0095\u009f\u00a4\u00a6\u00ab\u00b3\u00ba\u00c2\u00d0\u00d9\u00e2"+
		"\u00f7\u00fb\u0100\u0104\u010d\u0112\u0114\u011c\u011f\u012b\u0133\u013e"+
		"\u0143\u0148\u014b\u0159";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}