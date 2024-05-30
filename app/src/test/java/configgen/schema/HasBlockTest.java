package configgen.schema;

import configgen.schema.FieldFormat.Block;
import configgen.schema.cfg.CfgReader;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static configgen.schema.FieldFormat.AutoOrPack.AUTO;
import static configgen.schema.FieldType.Primitive.INT;
import static org.junit.jupiter.api.Assertions.*;

class HasBlockTest {
    private static CfgSchema cfg;

    @BeforeAll
    public static void beforeAll() {
        String str = """
                struct Attr {
                	Attr:int; // 属性id
                	Min:int; // 最小值
                	Max:int; // 最大值
                }
                table item[id]{
                	id:int;
                	attrs:Attr (block=3);
                }
                
                struct Attrs {
                    attrs:Attr (block=3);
                }
                
                struct WeightedAttrs {
                    weight:int;
                    attrs:Attrs;
                }
                
                interface condition {
                	struct checkAttrs {
                		 attrs:Attrs;
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
    public void structWithBlock() {
        FieldSchema fieldWithBlock = new FieldSchema("field1", INT, new Block(2), Metadata.of());
        StructSchema structSchema = new StructSchema("struct1", AUTO, Metadata.of(),
                List.of(fieldWithBlock), List.of());

        assertTrue(HasBlock.hasBlock(structSchema));
    }

    @Test
    public void tableWithBlock() {
        assertTrue(HasBlock.hasBlock(cfg.findTable("item")));
    }

    @Test
    public void directStructWithBlock() {
        assertTrue(HasBlock.hasBlock(cfg.findFieldable("Attrs")));
    }


    @Test
    public void innerStructWithBlock() {
        assertTrue(HasBlock.hasBlock(cfg.findFieldable("WeightedAttrs")));
        assertTrue(HasBlock.hasBlock(cfg.findFieldable("condition")));
        assertTrue(HasBlock.hasBlock(cfg.findFieldable("action")));
    }



}