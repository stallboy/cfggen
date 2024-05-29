package configgen.schema.cfg;

import configgen.schema.CfgSchema;
import configgen.schema.Nameable;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;

class CfgsTest {

    @Test
    public void readWriteReadEqual() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config1.cfg")) {
            String cfgStr = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
            CfgSchema cfg1 = CfgReader.parse(cfgStr);

            String cfgStr2 = CfgWriter.stringify(cfg1);
            CfgSchema cfg2 = CfgReader.parse(cfgStr);

            boolean equals = cfg1.equals(cfg2);
//            System.out.println(equals);
            if (!equals) {
                cfg1.printDiff(cfg2);
            }

            assertEquals(cfg1, cfg2);
            assertEquals(cfgStr, cfgStr2);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    public void readWriteSeparateReadEqual() {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("config1.cfg")) {
            String cfgStr = new String(Objects.requireNonNull(is).readAllBytes(), StandardCharsets.UTF_8);
            CfgSchema cfg1 = CfgReader.parse(cfgStr);

            Path path = Path.of("tmp/config.cfg");
            Path path1 = Path.of("tmp/config1.cfg");
            Cfgs.writeTo(path, true, cfg1);
            Cfgs.writeTo(path1, false, cfg1);
            CfgSchema cfgFromAllSubDir = Cfgs.readFrom(path, true);
            CfgSchema cfgFromOneFile = Cfgs.readFrom(path1, false);

            boolean equals = cfg1.equals(cfgFromAllSubDir);
//            System.out.println(equals);
            if (!equals) {
                int i = 0;
                for (Nameable item1 : cfg1.items()) {
                    Nameable item2 = cfgFromAllSubDir.items().get(i);
                    if (!item1.equals(item2)) {
                        System.out.println("=========not eq=========");
                        System.out.println(item1);
                        System.out.println(item2);
                    }
                    i++;
                }
            }

            assertEquals(cfg1, cfgFromAllSubDir);
            assertEquals(cfgFromAllSubDir, cfgFromOneFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


}