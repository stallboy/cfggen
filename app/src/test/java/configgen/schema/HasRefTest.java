package configgen.schema;

import configgen.schema.cfg.CfgReader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static configgen.schema.FieldFormat.AutoOrPack.AUTO;
import static configgen.schema.FieldType.Primitive.INT;
import static org.junit.jupiter.api.Assertions.*;

class HasRefTest {

    private static CfgSchema cfg;

    @BeforeAll
    public static void beforeAll() {
        String str = """
                table ability[id] (enum='name') {
                	id:int; // 属性类型
                	name:str; // 程序用名字
                	action:action;
                	attr:Attr;
                }
                struct Attr {
                	Attr:int; // 属性id
                	Min:int; // 最小值
                	Max:int; // 最大值
                }
                table item[id]{
                	id:int;
                	attr:Attr;
                }
                interface condition {
                	struct checkItem {
                		id:int ->item;
                	}
                
                	struct and {
                		c1:condition (pack);
                		c2:condition (pack);
                	}
                	struct not {
                		c:condition (pack);
                	}
                }
                interface action {
                    struct RewardIf {
                        cond:condition;
                        item: int;
                    }
                }
                """;
        cfg = CfgReader.parse(str);
        cfg.resolve();
    }

    @Test
    void no_ref_table() {
        assertFalse(HasRefOrBlock.hasRef(cfg.findTable("item")));
    }

    @Test
    void ref_in_table() {
        assertTrue(HasRefOrBlock.hasRef(cfg.findTable("ability")));
    }


    @Test
    void ref_in_interface() {
        assertTrue(HasRefOrBlock.hasRef(cfg.findFieldable("condition")));
    }

    @Test
    void ref_in_interface_indirect() {
        assertTrue(HasRefOrBlock.hasRef(cfg.findFieldable("action")));
    }


    @Test
    void ref_on_field() {
        TableSchema t = cfg.findTable("ability");
        assertTrue(HasRefOrBlock.hasRef(t.findField("action").type()));
    }

    @Test
    void no_ref_on_field() {
        TableSchema t = cfg.findTable("ability");
        assertFalse(HasRefOrBlock.hasRef(t.findField("attr").type()));
    }

    @Test
    public void throwException_ifNotResolve() {
        StructSchema structSchema = new StructSchema("struct1", AUTO, Metadata.of(),
                List.of(new FieldSchema("field1", INT, AUTO, Metadata.of())),
                List.of(new ForeignKeySchema("refField1",
                        new KeySchema(List.of("field1")), "table1",
                        new RefKey.RefPrimary(true), Metadata.of())));

        assertThrowsExactly(IllegalStateException.class, () -> HasRefOrBlock.hasRef(structSchema));
    }

}