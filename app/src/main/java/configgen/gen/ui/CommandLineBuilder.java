package configgen.gen.ui;

import java.util.ArrayList;
import java.util.List;

/**
 * 统一管理命令行参数的构建逻辑
 * 使用统一的参数收集方法消除重复代码
 */
public record CommandLineBuilder(GuiLauncher launcher) {
    /**
     * 命令参数的简单封装
     */
    private record CmdArg(String name, String value, boolean quoteSpaces) {
        CmdArg(String name, String value) {
            this(name, value, false);
        }
    }

    /**
     * 构建命令行预览字符串（用于显示）
     */
    public String buildPreviewCommand() {
        StringBuilder cmd = new StringBuilder("java -jar cfggen.jar");
        for (CmdArg arg : collectCmdArgs()) {
            appendTo(cmd, arg);
        }
        appendToolsAndGenerators(cmd);
        return cmd.toString();
    }

    /**
     * 构建命令行参数列表（用于执行）
     */
    public List<String> buildArgs() {
        List<String> args = new ArrayList<>();
        for (CmdArg arg : collectCmdArgs()) {
            addToList(args, arg);
        }
        addToolsAndGenerators(args);
        return args;
    }

    /**
     * 收集所有需要添加的命令行参数
     */
    private List<CmdArg> collectCmdArgs() {
        List<CmdArg> args = new ArrayList<>();

        // 基本参数
        addIf(args, "-datadir", launcher.datadirField.getText().trim(), null, true);
        addIf(args, "-encoding", launcher.encodingField.getText().trim(),
                UIConstants.DEFAULT_ENCODING, false);
        addIf(args, "-headrow", launcher.headRowField.getText().trim(),
                UIConstants.DEFAULT_HEAD_ROW, false);

        if (launcher.usePoiCheckBox.isSelected()) {
            args.add(new CmdArg("-usepoi", null));
        }

        // 高级目录参数
        addIf(args, "-asroot", launcher.asRootField.getText().trim(), null, true);
        addIf(args, "-exceldirs", launcher.excelDirsField.getText().trim(), null, true);
        addIf(args, "-jsondirs", launcher.jsonDirsField.getText().trim(), null, true);

        // I18n参数
        if (launcher.i18nFileRadio.isSelected()) {
            addIf(args, "-i18nfile", launcher.i18nfileField.getText().trim(), null, true);
        } else if (launcher.langSwitchRadio.isSelected()) {
            addIf(args, "-langswitchdir", launcher.langSwitchDirField.getText().trim(), null, true);
            addIf(args, "-defaultlang", launcher.defaultLangField.getText().trim(),
                    UIConstants.DEFAULT_LANG, false);
        }

        // 选项参数
        if (launcher.verboseCheckBox.isSelected()) args.add(new CmdArg("-v", null));
        if (launcher.verbose2CheckBox.isSelected()) args.add(new CmdArg("-vv", null));
        if (launcher.profileCheckBox.isSelected()) args.add(new CmdArg("-p", null));
        if (launcher.profileGcCheckBox.isSelected()) args.add(new CmdArg("-pp", null));
        if (launcher.noWarnCheckBox.isSelected()) args.add(new CmdArg("-nowarn", null));
        if (launcher.weakWarnCheckBox.isSelected()) args.add(new CmdArg("-weakwarn", null));

        return args;
    }

    /**
     * 条件添加参数的辅助方法
     */
    private void addIf(List<CmdArg> args, String name, String value, String defaultValue, boolean quoteSpaces) {
        if (value != null && !value.isEmpty() && !value.equals(defaultValue)) {
            args.add(new CmdArg(name, value, quoteSpaces));
        }
    }

    /**
     * 格式化参数到StringBuilder（用于预览命令）
     */
    private void appendTo(StringBuilder cmd, CmdArg arg) {
        if (arg.value() == null) {
            cmd.append(" ").append(arg.name());
        } else if (arg.quoteSpaces()) {
            cmd.append(" ").append(arg.name()).append(" \"").append(arg.value()).append("\"");
        } else {
            cmd.append(" ").append(arg.name()).append(" ").append(arg.value());
        }
    }

    /**
     * 添加参数到List（用于执行命令）
     */
    private void addToList(List<String> args, CmdArg arg) {
        args.add(arg.name());
        if (arg.value() != null) {
            args.add(arg.value());
        }
    }

    /**
     * 添加Tools和Generators参数到StringBuilder
     */
    private void appendToolsAndGenerators(StringBuilder cmd) {
        for (var panel : launcher.toolPanels) {
            String panelCmd = panel.buildCommand();
            if (!panelCmd.isEmpty()) {
                cmd.append(" -tool ").append(panelCmd);
            }
        }

        for (var panel : launcher.generatorPanels) {
            String panelCmd = panel.buildCommand();
            if (!panelCmd.isEmpty()) {
                cmd.append(" -gen ").append(panelCmd);
            }
        }
    }

    /**
     * 添加Tools和Generators参数到List
     */
    private void addToolsAndGenerators(List<String> args) {
        for (var panel : launcher.toolPanels) {
            String panelCmd = panel.buildCommand();
            if (!panelCmd.isEmpty()) {
                args.add("-tool");
                args.add(panelCmd);
            }
        }

        for (var panel : launcher.generatorPanels) {
            String panelCmd = panel.buildCommand();
            if (!panelCmd.isEmpty()) {
                args.add("-gen");
                args.add(panelCmd);
            }
        }
    }
}
