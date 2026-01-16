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
        buildCommand(new StringBuilderAppender(cmd));
        return cmd.toString();
    }

    /**
     * 构建命令行参数列表（用于执行）
     */
    public List<String> buildArgs() {
        List<String> args = new ArrayList<>();
        buildCommand(new ListAppender(args));
        return args;
    }

    /**
     * 核心构建逻辑，使用策略模式避免重复
     */
    private void buildCommand(CommandAppender appender) {
        appendDatadir(appender);
        appendEncoding(appender);
        appendHeadRow(appender);
        appendUsePoi(appender);
        appendAdvancedDirs(appender);
        appendI18n(appender);
        appendOptions(appender);
        appendToolsAndGenerators(appender);
    }

    private void appendDatadir(CommandAppender appender) {
        String value = launcher.getDatadirField().getText().trim();
        if (!value.isEmpty()) {
            appender.append("-datadir", value, true);
        }
    }

    private void appendEncoding(CommandAppender appender) {
        String value = launcher.getEncodingField().getText().trim();
        if (!value.isEmpty() && !value.equals(UIConstants.DEFAULT_ENCODING)) {
            appender.append("-encoding", value, false);
        }
    }

    private void appendHeadRow(CommandAppender appender) {
        String value = launcher.getHeadRowField().getText().trim();
        if (!value.isEmpty() && !value.equals(UIConstants.DEFAULT_HEAD_ROW)) {
            appender.append("-headrow", value, false);
        }
    }

    private void appendUsePoi(CommandAppender appender) {
        if (launcher.getUsePoiCheckBox().isSelected()) {
            appender.appendFlag("-usepoi");
        }
    }

    private void appendAdvancedDirs(CommandAppender appender) {
        String asRoot = launcher.getAsRootField().getText().trim();
        if (!asRoot.isEmpty()) {
            appender.append("-asroot", asRoot, true);
        }

        String excelDirs = launcher.getExcelDirsField().getText().trim();
        if (!excelDirs.isEmpty()) {
            appender.append("-exceldirs", excelDirs, true);
        }

        String jsonDirs = launcher.getJsonDirsField().getText().trim();
        if (!jsonDirs.isEmpty()) {
            appender.append("-jsondirs", jsonDirs, true);
        }
    }

    private void appendI18n(CommandAppender appender) {
        I18nConfig i18n = launcher.getI18nConfig();

        if (i18n.isI18nFileMode()) {
            String i18nfile = i18n.getI18nFile().trim();
            if (!i18nfile.isEmpty()) {
                appender.append("-i18nfile", i18nfile, true);
            }
        } else if (i18n.isLangSwitchMode()) {
            String langSwitchDir = i18n.getLangSwitchDir().trim();
            if (!langSwitchDir.isEmpty()) {
                appender.append("-langswitchdir", langSwitchDir, true);
            }

            String defaultLang = i18n.getDefaultLang().trim();
            if (!defaultLang.isEmpty() && !defaultLang.equals(UIConstants.DEFAULT_LANG)) {
                appender.append("-defaultlang", defaultLang, false);
            }
        }
    }

    private void appendOptions(CommandAppender appender) {
        if (launcher.getVerboseCheckBox().isSelected()) {
            appender.appendFlag("-v");
        }
        if (launcher.getVerbose2CheckBox().isSelected()) {
            appender.appendFlag("-vv");
        }
        if (launcher.getProfileCheckBox().isSelected()) {
            appender.appendFlag("-p");
        }
        if (launcher.getProfileGcCheckBox().isSelected()) {
            appender.appendFlag("-pp");
        }
        if (launcher.getNoWarnCheckBox().isSelected()) {
            appender.appendFlag("-nowarn");
        }
        if (launcher.getWeakWarnCheckBox().isSelected()) {
            appender.appendFlag("-weakwarn");
        }
    }

    private void appendToolsAndGenerators(CommandAppender appender) {
        for (var panel : launcher.getToolPanels()) {
            String panelCmd = panel.buildCommand();
            if (!panelCmd.isEmpty()) {
                appender.appendFlag("-tool");
                appender.appendRawValue(panelCmd);
            }
        }

        for (var panel : launcher.getGeneratorPanels()) {
            String panelCmd = panel.buildCommand();
            if (!panelCmd.isEmpty()) {
                appender.appendFlag("-gen");
                appender.appendRawValue(panelCmd);
            }
        }
    }

    /**
     * 策略接口：统一命令行参数的追加操作
     */
    private interface CommandAppender {
        void append(String flag, String value, boolean quoted);
        void appendFlag(String flag);
        void appendRawValue(String value);
    }

    /**
     * StringBuilder实现：用于生成命令行预览字符串
     */
    private static class StringBuilderAppender implements CommandAppender {
        private final StringBuilder cmd;

        StringBuilderAppender(StringBuilder cmd) {
            this.cmd = cmd;
        }

        @Override
        public void append(String flag, String value, boolean quoted) {
            if (quoted) {
                cmd.append(" ").append(flag).append(" \"").append(value).append("\"");
            } else {
                cmd.append(" ").append(flag).append(" ").append(value);
            }
        }

        @Override
        public void appendFlag(String flag) {
            cmd.append(" ").append(flag);
        }

        @Override
        public void appendRawValue(String value) {
            cmd.append(" ").append(value);
        }
    }

    /**
     * List实现：用于生成命令行参数列表
     */
    private static class ListAppender implements CommandAppender {
        private final List<String> args;

        ListAppender(List<String> args) {
            this.args = args;
        }

        @Override
        public void append(String flag, String value, boolean quoted) {
            args.add(flag);
            args.add(value);
        }

        @Override
        public void appendFlag(String flag) {
            args.add(flag);
        }

        @Override
        public void appendRawValue(String value) {
            args.add(value);
        }
    }
}
