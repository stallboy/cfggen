// Generated from E:/work/mygit/cfggen/app/src/main/java/configgen/schema/cfg/Cfg.g4 by ANTLR 4.13.1
package configgen.schema.cfg;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class CfgParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.13.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, T__1=2, STRUCT=3, INTERFACE=4, TABLE=5, TLIST=6, TMAP=7, TBASE=8, 
		REF=9, LISTREF=10, COMMENT=11, IDENT=12, SEMI=13, EQ=14, LP=15, RP=16, 
		LB=17, RB=18, LC=19, RC=20, DOT=21, COMMA=22, COLON=23, PLUS=24, MINUS=25, 
		STRING_CONSTANT=26, INTEGER_CONSTANT=27, HEX_INTEGER_CONSTANT=28, FLOAT_CONSTANT=29, 
		WS=30;
	public static final int
		RULE_schema = 0, RULE_schema_ele = 1, RULE_struct_decl = 2, RULE_interface_decl = 3, 
		RULE_table_decl = 4, RULE_field_decl = 5, RULE_foreign_decl = 6, RULE_type_ = 7, 
		RULE_type_ele = 8, RULE_ref = 9, RULE_key_decl = 10, RULE_key = 11, RULE_metadata = 12, 
		RULE_ident_with_opt_single_value = 13, RULE_single_value = 14, RULE_ns_ident = 15, 
		RULE_identifier = 16, RULE_keywords = 17;
	private static String[] makeRuleNames() {
		return new String[] {
			"schema", "schema_ele", "struct_decl", "interface_decl", "table_decl", 
			"field_decl", "foreign_decl", "type_", "type_ele", "ref", "key_decl", 
			"key", "metadata", "ident_with_opt_single_value", "single_value", "ns_ident", 
			"identifier", "keywords"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'<'", "'>'", "'struct'", "'interface'", "'table'", "'list'", "'map'", 
			null, "'->'", "'=>'", null, null, "';'", "'='", "'('", "')'", "'['", 
			"']'", "'{'", "'}'", "'.'", "','", "':'", "'+'", "'-'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, "STRUCT", "INTERFACE", "TABLE", "TLIST", "TMAP", "TBASE", 
			"REF", "LISTREF", "COMMENT", "IDENT", "SEMI", "EQ", "LP", "RP", "LB", 
			"RB", "LC", "RC", "DOT", "COMMA", "COLON", "PLUS", "MINUS", "STRING_CONSTANT", 
			"INTEGER_CONSTANT", "HEX_INTEGER_CONSTANT", "FLOAT_CONSTANT", "WS"
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
			enterOuterAlt(_localctx, 1);
			{
			setState(39);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 56L) != 0)) {
				{
				{
				setState(36);
				schema_ele();
				}
				}
				setState(41);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(42);
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
		public Schema_eleContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_schema_ele; }
	}

	public final Schema_eleContext schema_ele() throws RecognitionException {
		Schema_eleContext _localctx = new Schema_eleContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_schema_ele);
		try {
			setState(47);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case STRUCT:
				enterOuterAlt(_localctx, 1);
				{
				setState(44);
				struct_decl();
				}
				break;
			case INTERFACE:
				enterOuterAlt(_localctx, 2);
				{
				setState(45);
				interface_decl();
				}
				break;
			case TABLE:
				enterOuterAlt(_localctx, 3);
				{
				setState(46);
				table_decl();
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
	public static class Struct_declContext extends ParserRuleContext {
		public TerminalNode STRUCT() { return getToken(CfgParser.STRUCT, 0); }
		public Ns_identContext ns_ident() {
			return getRuleContext(Ns_identContext.class,0);
		}
		public MetadataContext metadata() {
			return getRuleContext(MetadataContext.class,0);
		}
		public TerminalNode LC() { return getToken(CfgParser.LC, 0); }
		public TerminalNode RC() { return getToken(CfgParser.RC, 0); }
		public TerminalNode COMMENT() { return getToken(CfgParser.COMMENT, 0); }
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
			enterOuterAlt(_localctx, 1);
			{
			setState(49);
			match(STRUCT);
			setState(50);
			ns_ident();
			setState(51);
			metadata();
			setState(52);
			match(LC);
			setState(54);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMENT) {
				{
				setState(53);
				match(COMMENT);
				}
			}

			setState(59);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 4600L) != 0)) {
				{
				{
				setState(56);
				field_decl();
				}
				}
				setState(61);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(65);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==REF) {
				{
				{
				setState(62);
				foreign_decl();
				}
				}
				setState(67);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(68);
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
		public TerminalNode LC() { return getToken(CfgParser.LC, 0); }
		public TerminalNode RC() { return getToken(CfgParser.RC, 0); }
		public TerminalNode COMMENT() { return getToken(CfgParser.COMMENT, 0); }
		public List<Struct_declContext> struct_decl() {
			return getRuleContexts(Struct_declContext.class);
		}
		public Struct_declContext struct_decl(int i) {
			return getRuleContext(Struct_declContext.class,i);
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
			enterOuterAlt(_localctx, 1);
			{
			setState(70);
			match(INTERFACE);
			setState(71);
			ns_ident();
			setState(72);
			metadata();
			setState(73);
			match(LC);
			setState(75);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMENT) {
				{
				setState(74);
				match(COMMENT);
				}
			}

			setState(78); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(77);
				struct_decl();
				}
				}
				setState(80); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==STRUCT );
			setState(82);
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
		public TerminalNode LC() { return getToken(CfgParser.LC, 0); }
		public TerminalNode RC() { return getToken(CfgParser.RC, 0); }
		public TerminalNode COMMENT() { return getToken(CfgParser.COMMENT, 0); }
		public List<Key_declContext> key_decl() {
			return getRuleContexts(Key_declContext.class);
		}
		public Key_declContext key_decl(int i) {
			return getRuleContext(Key_declContext.class,i);
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
			enterOuterAlt(_localctx, 1);
			{
			setState(84);
			match(TABLE);
			setState(85);
			ns_ident();
			setState(86);
			key();
			setState(87);
			metadata();
			setState(88);
			match(LC);
			setState(90);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMENT) {
				{
				setState(89);
				match(COMMENT);
				}
			}

			setState(95);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==LB) {
				{
				{
				setState(92);
				key_decl();
				}
				}
				setState(97);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(101);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 4600L) != 0)) {
				{
				{
				setState(98);
				field_decl();
				}
				}
				setState(103);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(107);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==REF) {
				{
				{
				setState(104);
				foreign_decl();
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
		public TerminalNode SEMI() { return getToken(CfgParser.SEMI, 0); }
		public RefContext ref() {
			return getRuleContext(RefContext.class,0);
		}
		public TerminalNode COMMENT() { return getToken(CfgParser.COMMENT, 0); }
		public Field_declContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_field_decl; }
	}

	public final Field_declContext field_decl() throws RecognitionException {
		Field_declContext _localctx = new Field_declContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_field_decl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(112);
			identifier();
			setState(113);
			match(COLON);
			setState(114);
			type_();
			setState(116);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==REF || _la==LISTREF) {
				{
				setState(115);
				ref();
				}
			}

			setState(118);
			metadata();
			setState(119);
			match(SEMI);
			setState(121);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMENT) {
				{
				setState(120);
				match(COMMENT);
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
		public TerminalNode SEMI() { return getToken(CfgParser.SEMI, 0); }
		public TerminalNode COMMENT() { return getToken(CfgParser.COMMENT, 0); }
		public Foreign_declContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_foreign_decl; }
	}

	public final Foreign_declContext foreign_decl() throws RecognitionException {
		Foreign_declContext _localctx = new Foreign_declContext(_ctx, getState());
		enterRule(_localctx, 12, RULE_foreign_decl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(123);
			match(REF);
			setState(124);
			identifier();
			setState(125);
			match(COLON);
			setState(126);
			key();
			setState(127);
			ref();
			setState(128);
			metadata();
			setState(129);
			match(SEMI);
			setState(131);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==COMMENT) {
				{
				setState(130);
				match(COMMENT);
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
	public static class Type_Context extends ParserRuleContext {
		public TerminalNode TLIST() { return getToken(CfgParser.TLIST, 0); }
		public List<Type_eleContext> type_ele() {
			return getRuleContexts(Type_eleContext.class);
		}
		public Type_eleContext type_ele(int i) {
			return getRuleContext(Type_eleContext.class,i);
		}
		public TerminalNode TMAP() { return getToken(CfgParser.TMAP, 0); }
		public TerminalNode COMMA() { return getToken(CfgParser.COMMA, 0); }
		public Type_Context(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type_; }
	}

	public final Type_Context type_() throws RecognitionException {
		Type_Context _localctx = new Type_Context(_ctx, getState());
		enterRule(_localctx, 14, RULE_type_);
		try {
			setState(146);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,14,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(133);
				match(TLIST);
				setState(134);
				match(T__0);
				setState(135);
				type_ele();
				setState(136);
				match(T__1);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(138);
				match(TMAP);
				setState(139);
				match(T__0);
				setState(140);
				type_ele();
				setState(141);
				match(COMMA);
				setState(142);
				type_ele();
				setState(143);
				match(T__1);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(145);
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
		enterRule(_localctx, 16, RULE_type_ele);
		try {
			setState(150);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(148);
				match(TBASE);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(149);
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
		enterRule(_localctx, 18, RULE_ref);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(152);
			_la = _input.LA(1);
			if ( !(_la==REF || _la==LISTREF) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(153);
			ns_ident();
			setState(155);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LB) {
				{
				setState(154);
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
	public static class Key_declContext extends ParserRuleContext {
		public KeyContext key() {
			return getRuleContext(KeyContext.class,0);
		}
		public TerminalNode SEMI() { return getToken(CfgParser.SEMI, 0); }
		public Key_declContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_key_decl; }
	}

	public final Key_declContext key_decl() throws RecognitionException {
		Key_declContext _localctx = new Key_declContext(_ctx, getState());
		enterRule(_localctx, 20, RULE_key_decl);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(157);
			key();
			setState(158);
			match(SEMI);
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
		enterRule(_localctx, 22, RULE_key);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(160);
			match(LB);
			setState(161);
			identifier();
			setState(166);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(162);
				match(COMMA);
				setState(163);
				identifier();
				}
				}
				setState(168);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(169);
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
		enterRule(_localctx, 24, RULE_metadata);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(182);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LP) {
				{
				setState(171);
				match(LP);
				setState(172);
				ident_with_opt_single_value();
				setState(177);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(173);
					match(COMMA);
					setState(174);
					ident_with_opt_single_value();
					}
					}
					setState(179);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(180);
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
		public Ident_with_opt_single_valueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_ident_with_opt_single_value; }
	}

	public final Ident_with_opt_single_valueContext ident_with_opt_single_value() throws RecognitionException {
		Ident_with_opt_single_valueContext _localctx = new Ident_with_opt_single_valueContext(_ctx, getState());
		enterRule(_localctx, 26, RULE_ident_with_opt_single_value);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(184);
			identifier();
			setState(187);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==EQ) {
				{
				setState(185);
				match(EQ);
				setState(186);
				single_value();
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
	public static class Single_valueContext extends ParserRuleContext {
		public TerminalNode INTEGER_CONSTANT() { return getToken(CfgParser.INTEGER_CONSTANT, 0); }
		public TerminalNode HEX_INTEGER_CONSTANT() { return getToken(CfgParser.HEX_INTEGER_CONSTANT, 0); }
		public TerminalNode FLOAT_CONSTANT() { return getToken(CfgParser.FLOAT_CONSTANT, 0); }
		public TerminalNode STRING_CONSTANT() { return getToken(CfgParser.STRING_CONSTANT, 0); }
		public Single_valueContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_single_value; }
	}

	public final Single_valueContext single_value() throws RecognitionException {
		Single_valueContext _localctx = new Single_valueContext(_ctx, getState());
		enterRule(_localctx, 28, RULE_single_value);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(189);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 1006632960L) != 0)) ) {
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
		enterRule(_localctx, 30, RULE_ns_ident);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(191);
			identifier();
			setState(196);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOT) {
				{
				{
				setState(192);
				match(DOT);
				setState(193);
				identifier();
				}
				}
				setState(198);
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
		public KeywordsContext keywords() {
			return getRuleContext(KeywordsContext.class,0);
		}
		public IdentifierContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_identifier; }
	}

	public final IdentifierContext identifier() throws RecognitionException {
		IdentifierContext _localctx = new IdentifierContext(_ctx, getState());
		enterRule(_localctx, 32, RULE_identifier);
		try {
			setState(201);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case IDENT:
				enterOuterAlt(_localctx, 1);
				{
				setState(199);
				match(IDENT);
				}
				break;
			case STRUCT:
			case INTERFACE:
			case TABLE:
			case TLIST:
			case TMAP:
			case TBASE:
				enterOuterAlt(_localctx, 2);
				{
				setState(200);
				keywords();
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
	public static class KeywordsContext extends ParserRuleContext {
		public TerminalNode STRUCT() { return getToken(CfgParser.STRUCT, 0); }
		public TerminalNode INTERFACE() { return getToken(CfgParser.INTERFACE, 0); }
		public TerminalNode TABLE() { return getToken(CfgParser.TABLE, 0); }
		public TerminalNode TLIST() { return getToken(CfgParser.TLIST, 0); }
		public TerminalNode TMAP() { return getToken(CfgParser.TMAP, 0); }
		public TerminalNode TBASE() { return getToken(CfgParser.TBASE, 0); }
		public KeywordsContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_keywords; }
	}

	public final KeywordsContext keywords() throws RecognitionException {
		KeywordsContext _localctx = new KeywordsContext(_ctx, getState());
		enterRule(_localctx, 34, RULE_keywords);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(203);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 504L) != 0)) ) {
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

	public static final String _serializedATN =
		"\u0004\u0001\u001e\u00ce\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001"+
		"\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004"+
		"\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007"+
		"\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b"+
		"\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007"+
		"\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0001\u0000\u0005"+
		"\u0000&\b\u0000\n\u0000\f\u0000)\t\u0000\u0001\u0000\u0001\u0000\u0001"+
		"\u0001\u0001\u0001\u0001\u0001\u0003\u00010\b\u0001\u0001\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0003\u00027\b\u0002\u0001"+
		"\u0002\u0005\u0002:\b\u0002\n\u0002\f\u0002=\t\u0002\u0001\u0002\u0005"+
		"\u0002@\b\u0002\n\u0002\f\u0002C\t\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0003\u0003L\b"+
		"\u0003\u0001\u0003\u0004\u0003O\b\u0003\u000b\u0003\f\u0003P\u0001\u0003"+
		"\u0001\u0003\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004"+
		"\u0001\u0004\u0003\u0004[\b\u0004\u0001\u0004\u0005\u0004^\b\u0004\n\u0004"+
		"\f\u0004a\t\u0004\u0001\u0004\u0005\u0004d\b\u0004\n\u0004\f\u0004g\t"+
		"\u0004\u0001\u0004\u0005\u0004j\b\u0004\n\u0004\f\u0004m\t\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0003"+
		"\u0005u\b\u0005\u0001\u0005\u0001\u0005\u0001\u0005\u0003\u0005z\b\u0005"+
		"\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006"+
		"\u0001\u0006\u0001\u0006\u0003\u0006\u0084\b\u0006\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007"+
		"\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0003\u0007"+
		"\u0093\b\u0007\u0001\b\u0001\b\u0003\b\u0097\b\b\u0001\t\u0001\t\u0001"+
		"\t\u0003\t\u009c\b\t\u0001\n\u0001\n\u0001\n\u0001\u000b\u0001\u000b\u0001"+
		"\u000b\u0001\u000b\u0005\u000b\u00a5\b\u000b\n\u000b\f\u000b\u00a8\t\u000b"+
		"\u0001\u000b\u0001\u000b\u0001\f\u0001\f\u0001\f\u0001\f\u0005\f\u00b0"+
		"\b\f\n\f\f\f\u00b3\t\f\u0001\f\u0001\f\u0003\f\u00b7\b\f\u0001\r\u0001"+
		"\r\u0001\r\u0003\r\u00bc\b\r\u0001\u000e\u0001\u000e\u0001\u000f\u0001"+
		"\u000f\u0001\u000f\u0005\u000f\u00c3\b\u000f\n\u000f\f\u000f\u00c6\t\u000f"+
		"\u0001\u0010\u0001\u0010\u0003\u0010\u00ca\b\u0010\u0001\u0011\u0001\u0011"+
		"\u0001\u0011\u0000\u0000\u0012\u0000\u0002\u0004\u0006\b\n\f\u000e\u0010"+
		"\u0012\u0014\u0016\u0018\u001a\u001c\u001e \"\u0000\u0003\u0001\u0000"+
		"\t\n\u0001\u0000\u001a\u001d\u0001\u0000\u0003\b\u00d4\u0000\'\u0001\u0000"+
		"\u0000\u0000\u0002/\u0001\u0000\u0000\u0000\u00041\u0001\u0000\u0000\u0000"+
		"\u0006F\u0001\u0000\u0000\u0000\bT\u0001\u0000\u0000\u0000\np\u0001\u0000"+
		"\u0000\u0000\f{\u0001\u0000\u0000\u0000\u000e\u0092\u0001\u0000\u0000"+
		"\u0000\u0010\u0096\u0001\u0000\u0000\u0000\u0012\u0098\u0001\u0000\u0000"+
		"\u0000\u0014\u009d\u0001\u0000\u0000\u0000\u0016\u00a0\u0001\u0000\u0000"+
		"\u0000\u0018\u00b6\u0001\u0000\u0000\u0000\u001a\u00b8\u0001\u0000\u0000"+
		"\u0000\u001c\u00bd\u0001\u0000\u0000\u0000\u001e\u00bf\u0001\u0000\u0000"+
		"\u0000 \u00c9\u0001\u0000\u0000\u0000\"\u00cb\u0001\u0000\u0000\u0000"+
		"$&\u0003\u0002\u0001\u0000%$\u0001\u0000\u0000\u0000&)\u0001\u0000\u0000"+
		"\u0000\'%\u0001\u0000\u0000\u0000\'(\u0001\u0000\u0000\u0000(*\u0001\u0000"+
		"\u0000\u0000)\'\u0001\u0000\u0000\u0000*+\u0005\u0000\u0000\u0001+\u0001"+
		"\u0001\u0000\u0000\u0000,0\u0003\u0004\u0002\u0000-0\u0003\u0006\u0003"+
		"\u0000.0\u0003\b\u0004\u0000/,\u0001\u0000\u0000\u0000/-\u0001\u0000\u0000"+
		"\u0000/.\u0001\u0000\u0000\u00000\u0003\u0001\u0000\u0000\u000012\u0005"+
		"\u0003\u0000\u000023\u0003\u001e\u000f\u000034\u0003\u0018\f\u000046\u0005"+
		"\u0013\u0000\u000057\u0005\u000b\u0000\u000065\u0001\u0000\u0000\u0000"+
		"67\u0001\u0000\u0000\u00007;\u0001\u0000\u0000\u00008:\u0003\n\u0005\u0000"+
		"98\u0001\u0000\u0000\u0000:=\u0001\u0000\u0000\u0000;9\u0001\u0000\u0000"+
		"\u0000;<\u0001\u0000\u0000\u0000<A\u0001\u0000\u0000\u0000=;\u0001\u0000"+
		"\u0000\u0000>@\u0003\f\u0006\u0000?>\u0001\u0000\u0000\u0000@C\u0001\u0000"+
		"\u0000\u0000A?\u0001\u0000\u0000\u0000AB\u0001\u0000\u0000\u0000BD\u0001"+
		"\u0000\u0000\u0000CA\u0001\u0000\u0000\u0000DE\u0005\u0014\u0000\u0000"+
		"E\u0005\u0001\u0000\u0000\u0000FG\u0005\u0004\u0000\u0000GH\u0003\u001e"+
		"\u000f\u0000HI\u0003\u0018\f\u0000IK\u0005\u0013\u0000\u0000JL\u0005\u000b"+
		"\u0000\u0000KJ\u0001\u0000\u0000\u0000KL\u0001\u0000\u0000\u0000LN\u0001"+
		"\u0000\u0000\u0000MO\u0003\u0004\u0002\u0000NM\u0001\u0000\u0000\u0000"+
		"OP\u0001\u0000\u0000\u0000PN\u0001\u0000\u0000\u0000PQ\u0001\u0000\u0000"+
		"\u0000QR\u0001\u0000\u0000\u0000RS\u0005\u0014\u0000\u0000S\u0007\u0001"+
		"\u0000\u0000\u0000TU\u0005\u0005\u0000\u0000UV\u0003\u001e\u000f\u0000"+
		"VW\u0003\u0016\u000b\u0000WX\u0003\u0018\f\u0000XZ\u0005\u0013\u0000\u0000"+
		"Y[\u0005\u000b\u0000\u0000ZY\u0001\u0000\u0000\u0000Z[\u0001\u0000\u0000"+
		"\u0000[_\u0001\u0000\u0000\u0000\\^\u0003\u0014\n\u0000]\\\u0001\u0000"+
		"\u0000\u0000^a\u0001\u0000\u0000\u0000_]\u0001\u0000\u0000\u0000_`\u0001"+
		"\u0000\u0000\u0000`e\u0001\u0000\u0000\u0000a_\u0001\u0000\u0000\u0000"+
		"bd\u0003\n\u0005\u0000cb\u0001\u0000\u0000\u0000dg\u0001\u0000\u0000\u0000"+
		"ec\u0001\u0000\u0000\u0000ef\u0001\u0000\u0000\u0000fk\u0001\u0000\u0000"+
		"\u0000ge\u0001\u0000\u0000\u0000hj\u0003\f\u0006\u0000ih\u0001\u0000\u0000"+
		"\u0000jm\u0001\u0000\u0000\u0000ki\u0001\u0000\u0000\u0000kl\u0001\u0000"+
		"\u0000\u0000ln\u0001\u0000\u0000\u0000mk\u0001\u0000\u0000\u0000no\u0005"+
		"\u0014\u0000\u0000o\t\u0001\u0000\u0000\u0000pq\u0003 \u0010\u0000qr\u0005"+
		"\u0017\u0000\u0000rt\u0003\u000e\u0007\u0000su\u0003\u0012\t\u0000ts\u0001"+
		"\u0000\u0000\u0000tu\u0001\u0000\u0000\u0000uv\u0001\u0000\u0000\u0000"+
		"vw\u0003\u0018\f\u0000wy\u0005\r\u0000\u0000xz\u0005\u000b\u0000\u0000"+
		"yx\u0001\u0000\u0000\u0000yz\u0001\u0000\u0000\u0000z\u000b\u0001\u0000"+
		"\u0000\u0000{|\u0005\t\u0000\u0000|}\u0003 \u0010\u0000}~\u0005\u0017"+
		"\u0000\u0000~\u007f\u0003\u0016\u000b\u0000\u007f\u0080\u0003\u0012\t"+
		"\u0000\u0080\u0081\u0003\u0018\f\u0000\u0081\u0083\u0005\r\u0000\u0000"+
		"\u0082\u0084\u0005\u000b\u0000\u0000\u0083\u0082\u0001\u0000\u0000\u0000"+
		"\u0083\u0084\u0001\u0000\u0000\u0000\u0084\r\u0001\u0000\u0000\u0000\u0085"+
		"\u0086\u0005\u0006\u0000\u0000\u0086\u0087\u0005\u0001\u0000\u0000\u0087"+
		"\u0088\u0003\u0010\b\u0000\u0088\u0089\u0005\u0002\u0000\u0000\u0089\u0093"+
		"\u0001\u0000\u0000\u0000\u008a\u008b\u0005\u0007\u0000\u0000\u008b\u008c"+
		"\u0005\u0001\u0000\u0000\u008c\u008d\u0003\u0010\b\u0000\u008d\u008e\u0005"+
		"\u0016\u0000\u0000\u008e\u008f\u0003\u0010\b\u0000\u008f\u0090\u0005\u0002"+
		"\u0000\u0000\u0090\u0093\u0001\u0000\u0000\u0000\u0091\u0093\u0003\u0010"+
		"\b\u0000\u0092\u0085\u0001\u0000\u0000\u0000\u0092\u008a\u0001\u0000\u0000"+
		"\u0000\u0092\u0091\u0001\u0000\u0000\u0000\u0093\u000f\u0001\u0000\u0000"+
		"\u0000\u0094\u0097\u0005\b\u0000\u0000\u0095\u0097\u0003\u001e\u000f\u0000"+
		"\u0096\u0094\u0001\u0000\u0000\u0000\u0096\u0095\u0001\u0000\u0000\u0000"+
		"\u0097\u0011\u0001\u0000\u0000\u0000\u0098\u0099\u0007\u0000\u0000\u0000"+
		"\u0099\u009b\u0003\u001e\u000f\u0000\u009a\u009c\u0003\u0016\u000b\u0000"+
		"\u009b\u009a\u0001\u0000\u0000\u0000\u009b\u009c\u0001\u0000\u0000\u0000"+
		"\u009c\u0013\u0001\u0000\u0000\u0000\u009d\u009e\u0003\u0016\u000b\u0000"+
		"\u009e\u009f\u0005\r\u0000\u0000\u009f\u0015\u0001\u0000\u0000\u0000\u00a0"+
		"\u00a1\u0005\u0011\u0000\u0000\u00a1\u00a6\u0003 \u0010\u0000\u00a2\u00a3"+
		"\u0005\u0016\u0000\u0000\u00a3\u00a5\u0003 \u0010\u0000\u00a4\u00a2\u0001"+
		"\u0000\u0000\u0000\u00a5\u00a8\u0001\u0000\u0000\u0000\u00a6\u00a4\u0001"+
		"\u0000\u0000\u0000\u00a6\u00a7\u0001\u0000\u0000\u0000\u00a7\u00a9\u0001"+
		"\u0000\u0000\u0000\u00a8\u00a6\u0001\u0000\u0000\u0000\u00a9\u00aa\u0005"+
		"\u0012\u0000\u0000\u00aa\u0017\u0001\u0000\u0000\u0000\u00ab\u00ac\u0005"+
		"\u000f\u0000\u0000\u00ac\u00b1\u0003\u001a\r\u0000\u00ad\u00ae\u0005\u0016"+
		"\u0000\u0000\u00ae\u00b0\u0003\u001a\r\u0000\u00af\u00ad\u0001\u0000\u0000"+
		"\u0000\u00b0\u00b3\u0001\u0000\u0000\u0000\u00b1\u00af\u0001\u0000\u0000"+
		"\u0000\u00b1\u00b2\u0001\u0000\u0000\u0000\u00b2\u00b4\u0001\u0000\u0000"+
		"\u0000\u00b3\u00b1\u0001\u0000\u0000\u0000\u00b4\u00b5\u0005\u0010\u0000"+
		"\u0000\u00b5\u00b7\u0001\u0000\u0000\u0000\u00b6\u00ab\u0001\u0000\u0000"+
		"\u0000\u00b6\u00b7\u0001\u0000\u0000\u0000\u00b7\u0019\u0001\u0000\u0000"+
		"\u0000\u00b8\u00bb\u0003 \u0010\u0000\u00b9\u00ba\u0005\u000e\u0000\u0000"+
		"\u00ba\u00bc\u0003\u001c\u000e\u0000\u00bb\u00b9\u0001\u0000\u0000\u0000"+
		"\u00bb\u00bc\u0001\u0000\u0000\u0000\u00bc\u001b\u0001\u0000\u0000\u0000"+
		"\u00bd\u00be\u0007\u0001\u0000\u0000\u00be\u001d\u0001\u0000\u0000\u0000"+
		"\u00bf\u00c4\u0003 \u0010\u0000\u00c0\u00c1\u0005\u0015\u0000\u0000\u00c1"+
		"\u00c3\u0003 \u0010\u0000\u00c2\u00c0\u0001\u0000\u0000\u0000\u00c3\u00c6"+
		"\u0001\u0000\u0000\u0000\u00c4\u00c2\u0001\u0000\u0000\u0000\u00c4\u00c5"+
		"\u0001\u0000\u0000\u0000\u00c5\u001f\u0001\u0000\u0000\u0000\u00c6\u00c4"+
		"\u0001\u0000\u0000\u0000\u00c7\u00ca\u0005\f\u0000\u0000\u00c8\u00ca\u0003"+
		"\"\u0011\u0000\u00c9\u00c7\u0001\u0000\u0000\u0000\u00c9\u00c8\u0001\u0000"+
		"\u0000\u0000\u00ca!\u0001\u0000\u0000\u0000\u00cb\u00cc\u0007\u0002\u0000"+
		"\u0000\u00cc#\u0001\u0000\u0000\u0000\u0017\'/6;AKPZ_ekty\u0083\u0092"+
		"\u0096\u009b\u00a6\u00b1\u00b6\u00bb\u00c4\u00c9";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}