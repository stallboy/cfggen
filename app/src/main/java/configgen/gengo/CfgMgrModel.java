package configgen.gengo;

import configgen.value.CfgValue;

public record CfgMgrModel(String pkg,
                          CfgValue cfgValue, java.util.List<String> languages) {
    public String languageArguments() {
        return languages.stream().map(name -> "\"" + name + "\"").collect(java.util.stream.Collectors.joining(", "));
    }
}
