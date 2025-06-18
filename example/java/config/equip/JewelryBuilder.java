package config.equip;

public class JewelryBuilder {
    public int iD;
    public String name;
    public String iconFile;
    public config.LevelRank lvlRank;
    public String jType;
    public int suitID;
    public int keyAbility;
    public int keyAbilityValue;
    public int salePrice;
    public String description;

    public Jewelry build() {
        if (name == null) {
            name = "";
        }
        if (iconFile == null) {
            iconFile = "";
        }
        java.util.Objects.requireNonNull(lvlRank);
        if (jType == null) {
            jType = "";
        }
        if (description == null) {
            description = "";
        }
        return new Jewelry(this);
    }

}
