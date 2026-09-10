package configgen.schema.cfg;

import configgen.schema.*;
import configgen.schema.FieldType.Primitive;
import configgen.schema.CommentData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static configgen.schema.FieldFormat.AutoOrPack.AUTO;
import static org.junit.jupiter.api.Assertions.*;

class CfgReaderTest {

    @Test
    void lexerErrorThrowsInsteadOfSilentSkip() {
        // lexer 默认的 ConsoleErrorListener 只打印 stderr 就跳过无法匹配的字符，
        // 之后语法可能仍然合法（@name 会被读成 name），坏 schema 被静默当成正确的读入
        String withIllegalChar = """
                struct Position {
                    @x:int;
                }
                """;
        assertThrows(CfgSyntaxException.class, () -> CfgReader.parse(withIllegalChar));

        String withDollar = """
                struct Po$sition {
                    x:int;
                }
                """;
        assertThrows(CfgSyntaxException.class, () -> CfgReader.parse(withDollar));
    }

    @Test
    void parseEnumTable() {
        String str = """
                table ability[id] (enum='name') {
                	id:int; // 属性类型
                	name:str; // 程序用名字
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        assertEquals(1, cfg.items().size());

        TableSchema table = (TableSchema) cfg.items().getFirst();
        assertEquals("ability", table.name());

        /*
            TableSchema[name=ability, primaryKey=KeySchema{name=[id]}, entry=EEnum{field='name'},
            isColumnMode=false, meta=Metadata[data={}], fields=[
                FieldSchema[name=id, type=INT, fmt=AUTO, meta=Metadata[data={__comment=MetaStr[value=属性类型]}]],
                FieldSchema[name=name, type=STR, fmt=AUTO, meta=Metadata[data={__comment=MetaStr[value=程序用名字]}]]],
            foreignKeys=[], uniqueKeys=[]]"
        */

        assertEquals(table.primaryKey().fields(), List.of("id"));
        assertTrue(table.entry() instanceof EntryType.EEnum e && e.field().equals("name"));
        assertFalse(table.isColumnMode());

        assertEquals(2, table.fields().size());
        {
            FieldSchema f1 = table.fields().getFirst();
            assertEquals("id", f1.name());
            assertEquals(Primitive.INT, f1.type());
            assertEquals(AUTO, f1.fmt());
            assertEquals(1, f1.meta().data().size());
            assertTrue(f1.meta().data().get("_comment") instanceof Metadata.MetaComment(CommentData cd) &&
                    cd.trailing().equals("属性类型"));
        }
        {
            FieldSchema f2 = table.fields().get(1);
            assertEquals("name", f2.name());
            assertEquals(Primitive.STRING, f2.type());
            assertEquals(AUTO, f2.fmt());
            assertEquals(1, f2.meta().data().size());
            assertTrue(f2.meta().data().get("_comment") instanceof Metadata.MetaComment(CommentData cd) &&
                    cd.trailing().equals("程序用名字"));
        }
    }

    @Test
    void parseStructWithRef() {
        String str = """
                struct AttrRandom {
                	Attr:int ->common.fightattrs; // 属性id
                	Min:int; // 最小值
                	Max:int; // 最大值
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        assertEquals(1, cfg.items().size());

        StructSchema struct = (StructSchema) cfg.items().getFirst();
        assertEquals("AttrRandom", struct.name());

        /*
        StructSchema[name=AttrRandom, fmt=AUTO, meta=Metadata[data={}], fields=[
            FieldSchema[name=Attr, type=INT, fmt=AUTO, meta=Metadata[data={__comment=MetaStr[value=属性id]}]],
            FieldSchema[name=Min, type=INT, fmt=AUTO, meta=Metadata[data={__comment=MetaStr[value=最小值]}]],
            FieldSchema[name=Max, type=INT, fmt=AUTO, meta=Metadata[data={__comment=MetaStr[value=最大值]}]]],
            foreignKeys=[ForeignKeySchema{name='Attr', key=KeySchema{name=[Attr]},
                refTable='common.fightattrs', refKey=RefPrimary[nullable=false], meta=Metadata[data={}]}]]
         */

        assertEquals(AUTO, struct.fmt());
        assertEquals(1, struct.foreignKeys().size());
        ForeignKeySchema fk = struct.foreignKeys().getFirst();
        assertEquals("Attr", fk.name());
        assertEquals(fk.key().fields(), List.of("Attr"));
        assertEquals("common.fightattrs", fk.refTable());
        assertTrue(fk.refKey() instanceof RefKey.RefPrimary(boolean nullable) && !nullable && fk.meta().data().isEmpty());
    }


    @Test
    void parseListType() {
        String str = """
                struct AddMonsterBuff {
                	monsterlist:list<int> (sep=';'); // 场内加buff的怪
                	bufflist:list<int> (sep=';'); // buff列表
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        assertEquals(1, cfg.items().size());

        StructSchema struct = (StructSchema) cfg.items().getFirst();
        assertEquals("AddMonsterBuff", struct.name());

        /*
        StructSchema[name=AddMonsterBuff, fmt=AUTO, meta=Metadata[data={}], fields=[
            FieldSchema[name=monsterlist, type=FList[item=INT], fmt=Sep[sep=;],
                meta=Metadata[data={__comment=MetaStr[value=场内加buff的怪]}]],
            FieldSchema[name=bufflist, type=FList[item=INT], fmt=Sep[sep=;],
                meta=Metadata[data={__comment=MetaStr[value=buff列表]}]]], foreignKeys=[]]
         */

        FieldSchema f1 = struct.fields().getFirst();
        assertTrue(f1.type() instanceof FieldType.FList(FieldType.SimpleType item) && item == Primitive.INT);
        assertTrue(f1.fmt() instanceof FieldFormat.Sep(char sep) && sep == ';');
    }

    @Test
    void parseInterface() {
        String str = """
                interface achievement.Achievementtype (enumRef='achievement.achievementtype') {
                	struct BiographyAchievement {
                		biographyid:int ->biography.biographyinfo;
                	}
                	struct TitleItemAchievement {
                		commonitemid:int ->item.commonitem;
                	}
                	struct MainMenuUnlockAchievement {
                		moduleopenid:int ->common.moduleopen;
                	}
                	struct VIPAchievement {
                		vipLevel:int ->vip.vip;
                	}
                	struct TitleIdAchievement {
                		titleid:int ->title.title;
                	}
                	struct EmptyAchievement {
                		_dummy:int;
                	}
                	struct RushRankAchievement {
                		rushrankid:int;
                	}
                	struct CommonAchievement {
                		_dummy:int;
                	}
                	struct CatteryAchievement {
                		typeid:int;
                	}
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        assertEquals(1, cfg.items().size());

        InterfaceSchema sInterface = (InterfaceSchema) cfg.items().getFirst();
        /*
        InterfaceSchema{name='achievement.Achievementtype', enumRef='achievement.achievementtype', defaultImpl='',
            fmt=AUTO, meta=Metadata[data={}], impls=[
            StructSchema[name=BiographyAchievement, fmt=AUTO, meta=Metadata[data={}],
                fields=[FieldSchema[name=biographyid, type=INT, fmt=AUTO, meta=Metadata[data={}]]],
                foreignKeys=[ForeignKeySchema{name='biographyid', key=KeySchema{name=[biographyid]},
                    refTable='biography.biographyinfo', refKey=RefPrimary[nullable=false], meta=Metadata[data={}]}]],
            ...
         */

        assertEquals("achievement.achievementtype", sInterface.enumRef());
        assertEquals("", sInterface.defaultImpl());
        assertEquals(AUTO, sInterface.fmt());
        assertEquals(9, sInterface.impls().size());
    }

    @Test
    void parseListRef() {
        String str = """
                table attrtype[Id] (enum='Ename') {
                	Id:int; // 属性ID
                	Ename:str; // 程序用名字
                	->EffectDamageTypes:[Id] =>buff.effectdamagetype[AttrType];
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        assertEquals(1, cfg.items().size());

        TableSchema table = (TableSchema) cfg.items().getFirst();

        /*
        TableSchema[name=attrtype, primaryKey=KeySchema{name=[Id]}, entry=EEnum{field='Ename'},
        isColumnMode=false, meta=Metadata[data={}], fields=[
            FieldSchema[name=Id, type=INT, fmt=AUTO, meta=Metadata[data={__comment=MetaStr[value=属性ID]}]],
            FieldSchema[name=Ename, type=STR, fmt=AUTO, meta=Metadata[data={__comment=MetaStr[value=程序用名字]}]]],
            foreignKeys=[
            ForeignKeySchema{name='EffectDamageTypes', key=KeySchema{name=[Id]},
                refTable='buff.effectdamagetype', refKey=RefList[key=KeySchema{name=[AttrType]}],
                meta=Metadata[data={}]}], uniqueKeys=[]]
         */
        ForeignKeySchema fk = table.foreignKeys().getFirst();
        assertTrue(fk.refKey() instanceof RefKey.RefList(KeySchema key) && key.fields().equals(List.of("AttrType")));
    }

    @Test
    void parseTableWithLeadingComment() {
        // 测试带有声明前注释的 table
        String str = """
                // 这是能力表
                // 用于定义游戏中的各种能力
                table ability[id] (enum='name') {
                	id:int; // 属性类型
                	name:str; // 程序用名字
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        assertEquals(1, cfg.items().size());

        TableSchema table = (TableSchema) cfg.items().getFirst();
        assertEquals("ability", table.name());

        // 验证声明前注释和行尾注释都被正确保存
        String comment = table.meta().data().get("_comment") instanceof Metadata.MetaComment(CommentData cd) ? cd.encode() : "";
        assertTrue(comment.contains("这是能力表"));
        assertTrue(comment.contains("用于定义游戏中的各种能力"));
    }

    @Test
    void parseStructWithLeadingComment() {
        // 测试带有声明前注释的 struct
        String str = """
                // 属性随机范围配置
                // 用于生成随机属性值
                struct AttrRandom {
                	Attr:int ->common.fightattrs; // 属性id
                	Min:int; // 最小值
                	Max:int; // 最大值
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        assertEquals(1, cfg.items().size());

        StructSchema struct = (StructSchema) cfg.items().getFirst();
        assertEquals("AttrRandom", struct.name());

        // 验证声明前注释
        String comment = struct.meta().data().get("_comment") instanceof Metadata.MetaComment(CommentData cd) ? cd.encode() : "";
        assertTrue(comment.contains("属性随机范围配置"));
        assertTrue(comment.contains("用于生成随机属性值"));
    }

    @Test
    void parseInterfaceWithLeadingComment() {
        // 测试带有声明前注释的 interface
        String str = """
                // 成就类型接口
                // 定义所有成就的基础结构
                interface achievement.AchievementType {
                	struct BiographyAchievement {
                		biographyid:int ->biography.biographyinfo;
                	}
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        assertEquals(1, cfg.items().size());

        InterfaceSchema sInterface = (InterfaceSchema) cfg.items().getFirst();
        assertEquals("achievement.AchievementType", sInterface.name());

        // 验证声明前注释
        String comment = sInterface.meta().data().get("_comment") instanceof Metadata.MetaComment(CommentData cd) ? cd.encode() : "";
        assertTrue(comment.contains("成就类型接口"));
        assertTrue(comment.contains("定义所有成就的基础结构"));
    }

    @Test
    void parseFieldWithLeadingComment() {
        // 测试带有声明前注释的字段
        String str = """
                struct AttrRandom {
                	// 属性ID
                	// 关联到fightprops表
                	Attr:int ->common.fightattrs;
                	Min:int; // 最小值
                	Max:int; // 最大值
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        assertEquals(1, cfg.items().size());

        StructSchema struct = (StructSchema) cfg.items().getFirst();
        FieldSchema field = struct.fields().getFirst();
        assertEquals("Attr", field.name());

        // 验证字段的声明前注释
        String comment = field.meta().data().get("_comment") instanceof Metadata.MetaComment(CommentData cd) ? cd.encode() : "";
        assertTrue(comment.contains("属性ID"));
        assertTrue(comment.contains("关联到fightprops表"));
    }

    @Test
    void parseMultiLineLeadingComment() {
        // 测试多行声明前注释
        String str = """
                // 第一行注释
                // 第二行注释
                // 第三行注释
                table ability[id] {
                	id:int;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        assertEquals(1, cfg.items().size());

        TableSchema table = (TableSchema) cfg.items().getFirst();
        String comment = table.meta().data().get("_comment") instanceof Metadata.MetaComment(CommentData cd) ? cd.encode() : "";
        assertTrue(comment.contains("第一行注释"));
        assertTrue(comment.contains("第二行注释"));
        assertTrue(comment.contains("第三行注释"));
    }

    @Test
    void parseOnlyLeadingComment() {
        // 测试只有声明前注释（没有行尾注释）
        String str = """
                // 只有声明前注释
                table ability[id] {
                	id:int;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        assertEquals(1, cfg.items().size());

        TableSchema table = (TableSchema) cfg.items().getFirst();
        String comment = table.meta().data().get("_comment") instanceof Metadata.MetaComment(CommentData cd) ? cd.encode() : "";
        assertTrue(comment.contains("只有声明前注释"));
    }

    @Test
    void parseOnlyTrailingComment() {
        // 测试只有行尾注释（没有声明前注释）
        String str = """
                table ability[id] { // 只有行尾注释
                	id:int;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        assertEquals(1, cfg.items().size());

        TableSchema table = (TableSchema) cfg.items().getFirst();
        String comment = table.meta().data().get("_comment") instanceof Metadata.MetaComment(CommentData cd) ? cd.encode() : "";
        assertTrue(comment.contains("只有行尾注释"));
        assertFalse(comment.contains(">>>"));
    }

    @Test
    void parseEnumDecl() {
        // 测试 schema 级别的 enum 声明
        String str = """
                enum ArgCaptureMode {
                    Snapshot; // 快照模式
                    Dynamic;  // 动态模式
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        assertEquals(1, cfg.items().size());

        TableSchema table = (TableSchema) cfg.items().getFirst();
        assertEquals("ArgCaptureMode", table.name());
        assertEquals(List.of("name"), table.primaryKey().fields());
        assertTrue(table.entry() instanceof EntryType.EEnum e && e.field().equals("name"));

        // 验证 enumValues
        Metadata.MetaEnumValues enumValues = table.meta().getEnumValues();
        assertNotNull(enumValues);
        assertTrue(enumValues instanceof Metadata.MetaEnumValues.OfEmpty(List<Metadata.EnumValueEmpty> values)
                && values.size() == 2);
        Metadata.MetaEnumValues.OfEmpty empty = (Metadata.MetaEnumValues.OfEmpty) enumValues;
        assertEquals("Snapshot", empty.values().get(0).name());
        assertEquals("快照模式", empty.values().get(0).comment());
        assertEquals("Dynamic", empty.values().get(1).name());
        assertEquals("动态模式", empty.values().get(1).comment());

        // 验证自动生成的字段：comment默认str（策划/开发向备注，不进多语言体系）
        assertEquals(2, table.fields().size());
        assertEquals("name", table.fields().get(0).name());
        assertEquals(Primitive.STRING, table.fields().get(0).type());
        assertEquals("comment", table.fields().get(1).name());
        assertEquals(Primitive.STRING, table.fields().get(1).type());
    }

    @Test
    void parseEnumDeclWithCommentText() {
        // 标记commentText时comment用text类型，且tag保留在meta里供CfgWriter还原
        String str = """
                enum ArgCaptureMode (commentText) {
                    Snapshot; // 快照模式
                    Dynamic;  // 动态模式
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        TableSchema table = (TableSchema) cfg.items().getFirst();
        assertEquals(Primitive.TEXT, table.fields().get(1).type());
        assertTrue(table.meta().isCommentText());
    }

    @Test
    void parseEnumDeclCommentTextWithValueRejected() {
        // commentText是tag，带值会静默不生效，必须报错
        String str = """
                enum ArgCaptureMode (commentText='y') {
                    Snapshot;
                }
                """;
        assertThrows(CfgSyntaxException.class, () -> CfgReader.parse(str));
    }

    @Test
    void reservedTagAtWrongPositionRejected() {
        // 保留tag放错位置直接报错，而不是静默忽略
        assertThrows(CfgSyntaxException.class, () -> CfgReader.parse("""
                struct s (json) {
                    x:int;
                }
                """));
        assertThrows(CfgSyntaxException.class, () -> CfgReader.parse("""
                table t[id] (lowercase) {
                    id:int;
                }
                """));
        assertThrows(CfgSyntaxException.class, () -> CfgReader.parse("""
                table t[id] (commentText) {
                    id:int;
                }
                """));
        assertThrows(CfgSyntaxException.class, () -> CfgReader.parse("""
                struct s (entry='x') {
                    x:int;
                }
                """));
        assertThrows(CfgSyntaxException.class, () -> CfgReader.parse("""
                enum e (sep=',') {
                    A;
                }
                """));
        assertThrows(CfgSyntaxException.class, () -> CfgReader.parse("""
                struct s (mustFill) {
                    x:int;
                }
                """));
        // 纯tag带了值，hasTag为false会静默不生效，必须报错
        assertThrows(CfgSyntaxException.class, () -> CfgReader.parse("""
                table t[id] (json='y') {
                    id:int;
                }
                """));
        // 必须带值的tag裸写会静默退回none/auto，必须报错
        assertThrows(CfgSyntaxException.class, () -> CfgReader.parse("""
                table t[id] (enum) {
                    id:int;
                    name:str;
                }
                """));
        assertThrows(CfgSyntaxException.class, () -> CfgReader.parse("""
                struct s {
                    x:list<int> (sep);
                }
                """));
    }

    @Test
    void reservedTagAtRightPositionAccepted() {
        // 正确位置的保留tag不受影响；-开头的用户filter排除tag不校验；
        // 普通字段上的(nullable)是现存被容忍的写法
        CfgSchema cfg = CfgReader.parse("""
                table t[id] (entry='name', columnMode, root) {
                    id:int;
                    name:str (lowercase, mustFill);
                    n:str (nullable);
                }
                struct s (sep=';') {
                    a:int;
                    b:int;
                }
                struct s2 (-json) {
                    x:int;
                }
                enum e (commentText) {
                    A;
                }
                """);
        assertEquals(4, cfg.items().size());
    }

    @Test
    void parseEnumDeclWithAssignedValues() {
        // 测试带赋值的 enum 声明
        String str = """
                enum StatClampMode {
                    None = 0;
                    Absolute = 1;
                    MaintainPercent = 2;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        assertEquals(1, cfg.items().size());

        TableSchema table = (TableSchema) cfg.items().getFirst();
        assertEquals("StatClampMode", table.name());
        assertEquals(List.of("name"), table.primaryKey().fields());
        assertTrue(table.entry() instanceof EntryType.EEnum e && e.field().equals("name"));

        // 验证 enumValues
        Metadata.MetaEnumValues enumValues = table.meta().getEnumValues();
        assertNotNull(enumValues);
        assertInstanceOf(Metadata.MetaEnumValues.OfAssigned.class, enumValues);
        Metadata.MetaEnumValues.OfAssigned assigned = (Metadata.MetaEnumValues.OfAssigned) enumValues;
        assertEquals(3, assigned.values().size());
        assertEquals("None", assigned.values().get(0).name());
        assertEquals(0, assigned.values().get(0).number());
        assertEquals("Absolute", assigned.values().get(1).name());
        assertEquals(1, assigned.values().get(1).number());
        assertEquals("MaintainPercent", assigned.values().get(2).name());
        assertEquals(2, assigned.values().get(2).number());

        // 验证自动生成的字段
        assertEquals(3, table.fields().size());
        assertEquals("name", table.fields().get(0).name());
        assertEquals(Primitive.STRING, table.fields().get(0).type());
        assertEquals("id", table.fields().get(1).name());
        assertEquals(Primitive.INT, table.fields().get(1).type());
        assertEquals("comment", table.fields().get(2).name());
        assertEquals(Primitive.STRING, table.fields().get(2).type());
    }

    @Test
    void parseEnumWithLeadingComment() {
        // 测试带有声明前注释的 enum
        String str = """
                // 参数捕获模式
                // 定义参数在触发时的捕获方式
                enum ArgCaptureMode {
                    Snapshot; // 快照模式
                    Dynamic;  // 动态模式
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        assertEquals(1, cfg.items().size());

        TableSchema table = (TableSchema) cfg.items().getFirst();
        String comment = table.meta().data().get("_comment") instanceof Metadata.MetaComment(CommentData cd) ? cd.encode() : "";
        assertTrue(comment.contains("参数捕获模式"));
        assertTrue(comment.contains("定义参数在触发时的捕获方式"));
    }

    @Test
    void parseStructWithSuffixComment() {
        // 测试结构体 } 前有注释
        String str = """
            struct Position {
                x:int;
                y:int;
                // 这是末尾注释
            }
            """;
        CfgSchema cfg = CfgReader.parse(str);
        assertEquals(1, cfg.items().size());

        StructSchema struct = (StructSchema) cfg.items().getFirst();
        String comment = struct.comment();
        // 使用 CommentUtils 解析注释
        CommentData parsed = CommentUtil.decode(comment);
        assertTrue(parsed.suffix().contains("这是末尾注释"));
    }

    @Test
    void parseFileEndComment() {
        // 测试文件末尾有注释
        String str = """
            struct Position {
                x:int;
            }
            // 这是文件末尾注释
            """;
        CfgSchema cfg = CfgReader.parse(str);
        // 文件末尾注释存储在 CfgSchema 中，key 是 ""（默认包名）
        String endComment = cfg.getFileEndComment("");
        assertTrue(endComment.contains("这是文件末尾注释"));
    }

    @Test
    void writeBackSuffixComment() {
        // 测试注释写回
        String str = """
            struct Position {
                x:int;
                // 末尾注释
            }
            // 文件末尾注释
            """;
        CfgSchema cfg = CfgReader.parse(str);
        String output = CfgWriter.stringify(cfg);

        // 验证注释被正确写回
        assertTrue(output.contains("// 末尾注释"));
        assertTrue(output.contains("// 文件末尾注释"));
    }

}