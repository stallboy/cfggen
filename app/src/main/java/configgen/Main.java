package configgen;

import configgen.data.CfgSchemaAlignToData;
import configgen.data.CfgData;
import configgen.data.CfgDataReader;
import configgen.schema.*;
import configgen.schema.cfg.Cfgs;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        Logger.enableVerbose();
//        Logger.enableMmGc();
        System.out.println("-----xml to cfg-----");
        xmlToCfg();

        System.out.println("-----read schema-----");
        CfgSchema schema = Cfgs.readFrom(Path.of("config.cfg"), true);
        SchemaErrs errs = schema.resolve();
        errs.print();
        Stat stat = new SchemaStat(schema);
        stat.print();

        System.out.println();
        System.out.println("-----read data-----");
        CfgData data = CfgDataReader.INSTANCE.readCfgData(Path.of("."), schema, 2);
        data.stat().print();
        data.print();

        System.out.println();
        System.out.println("-----align to data-----");
        CfgSchema alignedSchema = CfgSchemaAlignToData.INSTANCE.align(schema, data);
        System.out.println(schema.equals(alignedSchema));
//        schema.printDiff(alignedSchema);
        SchemaErrs alignErr = alignedSchema.resolve();
        alignErr.print();

        System.out.println();
        System.out.println("-----filtered by client-----");
        SchemaErrs clientErr = SchemaErrs.of();
        CfgSchema clientSchema = new CfgSchemaFilterByTag(alignedSchema, "client", clientErr).filter();
        new CfgSchemaResolver(clientSchema, clientErr).resolve();
        clientErr.print();
    }

    public static void xmlToCfg() {
        Logger.enableVerbose();
        CfgSchema cfg = Cfgs.readFromXml(Path.of("config.xml"), true);
        Path root = Path.of("config.cfg");
        System.out.println("-----write");
        Cfgs.writeTo(root, true, cfg);
        CfgSchema cfg2 = Cfgs.readFrom(root, true);
        System.out.println("-----rewrite");
        Cfgs.writeTo(root, true, cfg2);
        CfgSchema cfg3 = Cfgs.readFrom(root, true);

        System.out.println(cfg2.items().size());
        if (!cfg2.equals(cfg3)) {
            throw new IllegalStateException("should equal");
        }

        SchemaErrs fullErr = cfg2.resolve();
        fullErr.print();
    }
}
