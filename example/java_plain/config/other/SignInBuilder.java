package config.other;

public class SignInBuilder {
    public int id;
    public java.util.Map<Integer, Integer> item2countMap = new java.util.LinkedHashMap<>();
    public java.util.Map<Integer, Integer> vipitem2vipcountMap = new java.util.LinkedHashMap<>();
    public int viplevel;
    public String iconFile = "";

    public SignIn build() {
        return new SignIn(this);
    }
}
