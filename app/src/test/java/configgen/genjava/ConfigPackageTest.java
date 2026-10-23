package configgen.genjava;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ConfigPackageTest {
    @Test
    void collectsIndependentDependencyAndDirectoryErrorsBeforeRejectingThePackage() {
        var base = named("base", "");
        var one = named("one", "base,missing1");
        var two = named("two", "missing2");
        var order = assertThrows(ConfigErr.class, () -> ConfigPackage.validateOrder(List.of(base, one, two, base)));
        assertEquals(List.of(new LoadValueErrs.MissingDependency("one", "missing1"),
                new LoadValueErrs.MissingDependency("two", "missing2"), new LoadValueErrs.DuplicatePackage("base")), order.getErrors());
        var duplicate = assertThrows(ConfigErr.class, () -> ConfigPackage.read(new ConfigInput(bytes(out -> {
            out.writeInt(1); out.writeInt(2);
            out.writeString("pack"); out.writeString("base");
            out.writeString("pack"); out.writeString("other");
            out.writeInt(2);
            block(out, "strings", 0); block(out, "strings", 0);
        }))));
        assertEquals(2, duplicate.getErrors().size());
        assertTrue(duplicate.getErrors().stream().allMatch(LoadValueErrs.DuplicateKey.class::isInstance));
        var truncated = assertThrows(ConfigErr.class, () -> new ConfigInput(new byte[0]).readInt());
        assertInstanceOf(LoadValueErrs.OutOfBounds.class, truncated.getErrors().getFirst());
        assertEquals(1, truncated.getErrors().size(), "A separate input has no previous errors");
    }

    @Test
    void readsMetadataAndIndependentBoundedBlocks() {
        var pkg = ConfigPackage.read(new ConfigInput(bytes(out -> {
            out.writeInt(1);
            out.writeInt(2);
            out.writeString("pack"); out.writeString("沙漠地图");
            out.writeString("requires"); out.writeString("base, balance");
            out.writeInt(4);
            block(out, ".schema", 17);
            block(out, "schema", 23);
            block(out, "strings", 0);
            out.writeString("text/default"); out.writeInt(8); out.writeInt(0); out.writeInt(0);
        })));
        assertEquals("沙漠地图", pkg.name());
        assertEquals(List.of("base", "balance"), pkg.dependencies());
        assertThrows(UnsupportedOperationException.class, () -> pkg.dependencies().add("late"));
        assertEquals(17, pkg.block(".schema").readInt());
        assertEquals(23, pkg.block("schema").readInt());
        var table = pkg.block(".schema");
        assertEquals(17, table.readInt());
        table.requireEnd();
        assertThrows(ConfigErr.class, table::readInt);
        assertThrows(UnsupportedOperationException.class, () -> pkg.metadata().put("pack", "other"));
    }

    @Test
    void rejectsVersionBeforeAttemptingMetadata() {
        for (int version : new int[]{0, 2, 512}) {
            var err = assertThrows(ConfigErr.class,
                    () -> ConfigPackage.read(new ConfigInput(bytes(out -> out.writeInt(version)))));
            assertTrue(err.getMessage().contains(Integer.toString(version)));
            assertTrue(err.getMessage().contains(Integer.toString(ConfigPackage.FORMAT_VERSION)));
        }
    }

    @Test
    void rejectsDuplicateMetadataAndBlocks() {
        assertThrows(ConfigErr.class, () -> ConfigPackage.read(new ConfigInput(bytes(out -> {
            out.writeInt(1); out.writeInt(2);
            out.writeString("pack"); out.writeString("base");
            out.writeString("pack"); out.writeString("other");
            out.writeInt(0);
        }))));
        assertThrows(ConfigErr.class, () -> ConfigPackage.read(new ConfigInput(bytes(out -> {
            out.writeInt(1); out.writeInt(0); out.writeInt(2);
            block(out, "strings", 0); block(out, "strings", 0);
        }))));
    }

    @Test
    void rejectsTruncationAndUndeclaredTrailingBytes() {
        assertThrows(ConfigErr.class, () -> ConfigPackage.read(new ConfigInput(bytes(out -> {
            out.writeInt(1); out.writeInt(0); out.writeInt(1);
            out.writeString(".user"); out.writeInt(100); out.writeInt(0);
        }))));
        assertThrows(ConfigErr.class, () -> ConfigPackage.read(new ConfigInput(bytes(out -> {
            out.writeInt(1); out.writeInt(0); out.writeInt(0); out.writeInt(0);
        }))));
        assertThrows(ConfigErr.class, () -> new ConfigInput(new byte[0]).skipBytes(-1));
    }

    @Test
    void validatesDependenciesInCallerOrder() {
        var base = named("base", "");
        var balance = named("balance", "base");
        var map = named("map", "base,balance");
        assertDoesNotThrow(() -> ConfigPackage.validateOrder(List.of(base, balance, map)));
        assertThrows(ConfigErr.class, () -> ConfigPackage.validateOrder(List.of(base, map, balance)));
        assertThrows(ConfigErr.class, () -> ConfigPackage.validateOrder(List.of(base, base)));
        assertThrows(ConfigErr.class, () -> named("map", "base,,balance").dependencies());
        assertThrows(ConfigErr.class, () -> named("map", "map"));
        assertThrows(ConfigErr.class, () -> named("map", "base, base"));
        assertThrows(ConfigErr.class, () -> named(" map", ""));
    }

    @Test
    void rejectsMalformedSchemaCountsAndDuplicateFixedSymbols() {
        assertThrows(ConfigErr.class, () -> SchemaDeserializer.deserialize(new ConfigInput(bytes(out -> {
            out.writeInt(Schema.ENUM); out.writeBool(false); out.writeBool(true); out.writeInt(2);
            out.writeString("Red"); out.writeInt(1); out.writeString("Red"); out.writeInt(1);
        }))));
        assertThrows(ConfigErr.class, () -> SchemaDeserializer.deserialize(new ConfigInput(bytes(out -> {
            out.writeInt(Schema.INTERFACE); out.writeInt(-1);
        }))));
    }

    @Test
    void validatesSchemaInBothDirectionsAndCollectsIndependentMismatches() {
        byte[] empty = bytes(out -> { out.writeInt(Schema.INTERFACE); out.writeInt(0); });
        byte[] extra = bytes(out -> {
            out.writeInt(Schema.INTERFACE); out.writeInt(1);
            out.writeString("unit"); out.writeInt(Schema.INT);
        });
        var expected = new SchemaInterface();
        var actual = new SchemaInterface();
        actual.addImp("unit", SchemaPrimitive.SInt);
        assertTrue(expected.compatible(actual), "The existing compatible() alone permits extra structures");
        var errors = new LoadValueErrs();
        var mod = schemaPackage("map", extra);
        assertFalse(mod.validateSchema(expected, errors));
        assertTrue(mod.validateSchema(actual, errors));
        assertFalse(schemaPackage("base", empty).validateSchema(actual, errors));
        assertEquals(List.of("map", "base"), errors.getErrors().stream()
                .map(error -> assertInstanceOf(LoadValueErrs.SchemaMismatch.class, error).pack()).toList());
        assertThrows(ConfigErr.class, errors::checkErrors);
    }

    private static ConfigPackage schemaPackage(String name, byte[] schema) {
        return ConfigPackage.read(new ConfigInput(bytes(out -> {
            out.writeInt(1); out.writeInt(1); out.writeString("pack"); out.writeString(name);
            out.writeInt(1); out.writeString("schema"); out.writeInt(schema.length); out.writeRawBytes(schema);
        })));
    }

    @Test
    void rejectsIntegerWidthChangesInBothDirections() {
        for (var expectedType : List.of(SchemaPrimitive.SInt, SchemaPrimitive.SLong)) {
            var actualType = expectedType == SchemaPrimitive.SInt ? SchemaPrimitive.SLong : SchemaPrimitive.SInt;
            var expected = new SchemaInterface();
            var expectedTable = new SchemaBean(true);
            expectedTable.addColumn("value", expectedType);
            expected.addImp("unit", expectedTable);
            var actual = new SchemaInterface();
            var actualTable = new SchemaBean(true);
            actualTable.addColumn("value", actualType);
            actual.addImp("unit", actualTable);

            var pack = schemaPackage("base", bytes(out -> new SchemaSerializer(out).serializeInterface(actual)));
            var errors = new LoadValueErrs();
            assertFalse(pack.validateSchema(expected, errors));
            assertEquals(1, errors.getErrors().size());
            assertInstanceOf(LoadValueErrs.SchemaMismatch.class, errors.getErrors().getFirst());
            assertThrows(ConfigErr.class, errors::checkErrors);
        }
    }

    private ConfigPackage named(String name, String requires) {
        return ConfigPackage.read(new ConfigInput(bytes(out -> {
            out.writeInt(1); out.writeInt(2);
            out.writeString("pack"); out.writeString(name);
            out.writeString("requires"); out.writeString(requires);
            out.writeInt(0);
        })));
    }

    private static void block(ConfigOutput out, String name, int value) {
        out.writeString(name); out.writeInt(4); out.writeInt(value);
    }

    private static byte[] bytes(java.util.function.Consumer<ConfigOutput> writer) {
        var stream = new ByteArrayOutputStream();
        try (var out = new ConfigOutput(stream)) { writer.accept(out); }
        return stream.toByteArray();
    }
}
