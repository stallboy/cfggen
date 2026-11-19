package config.other;

import org.jspecify.annotations.Nullable;

import configgen.genjava.LoadValueErrs;

public class KeyTest {
    private final int id1;
    private final long id2;
    private final int id3;
    private final java.util.List<Integer> ids;
    private final String enumTest;
    private final java.util.List<String> enumList;
    private java.util.@Nullable List<config.other.@Nullable SignIn> RefIds;
    private config.other.@Nullable ArgCaptureMode RefEnumTest;
    private java.util.@Nullable List<config.other.@Nullable ArgCaptureMode> RefEnumList;

    private KeyTest(configgen.genjava.ConfigInput input) {
        this.id1 = input.readInt();
        this.id2 = input.readLong();
        this.id3 = input.readInt();
        {
            int c = input.readCount(0);
            if (c == 0) {
                this.ids = java.util.Collections.emptyList();
            } else {
                var items = new java.util.ArrayList<Integer>(c);
                for (; c > 0; c--) {
                    items.add(input.readInt());
                }
                this.ids = java.util.Collections.unmodifiableList(items);
            }
        }
        this.enumTest = input.readStringInPool();
        {
            int c = input.readCount(0);
            if (c == 0) {
                this.enumList = java.util.Collections.emptyList();
            } else {
                var items = new java.util.ArrayList<String>(c);
                for (; c > 0; c--) {
                    items.add(input.readStringInPool());
                }
                this.enumList = java.util.Collections.unmodifiableList(items);
            }
        }
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

    public java.util.@Nullable List<config.other.@Nullable SignIn> refIds() {
        return RefIds;
    }

    public config.other.@Nullable ArgCaptureMode refEnumTest() {
        return RefEnumTest;
    }

    public java.util.@Nullable List<config.other.@Nullable ArgCaptureMode> refEnumList() {
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
                var ref = mgr.getOtherSignIn(e);
                if (ref == null) {
                    errors.missingReference("other.key_test", "ids", "other.sign_in", e);
                }
                this.RefIds.add(ref);
            }
        }
        this.RefIds = java.util.Collections.unmodifiableList(this.RefIds);
        this.RefEnumTest = config.other.ArgCaptureMode.get(this.enumTest);
        if (this.RefEnumTest == null) {
            errors.missingReference("other.key_test", "enumTest", "other.ArgCaptureMode", this.enumTest);
        }
        if (this.enumList.isEmpty()) {
            this.RefEnumList = java.util.Collections.emptyList();
        } else {
            this.RefEnumList = new java.util.ArrayList<>(this.enumList.size());
            for (String e : this.enumList) {
                var ref = config.other.ArgCaptureMode.get(e);
                if (ref == null) {
                    errors.missingReference("other.key_test", "enumList", "other.ArgCaptureMode", e);
                }
                this.RefEnumList.add(ref);
            }
        }
        this.RefEnumList = java.util.Collections.unmodifiableList(this.RefEnumList);

    }

    public static KeyTest _create(configgen.genjava.ConfigInput input) {
        return new KeyTest(input);
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
        public boolean equals(@Nullable Object other) {
            if (!(other instanceof Id1Id2Key o)) {
                return false;
            }
            return id1 == o.id1 && id2 == o.id2;
        }

        @Override
        public String toString() {
            return "(" + id1 + "," + id2 + ")";
        }
    }

    public static @Nullable KeyTest get(int id1, long id2) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherKeyTest(id1, id2);
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
        public boolean equals(@Nullable Object other) {
            if (!(other instanceof Id1Id3Key o)) {
                return false;
            }
            return id1 == o.id1 && id3 == o.id3;
        }

        @Override
        public String toString() {
            return "(" + id1 + "," + id3 + ")";
        }
    }

    public static @Nullable KeyTest getById1Id3(int id1, int id3) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherKeyTestById1Id3(id1, id3);
    }

    public static @Nullable KeyTest getById2(long id2) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherKeyTestById2(id2);
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
        public boolean equals(@Nullable Object other) {
            if (!(other instanceof Id2Id3Key o)) {
                return false;
            }
            return id2 == o.id2 && id3 == o.id3;
        }

        @Override
        public String toString() {
            return "(" + id2 + "," + id3 + ")";
        }
    }

    public static @Nullable KeyTest getById2Id3(long id2, int id3) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherKeyTestById2Id3(id2, id3);
    }

    public static java.util.Collection<KeyTest> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allOtherKeyTest();
    }

    public static void _index(config.ConfigMgr mgr, java.util.Map<config.other.KeyTest.Id1Id2Key, KeyTest> rows, LoadValueErrs errors) {
        int c = rows.size();
        var other_key_test_Id1Id3Map = new java.util.LinkedHashMap<config.other.KeyTest.Id1Id3Key, KeyTest>(c);
        var other_key_test_Id2Map = new java.util.LinkedHashMap<Long, KeyTest>(c);
        var other_key_test_Id2Id3Map = new java.util.LinkedHashMap<config.other.KeyTest.Id2Id3Key, KeyTest>(c);
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
