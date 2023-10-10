package configgen;

import configgen.data.CfgSchemaAlignToData;
import configgen.data.CfgData;
import configgen.data.CfgDataReader;
import configgen.schema.*;
import configgen.schema.cfg.Cfgs;
import configgen.value.CfgValue;
import configgen.value.CfgValueParser;
import configgen.value.ValueErrs;

import java.nio.file.Path;

public class Main {
    public static void main(String[] args) {
        int headRow = 2;
        for (int i = 0; i < args.length; ++i) {
            switch (args[i]) {
                case "-headrow":
                    headRow = Integer.parseInt(args[++i]);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + args[i]);
            }
        }

        Logger.setVerboseLevel(1);
        Logger.enableProfile();

//        xmlToCfg();
        Logger.profile("-----read schema - start");

        CfgSchema schema = Cfgs.readFrom(Path.of("config.cfg"), true);
        Logger.profile("-----read schema - raw");
        SchemaErrs errs = schema.resolve();
        errs.print();
        Stat stat = new SchemaStat(schema);
        stat.print();
        Logger.profile("-----read schema - resolve");


        CfgData data = CfgDataReader.INSTANCE.readCfgData(Path.of("."), schema, headRow);
        data.stat().print();
        if (Logger.verboseLevel() > 1) {
            data.print();
        }
        Logger.profile("-----read data");


        SchemaErrs alignErr = SchemaErrs.of();
        CfgSchema alignedSchema = new CfgSchemaAlignToData(schema, data, alignErr).align();
        System.out.println(schema.equals(alignedSchema));
        new CfgSchemaResolver(alignedSchema, alignErr).resolve();
//        schema.printDiff(alignedSchema);
        alignErr.print();
        Logger.profile("-----schema align to data");

        SchemaErrs clientErr = SchemaErrs.of();
        CfgSchema clientSchema = new CfgSchemaFilterByTag(alignedSchema, "client", clientErr).filter();
        new CfgSchemaResolver(clientSchema, clientErr).resolve();
        clientErr.print();
        Logger.profile("-----schema filter by client");


        ValueErrs clientValueErr = ValueErrs.of();
        CfgValueParser cfgValueParser = new CfgValueParser(clientSchema, data, alignedSchema, clientValueErr);
        CfgValue cfgValue = cfgValueParser.parseCfgValue();
        clientValueErr.print();
        Logger.profile("-----parse to cfg value");


    }

    public static void xmlToCfg() {
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
