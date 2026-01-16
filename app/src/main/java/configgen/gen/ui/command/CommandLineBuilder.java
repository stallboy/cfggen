package configgen.gen.ui.command;

import configgen.gen.GuiLauncher;
import configgen.gen.ui.UIConstants;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 统一管理命令行参数的构建逻辑
 * 消除updateCommandPreview()和buildCommandLineArgs()的重复代码
 */
public class CommandLineBuilder {
    private final GuiLauncher launcher;

    public CommandLineBuilder(GuiLauncher launcher) {
        this.launcher = launcher;
    }

    /**
     * 构建命令行预览字符串（用于显示）
     */
    public String buildPreviewCommand() {
        StringBuilder cmd = new StringBuilder("java -jar cfggen.jar");
        appendDatadir(cmd);
        appendEncoding(cmd);
        appendHeadRow(cmd);
        appendUsePoi(cmd);
        appendAdvancedDirs(cmd);
        appendI18n(cmd);
        appendOptions(cmd);
        appendToolsAndGenerators(cmd);
        return cmd.toString();
    }

    /**
     * 构建命令行参数列表（用于执行）
     */
    public List<String> buildArgs() {
        List<String> args = new ArrayList<>();
        addDatadir(args);
        addEncoding(args);
        addHeadRow(args);
        addUsePoi(args);
        addAdvancedDirs(args);
        addI18n(args);
        addOptions(args);
        addToolsAndGenerators(args);
        return args;
    }

    // StringBuilder版本的方法

    private void appendDatadir(StringBuilder cmd) {
        String value = launcher.getDatadirField().getText().trim();
        if (!value.isEmpty()) {
            cmd.append(" -datadir \"").append(value).append("\"");
        }
    }

    private void appendEncoding(StringBuilder cmd) {
        String value = launcher.getEncodingField().getText().trim();
        if (!value.isEmpty() && !value.equals(UIConstants.DEFAULT_ENCODING)) {
            cmd.append(" -encoding ").append(value);
        }
    }

    private void appendHeadRow(StringBuilder cmd) {
        String value = launcher.getHeadRowField().getText().trim();
        if (!value.isEmpty() && !value.equals(UIConstants.DEFAULT_HEAD_ROW)) {
            cmd.append(" -headrow ").append(value);
        }
    }

    private void appendUsePoi(StringBuilder cmd) {
        if (launcher.getUsePoiCheckBox().isSelected()) {
            cmd.append(" -usepoi");
        }
    }

    private void appendAdvancedDirs(StringBuilder cmd) {
        String asRoot = launcher.getAsRootField().getText().trim();
        if (!asRoot.isEmpty()) {
            cmd.append(" -asroot \"").append(asRoot).append("\"");
        }

        String excelDirs = launcher.getExcelDirsField().getText().trim();
        if (!excelDirs.isEmpty()) {
            cmd.append(" -exceldirs \"").append(excelDirs).append("\"");
        }

        String jsonDirs = launcher.getJsonDirsField().getText().trim();
        if (!jsonDirs.isEmpty()) {
            cmd.append(" -jsondirs \"").append(jsonDirs).append("\"");
        }
    }

    private void appendI18n(StringBuilder cmd) {
        if (launcher.getI18nFileRadio().isSelected()) {
            String i18nfile = launcher.getI18nfileField().getText().trim();
            if (!i18nfile.isEmpty()) {
                cmd.append(" -i18nfile \"").append(i18nfile).append("\"");
            }
        } else if (launcher.getLangSwitchRadio().isSelected()) {
            String langSwitchDir = launcher.getLangSwitchDirField().getText().trim();
            if (!langSwitchDir.isEmpty()) {
                cmd.append(" -langswitchdir \"").append(langSwitchDir).append("\"");
            }

            String defaultLang = launcher.getDefaultLangField().getText().trim();
            if (!defaultLang.isEmpty() && !defaultLang.equals(UIConstants.DEFAULT_LANG)) {
                cmd.append(" -defaultlang ").append(defaultLang);
            }
        }
    }

