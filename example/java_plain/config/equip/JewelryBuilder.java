package config.equip;

public class JewelryBuilder {
    public int iD;
    public String name = "";
    public String iconFile = "";
    public config.LevelRank lvlRank = new config.LevelRank(0, 0);
    public String jType = "";
    public int suitID;
    public int keyAbility;
    public int keyAbilityValue;
    public int salePrice;
    public String description = "";

    public Jewelry build() {
        return new Jewelry(this);
    }
}
