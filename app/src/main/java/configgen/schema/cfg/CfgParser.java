// Generated from D:/work/mygit/cfggen/app/src/main/java/configgen/schema/cfg/Cfg.g4 by ANTLR 4.13.2
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
		T__0=1, T__1=2, STRUCT=3, INTERFACE=4, TABLE=5, TLIST=6, TMAP=7, TBASE=8, 
		REF=9, LISTREF=10, EQ=11, LP=12, RP=13, LB=14, RB=15, RC=16, DOT=17, COMMA=18, 
		COLON=19, PLUS=20, MINUS=21, LC_COMMENT=22, SEMI_COMMENT=23, BOOL_CONSTANT=24, 
		FLOAT_CONSTANT=25, HEX_INTEGER_CONSTANT=26, INTEGER_CONSTANT=27, STRING_CONSTANT=28, 
		IDENT=29, COMMENT=30, WS=31;
	public static final int
		RULE_schema = 0, RULE_schema_ele = 1, RULE_struct_decl = 2, RULE_interface_decl = 3, 
		RULE_table_decl = 4, RULE_field_decl = 5, RULE_foreign_decl = 6, RULE_key_decl = 7, 
		RULE_type_ = 8, RULE_type_ele = 9, RULE_ref = 10, RULE_key = 11, RULE_metadata = 12, 
		RULE_ident_with_opt_single_value = 13, RULE_minus_ident = 14, RULE_single_value = 15, 
		RULE_ns_ident = 16, RULE_identifier = 17, RULE_comment = 18;
	private static String[] makeRuleNames() {
		return new String[] {
			"schema", "schema_ele", "struct_decl", "interface_decl", "table_decl", 
			"field_decl", "foreign_decl", "key_decl", "type_", "type_ele", "ref", 
			"key", "metadata", "ident_with_opt_single_value", "minus_ident", "single_value", 
			"ns_ident", "identifier", "comment"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'<'", "'>'", "'struct'", "'interface'", "'table'", "'list'", "'map'", 
			null, "'->'", "'=>'", "'='", "'('", "')'", "'['", "']'", "'}'", "'.'", 
			"','", "':'", "'+'", "'-'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, "STRUCT", "INTERFACE", "TABLE", "TLIST", "TMAP", "TBASE", 
			"REF", "LISTREF", "EQ", "LP", "RP", "LB", "RB", "RC", "DOT", "COMMA", 
			"COLON", "PLUS", "MINUS", "LC_COMMENT", "SEMI_COMMENT", "BOOL_CONSTANT", 
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
			setState(41);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 1073741880L) != 0)) {
				{
				{
				setState(38);
				schema_ele();
				}
				}
				setState(43);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(44);
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
			setState(49);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,1,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(46);
				struct_decl();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(47);
				interface_decl();
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(48);
				table_decl();
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
		public List<CommentContext> comment() {
			return getRuleContexts(CommentContext.class);
		}
		public CommentContext comment(int i) {
			return getRuleContext(CommentContext.class,i);
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
			setState(54);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(51);
				comment();
				}
				}
				setState(56);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(57);
			match(STRUCT);
			setState(58);
			ns_ident();
			setState(59);
			metadata();
			setState(60);
			match(LC_COMMENT);
			setState(65);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while ((((_la) & ~0x3f) == 0 && ((1L << _la) & 1610613752L) != 0)) {
				{
				setState(63);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
				case 1:
					{
					setState(61);
					field_decl();
					}
					break;
				case 2:
					{
					setState(62);
					foreign_decl();
					}
					break;
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
		public TerminalNode LC_COMMENT() { return getToken(CfgParser.LC_COMMENT, 0); }
		public TerminalNode RC() { return getToken(CfgParser.RC, 0); }
		public List<CommentContext> comment() {
			return getRuleContexts(CommentContext.class);
		}
		public CommentContext comment(int i) {
			return getRuleContext(CommentContext.class,i);
		}
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
			setState(73);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(70);
				comment();
				}
				}
				setState(75);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(76);
			match(INTERFACE);
			setState(77);
			ns_ident();
			setState(78);
			metadata();
			setState(79);
			match(LC_COMMENT);
			setState(81); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				{
				setState(80);
				struct_decl();
				}
				}
				setState(83); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( _la==STRUCT || _la==COMMENT );
			setState(85);
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
		public List<CommentContext> comment() {
			return getRuleContexts(CommentContext.class);
		}
		public CommentContext comment(int i) {
			return getRuleContext(CommentContext.class,i);
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
			setState(90);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(87);
				comment();
				}
				}
				setState(92);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(93);
			match(TABLE);
			setState(94);
			ns_ident();
			setState(95);
			key();
			setState(96);
			metadata();
			setState(97);
			match(LC_COMMENT);
			setState(101); 
			_errHandler.sync(this);
			_la = _input.LA(1);
			do {
				{
				setState(101);
				_errHandler.sync(this);
				switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
				case 1:
					{
					setState(98);
					field_decl();
					}
					break;
				case 2:
					{
					setState(99);
					foreign_decl();
					}
					break;
				case 3:
					{
					setState(100);
					key_decl();
					}
					break;
				}
				}
				setState(103); 
				_errHandler.sync(this);
				_la = _input.LA(1);
			} while ( (((_la) & ~0x3f) == 0 && ((1L << _la) & 1610630136L) != 0) );
			setState(105);
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
		public List<CommentContext> comment() {
			return getRuleContexts(CommentContext.class);
		}
		public CommentContext comment(int i) {
			return getRuleContext(CommentContext.class,i);
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
		enterRule(_localctx, 10, RULE_field_decl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(110);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(107);
				comment();
				}
				}
				setState(112);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(113);
			identifier();
			setState(114);
			match(COLON);
			setState(115);
			type_();
			setState(117);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==REF || _la==LISTREF) {
				{
				setState(116);
				ref();
				}
			}

			setState(119);
			metadata();
			setState(120);
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
		public List<CommentContext> comment() {
			return getRuleContexts(CommentContext.class);
		}
		public CommentContext comment(int i) {
			return getRuleContext(CommentContext.class,i);
		}
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
			setState(125);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(122);
				comment();
				}
				}
				setState(127);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(128);
			match(REF);
			setState(129);
			identifier();
			setState(130);
			match(COLON);
			setState(131);
			key();
			setState(132);
			ref();
			setState(133);
			metadata();
			setState(134);
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
		public List<CommentContext> comment() {
			return getRuleContexts(CommentContext.class);
		}
		public CommentContext comment(int i) {
			return getRuleContext(CommentContext.class,i);
		}
		public Key_declContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_key_decl; }
	}

	public final Key_declContext key_decl() throws RecognitionException {
		Key_declContext _localctx = new Key_declContext(_ctx, getState());
		enterRule(_localctx, 14, RULE_key_decl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(139);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMENT) {
				{
				{
				setState(136);
				comment();
				}
				}
				setState(141);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(142);
			key();
			setState(143);
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
		enterRule(_localctx, 16, RULE_type_);
		try {
			setState(158);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,14,_ctx) ) {
			case 1:
				_localctx = new TypeListContext(_localctx);
				enterOuterAlt(_localctx, 1);
				{
				setState(145);
				match(TLIST);
				setState(146);
				match(T__0);
				setState(147);
				type_ele();
				setState(148);
				match(T__1);
				}
				break;
			case 2:
				_localctx = new TypeMapContext(_localctx);
				enterOuterAlt(_localctx, 2);
				{
				setState(150);
				match(TMAP);
				setState(151);
				match(T__0);
				setState(152);
				type_ele();
				setState(153);
				match(COMMA);
				setState(154);
				type_ele();
				setState(155);
				match(T__1);
				}
				break;
			case 3:
				_localctx = new TypeBasicContext(_localctx);
				enterOuterAlt(_localctx, 3);
				{
				setState(157);
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
		enterRule(_localctx, 18, RULE_type_ele);
		try {
			setState(162);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(160);
				match(TBASE);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(161);
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
		enterRule(_localctx, 20, RULE_ref);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(164);
			_la = _input.LA(1);
			if ( !(_la==REF || _la==LISTREF) ) {
			_errHandler.recoverInline(this);
			}
			else {
				if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
				_errHandler.reportMatch(this);
				consume();
			}
			setState(165);
			ns_ident();
			setState(167);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LB) {
				{
				setState(166);
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
		enterRule(_localctx, 22, RULE_key);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(169);
			match(LB);
			setState(170);
			identifier();
			setState(175);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(171);
				match(COMMA);
				setState(172);
				identifier();
				}
				}
				setState(177);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			setState(178);
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
			setState(191);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LP) {
				{
				setState(180);
				match(LP);
				setState(181);
				ident_with_opt_single_value();
				setState(186);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(182);
					match(COMMA);
					setState(183);
					ident_with_opt_single_value();
					}
					}
					setState(188);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(189);
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
		enterRule(_localctx, 26, RULE_ident_with_opt_single_value);
		int _la;
		try {
			setState(199);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case STRUCT:
			case INTERFACE:
			case TABLE:
			case TLIST:
			case TMAP:
			case TBASE:
			case IDENT:
				enterOuterAlt(_localctx, 1);
				{
				setState(193);
				identifier();
				setState(196);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==EQ) {
					{
					setState(194);
					match(EQ);
					setState(195);
					single_value();
					}
				}

				}
				break;
			case MINUS:
				enterOuterAlt(_localctx, 2);
				{
				setState(198);
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
		enterRule(_localctx, 28, RULE_minus_ident);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(201);
			match(MINUS);
			setState(202);
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
		enterRule(_localctx, 30, RULE_single_value);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(204);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 520093696L) != 0)) ) {
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
		enterRule(_localctx, 32, RULE_ns_ident);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(206);
			identifier();
			setState(211);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==DOT) {
				{
				{
				setState(207);
				match(DOT);
				setState(208);
				identifier();
				}
				}
				setState(213);
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
		enterRule(_localctx, 34, RULE_identifier);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(214);
			_la = _input.LA(1);
			if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & 536871416L) != 0)) ) {
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
	public static class CommentContext extends ParserRuleContext {
		public TerminalNode COMMENT() { return getToken(CfgParser.COMMENT, 0); }
		public CommentContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_comment; }
	}

	public final CommentContext comment() throws RecognitionException {
		CommentContext _localctx = new CommentContext(_ctx, getState());
		enterRule(_localctx, 36, RULE_comment);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(216);
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
		"\u0004\u0001\u001f\u00db\u0002\u0000\u0007\u0000\u0002\u0001\u0007\u0001"+
		"\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004\u0007\u0004"+
		"\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007\u0007\u0007"+
		"\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b\u0007\u000b"+
		"\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002\u000f\u0007"+
		"\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0002\u0012\u0007"+
		"\u0012\u0001\u0000\u0005\u0000(\b\u0000\n\u0000\f\u0000+\t\u0000\u0001"+
		"\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0001\u0003\u00012\b"+
		"\u0001\u0001\u0002\u0005\u00025\b\u0002\n\u0002\f\u00028\t\u0002\u0001"+
		"\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0001\u0002\u0005"+
		"\u0002@\b\u0002\n\u0002\f\u0002C\t\u0002\u0001\u0002\u0001\u0002\u0001"+
		"\u0003\u0005\u0003H\b\u0003\n\u0003\f\u0003K\t\u0003\u0001\u0003\u0001"+
		"\u0003\u0001\u0003\u0001\u0003\u0001\u0003\u0004\u0003R\b\u0003\u000b"+
		"\u0003\f\u0003S\u0001\u0003\u0001\u0003\u0001\u0004\u0005\u0004Y\b\u0004"+
		"\n\u0004\f\u0004\\\t\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004"+
		"\u0001\u0004\u0001\u0004\u0001\u0004\u0001\u0004\u0004\u0004f\b\u0004"+
		"\u000b\u0004\f\u0004g\u0001\u0004\u0001\u0004\u0001\u0005\u0005\u0005"+
		"m\b\u0005\n\u0005\f\u0005p\t\u0005\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0005\u0003\u0005v\b\u0005\u0001\u0005\u0001\u0005\u0001\u0005"+
		"\u0001\u0006\u0005\u0006|\b\u0006\n\u0006\f\u0006\u007f\t\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0007\u0005\u0007\u008a\b\u0007\n\u0007\f\u0007"+
		"\u008d\t\u0007\u0001\u0007\u0001\u0007\u0001\u0007\u0001\b\u0001\b\u0001"+
		"\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001\b\u0001"+
		"\b\u0001\b\u0003\b\u009f\b\b\u0001\t\u0001\t\u0003\t\u00a3\b\t\u0001\n"+
		"\u0001\n\u0001\n\u0003\n\u00a8\b\n\u0001\u000b\u0001\u000b\u0001\u000b"+
		"\u0001\u000b\u0005\u000b\u00ae\b\u000b\n\u000b\f\u000b\u00b1\t\u000b\u0001"+
		"\u000b\u0001\u000b\u0001\f\u0001\f\u0001\f\u0001\f\u0005\f\u00b9\b\f\n"+
		"\f\f\f\u00bc\t\f\u0001\f\u0001\f\u0003\f\u00c0\b\f\u0001\r\u0001\r\u0001"+
		"\r\u0003\r\u00c5\b\r\u0001\r\u0003\r\u00c8\b\r\u0001\u000e\u0001\u000e"+
		"\u0001\u000e\u0001\u000f\u0001\u000f\u0001\u0010\u0001\u0010\u0001\u0010"+
		"\u0005\u0010\u00d2\b\u0010\n\u0010\f\u0010\u00d5\t\u0010\u0001\u0011\u0001"+
		"\u0011\u0001\u0012\u0001\u0012\u0001\u0012\u0000\u0000\u0013\u0000\u0002"+
		"\u0004\u0006\b\n\f\u000e\u0010\u0012\u0014\u0016\u0018\u001a\u001c\u001e"+
		" \"$\u0000\u0003\u0001\u0000\t\n\u0001\u0000\u0018\u001c\u0002\u0000\u0003"+
		"\b\u001d\u001d\u00e1\u0000)\u0001\u0000\u0000\u0000\u00021\u0001\u0000"+
		"\u0000\u0000\u00046\u0001\u0000\u0000\u0000\u0006I\u0001\u0000\u0000\u0000"+
		"\bZ\u0001\u0000\u0000\u0000\nn\u0001\u0000\u0000\u0000\f}\u0001\u0000"+
		"\u0000\u0000\u000e\u008b\u0001\u0000\u0000\u0000\u0010\u009e\u0001\u0000"+
		"\u0000\u0000\u0012\u00a2\u0001\u0000\u0000\u0000\u0014\u00a4\u0001\u0000"+
		"\u0000\u0000\u0016\u00a9\u0001\u0000\u0000\u0000\u0018\u00bf\u0001\u0000"+
		"\u0000\u0000\u001a\u00c7\u0001\u0000\u0000\u0000\u001c\u00c9\u0001\u0000"+
		"\u0000\u0000\u001e\u00cc\u0001\u0000\u0000\u0000 \u00ce\u0001\u0000\u0000"+
		"\u0000\"\u00d6\u0001\u0000\u0000\u0000$\u00d8\u0001\u0000\u0000\u0000"+
		"&(\u0003\u0002\u0001\u0000\'&\u0001\u0000\u0000\u0000(+\u0001\u0000\u0000"+
		"\u0000)\'\u0001\u0000\u0000\u0000)*\u0001\u0000\u0000\u0000*,\u0001\u0000"+
		"\u0000\u0000+)\u0001\u0000\u0000\u0000,-\u0005\u0000\u0000\u0001-\u0001"+
		"\u0001\u0000\u0000\u0000.2\u0003\u0004\u0002\u0000/2\u0003\u0006\u0003"+
		"\u000002\u0003\b\u0004\u00001.\u0001\u0000\u0000\u00001/\u0001\u0000\u0000"+
		"\u000010\u0001\u0000\u0000\u00002\u0003\u0001\u0000\u0000\u000035\u0003"+
		"$\u0012\u000043\u0001\u0000\u0000\u000058\u0001\u0000\u0000\u000064\u0001"+
		"\u0000\u0000\u000067\u0001\u0000\u0000\u000079\u0001\u0000\u0000\u0000"+
		"86\u0001\u0000\u0000\u00009:\u0005\u0003\u0000\u0000:;\u0003 \u0010\u0000"+
		";<\u0003\u0018\f\u0000<A\u0005\u0016\u0000\u0000=@\u0003\n\u0005\u0000"+
		">@\u0003\f\u0006\u0000?=\u0001\u0000\u0000\u0000?>\u0001\u0000\u0000\u0000"+
		"@C\u0001\u0000\u0000\u0000A?\u0001\u0000\u0000\u0000AB\u0001\u0000\u0000"+
		"\u0000BD\u0001\u0000\u0000\u0000CA\u0001\u0000\u0000\u0000DE\u0005\u0010"+
		"\u0000\u0000E\u0005\u0001\u0000\u0000\u0000FH\u0003$\u0012\u0000GF\u0001"+
		"\u0000\u0000\u0000HK\u0001\u0000\u0000\u0000IG\u0001\u0000\u0000\u0000"+
		"IJ\u0001\u0000\u0000\u0000JL\u0001\u0000\u0000\u0000KI\u0001\u0000\u0000"+
		"\u0000LM\u0005\u0004\u0000\u0000MN\u0003 \u0010\u0000NO\u0003\u0018\f"+
		"\u0000OQ\u0005\u0016\u0000\u0000PR\u0003\u0004\u0002\u0000QP\u0001\u0000"+
		"\u0000\u0000RS\u0001\u0000\u0000\u0000SQ\u0001\u0000\u0000\u0000ST\u0001"+
		"\u0000\u0000\u0000TU\u0001\u0000\u0000\u0000UV\u0005\u0010\u0000\u0000"+
		"V\u0007\u0001\u0000\u0000\u0000WY\u0003$\u0012\u0000XW\u0001\u0000\u0000"+
		"\u0000Y\\\u0001\u0000\u0000\u0000ZX\u0001\u0000\u0000\u0000Z[\u0001\u0000"+
		"\u0000\u0000[]\u0001\u0000\u0000\u0000\\Z\u0001\u0000\u0000\u0000]^\u0005"+
		"\u0005\u0000\u0000^_\u0003 \u0010\u0000_`\u0003\u0016\u000b\u0000`a\u0003"+
		"\u0018\f\u0000ae\u0005\u0016\u0000\u0000bf\u0003\n\u0005\u0000cf\u0003"+
		"\f\u0006\u0000df\u0003\u000e\u0007\u0000eb\u0001\u0000\u0000\u0000ec\u0001"+
		"\u0000\u0000\u0000ed\u0001\u0000\u0000\u0000fg\u0001\u0000\u0000\u0000"+
		"ge\u0001\u0000\u0000\u0000gh\u0001\u0000\u0000\u0000hi\u0001\u0000\u0000"+
		"\u0000ij\u0005\u0010\u0000\u0000j\t\u0001\u0000\u0000\u0000km\u0003$\u0012"+
		"\u0000lk\u0001\u0000\u0000\u0000mp\u0001\u0000\u0000\u0000nl\u0001\u0000"+
		"\u0000\u0000no\u0001\u0000\u0000\u0000oq\u0001\u0000\u0000\u0000pn\u0001"+
		"\u0000\u0000\u0000qr\u0003\"\u0011\u0000rs\u0005\u0013\u0000\u0000su\u0003"+
		"\u0010\b\u0000tv\u0003\u0014\n\u0000ut\u0001\u0000\u0000\u0000uv\u0001"+
		"\u0000\u0000\u0000vw\u0001\u0000\u0000\u0000wx\u0003\u0018\f\u0000xy\u0005"+
		"\u0017\u0000\u0000y\u000b\u0001\u0000\u0000\u0000z|\u0003$\u0012\u0000"+
		"{z\u0001\u0000\u0000\u0000|\u007f\u0001\u0000\u0000\u0000}{\u0001\u0000"+
		"\u0000\u0000}~\u0001\u0000\u0000\u0000~\u0080\u0001\u0000\u0000\u0000"+
		"\u007f}\u0001\u0000\u0000\u0000\u0080\u0081\u0005\t\u0000\u0000\u0081"+
		"\u0082\u0003\"\u0011\u0000\u0082\u0083\u0005\u0013\u0000\u0000\u0083\u0084"+
		"\u0003\u0016\u000b\u0000\u0084\u0085\u0003\u0014\n\u0000\u0085\u0086\u0003"+
		"\u0018\f\u0000\u0086\u0087\u0005\u0017\u0000\u0000\u0087\r\u0001\u0000"+
		"\u0000\u0000\u0088\u008a\u0003$\u0012\u0000\u0089\u0088\u0001\u0000\u0000"+
		"\u0000\u008a\u008d\u0001\u0000\u0000\u0000\u008b\u0089\u0001\u0000\u0000"+
		"\u0000\u008b\u008c\u0001\u0000\u0000\u0000\u008c\u008e\u0001\u0000\u0000"+
		"\u0000\u008d\u008b\u0001\u0000\u0000\u0000\u008e\u008f\u0003\u0016\u000b"+
		"\u0000\u008f\u0090\u0005\u0017\u0000\u0000\u0090\u000f\u0001\u0000\u0000"+
		"\u0000\u0091\u0092\u0005\u0006\u0000\u0000\u0092\u0093\u0005\u0001\u0000"+
		"\u0000\u0093\u0094\u0003\u0012\t\u0000\u0094\u0095\u0005\u0002\u0000\u0000"+
		"\u0095\u009f\u0001\u0000\u0000\u0000\u0096\u0097\u0005\u0007\u0000\u0000"+
		"\u0097\u0098\u0005\u0001\u0000\u0000\u0098\u0099\u0003\u0012\t\u0000\u0099"+
		"\u009a\u0005\u0012\u0000\u0000\u009a\u009b\u0003\u0012\t\u0000\u009b\u009c"+
		"\u0005\u0002\u0000\u0000\u009c\u009f\u0001\u0000\u0000\u0000\u009d\u009f"+
		"\u0003\u0012\t\u0000\u009e\u0091\u0001\u0000\u0000\u0000\u009e\u0096\u0001"+
		"\u0000\u0000\u0000\u009e\u009d\u0001\u0000\u0000\u0000\u009f\u0011\u0001"+
		"\u0000\u0000\u0000\u00a0\u00a3\u0005\b\u0000\u0000\u00a1\u00a3\u0003 "+
		"\u0010\u0000\u00a2\u00a0\u0001\u0000\u0000\u0000\u00a2\u00a1\u0001\u0000"+
		"\u0000\u0000\u00a3\u0013\u0001\u0000\u0000\u0000\u00a4\u00a5\u0007\u0000"+
		"\u0000\u0000\u00a5\u00a7\u0003 \u0010\u0000\u00a6\u00a8\u0003\u0016\u000b"+
		"\u0000\u00a7\u00a6\u0001\u0000\u0000\u0000\u00a7\u00a8\u0001\u0000\u0000"+
		"\u0000\u00a8\u0015\u0001\u0000\u0000\u0000\u00a9\u00aa\u0005\u000e\u0000"+
		"\u0000\u00aa\u00af\u0003\"\u0011\u0000\u00ab\u00ac\u0005\u0012\u0000\u0000"+
		"\u00ac\u00ae\u0003\"\u0011\u0000\u00ad\u00ab\u0001\u0000\u0000\u0000\u00ae"+
		"\u00b1\u0001\u0000\u0000\u0000\u00af\u00ad\u0001\u0000\u0000\u0000\u00af"+
		"\u00b0\u0001\u0000\u0000\u0000\u00b0\u00b2\u0001\u0000\u0000\u0000\u00b1"+
		"\u00af\u0001\u0000\u0000\u0000\u00b2\u00b3\u0005\u000f\u0000\u0000\u00b3"+
		"\u0017\u0001\u0000\u0000\u0000\u00b4\u00b5\u0005\f\u0000\u0000\u00b5\u00ba"+
		"\u0003\u001a\r\u0000\u00b6\u00b7\u0005\u0012\u0000\u0000\u00b7\u00b9\u0003"+
		"\u001a\r\u0000\u00b8\u00b6\u0001\u0000\u0000\u0000\u00b9\u00bc\u0001\u0000"+
		"\u0000\u0000\u00ba\u00b8\u0001\u0000\u0000\u0000\u00ba\u00bb\u0001\u0000"+
		"\u0000\u0000\u00bb\u00bd\u0001\u0000\u0000\u0000\u00bc\u00ba\u0001\u0000"+
		"\u0000\u0000\u00bd\u00be\u0005\r\u0000\u0000\u00be\u00c0\u0001\u0000\u0000"+
		"\u0000\u00bf\u00b4\u0001\u0000\u0000\u0000\u00bf\u00c0\u0001\u0000\u0000"+
		"\u0000\u00c0\u0019\u0001\u0000\u0000\u0000\u00c1\u00c4\u0003\"\u0011\u0000"+
		"\u00c2\u00c3\u0005\u000b\u0000\u0000\u00c3\u00c5\u0003\u001e\u000f\u0000"+
		"\u00c4\u00c2\u0001\u0000\u0000\u0000\u00c4\u00c5\u0001\u0000\u0000\u0000"+
		"\u00c5\u00c8\u0001\u0000\u0000\u0000\u00c6\u00c8\u0003\u001c\u000e\u0000"+
		"\u00c7\u00c1\u0001\u0000\u0000\u0000\u00c7\u00c6\u0001\u0000\u0000\u0000"+
		"\u00c8\u001b\u0001\u0000\u0000\u0000\u00c9\u00ca\u0005\u0015\u0000\u0000"+
		"\u00ca\u00cb\u0003\"\u0011\u0000\u00cb\u001d\u0001\u0000\u0000\u0000\u00cc"+
		"\u00cd\u0007\u0001\u0000\u0000\u00cd\u001f\u0001\u0000\u0000\u0000\u00ce"+
		"\u00d3\u0003\"\u0011\u0000\u00cf\u00d0\u0005\u0011\u0000\u0000\u00d0\u00d2"+
		"\u0003\"\u0011\u0000\u00d1\u00cf\u0001\u0000\u0000\u0000\u00d2\u00d5\u0001"+
		"\u0000\u0000\u0000\u00d3\u00d1\u0001\u0000\u0000\u0000\u00d3\u00d4\u0001"+
		"\u0000\u0000\u0000\u00d4!\u0001\u0000\u0000\u0000\u00d5\u00d3\u0001\u0000"+
		"\u0000\u0000\u00d6\u00d7\u0007\u0002\u0000\u0000\u00d7#\u0001\u0000\u0000"+
		"\u0000\u00d8\u00d9\u0005\u001e\u0000\u0000\u00d9%\u0001\u0000\u0000\u0000"+
		"\u0017)16?AISZegnu}\u008b\u009e\u00a2\u00a7\u00af\u00ba\u00bf\u00c4\u00c7"+
		"\u00d3";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}