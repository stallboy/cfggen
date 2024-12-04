package configgen.schema;

import configgen.schema.cfg.CfgReader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static configgen.schema.FieldType.Primitive.INT;

import java.util.List;

import static configgen.schema.FieldFormat.AutoOrPack.AUTO;
import static org.junit.jupiter.api.Assertions.*;

class SpanTest {

    private static CfgSchema cfg;

    @BeforeAll
    public static void beforeAll() {
        String str = """
                table ability[id] (enum='name') {
                	id:int; // 属性类型
                	name:str; // 程序用名字
                }
                struct AttrRandom {
                	Attr:int; // 属性id
                	Min:int; // 最小值
                	Max:int; // 最大值
                }
                struct AttrSep (sep=',') {
                	Attr:int; // 属性id
                	Min:int; // 最小值
                	Max:int; // 最大值
                }
                table item[id]{
                	id:int;
                	attr:AttrRandom;
                }
                table test[id]{
                	id:int;
                	bool1:bool;
                	long1:long;
                	float1:float;
                	str1:str;
                	text1:text;
                	attrPack1:AttrRandom (pack);
                	attrSep1:AttrSep;
                                
                	listFix1:list<int> (fix=3);
                	listSep1:list<int> (sep=',');
                	listBlock1:list<int> (block=3);
                	listPack1:list<int> (pack);
                                
                	listFix2:list<AttrRandom> (fix=2);
                	listBlock2:list<AttrRandom> (block=2);
                	listPack2:list<AttrRandom> (pack);
                                
                	mapFix1:map<int,int> (fix=3);
                	mapBlock1:map<int,int> (block=3);
                	mapPack1:map<int,int> (pack=3);
                                
                	mapFix2:map<int,AttrRandom> (fix=2);
                	
                	cond:condition;
                }
                interface condition {
                	struct checkItem {
                		id:int;
                	}
                                
                	struct checkInfo {
                		id:int;
                	}
                                
                	struct checkUnlockVideo {
                		id:int;
                	}
                	  
                	struct checkSelectOpt {
                		id:int;
                		opt:int; 
                	}
                                
                	struct checkLovePoint {
                		roleId:int;
                		point:int;
                	}
                                
                	struct checkLovePointPercent {
                		roleId:int;
                		nodeid:int;
                		point:int;
                	}
                                
                	struct and {
                		c1:condition (pack);
                		c2:condition (pack);
                               
                	}
                                
                	struct or {
                		c1:condition (pack);
                		c2:condition (pack);
                	}
                                
                	struct not {
                		c:condition (pack);
                	}
                }
                struct NoNoNo {
                	abc:int; 
                	no:NoNoNo; //可以引用自己，不会去计算span
                }
                """;
        cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();
        assertEquals(0, errs.errs().size());

    }

    @Test
    public void span_for_simple_table() {
        assertEquals(2, Span.span(cfg.findTable("ability")));
    }

    @Test
    public void span_for_simple_struct() {
        assertEquals(3, Span.span(cfg.findFieldable("AttrRandom")));
        assertEquals(4, Span.span(cfg.findTable("item")));
    }

    @Test
    public void span1_for_primitive() {
        TableSchema t = cfg.findTable("test");
        assertEquals(1, Span.fieldSpan(t.findField("id")));
        assertEquals(1, Span.fieldSpan(t.findField("bool1")));
        assertEquals(1, Span.fieldSpan(t.findField("long1")));
        assertEquals(1, Span.fieldSpan(t.findField("float1")));
        assertEquals(1, Span.fieldSpan(t.findField("str1")));
        assertEquals(1, Span.fieldSpan(t.findField("text1")));
    }

    @Test
    public void span1_for_struct_pack() {
        TableSchema t = cfg.findTable("test");
        assertEquals(1, Span.fieldSpan(t.findField("attrPack1")));
    }

    @Test
    public void span1_for_struct_sep() {
        TableSchema t = cfg.findTable("test");
        assertEquals(1, Span.fieldSpan(t.findField("attrSep1")));
    }

    @Test
    public void span_for_list() {
        TableSchema t = cfg.findTable("test");
        assertEquals(3, Span.fieldSpan(t.findField("listFix1")));
        assertEquals(3, Span.fieldSpan(t.findField("listBlock1")));
        assertEquals(1, Span.fieldSpan(t.findField("listSep1")));
        assertEquals(1, Span.fieldSpan(t.findField("listPack1")));

        assertEquals(6, Span.fieldSpan(t.findField("listFix2")));
        assertEquals(6, Span.fieldSpan(t.findField("listBlock2")));
        assertEquals(1, Span.fieldSpan(t.findField("listPack2")));
    }

    @Test
    public void span_for_map() {
        TableSchema t = cfg.findTable("test");
        assertEquals(6, Span.fieldSpan(t.findField("mapFix1")));
        assertEquals(6, Span.fieldSpan(t.findField("mapBlock1")));
        assertEquals(1, Span.fieldSpan(t.findField("mapPack1")));
        assertEquals(8, Span.fieldSpan(t.findField("mapFix2")));
    }

    @Test
    public void span_for_interface() {
        Fieldable t = cfg.findFieldable("condition");
        assertEquals(4, Span.span(t));
    }

    @Test
    public void span_no_calc_for_unused_struct() {
        Fieldable t = cfg.findFieldable("NoNoNo");
        assertThrows(IllegalStateException.class, () -> Span.span(t));
    }

    @Test
    public void test_preCalculateAllNeededSpans_validCfgSchema() {
        CfgSchema cfgSchema = CfgSchema.of();
        Metadata meta = Metadata.of();
        List<FieldSchema> fields = List.of(new FieldSchema("field1", INT, AUTO, meta));
        TableSchema table = new TableSchema("table1",
                new KeySchema(List.of("field1")), EntryType.ENo.NO, false, meta, fields, List.of(), List.of());
        cfgSchema.add(table);
        cfgSchema.resolve();

        assertEquals(1, Span.span(table));
    }

    @Test
    public void test_field_span_returns_correct_span() {
        FieldSchema field = new FieldSchema("testField", INT, AUTO, Metadata.of());
        int span = Span.fieldSpan(field);
        assertEquals(1, span);
    }

    @Test
    public void test_simple_type_span() {
        int span = Span.simpleTypeSpan(FieldType.Primitive.STRING);
        assertEquals(1, span);
    }

}