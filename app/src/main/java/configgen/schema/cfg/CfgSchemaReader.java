package configgen.schema.cfg;

import configgen.schema.CfgSchema;

import java.nio.file.Path;

public interface CfgSchemaReader {
    void readTo(CfgSchema destination, Path source, String pkgNameDot);
}
