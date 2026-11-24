import config.ConfigMgrLoader;
import configgen.genjava.ConfigPackage;

import java.io.IOException;
import java.nio.file.Path;

public class LoadConfig {
    public static void main(String[] args) throws IOException {
        var mgr = ConfigMgrLoader.loadPackages(ConfigPackage.read(Path.of("base.bytes")));
        var drops = mgr.allOtherDrop();
        if (drops.size() != 4 || mgr.getOtherDrop(0) != drops.getFirst()
                || drops.getFirst().getDropid() != 0 || mgr.getOtherDrop(drops.size()) != null) {
            throw new IllegalStateException("Unexpected drop sequence lookup");
        }
        System.out.println("drop seq primary key: " + drops.stream().map(drop -> drop.getDropid()).toList());

        var first = mgr.getOtherKeyTestById1(0);
        var second = mgr.getOtherKeyTestById1(1);
        var missing = mgr.getOtherKeyTestById1(2);
        if (first == null || first.getId1() != 0 || second == null || second.getId1() != 1 || missing != null) {
            throw new IllegalStateException("Unexpected key_test sequence lookup");
        }
        System.out.printf("key_test seq lookup: 0 -> %d, 1 -> %d, 2 -> %s%n", first.getId1(), second.getId1(), missing);
    }
}
