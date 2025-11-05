package configgen.value;

import configgen.Resources;
import configgen.ctx.Context;
import configgen.data.CfgData;
import configgen.data.Source;
import configgen.schema.TableSchema;
import configgen.value.CfgValue.*;
import configgen.value.CfgValueErrs.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ValueParserConversionTest {

    private @TempDir Path tempDir;

    @Test
    void shouldConvertComplexJsonStructures() {
        // Given: 复杂JSON结构
        String cfgStr = """
                table user[id] {
                    id:int;
                    name:str;
                    profile:text; // JSON类型字段
                    settings:text (nullable); // 可选JSON字段
                }
                """;

        String csvData = """
                用户ID,姓名,个人资料,设置
                id,name,profile,settings
                1,Alice,profile1,settings1
                2,Bob,profile2,
                3,Charlie,profile3,settings3
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("user.csv", tempDir, csvData);

        // When: 解析值
        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();

        // Then: 验证转换正确
        VTable userTable = cfgValue.getTable("user");
        assertNotNull(userTable);
        assertEquals(3, userTable.valueList().size());

        // 验证第一个用户的数据
        VStruct user1 = userTable.valueList().get(0);
        VText profile1 = (VText) user1.values().get(2);
        VText settings1 = (VText) user1.values().get(3);

        assertEquals("profile1", profile1.value());
        assertEquals("settings1", settings1.value());

        // 验证第二个用户的数据（settings为空）
        VStruct user2 = userTable.valueList().get(1);
        VText profile2 = (VText) user2.values().get(2);
        VText settings2 = (VText) user2.values().get(3);

        assertEquals("profile2", profile2.value());
        assertEquals("", settings2.value()); // 空值应该为空字符串
    }

    @Test
    void shouldValidateReferenceIntegrity() {
        // Given: 包含引用的值
        String cfgStr = """
                table department[id] {
                    id:int;
                    name:str;
                }

                table employee[id] {
                    id:int;
                    name:str;
                    department_id:int ->department; // 外键引用
                    manager_id:int ->employee (nullable); // 自引用外键
                }
                """;

        String deptCsv = """
                部门ID,部门名称
                id,name
                1,Engineering
                2,Marketing
                3,Sales
                """;

        String empCsv = """
                员工ID,姓名,部门ID,经理ID
                id,name,department_id,manager_id
                1,Alice,1,
                2,Bob,1,1
                3,Charlie,2,1
                4,David,3,2
                5,Eve,1,3
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("department.csv", tempDir, deptCsv);
        Resources.addTempFileFromText("employee.csv", tempDir, empCsv);

        // When: 验证引用
        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();

        // Then: 验证引用完整性
        VTable deptTable = cfgValue.getTable("department");
        VTable empTable = cfgValue.getTable("employee");

        assertNotNull(deptTable);
        assertNotNull(empTable);

        // 验证部门引用
        assertEquals(3, deptTable.valueList().size());
        assertEquals(5, empTable.valueList().size());

        // 验证每个员工的部门引用都存在
        for (VStruct employee : empTable.valueList()) {
            VInt deptId = (VInt) employee.values().get(2);
            VTable deptRef = cfgValue.getTable("department");
            VStruct department = deptRef.primaryKeyMap().get(deptId);
            assertNotNull(department, "Employee " + employee.values().get(1) + " references non-existent department " + deptId.value());
        }

        // 验证经理引用（可选）
        for (VStruct employee : empTable.valueList()) {
            VInt managerId = (VInt) employee.values().get(3);
            if (managerId.value() != 0) { // 非空值
                VStruct manager = empTable.primaryKeyMap().get(managerId);
                assertNotNull(manager, "Employee references non-existent manager " + managerId.value());
            }
        }
    }

    @Test
    void shouldHandleNullAndEmptyValues() {
        // Given: 空值和null值
        String cfgStr = """
                table product[id] {
                    id:int;
                    name:str;
                    description:text (nullable); // 可选文本
                    price:float (nullable); // 可选浮点数
                    tags:list<str> (pack, nullable); // 可选字符串数组
                    metadata:text (nullable); // 可选JSON
                    is_active:bool (nullable); // 可选布尔值
                }
                """;

        String csvData = """
                产品ID,产品名称,描述,价格,标签,元数据,是否激活
                id,name,description,price,tags,metadata,is_active
                1,Product1,"First product",99.99,"tag1,tag2,tag3","{\"color\":\"red\"}",true
                2,Product2,,149.50,,,
                3,Product3,"Third product",,,"{\"weight\":2.5}",false
                4,Product4,"",0.00,"","",
                5,Product5,"Last product",199.99,"single","{\"size\":\"large\"}",true
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("product.csv", tempDir, csvData);

        // When: 处理值
        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();

        // Then: 验证正确处理
        VTable productTable = cfgValue.getTable("product");
        assertNotNull(productTable);
        assertEquals(5, productTable.valueList().size());

        // 验证第一个产品（所有字段都有值）
        VStruct product1 = productTable.valueList().get(0);
        assertEquals("Product1", ((VString) product1.values().get(1)).value());
        assertEquals("First product", ((VText) product1.values().get(2)).value());
        assertEquals(99.99f, ((VFloat) product1.values().get(3)).value(), 0.001);
        assertEquals(3, ((VList) product1.values().get(4)).valueList().size());
        assertNotNull(((VText) product1.values().get(5)).value());
        assertTrue(((VBool) product1.values().get(6)).value());

        // 验证第二个产品（多个空值）
        VStruct product2 = productTable.valueList().get(1);
        assertEquals("Product2", ((VString) product2.values().get(1)).value());
        assertEquals("", ((VText) product2.values().get(2)).value()); // 空description
        assertEquals(149.50f, ((VFloat) product2.values().get(3)).value(), 0.001);
        assertEquals(0, ((VList) product2.values().get(4)).valueList().size()); // 空tags
        assertEquals("", ((VText) product2.values().get(5)).value()); // 空metadata
        assertEquals(false, ((VBool) product2.values().get(6)).value()); // 空is_active

        // 验证第三个产品（部分空值）
        VStruct product3 = productTable.valueList().get(2);
        assertEquals("Product3", ((VString) product3.values().get(1)).value());
        assertEquals("Third product", ((VText) product3.values().get(2)).value());
        assertEquals(0.0f, ((VFloat) product3.values().get(3)).value(), 0.001); // 空price
        assertEquals(0, ((VList) product3.values().get(4)).valueList().size()); // 空tags
        assertNotNull(((VText) product3.values().get(5)).value());
        assertFalse(((VBool) product3.values().get(6)).value());

        // 验证第四个产品（空字符串）
        VStruct product4 = productTable.valueList().get(3);
        assertEquals("Product4", ((VString) product4.values().get(1)).value());
        assertEquals("", ((VText) product4.values().get(2)).value()); // 空字符串
        assertEquals(0.00f, ((VFloat) product4.values().get(3)).value(), 0.001);
        assertEquals(0, ((VList) product4.values().get(4)).valueList().size()); // 空数组
        assertEquals("", ((VText) product4.values().get(5)).value()); // 空JSON字符串
        assertEquals(false, ((VBool) product4.values().get(6)).value()); // 空布尔值
    }

    @Test
    void shouldConvertNestedDataStructures() {
        // Given: 嵌套数据结构 - 使用与DebugSimpleNestedStructTest相同的结构
        String cfgStr = """
                struct address {
                    street:str;
                    city:str;
                }

                struct contact {
                    phone:str;
                    email:str;
                    addr:address;
                }

                table customer[id] {
                    id:int;
                    name:str;
                    contact:contact;
                }
                """;

        String csvData = """
                客户ID,客户名称,联系人电话,联系人邮箱,联系人地址街道,联系人地址城市
                id,name,"contact,phone",contact.email,contact.addr.street,contact.addr.city
                1,Company1,123-456,a@company1.com,Main St,City1
                2,Company2,345-678,c@company2.com,Third St,City3
                3,Company3,,,Main St,City1
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("customer.csv", tempDir, csvData);

        // When: 解析值
        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();

        // Then: 验证嵌套结构转换正确
        VTable customerTable = cfgValue.getTable("customer");
        assertNotNull(customerTable);
        assertEquals(3, customerTable.valueList().size());

        // 验证第一个客户（完整数据）
        VStruct customer1 = customerTable.valueList().get(0);
        VStruct contact1 = (VStruct) customer1.values().get(2);

        assertNotNull(contact1);

        // 验证第一个联系人的嵌套地址
        VStruct address1 = (VStruct) contact1.values().get(2);
        assertEquals("123-456", ((VString) contact1.values().get(0)).value());
        assertEquals("a@company1.com", ((VString) contact1.values().get(1)).value());
        assertEquals("Main St", ((VString) address1.values().get(0)).value());
        assertEquals("City1", ((VString) address1.values().get(1)).value());

        // 验证第二个客户
        VStruct customer2 = customerTable.valueList().get(1);
        VStruct contact2 = (VStruct) customer2.values().get(2);

        assertNotNull(contact2);

        // 验证第三个客户
        VStruct customer3 = customerTable.valueList().get(2);
        VStruct contact3 = (VStruct) customer3.values().get(2);

        assertNotNull(contact3);
    }

    @Test
    void shouldHandleComplexEnumValues() {
        // Given: 复杂枚举值 - 简化版本，避免CSV对齐问题
        String cfgStr = """
                table order_status[status_id] (enum='status_name'){
                    status_id:int;
                    status_name:str;
                    description:text;
                    color:str (nullable);
                }

                table order[order_id] {
                    order_id:int;
                    customer_name:str;
                    status:int ->order_status;
                    priority:int; // 1=Low, 2=Medium, 3=High, 4=Urgent
                }
                """;

        // 使用更简单的CSV格式，只有字段名和数据行
        String statusCsv = """
                状态ID,状态名称,描述,颜色
                status_id,status_name,description,color
                1,pending,Order received but not processed,yellow
                2,processing,Order is being processed,blue
                3,shipped,Order has been shipped,green
                4,delivered,Order has been delivered,purple
                5,cancelled,Order has been cancelled,red
                """;

        String orderCsv = """
                订单ID,客户名称,状态,优先级
                order_id,customer_name,status,priority
                1001,Alice,1,2
                1002,Bob,2,3
                1003,Charlie,3,1
                1004,David,4,4
                1005,Eve,5,2
                """;

        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);
        Resources.addTempFileFromText("order_status.csv", tempDir, statusCsv);
        Resources.addTempFileFromText("order.csv", tempDir, orderCsv);

        // When: 解析值
        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();

        // Then: 验证枚举值转换正确
        VTable statusTable = cfgValue.getTable("order_status");
        VTable orderTable = cfgValue.getTable("order");

        assertNotNull(statusTable);
        assertNotNull(orderTable);

        // 验证状态表
        assertEquals(5, statusTable.valueList().size());

        // 验证订单表的状态引用
        for (VStruct order : orderTable.valueList()) {
            VInt statusId = (VInt) order.values().get(2);
            VStruct status = statusTable.primaryKeyMap().get(statusId);
            assertNotNull(status, "Order references non-existent status " + statusId.value());

            // 验证优先级值
            VInt priority = (VInt) order.values().get(3);
            assertTrue(priority.value() >= 1 && priority.value() <= 4,
                "Priority should be between 1 and 4, got " + priority.value());
        }

        // 验证具体订单的状态
        VStruct order1 = orderTable.primaryKeyMap().get(new VInt(1001, Source.of()));
        VInt status1 = (VInt) order1.values().get(2);
        VStruct statusRef1 = statusTable.primaryKeyMap().get(status1);
        assertEquals("pending", ((VString) statusRef1.values().get(1)).value());

        VStruct order5 = orderTable.primaryKeyMap().get(new VInt(1005, Source.of()));
        VInt status5 = (VInt) order5.values().get(2);
        VStruct statusRef5 = statusTable.primaryKeyMap().get(status5);
        assertEquals("cancelled", ((VString) statusRef5.values().get(1)).value());
    }


    @Test
    void should_handleMixedFormats_when_blockContainsSepAndPackFormats() {
        String cfgStr = """
                struct MixedData {
                    id:int;
                    tags:list<str> (sep=',');
                    metadata:text;
                }
                table t[id] {
                    id:int;
                    data:MixedData;
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);

        String csvStr = """
                ,,,
                id,data.id,data.tags,data.metadata
                1,100,"tag1,tag2,tag3","metadata1"
                2,200,"tag4,tag5","metadata2"
                """;
        Resources.addTempFileFromText("t.csv", tempDir, csvStr);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        VTable tVTable = cfgValue.getTable("t");

        assertEquals(2, tVTable.valueList().size());

        VStruct data1 = tVTable.valueList().get(0);
        VStruct mixedData1 = (VStruct) data1.values().get(1);
        assertEquals(100, ((VInt) mixedData1.values().getFirst()).value());

        VList tags1 = (VList) mixedData1.values().get(1);
        assertEquals(3, tags1.valueList().size());
        assertEquals("tag1", ((VString) tags1.valueList().get(0)).value());

        VText metadata1 = (VText) mixedData1.values().get(2);
        assertEquals("metadata1", metadata1.value());
    }

    @Test
    void should_handleEmptyAndNullValues_when_complexNestedStructuresPresent() {
        String cfgStr = """
                struct Address {
                    street:str (nullable);
                    city:str (nullable);
                    zip:str (nullable);
                }
                struct Contact {
                    phone:str (nullable);
                    email:str (nullable);
                    address:Address (nullable);
                }
                table user[id] {
                    id:int;
                    name:str;
                    contact:Contact (nullable);
                    tags:list<str> (pack, nullable);
                }
                """;
        Resources.addTempFileFromText("config.cfg", tempDir, cfgStr);

        String csvStr = """
                ,,,,,
                id,name,contact.phone,contact.email,contact.address.street,contact.address.city,tags
                1,Alice,123-456,a@test.com,Main St,City1,"tag1,tag2"
                2,Bob,,,,,
                3,Charlie,789-012,,Third St,,
                4,David,,,,,
                """;
        Resources.addTempFileFromText("user.csv", tempDir, csvStr);

        Context ctx = new Context(tempDir);
        CfgValue cfgValue = ctx.makeValue();
        VTable userTable = cfgValue.getTable("user");

        assertEquals(4, userTable.valueList().size());

        // 验证完整数据
        VStruct user1 = userTable.valueList().get(0);
        VStruct contact1 = (VStruct) user1.values().get(2);
        VStruct address1 = (VStruct) contact1.values().get(2);
        assertEquals("123-456", ((VString) contact1.values().getFirst()).value());
        assertEquals("Main St", ((VString) address1.values().getFirst()).value());

        // 验证空数据
        VStruct user2 = userTable.valueList().get(1);
        VStruct contact2 = (VStruct) user2.values().get(2);
        VStruct address2 = (VStruct) contact2.values().get(2);
        assertEquals("", ((VString) contact2.values().getFirst()).value());
        assertEquals("", ((VString) address2.values().getFirst()).value());

        // 验证部分空数据
        VStruct user3 = userTable.valueList().get(2);
        VStruct contact3 = (VStruct) user3.values().get(2);
        VStruct address3 = (VStruct) contact3.values().get(2);
        assertEquals("789-012", ((VString) contact3.values().getFirst()).value());
        assertEquals("", ((VString) contact3.values().get(1)).value());
        assertEquals("Third St", ((VString) address3.values().getFirst()).value());
    }

}