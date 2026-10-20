package config.other;

import configgen.genjava.LoadValueErrs;

public class CfgKey_test {
    private int id1;
    private long id2;
    private int id3;
    private java.util.List<Integer> ids;
    private String enumTest;
    private java.util.List<String> enumList;
    private java.util.List<config.other.CfgSign_in> RefIds;
    private config.other.CfgArgCaptureMode RefEnumTest;
    private java.util.List<config.other.CfgArgCaptureMode> RefEnumList;

    private CfgKey_test() {
    }

    public static CfgKey_test _create(configgen.genjava.ConfigInput input) {
        CfgKey_test self = new CfgKey_test();
        self.id1 = input.readInt();
        self.id2 = input.readLong();
        self.id3 = input.readInt();
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.ids = java.util.Collections.emptyList();
            } else {
                self.ids = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.ids.add(input.readInt());
                }
                self.ids = java.util.Collections.unmodifiableList(self.ids);
            }
        }
        self.enumTest = input.readStringInPool();
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.enumList = java.util.Collections.emptyList();
            } else {
                self.enumList = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.enumList.add(input.readStringInPool());
                }
                self.enumList = java.util.Collections.unmodifiableList(self.enumList);
            }
        }
        return self;
    }

    public int getId1() {
        return id1;
    }

    public long getId2() {
        return id2;
    }

    public int getId3() {
        return id3;
    }

    public java.util.List<Integer> getIds() {
        return ids;
    }

    public String getEnumTest() {
        return enumTest;
    }

    public java.util.List<String> getEnumList() {
        return enumList;
    }

    public java.util.List<config.other.CfgSign_in> refIds() {
        return RefIds;
    }

    public config.other.CfgArgCaptureMode refEnumTest() {
        return RefEnumTest;
    }

    public java.util.List<config.other.CfgArgCaptureMode> refEnumList() {
        return RefEnumList;
    }

    @Override
    public String toString() {
        return "(" + id1 + "," + id2 + "," + id3 + "," + ids + "," + enumTest + "," + enumList + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        if (this.ids.isEmpty()) {
            this.RefIds = java.util.Collections.emptyList();
        } else {
            this.RefIds = new java.util.ArrayList<>(this.ids.size());
            for (Integer e : this.ids) {
                var ref = mgr.getOtherSign_in(e);
                if (ref == null) errors.missingReference("other.key_test", "ids", "other.sign_in", e);
                this.RefIds.add(ref);
            }
        }
        this.RefIds = java.util.Collections.unmodifiableList(this.RefIds);
        this.RefEnumTest = config.other.CfgArgCaptureMode.get(this.enumTest);
        if (this.RefEnumTest == null) errors.missingReference("other.key_test", "enumTest", "other.ArgCaptureMode", this.enumTest);
        if (this.enumList.isEmpty()) {
            this.RefEnumList = java.util.Collections.emptyList();
        } else {
            this.RefEnumList = new java.util.ArrayList<>(this.enumList.size());
            for (String e : this.enumList) {
                var ref = config.other.CfgArgCaptureMode.get(e);
                if (ref == null) errors.missingReference("other.key_test", "enumList", "other.ArgCaptureMode", e);
                this.RefEnumList.add(ref);
            }
        }
        this.RefEnumList = java.util.Collections.unmodifiableList(this.RefEnumList);

    }

    public static class Id1Id2Key {
        private final int id1;
        private final long id2;

        public Id1Id2Key(int id1, long id2) {
            this.id1 = id1;
            this.id2 = id2;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(id1, id2);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Id1Id2Key o))
                return false;
            return id1 == o.id1 && id2 == o.id2;
        }

        @Override
        public String toString() {
            return "(" + id1 + "," + id2 + ")";
        }
    }

    public static CfgKey_test get(int id1, long id2) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherKey_test(id1, id2);
    }

    public static class Id1Id3Key {
        private final int id1;
        private final int id3;

        public Id1Id3Key(int id1, int id3) {
            this.id1 = id1;
            this.id3 = id3;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(id1, id3);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Id1Id3Key o))
                return false;
            return id1 == o.id1 && id3 == o.id3;
        }

        @Override
        public String toString() {
            return "(" + id1 + "," + id3 + ")";
        }
    }

    public static CfgKey_test getById1Id3(int id1, int id3) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherKey_testById1Id3(id1, id3);
    }

    public static CfgKey_test getById2(long id2) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherKey_testById2(id2);
    }

    public static class Id2Id3Key {
        private final long id2;
        private final int id3;

        public Id2Id3Key(long id2, int id3) {
            this.id2 = id2;
            this.id3 = id3;
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(id2, id3);
        }

        @Override
        public boolean equals(Object other) {
            if (!(other instanceof Id2Id3Key o))
                return false;
            return id2 == o.id2 && id3 == o.id3;
        }

        @Override
        public String toString() {
            return "(" + id2 + "," + id3 + ")";
        }
    }

    public static CfgKey_test getById2Id3(long id2, int id3) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherKey_testById2Id3(id2, id3);
    }

    public static java.util.Collection<CfgKey_test> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allOtherKey_test();
    }

    public static void _index(config.ConfigMgr mgr, java.util.Map<config.other.CfgKey_test.Id1Id2Key, CfgKey_test> rows, LoadValueErrs errors) {
        int c = rows.size();
        var other_key_test_Id1Id3Map = new java.util.LinkedHashMap<config.other.CfgKey_test.Id1Id3Key, CfgKey_test>(c);
        var other_key_test_Id2Map = new java.util.LinkedHashMap<Long, CfgKey_test>(c);
        var other_key_test_Id2Id3Map = new java.util.LinkedHashMap<config.other.CfgKey_test.Id2Id3Key, CfgKey_test>(c);
        for (var self : rows.values()) {
            if (other_key_test_Id1Id3Map.putIfAbsent(new Id1Id3Key(self.id1, self.id3), self) != null) {
                var primaryKey = new Id1Id2Key(self.id1, self.id2);
                errors.duplicateUniqueKey("other.key_test", "id1,id3", primaryKey, new Id1Id3Key(self.id1, self.id3), mgr.explain("other.key_test", primaryKey));
            }
            if (other_key_test_Id2Map.putIfAbsent(self.id2, self) != null) {
                var primaryKey = new Id1Id2Key(self.id1, self.id2);
                errors.duplicateUniqueKey("other.key_test", "id2", primaryKey, self.id2, mgr.explain("other.key_test", primaryKey));
            }
            if (other_key_test_Id2Id3Map.putIfAbsent(new Id2Id3Key(self.id2, self.id3), self) != null) {
                var primaryKey = new Id1Id2Key(self.id1, self.id2);
                errors.duplicateUniqueKey("other.key_test", "id2,id3", primaryKey, new Id2Id3Key(self.id2, self.id3), mgr.explain("other.key_test", primaryKey));
            }
        }
        mgr._set_other_key_test_All(rows);
        mgr._set_other_key_test_Id1Id3Map(other_key_test_Id1Id3Map);
        mgr._set_other_key_test_Id2Map(other_key_test_Id2Map);
        mgr._set_other_key_test_Id2Id3Map(other_key_test_Id2Id3Map);
    }

}
