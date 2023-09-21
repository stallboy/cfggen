package config.other;

public class Monster {
    private int id;
    private java.util.List<config.Position> posList;

    private Monster() {
    }

    public static Monster _create(configgen.genjava.ConfigInput input) {
        Monster self = new Monster();
        self.id = input.readInt();
        self.posList = new java.util.ArrayList<>();
        for (int c = input.readInt(); c > 0; c--) {
            self.posList.add(config.Position._create(input));
        }
        return self;
    }

    /**
     * id
     */
    public int getId() {
        return id;
    }

    public java.util.List<config.Position> getPosList() {
        return posList;
    }

    @Override
    public String toString() {
        return "(" + id + "," + posList + ")";
    }

    public static Monster get(int id) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.other_monster_All.get(id);
    }

    public static java.util.Collection<Monster> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.other_monster_All.values();
    }

    public static class _ConfigLoader implements config.ConfigLoader {

        @Override
        public void createAll(config.ConfigMgr mgr, configgen.genjava.ConfigInput input) {
            for (int c = input.readInt(); c > 0; c--) {
                Monster self = Monster._create(input);
                mgr.other_monster_All.put(self.id, self);
            }
        }

        @Override
        public void resolveAll(config.ConfigMgr mgr) {
            // no resolve
        }

    }

}
