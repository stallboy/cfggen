package config.other;

// 来自：other/drop.csv
public class CfgDrop {
    private int dropid;
    private config.Text name;
    private java.util.List<config.other.CfgDropItem> items;
    private java.util.Map<Integer, Integer> testmap;
    private CfgDrop() {
    }

    public static CfgDrop _create(configgen.genjava.ConfigInput input) {
        CfgDrop self = new CfgDrop();
        self.dropid = input.readInt();
        self.name = config.Text._create(input);
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.items = java.util.Collections.emptyList();
            } else {
                self.items = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.items.add(config.other.CfgDropItem._create(input));
                }
                self.items = java.util.Collections.unmodifiableList(self.items);
            }
        }
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.testmap = java.util.Collections.emptyMap();
            } else {
                self.testmap = new java.util.LinkedHashMap<>(c);
                for (; c > 0; c--) {
                    var key = input.readInt();
                    var value = input.readInt();
                    if (self.testmap.putIfAbsent(key, value) != null) input.errors().duplicateKey("other.drop.testmap", key);
                }
                self.testmap = java.util.Collections.unmodifiableMap(self.testmap);
            }
        }
        return self;
    }

    /**
     * 序号
     */
    public int getDropid() {
        return dropid;
    }

    /**
     * 名字
     */
    public config.Text getName() {
        return name;
    }

    /**
     * 掉落概率
     */
    public java.util.List<config.other.CfgDropItem> getItems() {
        return items;
    }

    /**
     * 测试map block
     */
    public java.util.Map<Integer, Integer> getTestmap() {
        return testmap;
    }

    @Override
    public String toString() {
        return "(" + dropid + "," + name + "," + items + "," + testmap + ")";
    }

    public static CfgDrop get(int dropid) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherDrop(dropid);
    }

    public static java.util.Collection<CfgDrop> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allOtherDrop();
    }


}
