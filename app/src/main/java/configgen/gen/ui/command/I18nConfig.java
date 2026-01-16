package configgen.gen.ui.command;

import javax.swing.*;

/**
 * I18n配置的封装对象
 */
public class I18nConfig {
    private final JRadioButton i18nFileRadio;
    private final JRadioButton langSwitchRadio;
    private final JTextField i18nfileField;
    private final JTextField langSwitchDirField;
    private final JTextField defaultLangField;

    public I18nConfig(JRadioButton i18nFileRadio, JRadioButton langSwitchRadio,
                     JTextField i18nfileField, JTextField langSwitchDirField,
                     JTextField defaultLangField) {
        this.i18nFileRadio = i18nFileRadio;
        this.langSwitchRadio = langSwitchRadio;
        this.i18nfileField = i18nfileField;
        this.langSwitchDirField = langSwitchDirField;
        this.defaultLangField = defaultLangField;
    }

    public boolean isI18nFileMode() {
        return i18nFileRadio.isSelected();
    }

    public boolean isLangSwitchMode() {
        return langSwitchRadio.isSelected();
    }

    public String getI18nFile() {
        return i18nfileField.getText();
    }

    public String getLangSwitchDir() {
        return langSwitchDirField.getText();
    }

    public String getDefaultLang() {
        return defaultLangField.getText();
    }
}
