package configgen.gen;

import configgen.util.Logger;
import configgen.util.LocaleUtil;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.PrintStream;
import java.util.*;
import java.util.List;

public class GuiLauncher {
    private JFrame mainFrame;
    private JTextArea outputArea;
    private JTextArea commandPreview;

    private JTextField datadirField;
    private JTextField encodingField;
    private JTextField headRowField;
    private JCheckBox usePoiCheckBox;
    private JTextField asRootField;
    private JTextField excelDirsField;
    private JTextField jsonDirsField;
    private JTextField i18nfileField;
    private JTextField langSwitchDirField;
    private JTextField defaultLangField;
    private JRadioButton i18nFileRadio;
    private JRadioButton langSwitchRadio;
    private JCheckBox verboseCheckBox;
    private JCheckBox verbose2CheckBox;
    private JCheckBox profileCheckBox;
    private JCheckBox profileGcCheckBox;
    private JCheckBox noWarnCheckBox;
    private JCheckBox weakWarnCheckBox;

    private final List<ParameterPanel> toolPanels = new ArrayList<>();
    private final List<ParameterPanel> generatorPanels = new ArrayList<>();

    private JPanel toolsPanel;
    private JPanel generatorsPanel;
    private JButton runButton;

    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private final Logger.Printer originalPrinter = Logger.getPrinter();

