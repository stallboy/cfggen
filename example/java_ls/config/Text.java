
package config;

public class Text {
    private String zh_cn;
    private String en;
    private String tw;
    private Text() {
    }

    public Text(String zh_cn, String en, String tw) {
        this.zh_cn = zh_cn;
        this.en = en;
        this.tw = tw;
    }

    public static Text _create(configgen.genjava.ConfigInput input) {
        Text self = new Text();
        String[] texts = input.readTextsInPool();
        self.zh_cn = texts[0];
        self.en = texts[1];
        self.tw = texts[2];
        return self;
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
}