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

        Logger.setVerboseLevel(0);
        Logger.enableProfile();

        Logger.profile("profiler start");
//        xmlToCfg();
//        Logger.profile("xml to cfg");


        Path root = Path.of("config.cfg");
        CfgSchema schema = Cfgs.readFrom(root, true);
        Logger.profile("schema read");
        SchemaErrs errs = schema.resolve();
        errs.print();
        Stat stat = new SchemaStat(schema);
        stat.print();
        Logger.profile("schema resolve");


        CfgData data = CfgDataReader.INSTANCE.readCfgData(Path.of("."), schema, headRow, "GBK");
        data.stat().print();
        if (Logger.verboseLevel() > 1) {
            data.print();
        }


        SchemaErrs alignErr = SchemaErrs.of();
        CfgSchema alignedSchema = new CfgSchemaAlignToData(schema, data, alignErr).align();
        new CfgSchemaResolver(alignedSchema, alignErr).resolve();
        alignErr.print();
        Logger.profile("schema aligned by data");
        if (!schema.equals(alignedSchema)){
            schema.printDiff(alignedSchema);
            Cfgs.writeTo(root, true, alignedSchema);
            Logger.profile("schema write");
        }



        ValueErrs valueErr = ValueErrs.of();
        CfgValueParser cfgValueParser = new CfgValueParser(alignedSchema, data, alignedSchema, valueErr);
        CfgValue cfgValue = cfgValueParser.parseCfgValue();
        valueErr.print();

        SchemaErrs clientErr = SchemaErrs.of();
        CfgSchema clientSchema = new CfgSchemaFilterByTag(alignedSchema, "client", clientErr).filter();
        new CfgSchemaResolver(clientSchema, clientErr).resolve();
        clientErr.print();
        Logger.profile("schema filtered by client");

        ValueErrs clientValueErr = ValueErrs.of();
        CfgValueParser clientValueParser = new CfgValueParser(clientSchema, data, alignedSchema, clientValueErr);
        CfgValue clientValue = clientValueParser.parseCfgValue();
        clientValueErr.print();

    }

    public static void xmlToCfg() {
        CfgSchema cfg = Cfgs.readFromXml(Path.of("config.xml"), true);
        Path root = Path.of("config.cfg");

        Cfgs.writeTo(root, true, cfg);
        CfgSchema cfg2 = Cfgs.readFrom(root, true);

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
