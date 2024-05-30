package configgen.schema.cfg;

import configgen.schema.*;
import configgen.schema.FieldType.Primitive;
import org.junit.jupiter.api.Test;

import java.util.List;

import static configgen.schema.FieldFormat.AutoOrPack.AUTO;
import static org.junit.jupiter.api.Assertions.*;

class CfgReaderTest {

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
        assertEquals(table.name(), "ability");
//        System.out.println(table);
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
        assertTrue(table.meta().data().isEmpty());

        assertEquals(table.fields().size(), 2);
        {
            FieldSchema f1 = table.fields().getFirst();
            assertEquals(f1.name(), "id");
            assertEquals(f1.type(), Primitive.INT);
            assertEquals(f1.fmt(), AUTO);
            assertEquals(f1.meta().data().size(), 1);
            assertTrue(f1.meta().data().get("_comment") instanceof Metadata.MetaStr ms &&
                    ms.value().equals("属性类型"));
        }
        {
            FieldSchema f2 = table.fields().get(1);
            assertEquals(f2.name(), "name");
            assertEquals(f2.type(), Primitive.STRING);
            assertEquals(f2.fmt(), AUTO);
            assertEquals(f2.meta().data().size(), 1);
            assertTrue(f2.meta().data().get("_comment") instanceof Metadata.MetaStr ms &&
                    ms.value().equals("程序用名字"));
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

        StructSchema struct = (StructSchema) cfg.items().get(0);
        assertEquals(struct.name(), "AttrRandom");

//        System.out.println(table);
        /*
        StructSchema[name=AttrRandom, fmt=AUTO, meta=Metadata[data={}], fields=[
            FieldSchema[name=Attr, type=INT, fmt=AUTO, meta=Metadata[data={__comment=MetaStr[value=属性id]}]],
            FieldSchema[name=Min, type=INT, fmt=AUTO, meta=Metadata[data={__comment=MetaStr[value=最小值]}]],
            FieldSchema[name=Max, type=INT, fmt=AUTO, meta=Metadata[data={__comment=MetaStr[value=最大值]}]]],
            foreignKeys=[ForeignKeySchema{name='Attr', key=KeySchema{name=[Attr]},
                refTable='common.fightattrs', refKey=RefPrimary[nullable=false], meta=Metadata[data={}]}]]
         */

        assertEquals(struct.fmt(), AUTO);
        assertEquals(struct.foreignKeys().size(), 1);
        ForeignKeySchema fk = struct.foreignKeys().get(0);
        assertEquals(fk.name(), "Attr");
        assertEquals(fk.key().fields(), List.of("Attr"));
        assertEquals(fk.refTable(), "common.fightattrs");
        assertTrue(fk.refKey() instanceof RefKey.RefPrimary p && !p.nullable() && fk.meta().data().isEmpty());
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
        assertEquals(struct.name(), "AddMonsterBuff");
//        System.out.println(struct);
        /*
        StructSchema[name=AddMonsterBuff, fmt=AUTO, meta=Metadata[data={}], fields=[
            FieldSchema[name=monsterlist, type=FList[item=INT], fmt=Sep[sep=;],
                meta=Metadata[data={__comment=MetaStr[value=场内加buff的怪]}]],
            FieldSchema[name=bufflist, type=FList[item=INT], fmt=Sep[sep=;],
                meta=Metadata[data={__comment=MetaStr[value=buff列表]}]]], foreignKeys=[]]
         */

        FieldSchema f1 = struct.fields().getFirst();
        assertTrue(f1.type() instanceof FieldType.FList flist && flist.item() == Primitive.INT);
        assertTrue(f1.fmt() instanceof FieldFormat.Sep sep && sep.sep() == ';');
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
//        System.out.println(sInterface);
        /*
        InterfaceSchema{name='achievement.Achievementtype', enumRef='achievement.achievementtype', defaultImpl='',
            fmt=AUTO, meta=Metadata[data={}], impls=[
            StructSchema[name=BiographyAchievement, fmt=AUTO, meta=Metadata[data={}],
                fields=[FieldSchema[name=biographyid, type=INT, fmt=AUTO, meta=Metadata[data={}]]],
                foreignKeys=[ForeignKeySchema{name='biographyid', key=KeySchema{name=[biographyid]},
                    refTable='biography.biographyinfo', refKey=RefPrimary[nullable=false], meta=Metadata[data={}]}]],
            ...
         */

        assertEquals(sInterface.enumRef(), "achievement.achievementtype");
        assertEquals(sInterface.defaultImpl(), "");
        assertEquals(sInterface.fmt(), AUTO);
        assertEquals(sInterface.impls().size(), 9);
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
//        System.out.print(table);
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
        assertTrue(fk.refKey() instanceof RefKey.RefList rl && rl.key().fields().equals(List.of("AttrType")));
    }
}