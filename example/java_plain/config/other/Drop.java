package config.other;

import configgen.genjava.LoadValueErrs;

// 来自：other/drop.csv
public class Drop {
    private final int dropid;
    private final String name;
    private final java.util.List<config.other.DropItem> items;
    private final java.util.Map<Integer, Integer> testmap;

    private Drop(configgen.genjava.ConfigInput input) {
        this.dropid = input.readInt();
        this.name = input.readTextInPool();
        {
            int c = input.readCount(0);
            if (c == 0) {
                this.items = java.util.Collections.emptyList();
            } else {
                var items = new java.util.ArrayList<config.other.DropItem>(c);
                for (; c > 0; c--) {
                    items.add(config.other.DropItem._create(input));
                }
                this.items = java.util.Collections.unmodifiableList(items);
            }
        }
        {
            int c = input.readCount(0);
            if (c == 0) {
                this.testmap = java.util.Collections.emptyMap();
            } else {
                var entries = new java.util.LinkedHashMap<Integer, Integer>(c);
                for (; c > 0; c--) {
                    var key = input.readInt();
                    var value = input.readInt();
                    if (entries.putIfAbsent(key, value) != null) {
                        input.errors().duplicateKey("other.drop.testmap", key);
                    }
                }
                this.testmap = java.util.Collections.unmodifiableMap(entries);
            }
        }
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
    public String getName() {
        return name;
    }

    /**
     * 掉落概率
     */
    public java.util.List<config.other.DropItem> getItems() {
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

    public static Drop _create(configgen.genjava.ConfigInput input) {
        return new Drop(input);
    }

    public static Drop get(int dropid) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherDrop(dropid);
    }

    public static java.util.List<Drop> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allOtherDrop();
    }

    public static void _index(config.ConfigMgr mgr, java.util.Map<Integer, Drop> rows, LoadValueErrs errors) {
        int c = rows.size();
        boolean validSeq = true;
        var other_drop_All = new Drop[c];
        for (var self : rows.values()) {
            {
                int seq = self.getDropid();
                if (seq < 0 || seq >= c || other_drop_All[seq] != null) {
                    var primaryKey = self.dropid;
                    errors.invalidSeq("other.drop", "dropid", primaryKey, seq, c, mgr.explain("other.drop", primaryKey));
                    validSeq = false;
                } else {
                    other_drop_All[seq] = self;
                }
            }
        }
        if (!validSeq) {
            return;
        }
        mgr._set_other_drop_All(java.util.Arrays.asList(other_drop_All));
    }

}