    public static void launch() {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
            new GuiLauncher().init();
        });
    }

    private void init() {
        String currentDir = System.getProperty("user.dir");
        String title = LocaleUtil.getLocaleString("GuiLauncher.Title", "Configuration Generator") + " - " + currentDir;

        mainFrame = new JFrame(title);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(1400, 800);
        mainFrame.setLocationRelativeTo(null);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(900);
        splitPane.setResizeWeight(0.6);

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        JScrollPane leftScroll = new JScrollPane(leftPanel);
        leftScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        JPanel rightPanel = createRightPanel();

        splitPane.setLeftComponent(leftScroll);
        splitPane.setRightComponent(rightPanel);
        mainFrame.add(splitPane, BorderLayout.CENTER);

        leftPanel.add(createToolsPanel());
        leftPanel.add(Box.createVerticalStrut(10));
        leftPanel.add(createBasicParametersPanel());
        leftPanel.add(Box.createVerticalStrut(10));
        leftPanel.add(createGeneratorsPanel());

        mainFrame.setVisible(true);
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(450, 0));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(createCommandPreviewPanel());
        topPanel.add(Box.createVerticalStrut(10));
        topPanel.add(createRunButtonPanel());
        topPanel.add(Box.createVerticalStrut(10));

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(createOutputPanel(), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createAdvancedDirPanel() {
        JPanel content = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        gbc.gridx = 0;
        gbc.gridy = row;
        content.add(new JLabel(LocaleUtil.getLocaleString("GuiLauncher.AsRoot", "As Root:")), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        asRootField = createTextField();
        content.add(asRootField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        content.add(new JLabel(LocaleUtil.getLocaleString("GuiLauncher.ExcelDirs", "Excel Dirs:")), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        excelDirsField = createTextField();
        content.add(excelDirsField, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        content.add(new JLabel(LocaleUtil.getLocaleString("GuiLauncher.JsonDirs", "Json Dirs:")), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        jsonDirsField = createTextField();
        content.add(jsonDirsField, gbc);

        JButton toggleButton = new JButton("▶ " + LocaleUtil.getLocaleString("GuiLauncher.AdvancedDirectories", "Advanced Directories"));
        toggleButton.addActionListener(e -> {
            boolean visible = !content.isVisible();
            content.setVisible(visible);
            String prefix = visible ? "▼ " : "▶ ";
            toggleButton.setText(prefix + LocaleUtil.getLocaleString("GuiLauncher.AdvancedDirectories", "Advanced Directories"));
        });

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(toggleButton, BorderLayout.NORTH);
        wrapper.add(content, BorderLayout.CENTER);
        content.setVisible(false);

        return wrapper;
    }

    private JPanel createI18nPanel() {
        JPanel content = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        JRadioButton i18nNoneRadio = new JRadioButton(LocaleUtil.getLocaleString("GuiLauncher.None", "None"));
        i18nNoneRadio.setSelected(true);
        i18nNoneRadio.addActionListener(e -> onI18nModeChanged());

        gbc.gridx = 0;
        gbc.gridy = row;
        content.add(i18nNoneRadio, gbc);
        row++;

        i18nFileRadio = new JRadioButton(LocaleUtil.getLocaleString("GuiLauncher.I18nFile", "I18n File:"));
        i18nFileRadio.addActionListener(e -> onI18nModeChanged());

        gbc.gridx = 0;
        gbc.gridy = row;
        content.add(i18nFileRadio, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        i18nfileField = createTextField();
        i18nfileField.setEnabled(false);
        content.add(i18nfileField, gbc);
        row++;

        langSwitchRadio = new JRadioButton(LocaleUtil.getLocaleString("GuiLauncher.LangSwitch", "Lang Switch:"));
        langSwitchRadio.addActionListener(e -> onI18nModeChanged());

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        content.add(langSwitchRadio, gbc);
        row++;

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        content.add(new JLabel(LocaleUtil.getLocaleString("GuiLauncher.Dir", "  Dir:")), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.5;
        langSwitchDirField = createTextField();
        langSwitchDirField.setEnabled(false);
        content.add(langSwitchDirField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        content.add(new JLabel(LocaleUtil.getLocaleString("GuiLauncher.DefaultLang", "Default Lang:")), gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.5;
        defaultLangField = new JTextField("zh_cn");
        defaultLangField.getDocument().addDocumentListener(new SimpleDocumentListener(this::updateCommandPreview));
        defaultLangField.setEnabled(false);
        content.add(defaultLangField, gbc);

        ButtonGroup group = new ButtonGroup();
        group.add(i18nNoneRadio);
        group.add(i18nFileRadio);
        group.add(langSwitchRadio);

        JButton toggleButton = new JButton("▶ " + LocaleUtil.getLocaleString("GuiLauncher.I18nConfiguration", "I18n Configuration"));
        toggleButton.addActionListener(e -> {
            boolean visible = !content.isVisible();
            content.setVisible(visible);
            String prefix = visible ? "▼ " : "▶ ";
            toggleButton.setText(prefix + LocaleUtil.getLocaleString("GuiLauncher.I18nConfiguration", "I18n Configuration"));
        });

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(toggleButton, BorderLayout.NORTH);
        wrapper.add(content, BorderLayout.CENTER);
        content.setVisible(false);

        return wrapper;
    }

    private void onI18nModeChanged() {
        i18nfileField.setEnabled(i18nFileRadio.isSelected());
        langSwitchDirField.setEnabled(langSwitchRadio.isSelected());
        defaultLangField.setEnabled(langSwitchRadio.isSelected());
        updateCommandPreview();
    }

    private JPanel createBasicParametersPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(LocaleUtil.getLocaleString("GuiLauncher.BasicConfiguration", "Basic Configuration")));

        JPanel datadirPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        datadirPanel.add(new JLabel(LocaleUtil.getLocaleString("GuiLauncher.DataDir", "Data Dir:*")));
        datadirField = createTextField();
        datadirField.setPreferredSize(new Dimension(300, 25));
        datadirPanel.add(datadirField);
        JButton browseButton = new JButton(LocaleUtil.getLocaleString("GuiLauncher.Browse", "Browse..."));
        browseButton.addActionListener(this::browseDataDir);
        datadirPanel.add(browseButton);
        panel.add(datadirPanel);
        panel.add(Box.createVerticalStrut(3));

        JPanel encodingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        encodingPanel.add(new JLabel(LocaleUtil.getLocaleString("GuiLauncher.Encoding", "Encoding:")));
        encodingField = new JTextField("GBK");
        encodingField.setPreferredSize(new Dimension(150, 25));
        encodingField.getDocument().addDocumentListener(new SimpleDocumentListener(this::updateCommandPreview));
        encodingPanel.add(encodingField);
        panel.add(encodingPanel);
        panel.add(Box.createVerticalStrut(3));

        JPanel headRowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headRowPanel.add(new JLabel(LocaleUtil.getLocaleString("GuiLauncher.HeadRow", "Head Row:")));
        headRowField = new JTextField("2");
        headRowField.setPreferredSize(new Dimension(100, 25));
        headRowField.getDocument().addDocumentListener(new SimpleDocumentListener(this::updateCommandPreview));
        headRowPanel.add(headRowField);
        panel.add(headRowPanel);
        panel.add(Box.createVerticalStrut(3));

        JPanel usePoiPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        usePoiPanel.add(new JLabel(LocaleUtil.getLocaleString("GuiLauncher.UsePOI", "Use POI:")));
        usePoiCheckBox = new JCheckBox();
        usePoiCheckBox.addActionListener(e -> updateCommandPreview());
        usePoiPanel.add(usePoiCheckBox);
        panel.add(usePoiPanel);

        panel.add(Box.createVerticalStrut(5));
        panel.add(createAdvancedDirPanel());

        panel.add(Box.createVerticalStrut(5));
        panel.add(createI18nPanel());

        panel.add(Box.createVerticalStrut(5));

        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        verboseCheckBox = createCheckBox(LocaleUtil.getLocaleString("GuiLauncher.Verbose", "Verbose (-v)"));
        verbose2CheckBox = createCheckBox(LocaleUtil.getLocaleString("GuiLauncher.Verbose2", "Verbose2 (-vv)"));
        profileCheckBox = createCheckBox(LocaleUtil.getLocaleString("GuiLauncher.Profile", "Profile (-p)"));
        profileGcCheckBox = createCheckBox(LocaleUtil.getLocaleString("GuiLauncher.ProfileGC", "Profile+GC (-pp)"));
        noWarnCheckBox = createCheckBox(LocaleUtil.getLocaleString("GuiLauncher.NoWarn", "No Warn"));
        weakWarnCheckBox = createCheckBox(LocaleUtil.getLocaleString("GuiLauncher.WeakWarn", "Weak Warn"));

        optionsPanel.add(verboseCheckBox);
        optionsPanel.add(verbose2CheckBox);
        optionsPanel.add(profileCheckBox);
        optionsPanel.add(profileGcCheckBox);
        optionsPanel.add(noWarnCheckBox);
        optionsPanel.add(weakWarnCheckBox);

        panel.add(optionsPanel);

        datadirField.setText(".");

        return panel;
    }

    private JCheckBox createCheckBox(String label) {
        JCheckBox checkBox = new JCheckBox(label);
        checkBox.addActionListener(e -> updateCommandPreview());
        return checkBox;
    }

    private JPanel createToolsPanel() {
        toolsPanel = new JPanel();
        toolsPanel.setLayout(new BoxLayout(toolsPanel, BoxLayout.Y_AXIS));
        toolsPanel.setBorder(BorderFactory.createTitledBorder("Tools"));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        Map<String, Tools.ToolProvider> toolProviders = Tools.getAllProviders();
        JComboBox<String> toolCombo = new JComboBox<>();
        String selectPrompt = LocaleUtil.getLocaleString("GuiLauncher.SelectTool", "Select tool...");
        toolCombo.addItem(selectPrompt);
        for (String toolName : toolProviders.keySet()) {
            toolCombo.addItem(toolName);
        }

        JButton addButton = new JButton(LocaleUtil.getLocaleString("GuiLauncher.Add", "Add"));
        addButton.addActionListener(e -> {
            String selected = (String) toolCombo.getSelectedItem();
            if (selected != null && !selected.equals(selectPrompt)) {
                addParameterPanel(toolPanels, toolsPanel, "tool", selected);
                toolCombo.setSelectedItem(selectPrompt);
            }
        });

        buttonPanel.add(toolCombo);
        buttonPanel.add(addButton);
        toolsPanel.add(buttonPanel);

        JPanel container = new JPanel(new BorderLayout());
        container.add(toolsPanel, BorderLayout.NORTH);
        return container;
    }

    private JPanel createGeneratorsPanel() {
        generatorsPanel = new JPanel();
        generatorsPanel.setLayout(new BoxLayout(generatorsPanel, BoxLayout.Y_AXIS));
        generatorsPanel.setBorder(BorderFactory.createTitledBorder("Generators"));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        Map<String, Generators.GeneratorProvider> genProviders = Generators.getAllProviders();
        JComboBox<String> genCombo = new JComboBox<>();
        String selectPrompt = LocaleUtil.getLocaleString("GuiLauncher.SelectGenerator", "Select generator...");
        genCombo.addItem(selectPrompt);
        for (String genName : genProviders.keySet()) {
            genCombo.addItem(genName);
        }

        JButton addButton = new JButton(LocaleUtil.getLocaleString("GuiLauncher.Add", "Add"));
        addButton.addActionListener(e -> {
            String selected = (String) genCombo.getSelectedItem();
            if (selected != null && !selected.equals(selectPrompt)) {
                addParameterPanel(generatorPanels, generatorsPanel, "gen", selected);
                genCombo.setSelectedItem(selectPrompt);
            }
        });

        buttonPanel.add(genCombo);
        buttonPanel.add(addButton);
        generatorsPanel.add(buttonPanel);

        JPanel container = new JPanel(new BorderLayout());
        container.add(generatorsPanel, BorderLayout.NORTH);
        return container;
    }

    private JPanel createCommandPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(LocaleUtil.getLocaleString("GuiLauncher.CommandPreview", "Command Preview")));
        panel.setPreferredSize(new Dimension(0, 80));

        commandPreview = new JTextArea();
        commandPreview.setEditable(false);
        commandPreview.setBackground(new Color(240, 240, 240));
        commandPreview.setLineWrap(true);
        commandPreview.setWrapStyleWord(true);
        commandPreview.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));

        JScrollPane scrollPane = new JScrollPane(commandPreview);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createRunButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JButton copyButton = new JButton(LocaleUtil.getLocaleString("GuiLauncher.Copy", "Copy"));
        copyButton.addActionListener(e -> {
            String command = commandPreview.getText();
            if (!command.isEmpty()) {
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(command), null);
            }
        });
        panel.add(copyButton);

        runButton = new JButton(LocaleUtil.getLocaleString("GuiLauncher.Run", "Run"));
        runButton.addActionListener(this::runGeneration);
        panel.add(runButton);

        JButton clearButton = new JButton(LocaleUtil.getLocaleString("GuiLauncher.ClearOutput", "Clear Output"));
        clearButton.addActionListener(e -> outputArea.setText(""));
        panel.add(clearButton);

        return panel;
    }

    private JPanel createOutputPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(LocaleUtil.getLocaleString("GuiLauncher.Output", "Output")));

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        outputArea.setBackground(new Color(250, 250, 250));

        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void addParameterPanel(List<ParameterPanel> panels, JPanel container, String type, String name) {
        ParameterPanel panel = new ParameterPanel(type, name);
        panels.add(panel);
        container.add(panel.getPanel());
        container.revalidate();
        container.repaint();
        updateCommandPreview();
    }

    private void browseDataDir(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle(LocaleUtil.getLocaleString("GuiLauncher.SelectDataDirectory", "Select Data Directory"));
        if (chooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
            datadirField.setText(chooser.getSelectedFile().getAbsolutePath());
            updateCommandPreview();
        }
    }
    private void updateCommandPreview() {
        StringBuilder cmd = new StringBuilder("java -jar cfggen.jar");

        appendQuotedIfNotEmpty(cmd, "-datadir", datadirField.getText().trim());
        appendIfNotDefault(cmd, "-encoding", encodingField.getText().trim(), "GBK");
        appendIfNotDefault(cmd, "-headrow", headRowField.getText().trim(), "2");

        if (usePoiCheckBox.isSelected()) {
            cmd.append(" -usepoi");
        }

        appendQuotedIfNotEmpty(cmd, "-asroot", asRootField.getText().trim());
        appendQuotedIfNotEmpty(cmd, "-exceldirs", excelDirsField.getText().trim());
        appendQuotedIfNotEmpty(cmd, "-jsondirs", jsonDirsField.getText().trim());

        appendI18nArgs(cmd);

        if (verboseCheckBox.isSelected()) {
            cmd.append(" -v");
        }
        if (verbose2CheckBox.isSelected()) {
            cmd.append(" -vv");
        }
        if (profileCheckBox.isSelected()) {
            cmd.append(" -p");
        }
        if (profileGcCheckBox.isSelected()) {
            cmd.append(" -pp");
        }
        if (noWarnCheckBox.isSelected()) {
            cmd.append(" -nowarn");
        }
        if (weakWarnCheckBox.isSelected()) {
            cmd.append(" -weakwarn");
        }

        for (ParameterPanel panel : toolPanels) {
            String panelCmd = panel.buildCommand();
            if (!panelCmd.isEmpty()) {
                cmd.append(" -tool ").append(panelCmd);
            }
        }

        for (ParameterPanel panel : generatorPanels) {
            String panelCmd = panel.buildCommand();
            if (!panelCmd.isEmpty()) {
                cmd.append(" -gen ").append(panelCmd);
            }
        }

        commandPreview.setText(cmd.toString());
    }

    private void appendQuotedIfNotEmpty(StringBuilder cmd, String flag, String value) {
        if (!value.isEmpty()) {
            cmd.append(" ").append(flag).append(" \"").append(value).append("\"");
        }
    }

    private void appendIfNotDefault(StringBuilder cmd, String flag, String value, String defaultValue) {
        if (!value.isEmpty() && !value.equals(defaultValue)) {
            cmd.append(" ").append(flag).append(" ").append(value);
        }
    }

    private void appendI18nArgs(StringBuilder cmd) {
        if (i18nFileRadio.isSelected()) {
            String i18nfile = i18nfileField.getText().trim();
            if (!i18nfile.isEmpty()) {
                cmd.append(" -i18nfile \"").append(i18nfile).append("\"");
            }
        } else if (langSwitchRadio.isSelected()) {
            String langSwitchDir = langSwitchDirField.getText().trim();
            if (!langSwitchDir.isEmpty()) {
                cmd.append(" -langswitchdir \"").append(langSwitchDir).append("\"");
            }
            String defaultLang = defaultLangField.getText().trim();
            if (!defaultLang.isEmpty() && !defaultLang.equals("zh_cn")) {
                cmd.append(" -defaultlang ").append(defaultLang);
            }
        }
    }

    private void runGeneration(ActionEvent e) {
        if (datadirField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, LocaleUtil.getLocaleString("GuiLauncher.PleaseSetDataDirectory", "Please set data directory"),
                LocaleUtil.getLocaleString("GuiLauncher.Error", "Error"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (generatorPanels.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, LocaleUtil.getLocaleString("GuiLauncher.PleaseAddAtLeastOneGenerator", "Please add at least one generator"),
                LocaleUtil.getLocaleString("GuiLauncher.Error", "Error"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        runButton.setEnabled(false);
        outputArea.setText("");

        redirectOutput();

        SwingWorker<Integer, String> worker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() {
                return Main.runWithCatch(buildCommandLineArgs().toArray(new String[0]));
            }

            @Override
            protected void done() {
                restoreOutput();
                runButton.setEnabled(true);
            }
        };
        worker.execute();
    }

    private List<String> buildCommandLineArgs() {
        List<String> args = new ArrayList<>();

        addIfNotEmpty(args, "-datadir", datadirField.getText().trim());
        addIfNotEmpty(args, "-encoding", encodingField.getText().trim());
        addIfNotEmpty(args, "-headrow", headRowField.getText().trim());

        if (usePoiCheckBox.isSelected()) {
            args.add("-usepoi");
        }

        addIfNotEmpty(args, "-asroot", asRootField.getText().trim());
        addIfNotEmpty(args, "-exceldirs", excelDirsField.getText().trim());
        addIfNotEmpty(args, "-jsondirs", jsonDirsField.getText().trim());

        addI18nArgs(args);

        if (verboseCheckBox.isSelected()) {
            args.add("-v");
        }
        if (verbose2CheckBox.isSelected()) {
            args.add("-vv");
        }
        if (profileCheckBox.isSelected()) {
            args.add("-p");
        }
        if (profileGcCheckBox.isSelected()) {
            args.add("-pp");
        }
        if (noWarnCheckBox.isSelected()) {
            args.add("-nowarn");
        }
        if (weakWarnCheckBox.isSelected()) {
            args.add("-weakwarn");
        }

        for (ParameterPanel panel : toolPanels) {
            String panelCmd = panel.buildCommand();
            if (!panelCmd.isEmpty()) {
                args.add("-tool");
                args.add(panelCmd);
            }
        }

        for (ParameterPanel panel : generatorPanels) {
            String panelCmd = panel.buildCommand();
            if (!panelCmd.isEmpty()) {
                args.add("-gen");
                args.add(panelCmd);
            }
        }

        return args;
    }

    private void addIfNotEmpty(List<String> args, String flag, String value) {
        if (!value.isEmpty()) {
            args.add(flag);
            args.add(value);
        }
    }

    private void addI18nArgs(List<String> args) {
        if (i18nFileRadio.isSelected()) {
            String i18nfile = i18nfileField.getText().trim();
            if (!i18nfile.isEmpty()) {
                args.add("-i18nfile");
                args.add(i18nfile);
            }
        } else if (langSwitchRadio.isSelected()) {
            String langSwitchDir = langSwitchDirField.getText().trim();
            if (!langSwitchDir.isEmpty()) {
                args.add("-langswitchdir");
                args.add(langSwitchDir);
            }
            String defaultLang = defaultLangField.getText().trim();
            if (!defaultLang.isEmpty()) {
                args.add("-defaultlang");
                args.add(defaultLang);
            }
        }
    }

    private void redirectOutput() {
        System.setOut(new PrintStream(new TextAreaOutputStream(outputArea), true));
        System.setErr(new PrintStream(new TextAreaOutputStream(outputArea), true));
        Logger.setPrinter(new GuiPrinter());
    }

    private void restoreOutput() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        Logger.setPrinter(originalPrinter);
    }

    private JTextField createTextField() {
        JTextField field = new JTextField();
        field.getDocument().addDocumentListener(new SimpleDocumentListener(this::updateCommandPreview));
        return field;
    }

    private JButton createDeleteButton(Runnable action) {
        JButton button = new JButton(LocaleUtil.getLocaleString("GuiLauncher.Remove", "Remove"));

        // 使用统一宽度，兼容中英文文本
        int buttonWidth = 100;

        button.setPreferredSize(new Dimension(buttonWidth, 25));
        button.setMaximumSize(new Dimension(buttonWidth, 25));
        button.setMinimumSize(new Dimension(buttonWidth, 25));
        button.setForeground(new Color(200, 50, 50));
        button.addActionListener(e -> action.run());
        return button;
    }

    private void removeParameterPanel(List<ParameterPanel> panels, JPanel container, ParameterPanel panel) {
        panels.remove(panel);
        container.remove(panel.getPanel());
        container.revalidate();
        container.repaint();
        updateCommandPreview();
    }

    private class ParameterPanel {
        private final String name;
        private final String type;
        private final JPanel panel;
        private final Map<String, JComponent> paramComponents = new LinkedHashMap<>();

        public ParameterPanel(String type, String name) {
            this.type = type;
            this.name = name;
            this.panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createTitledBorder(name));

            ParameterInfoCollector info = new ParameterInfoCollector(type, name);

            if ("tool".equals(type)) {
                Tools.ToolProvider provider = Tools.getAllProviders().get(name);
                if (provider != null) {
                    provider.create(info);
                }
            } else {
                Generators.GeneratorProvider provider = Generators.getAllProviders().get(name);
                if (provider != null) {
                    provider.create(info);
                }
            }

            buildParameterComponents(info);
        }

        private void buildParameterComponents(ParameterInfoCollector info) {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(3, 3, 3, 3);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            int row = 0;
            int columns = 3;
            int col = 0;

            for (var entry : info.getInfos().entrySet()) {
                String paramName = entry.getKey();
                var paramInfo = entry.getValue();

                gbc.gridx = col * 2;
                gbc.gridy = row;
                gbc.weightx = 0;
                gbc.insets = new Insets(3, 3, 3, 0);
                panel.add(new JLabel(paramName + ":"), gbc);

                gbc.gridx = col * 2 + 1;
                gbc.weightx = 1.0;
                gbc.insets = new Insets(3, 2, 3, 3);

                JComponent component;
                if (paramInfo.isFlag()) {
                    JCheckBox checkBox = new JCheckBox();
                    checkBox.addActionListener(e -> updateCommandPreview());
                    component = checkBox;
                } else {
                    JTextField textField = new JTextField(paramInfo.def() != null ? paramInfo.def() : "");
                    textField.getDocument().addDocumentListener(new SimpleDocumentListener(GuiLauncher.this::updateCommandPreview));
                    component = textField;
                }

                panel.add(component, gbc);
                paramComponents.put(paramName, component);

                col++;
                if (col >= columns) {
                    col = 0;
                    row++;
                }
            }

            gbc.gridx = 0;
            gbc.gridy = row + 1;
            gbc.gridwidth = 1;
            gbc.weightx = 0;
            gbc.fill = GridBagConstraints.NONE;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.insets = new Insets(3, 3, 3, 3);

            List<ParameterPanel> parentList = "tool".equals(type) ? toolPanels : generatorPanels;
            JPanel parentContainer = "tool".equals(type) ? toolsPanel : generatorsPanel;
            JButton deleteButton = createDeleteButton(() -> removeParameterPanel(parentList, parentContainer, this));
            panel.add(deleteButton, gbc);

            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(Box.createHorizontalGlue(), gbc);
        }

        public JPanel getPanel() {
            return panel;
        }

        public String buildCommand() {
            StringBuilder cmd = new StringBuilder(name);
            for (var entry : paramComponents.entrySet()) {
                String paramName = entry.getKey();
                JComponent component = entry.getValue();

                if (component instanceof JCheckBox checkBox) {
                    if (checkBox.isSelected()) {
                        cmd.append(",").append(paramName);
                    }
                } else if (component instanceof JTextField textField) {
                    String value = textField.getText().trim();
                    if (!value.isEmpty()) {
                        cmd.append(",").append(paramName).append("=").append(value);
                    }
                }
            }
            return cmd.toString();
        }
    }

    private class GuiPrinter implements Logger.Printer {
        @Override
        public void printf(String fmt, Object... args) {
            try {
                String message = String.format(fmt, args);
                SwingUtilities.invokeLater(() -> {
                    outputArea.append(message);
                    outputArea.setCaretPosition(outputArea.getDocument().getLength());
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> {
                    outputArea.append("Format error: " + fmt);
                    for (Object arg : args) {
                        outputArea.append(" " + arg);
                    }
                    outputArea.append(System.lineSeparator());
                    outputArea.setCaretPosition(outputArea.getDocument().getLength());
                });
            }
        }
    }

    private static class TextAreaOutputStream extends java.io.OutputStream {
        private final JTextArea textArea;

        public TextAreaOutputStream(JTextArea textArea) {
            this.textArea = textArea;
        }

        @Override
        public void write(int b) {
            SwingUtilities.invokeLater(() -> {
                textArea.append(String.valueOf((char) b));
                textArea.setCaretPosition(textArea.getDocument().getLength());
            });
        }

        @Override
        public void write(byte[] b, int off, int len) {
            String text = new String(b, off, len);
            SwingUtilities.invokeLater(() -> {
                textArea.append(text);
                textArea.setCaretPosition(textArea.getDocument().getLength());
            });
        }
    }

    private record SimpleDocumentListener(Runnable callback) implements DocumentListener {

        @Override
            public void insertUpdate(DocumentEvent e) {
                callback.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                callback.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                callback.run();
            }
        }
}
