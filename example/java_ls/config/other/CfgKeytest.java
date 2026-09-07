package config.other;

public class CfgKeytest {
    private int id1;
    private long id2;
    private int id3;
    private java.util.List<Integer> ids;
    private String enumTest;
    private java.util.List<String> enumList;
    private java.util.List<config.other.CfgSignin> RefIds;
    private config.other.CfgArgCaptureMode RefEnumTest;
    private java.util.List<config.other.CfgArgCaptureMode> RefEnumList;

    private CfgKeytest() {
    }

    public static CfgKeytest _create(configgen.genjava.ConfigInput input) {
        CfgKeytest self = new CfgKeytest();
        self.id1 = input.readInt();
        self.id2 = input.readLong();
        self.id3 = input.readInt();
        {
            int c = input.readInt();
            if (c == 0) {
                self.ids = java.util.Collections.emptyList();
            } else {
                self.ids = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.ids.add(input.readInt());
                }
            }
        }
        self.enumTest = input.readStringInPool();
        {
            int c = input.readInt();
            if (c == 0) {
                self.enumList = java.util.Collections.emptyList();
            } else {
                self.enumList = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.enumList.add(input.readStringInPool());
                }
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

    public java.util.List<config.other.CfgSignin> refIds() {
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

    public void _resolveDirect(config.ConfigMgr mgr) {
        if (ids.isEmpty()) {
            RefIds = java.util.Collections.emptyList();
        } else {
            RefIds = new java.util.ArrayList<>(ids.size());
            for (Integer e : ids) {
                config.other.CfgSignin r = mgr.other_signin_All.get(e);
                configgen.genjava.LoadValueErrs.requireNonNull(r, "other.keytest.ids -> other.signin", e);
                RefIds.add(r);
            }
        }
        RefEnumTest = config.other.CfgArgCaptureMode.get(enumTest);
        configgen.genjava.LoadValueErrs.requireNonNull(RefEnumTest, "other.keytest.enumTest -> other.ArgCaptureMode", enumTest);
        if (enumList.isEmpty()) {
            RefEnumList = java.util.Collections.emptyList();
        } else {
            RefEnumList = new java.util.ArrayList<>(enumList.size());
            for (String e : enumList) {
                config.other.CfgArgCaptureMode r = config.other.CfgArgCaptureMode.get(e);
                configgen.genjava.LoadValueErrs.requireNonNull(r, "other.keytest.enumList -> other.ArgCaptureMode", e);
                RefEnumList.add(r);
            }
        }
    }

    public void _resolve(config.ConfigMgr mgr) {
        _resolveDirect(mgr);
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
            if (!(other instanceof Id1Id2Key))
                return false;
            Id1Id2Key o = (Id1Id2Key) other;
            return id1 == o.id1 && id2 == o.id2;
        }
    }

    public static CfgKeytest get(int id1, long id2) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherKeytest(id1, id2);
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
            if (!(other instanceof Id1Id3Key))
                return false;
            Id1Id3Key o = (Id1Id3Key) other;
            return id1 == o.id1 && id3 == o.id3;
        }
    }

    public static CfgKeytest getById1Id3(int id1, int id3) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherKeytestById1Id3(id1, id3);
    }

    public static CfgKeytest getById2(long id2) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherKeytestById2(id2);
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
            if (!(other instanceof Id2Id3Key))
                return false;
            Id2Id3Key o = (Id2Id3Key) other;
            return id2 == o.id2 && id3 == o.id3;
        }
    }

    public static CfgKeytest getById2Id3(long id2, int id3) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getOtherKeytestById2Id3(id2, id3);
    }

    public static java.util.Collection<CfgKeytest> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allOtherKeytest();
    }
    public static class _ConfigLoader implements config.ConfigLoader {

        @Override
        public void createAll(config.ConfigMgr mgr, configgen.genjava.ConfigInput input) {
            int c = input.readInt();
            mgr.other_keytest_All = new java.util.LinkedHashMap<>(c);
            mgr.other_keytest_Id1Id3Map = new java.util.LinkedHashMap<>(c);
            mgr.other_keytest_Id2Map = new java.util.LinkedHashMap<>(c);
            mgr.other_keytest_Id2Id3Map = new java.util.LinkedHashMap<>(c);
            for (; c > 0; c--) {
                CfgKeytest self = CfgKeytest._create(input);
                mgr.other_keytest_All.put(new Id1Id2Key(self.id1, self.id2), self);
                mgr.other_keytest_Id1Id3Map.put(new Id1Id3Key(self.id1, self.id3), self);
                mgr.other_keytest_Id2Map.put(self.id2, self);
                mgr.other_keytest_Id2Id3Map.put(new Id2Id3Key(self.id2, self.id3), self);
            }
        }

        @Override
        public void resolveAll(config.ConfigMgr mgr) {
            for (CfgKeytest e : mgr.other_keytest_All.values()) {
                e._resolve(mgr);
            }
        }

    }

}
