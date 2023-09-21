package config.equip;

public class JewelryBuilder {
    public int iD;
    public String name;
    public String iconFile;
    public config.LevelRank lvlRank;
    public String type;
    public int suitID;
    public int keyAbility;
    public int keyAbilityValue;
    public int salePrice;
    public String description;

    public Jewelry build() {
        java.util.Objects.requireNonNull(name);
        java.util.Objects.requireNonNull(iconFile);
        java.util.Objects.requireNonNull(lvlRank);
        java.util.Objects.requireNonNull(type);
        java.util.Objects.requireNonNull(description);
        return new Jewelry(this);
    }

}
