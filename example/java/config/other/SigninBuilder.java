package config.other;

public class SigninBuilder {
    public int id;
    public java.util.Map<Integer, Integer> item2countMap;
    public java.util.Map<Integer, Integer> vipitem2vipcountMap;
    public int viplevel;
    public String iconFile;

    public Signin build() {
        java.util.Objects.requireNonNull(item2countMap);
        java.util.Objects.requireNonNull(vipitem2vipcountMap);
        java.util.Objects.requireNonNull(iconFile);
        return new Signin(this);
    }

}
