package configgen.schema;

import configgen.schema.CfgSchemaErrs.*;
import configgen.schema.cfg.CfgReader;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static configgen.schema.FieldFormat.AutoOrPack.AUTO;
import static org.junit.jupiter.api.Assertions.*;

class CfgSchemaResolverTest {

    @Test
    public void warn_NameMayConflictByRef() {
        String str = """
                struct attr {
                	Attr:int; // 属性id
                	Min:int; // 最小值
                	Max:int; // 最大值
                }
                table t1 [id]{
                	id:int;
                	attr:attr;
                }
                struct module1.attr {
                	attr:int;
                	val:int;
                }
                table module1.item[id]{
                	id:int;
                	attr:attr;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();
        assertEquals(1, errs.warns().size());
        assertEquals(0, errs.errs().size());
        Warn warn = errs.warns().getFirst();
        assertInstanceOf(NameMayConflictByRef.class, warn);
        NameMayConflictByRef w = (NameMayConflictByRef) warn;
        assertEquals(Set.of("attr", "module1.attr"), Set.of(w.name1(), w.name2()));
    }

    @Test
    public void warn_StructNotUsed() {
        String str = """
                struct attr {
                	Attr:int;
                	Min:int;
                	Max:int;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();
        assertEquals(1, errs.warns().size());
        assertEquals(0, errs.errs().size());
        Warn warn = errs.warns().getFirst();
        assertInstanceOf(StructNotUsed.class, warn);
        assertEquals("attr", ((StructNotUsed) warn).name());
    }

    @Test
    public void warn_InterfaceNotUsed() {
        String str = """
                interface interface1 {
                    struct impl1 {
                    }
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();
        assertEquals(1, errs.warns().size());
        assertEquals(0, errs.errs().size());
        assertInstanceOf(InterfaceNotUsed.class, errs.warns().getFirst());
    }


    @Test
    public void TableNameNotLowerCase() {
        String str = """
                table Tab1[id] {
                    id:int;
                    v:int;
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();
        assertEquals(0, errs.warns().size());
        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(TableNameNotLowerCase.class, err);
        assertEquals("Tab1", ((TableNameNotLowerCase) err).tableName());
    }

    @Test
    public void ImplNamespaceNotEmpty() {
        String str = """
                interface action {
                    struct ns.impl1{
                    }
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();
        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(ImplNamespaceNotEmpty.class, err);
        ImplNamespaceNotEmpty e = (ImplNamespaceNotEmpty) err;
        assertEquals("action", e.sInterface());
        assertEquals("ns.impl1", e.errImplName());
    }

    @Test
    public void NameConflict() {
        String str = """
                struct action {
                    a:int;
                }
                table action[id]{
                    id:int;
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();
        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(NameConflict.class, err);
        NameConflict e = (NameConflict) err;
        assertEquals("action", e.name());
    }

    @Test
    public void InnerNameConflict() {
        String str = """
                table action[id]{
                    id:int;
                    id:str;
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();
        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(InnerNameConflict.class, err);
        InnerNameConflict e = (InnerNameConflict) err;
        assertEquals("action", e.item());
        assertEquals("id", e.name());
    }

    @Test
    public void TypeStructNotFound() {
        String str = """
                table action[id]{
                    id:int;
                    v:unknownType;
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();
        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(TypeStructNotFound.class, err);
        TypeStructNotFound e = (TypeStructNotFound) err;
        assertEquals("action", e.struct());
        assertEquals("v", e.field());
        assertEquals("unknownType", e.notFoundStruct());
    }

    @Test
    public void primitiveFieldNotAuto() {
        String str = """
                table action[id]{
                    id:int;
                    v:int (pack);
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();
        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(PrimitiveFieldFmtMustBeAuto.class, err);
        PrimitiveFieldFmtMustBeAuto e = (PrimitiveFieldFmtMustBeAuto) err;
        assertEquals("action", e.struct());
        assertEquals("v", e.field());
        assertEquals("int", e.type());
        assertEquals("pack", e.errFmt());
    }

    @Test
    public void structRefFieldNotAutoOrPack() {
        String str = """
                struct s {
                    a:int;
                }
                table action[id]{
                    id:int;
                    v:s (sep=',');
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();
        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(StructFieldFmtMustBeAutoOrPack.class, err);
        StructFieldFmtMustBeAutoOrPack e = (StructFieldFmtMustBeAutoOrPack) err;
        assertEquals("action", e.struct());
        assertEquals("v", e.field());
        assertEquals("s", e.type());
        assertEquals("sep=','", e.errFmt());
    }

    @Test
    public void listFieldAuto() {
        String str = """
                table action[id]{
                    id:int;
                    v:list<int>;
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();
        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(ListFieldFmtMustBePackOrSepOrFixOrBlock.class, err);
        ListFieldFmtMustBePackOrSepOrFixOrBlock e = (ListFieldFmtMustBePackOrSepOrFixOrBlock) err;
        assertEquals("action", e.struct());
        assertEquals("v", e.field());
        assertEquals("list<int>", e.type());
        assertEquals("", e.errFmt());
    }

    @Test
    public void mapFieldAutoOrSep() {
        String str = """
                table action[id]{
                    id:int;
                    v:map<int,int>;
                    v2:map<int,int> (sep=',');
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();
        assertEquals(2, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(MapFieldFmtMustBePackOrFixOrBlock.class, err);
        MapFieldFmtMustBePackOrFixOrBlock e = (MapFieldFmtMustBePackOrFixOrBlock) err;
        assertEquals("action", e.struct());
        assertEquals("v", e.field());
        assertEquals("map<int,int>", e.type());
        assertEquals("", e.errFmt());

        assertInstanceOf(MapFieldFmtMustBePackOrFixOrBlock.class, errs.errs().get(1));
    }

    @Test
    public void ImplFmtNotSupport() {
        String str = """
                interface action{
                    struct impl1 (pack){
                        a:int;
                    }
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();
        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(ImplFmtNotSupport.class, err);
        ImplFmtNotSupport e = (ImplFmtNotSupport) err;
        assertEquals("action", e.inInterface());
        assertEquals("impl1", e.impl());
        assertEquals("pack", e.errFmt());
    }

    @Test
    public void SepFmtStructHasUnPrimitiveField() {
        String str = """
                struct s1 (sep=','){
                    a:int;
                    b:str;
                }
                struct s2 (sep=','){
                    a:int;
                    b:list<int>;
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();
        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(SepFmtStructHasUnPrimitiveField.class, err);
        SepFmtStructHasUnPrimitiveField e = (SepFmtStructHasUnPrimitiveField) err;
        assertEquals("s2", e.struct());
    }

    @Test
    public void ListStructSepEqual() {
        String str = """
                struct s1 (sep=','){
                    a:int;
                    b:str;
                }
                table t1[id] {
                    id:int;
                    b:list<s1>(sep='#');
                }
                table t2[id] {
                    id:int;
                    b:list<s1>(sep=',');
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();
        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(ListStructSepEqual.class, err);
        ListStructSepEqual e = (ListStructSepEqual) err;
        assertEquals("t2", e.structural());
        assertEquals("b", e.field());
    }

    @Test
    public void EnumRefNotFound() {
        String str = """
                interface action(enumRef='notExistTable') {
                    struct impl1{
                    }
                }
                """;

        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();
        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(EnumRefNotFound.class, err);
        EnumRefNotFound e = (EnumRefNotFound) err;
        assertEquals("action", e.sInterface());
        assertEquals("notExistTable", e.enumRef());
    }

    @Test
    public void DefaultImplNotFound() {
        String str = """
                interface action(defaultImpl='notExistImpl') {
                    struct impl1{
                    }
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(DefaultImplNotFound.class, err);
        DefaultImplNotFound e = (DefaultImplNotFound) err;
        assertEquals("action", e.sInterface());
        assertEquals("notExistImpl", e.defaultImpl());
    }

    @Test
    public void InterfaceImplEmpty() {
        InterfaceSchema action = new InterfaceSchema("action", "", "", AUTO,
                Metadata.of(), List.of());
        CfgSchema cfg = CfgSchema.of();
        cfg.add(action);
        CfgSchemaErrs errs = cfg.resolve();

        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(InterfaceImplEmpty.class, err);
        InterfaceImplEmpty e = (InterfaceImplEmpty) err;
        assertEquals("action", e.sInterface());
    }

    @Test
    public void EntryNotFound() {
        String str = """
                table t1[id](entry='notExistEntry') {
                    id:int;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(EntryNotFound.class, err);
        EntryNotFound e = (EntryNotFound) err;
        assertEquals("t1", e.table());
        assertEquals("notExistEntry", e.entry());
    }

    @Test
    public void EntryNotFound_enum() {
        String str = """
                table t1[id](enum='notExistEntry') {
                    id:int;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(EntryNotFound.class, err);
        EntryNotFound e = (EntryNotFound) err;
        assertEquals("t1", e.table());
        assertEquals("notExistEntry", e.entry());
    }


    @Test
    public void EntryFieldTypeNotStr() {
        String str = """
                table t1[id](enum='enumName') {
                    id:int;
                    enumName:text;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(EntryFieldTypeNotStr.class, err);
        EntryFieldTypeNotStr e = (EntryFieldTypeNotStr) err;
        assertEquals("t1", e.table());
        assertEquals("enumName", e.entry());
    }

    @Test
    public void BlockTableFirstFieldNotInPrimaryKey() {
        String str = """
                table t1[id] {
                    note:str;
                    id:int;
                    blockIds:list<int> (block=1);
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(BlockTableFirstFieldNotInPrimaryKey.class, err);
        BlockTableFirstFieldNotInPrimaryKey e = (BlockTableFirstFieldNotInPrimaryKey) err;
        assertEquals("t1", e.table());
    }

    @Test
    public void KeyNotFound_primaryKey() {
        String str = """
                table t1[id] {
                    note:str;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(KeyNotFound.class, err);
        KeyNotFound e = (KeyNotFound) err;
        assertEquals("t1", e.structural());
        assertEquals("id", e.key());
    }

    @Test
    public void KeyNotFound_uniqKey() {
        String str = """
                table t1[id] {
                    [uk];
                    id:int;
                    note:str;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(KeyNotFound.class, err);
        KeyNotFound e = (KeyNotFound) err;
        assertEquals("t1", e.structural());
        assertEquals("uk", e.key());
    }

    @Test
    public void KeyNotFound_localKey() {
        String str = """
                table t1[id] {
                    id:int;
                    note:str;
                    ->refT2:[id2] ->t2;
                }
                table t2[id] {
                    id:int;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(KeyNotFound.class, err);
        KeyNotFound e = (KeyNotFound) err;
        assertEquals("t1", e.structural());
        assertEquals("id2", e.key());
    }

    @Test
    public void KeyTypeNotSupport_float() {
        String str = """
                table t1[id] {
                    id:float;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(KeyTypeNotSupport.class, err);
        KeyTypeNotSupport e = (KeyTypeNotSupport) err;
        assertEquals("t1", e.structural());
        assertEquals("id", e.field());
        assertEquals("FLOAT", e.errType());

    }

    @Test
    public void KeyTypeNotSupport_list() {
        String str = """
                table t1[id] {
                    id:list<int>;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(KeyTypeNotSupport.class, err);
        KeyTypeNotSupport e = (KeyTypeNotSupport) err;
        assertEquals("t1", e.structural());
        assertEquals("id", e.field());
    }

    @Test
    public void KeyTypeNotSupport_text() {
        String str = """
                table t1[id] {
                    id:text;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(KeyTypeNotSupport.class, err);
        KeyTypeNotSupport e = (KeyTypeNotSupport) err;
        assertEquals("t1", e.structural());
        assertEquals("id", e.field());
    }

    @Test
    public void RefTableNotFound() {
        String str = """
                table t1[id] {
                    id:int ->t2;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(RefTableNotFound.class, err);
        RefTableNotFound e = (RefTableNotFound) err;
        assertEquals("t1", e.table());
        assertEquals("id", e.foreignKey());
        assertEquals("t2", e.errRefTable());

    }


    @Test
    public void RefTableKeyNotUniq() {
        String str = """
                table t1[id] {
                    id:int ->t2[key];
                }
                table t2[id] {
                    id:int;
                    key:int;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(RefTableKeyNotUniq.class, err);
        RefTableKeyNotUniq e = (RefTableKeyNotUniq) err;
        assertEquals("t1", e.table());
        assertEquals("id", e.foreignKey());
        assertEquals("t2", e.refTable());
        assertEquals("key", e.notUniqRefKey().getFirst());
    }


    @Test
    public void ok_RefMultiKeySupport() {
        String str = """
                table t1[id] {
                    id:int;
                    key1:int;
                    key2:int;
                    ->refListKeys:[key1,key2] ->t2[key1, key2];
                }
                table t2[id] {
                    [key1, key2];
                    id:int;
                    key1:int;
                    key2:int;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        assertEquals(0, errs.warns().size());
        assertEquals(0, errs.errs().size());
    }

    @Test
    public void ok_ListRefSingleKeySupport() {
        String str = """
                table t1[id] {
                    id:int;
                    key1:int => t2[key1];
                }
                table t2[id] {
                    id:int;
                    key1:int;
                    key2:int;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        assertEquals(0, errs.warns().size());
        assertEquals(0, errs.errs().size());
    }

    @Test
    public void ListRefMultiKeyNotSupport() {
        String str = """
                table t1[id] {
                    id:int;
                    key1:int;
                    key2:int;
                    ->refListKeys:[key1,key2] =>t2[key1, key2];
                }
                table t2[id] {
                    id:int;
                    key1:int;
                    key2:int;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(ListRefMultiKeyNotSupport.class, err);
        ListRefMultiKeyNotSupport e = (ListRefMultiKeyNotSupport) err;
        assertEquals("t1", e.table());
        assertEquals("refListKeys", e.foreignKey());
        assertEquals(List.of("key1", "key2"), e.errMultiKey());
    }

    @Test
    public void RefLocalKeyRemoteKeyCountNotMatch() {
        String str = """
                table t1[id] {
                    id:int ->t2;
                    key1:int;
                }
                table t2[key1,key2] {
                    id:int;
                    key1:int;
                    key2:int;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(RefLocalKeyRemoteKeyCountNotMatch.class, err);
        RefLocalKeyRemoteKeyCountNotMatch e = (RefLocalKeyRemoteKeyCountNotMatch) err;
        assertEquals("t1", e.table());
    }

    @Test
    public void RefLocalKeyRemoteKeyTypeNotMatch() {
        String str = """
                table t1[id] {
                    id:int ->t2;
                }
                table t2[id] {
                    id:str;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(RefLocalKeyRemoteKeyTypeNotMatch.class, err);
        RefLocalKeyRemoteKeyTypeNotMatch e = (RefLocalKeyRemoteKeyTypeNotMatch) err;
        assertEquals("t1", e.structural());
        assertEquals("id", e.foreignKey());
        assertEquals("INT", e.localType());
        assertEquals("STRING", e.refType());
    }

    @Test
    public void RefContainerNullable() {
        String str = """
                table t1[id] {
                    id:int;
                    listData:list<int> ->t2 (nullable);
                }
                table t2[id] {
                    id:int;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(RefContainerNullable.class, err);
        RefContainerNullable e = (RefContainerNullable) err;
        assertEquals("t1", e.structural());
        assertEquals("listData", e.foreignKey());
    }


    @Test
    public void MappingToExcelLoop() {
        String str = """
                struct s1 {
                    v:int;
                    child:s1;
                }
                table t1[id] {
                    id:int;
                    s:s1;
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();

        assertEquals(1, errs.errs().size());
        Err err = errs.errs().getFirst();
        assertInstanceOf(MappingToExcelLoop.class, err);
        MappingToExcelLoop e = (MappingToExcelLoop) err;
        assertEquals(1, e.structNameLoop().size());
        assertEquals("s1", e.structNameLoop().getFirst());
    }


    @Test
    public void ok_UsePackToSolve_MappingToExcelLoopErr() {
        String str = """
                struct s1 {
                    v:int;
                    child:s1 ;
                }
                table t1[id] {
                    id:int;
                    s:s1 (pack);
                }
                """;
        CfgSchema cfg = CfgReader.parse(str);
        CfgSchemaErrs errs = cfg.resolve();
        assertEquals(0, errs.warns().size());
        assertEquals(0, errs.errs().size());
    }

}