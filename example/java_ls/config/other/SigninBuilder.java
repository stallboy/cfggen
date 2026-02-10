package config.other;

public class SigninBuilder {
    public int id;
    public java.util.Map<Integer, Integer> item2countMap;
    public java.util.Map<Integer, Integer> vipitem2vipcountMap;
    public int viplevel;
    public String iconFile;

    public Signin build() {
        if (item2countMap == null) {
            item2countMap = new java.util.LinkedHashMap<>();
        }
        if (vipitem2vipcountMap == null) {
            vipitem2vipcountMap = new java.util.LinkedHashMap<>();
        }
        if (iconFile == null) {
            iconFile = "";
        }
        return new Signin(this);
    }

}
