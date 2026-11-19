
package config;

public class Text {
    private final String zh_cn;
    private final String en;
    private final String tw;

    public Text(String zh_cn, String en, String tw) {
        this.zh_cn = zh_cn;
        this.en = en;
        this.tw = tw;
    }

    private Text(configgen.genjava.ConfigInput input) {
        int index = input.readTextIndex();
        this.zh_cn = input.textInPool(0, index);
        this.en = input.textInPool(1, index);
        this.tw = input.textInPool(2, index);
    }

    public String getZh_cn() {
        return zh_cn;
    }

    public String getEn() {
        return en;
    }

    public String getTw() {
        return tw;
    }

    @Override
    public String toString() {
        return "Text(" + zh_cn + "," + en + "," + tw + ")";
    }

    public static Text _create(configgen.genjava.ConfigInput input) {
        return new Text(input);
    }
}
