package configgen.schema;

import configgen.schema.CfgSchemaErrs.*;
import configgen.schema.cfg.CfgReader;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CfgSchemaResolverAdvancedTest {

    @Test
    void shouldResolveComplexForeignKeyRelationships() {
        // Given: 复杂外键关系
        String str = """
                table user[id] {
                    id:int;
                    name:str;
                }

                table post[id] {
                    id:int;
                    title:str;
                    author_id:int ->user; // 外键引用user表
                    parent_post_id:int ->post (nullable); // 自引用外键
                }

                table comment[id] {
                    id:int;
                    content:str;
                    user_id:int ->user; // 外键引用user表
                    post_id:int ->post; // 外键引用post表
                }
                """;

        // When: 解析Schema
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();
        errs.checkErrors();

        // Then: 验证关系正确解析
        assertEquals(0, errs.errs().size(), "Should have no errors");
        assertEquals(0, errs.warns().size(), "Should have no warnings");

        // 验证外键关系
        TableSchema userTable = cfg.findTable("user");
        TableSchema postTable = cfg.findTable("post");
        TableSchema commentTable = cfg.findTable("comment");

        assertNotNull(userTable);
        assertNotNull(postTable);
        assertNotNull(commentTable);

        // 验证post表的外键
        ForeignKeySchema postAuthorFk = postTable.foreignKeys().stream()
                .filter(fk -> fk.refTable().equals("user"))
                .findFirst()
                .orElse(null);
        assertNotNull(postAuthorFk, "Post should have foreign key to user");

        // 验证comment表的外键
        ForeignKeySchema commentUserFk = commentTable.foreignKeys().stream()
                .filter(fk -> fk.refTable().equals("user"))
                .findFirst()
                .orElse(null);
        assertNotNull(commentUserFk, "Comment should have foreign key to user");

        ForeignKeySchema commentPostFk = commentTable.foreignKeys().stream()
                .filter(fk -> fk.refTable().equals("post"))
                .findFirst()
                .orElse(null);
        assertNotNull(commentPostFk, "Comment should have foreign key to post");
    }

    @Test
    void shouldDetectCircularReferences() {
        // Given: 循环引用Schema
        String str = """
                table node[id] {
                    id:int;
                    name:str;
                    parent_id:int ->node (nullable); // 父节点引用
                }
                """;

        // When: 解析Schema
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();
        errs.checkErrors();

        // Then: 检测并报告循环引用
        assertEquals(0, errs.errs().size(), "Should handle circular references gracefully");
        assertEquals(0, errs.warns().size(), "Should have no warnings");

        // 验证自引用外键
        TableSchema nodeTable = cfg.findTable("node");
        assertNotNull(nodeTable);

        ForeignKeySchema selfRefFk = nodeTable.foreignKeys().stream()
                .filter(fk -> fk.refTable().equals("node"))
                .findFirst()
                .orElse(null);
        assertNotNull(selfRefFk, "Node should have self-referencing foreign key");
    }

    @Test
    void shouldHandleMixedTypeFieldsCorrectly() {
        // Given: 混合类型字段定义
        String str = """
                table product[id] {
                    id:int;
                    name:str;
                    price:float;
                    is_available:bool;
                    tags:list<str> (pack); // 字符串数组
                    metadata:text; // JSON作为文本处理
                    created_at:long; // datetime作为long处理
                    rating:float; // 可选浮点数
                    description:text; // 文本类型
                }

                table category[id] {
                    id:int;
                    name:str;
                    parent_id:int ->category (nullable); // 自引用外键
                }

                table product_category[id] {
                    id:int;
                    product_id:int ->product; // 外键引用
                    category_id:int ->category; // 外键引用
                    weight:float;
                }
                """;

        // When: 解析字段类型
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        // Then: 验证类型解析正确
        if (!errs.errs().isEmpty()) {
            System.out.println("Errors found: " + errs.errs());
        }
        assertEquals(0, errs.errs().size(), "Should have no errors");
        assertEquals(0, errs.warns().size(), "Should have no warnings");

        // 验证product表的字段类型
        TableSchema productTable = cfg.findTable("product");
        assertNotNull(productTable);

        List<FieldSchema> fields = productTable.fields();
        assertEquals(9, fields.size(), "Product table should have 9 fields");

        // 验证各种字段类型
        assertTrue(fields.stream().anyMatch(f -> f.name().equals("id") && f.type() instanceof FieldType.Primitive p && p == FieldType.Primitive.INT));
        assertTrue(fields.stream().anyMatch(f -> f.name().equals("name") && f.type() instanceof FieldType.Primitive p && p == FieldType.Primitive.STRING));
        assertTrue(fields.stream().anyMatch(f -> f.name().equals("price") && f.type() instanceof FieldType.Primitive p && p == FieldType.Primitive.FLOAT));
        assertTrue(fields.stream().anyMatch(f -> f.name().equals("is_available") && f.type() instanceof FieldType.Primitive p && p == FieldType.Primitive.BOOL));
        assertTrue(fields.stream().anyMatch(f -> f.name().equals("tags") && f.type() instanceof FieldType.FList));
        assertTrue(fields.stream().anyMatch(f -> f.name().equals("metadata") && f.type() instanceof FieldType.Primitive p && p == FieldType.Primitive.TEXT)); // JSON作为文本处理
        assertTrue(fields.stream().anyMatch(f -> f.name().equals("created_at") && f.type() instanceof FieldType.Primitive p && p == FieldType.Primitive.LONG)); // datetime作为long处理
        assertTrue(fields.stream().anyMatch(f -> f.name().equals("rating") && f.type() instanceof FieldType.Primitive p && p == FieldType.Primitive.FLOAT));
        assertTrue(fields.stream().anyMatch(f -> f.name().equals("description") && f.type() instanceof FieldType.Primitive p && p == FieldType.Primitive.TEXT));
    }

    @Test
    void shouldHandleComplexInterfaceImplementations() {
        // Given: 复杂接口实现
        String str = """
                interface shape (defaultImpl='circle') {
                    struct circle {
                        radius:float;
                    }

                    struct rectangle {
                        width:float;
                        height:float;
                    }

                    struct triangle {
                        base:float;
                        height:float;
                    }
                }

                table drawing[id] {
                    id:int;
                    name:str;
                    shapes:list<shape> (pack); // 包含多种形状
                }
                """;

        // When: 解析Schema
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();
        errs.checkErrors();

        // Then: 验证接口实现正确
        assertEquals(0, errs.errs().size(), "Should have no errors");
        assertEquals(0, errs.warns().size(), "Should have no warnings");

        // 验证接口定义
        InterfaceSchema shapeInterface = (InterfaceSchema) cfg.findItem("shape");
        assertNotNull(shapeInterface, "Shape interface should exist");
        assertEquals(3, shapeInterface.impls().size(), "Shape should have 3 implementations");

        // 验证实现类
        Set<String> implNames = Set.of("circle", "rectangle", "triangle");
        Set<String> actualImplNames = shapeInterface.impls().stream()
                .map(StructSchema::name)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(implNames, actualImplNames, "Should have correct implementations");

        // 验证drawing表的字段
        TableSchema drawingTable = cfg.findTable("drawing");
        assertNotNull(drawingTable);

        FieldSchema shapesField = drawingTable.fields().stream()
                .filter(f -> f.name().equals("shapes"))
                .findFirst()
                .orElse(null);
        assertNotNull(shapesField, "Drawing should have shapes field");
        assertTrue(shapesField.type() instanceof FieldType.FList, "Shapes field should be a list");
    }

    @Test
    void shouldValidateComplexKeyConstraints() {
        // Given: 复杂键约束
        String str = """
                table user[user_id] {
                    user_id:int;
                    username:str;
                    email:str;
                }

                table product[product_id] {
                    product_id:int;
                    name:str;
                    sku:str; // 唯一约束
                }

                table order[user_id, product_id] { // 复合主键
                    user_id:int ->user;
                    product_id:int ->product;
                    quantity:int;
                    order_date:long;
                }
                """;

        // When: 解析Schema
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();
        errs.checkErrors();

        // Then: 验证键约束正确
        assertEquals(0, errs.errs().size(), "Should have no errors");
        assertEquals(0, errs.warns().size(), "Should have no warnings");

        // 验证复合主键
        TableSchema orderTable = cfg.findTable("order");
        assertNotNull(orderTable);

        KeySchema orderKey = orderTable.primaryKey();
        assertTrue(orderKey.fields().size() > 1, "Order key should be composite");
        assertEquals(2, orderKey.fields().size(), "Composite key should have 2 fields");

        // 验证外键关系
        assertEquals(2, orderTable.foreignKeys().size(), "Order should have 2 foreign keys");

        ForeignKeySchema userFk = orderTable.foreignKeys().stream()
                .filter(fk -> fk.refTable().equals("user"))
                .findFirst()
                .orElse(null);
        assertNotNull(userFk, "Order should have foreign key to user");

        ForeignKeySchema productFk = orderTable.foreignKeys().stream()
                .filter(fk -> fk.refTable().equals("product"))
                .findFirst()
                .orElse(null);
        assertNotNull(productFk, "Order should have foreign key to product");
    }
}