    private void appendOptions(StringBuilder cmd) {
        if (launcher.getVerboseCheckBox().isSelected()) {
            cmd.append(" -v");
        }
        if (launcher.getVerbose2CheckBox().isSelected()) {
            cmd.append(" -vv");
        }
        if (launcher.getProfileCheckBox().isSelected()) {
            cmd.append(" -p");
        }
        if (launcher.getProfileGcCheckBox().isSelected()) {
            cmd.append(" -pp");
        }
        if (launcher.getNoWarnCheckBox().isSelected()) {
            cmd.append(" -nowarn");
        }
        if (launcher.getWeakWarnCheckBox().isSelected()) {
            cmd.append(" -weakwarn");
        }
    }

    private void appendToolsAndGenerators(StringBuilder cmd) {
        for (var panel : launcher.getToolPanels()) {
            String panelCmd = panel.buildCommand();
            if (!panelCmd.isEmpty()) {
                cmd.append(" -tool ").append(panelCmd);
            }
        }

        for (var panel : launcher.getGeneratorPanels()) {
            String panelCmd = panel.buildCommand();
            if (!panelCmd.isEmpty()) {
                cmd.append(" -gen ").append(panelCmd);
            }
        }
    }

    // List版本的方法

    private void addDatadir(List<String> args) {
        String value = launcher.getDatadirField().getText().trim();
        if (!value.isEmpty()) {
            args.add("-datadir");
            args.add(value);
        }
    }

    private void addEncoding(List<String> args) {
        String value = launcher.getEncodingField().getText().trim();
        if (!value.isEmpty() && !value.equals(UIConstants.DEFAULT_ENCODING)) {
            args.add("-encoding");
            args.add(value);
        }
    }

    private void addHeadRow(List<String> args) {
        String value = launcher.getHeadRowField().getText().trim();
        if (!value.isEmpty() && !value.equals(UIConstants.DEFAULT_HEAD_ROW)) {
            args.add("-headrow");
            args.add(value);
        }
    }

    private void addUsePoi(List<String> args) {
        if (launcher.getUsePoiCheckBox().isSelected()) {
            args.add("-usepoi");
        }
    }

    private void addAdvancedDirs(List<String> args) {
        String asRoot = launcher.getAsRootField().getText().trim();
        if (!asRoot.isEmpty()) {
            args.add("-asroot");
            args.add(asRoot);
        }

        String excelDirs = launcher.getExcelDirsField().getText().trim();
        if (!excelDirs.isEmpty()) {
            args.add("-exceldirs");
            args.add(excelDirs);
        }

        String jsonDirs = launcher.getJsonDirsField().getText().trim();
        if (!jsonDirs.isEmpty()) {
            args.add("-jsondirs");
            args.add(jsonDirs);
        }
    }

    private void addI18n(List<String> args) {
        if (launcher.getI18nFileRadio().isSelected()) {
            String i18nfile = launcher.getI18nfileField().getText().trim();
            if (!i18nfile.isEmpty()) {
                args.add("-i18nfile");
                args.add(i18nfile);
            }
        } else if (launcher.getLangSwitchRadio().isSelected()) {
            String langSwitchDir = launcher.getLangSwitchDirField().getText().trim();
            if (!langSwitchDir.isEmpty()) {
                args.add("-langswitchdir");
                args.add(langSwitchDir);
            }

            String defaultLang = launcher.getDefaultLangField().getText().trim();
            if (!defaultLang.isEmpty()) {
                args.add("-defaultlang");
                args.add(defaultLang);
            }
        }
    }

    private void addOptions(List<String> args) {
        if (launcher.getVerboseCheckBox().isSelected()) {
            args.add("-v");
        }
        if (launcher.getVerbose2CheckBox().isSelected()) {
            args.add("-vv");
        }
        if (launcher.getProfileCheckBox().isSelected()) {
            args.add("-p");
        }
        if (launcher.getProfileGcCheckBox().isSelected()) {
            args.add("-pp");
        }
        if (launcher.getNoWarnCheckBox().isSelected()) {
            args.add("-nowarn");
        }
        if (launcher.getWeakWarnCheckBox().isSelected()) {
            args.add("-weakwarn");
        }
    }

    private void addToolsAndGenerators(List<String> args) {
        for (var panel : launcher.getToolPanels()) {
            String panelCmd = panel.buildCommand();
            if (!panelCmd.isEmpty()) {
                args.add("-tool");
                args.add(panelCmd);
            }
        }

        for (var panel : launcher.getGeneratorPanels()) {
            String panelCmd = panel.buildCommand();
            if (!panelCmd.isEmpty()) {
                args.add("-gen");
                args.add(panelCmd);
            }
        }
    }
}
