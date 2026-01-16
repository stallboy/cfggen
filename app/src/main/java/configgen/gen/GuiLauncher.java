package configgen.gen;

import configgen.gen.ui.UIConstants;
import configgen.gen.ui.command.CommandLineBuilder;
import configgen.gen.ui.panel.ParameterPanelItem;
import configgen.gen.ui.panel.ProviderPanelFactory;
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

    private final List<ParameterPanelItem> toolPanels = new ArrayList<>();
    private final List<ParameterPanelItem> generatorPanels = new ArrayList<>();
    private final CommandLineBuilder commandBuilder = new CommandLineBuilder(this);

    // 面板引用，用于添加参数面板
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
        mainFrame.setSize(UIConstants.WINDOW_WIDTH, UIConstants.WINDOW_HEIGHT);
        mainFrame.setLocationRelativeTo(null);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(UIConstants.SPLIT_PANE_DIVIDER_LOCATION);
        splitPane.setResizeWeight(UIConstants.SPLIT_PANE_RESIZE_WEIGHT);

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        JScrollPane leftScroll = new JScrollPane(leftPanel);
        leftScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        JPanel rightPanel = createRightPanel();

        splitPane.setLeftComponent(leftScroll);
        splitPane.setRightComponent(rightPanel);
        mainFrame.add(splitPane, BorderLayout.CENTER);

        leftPanel.add(createToolsPanel());
        leftPanel.add(Box.createVerticalStrut(UIConstants.VERTICAL_STRUT_LARGE));
        leftPanel.add(createBasicParametersPanel());
        leftPanel.add(Box.createVerticalStrut(UIConstants.VERTICAL_STRUT_LARGE));
        leftPanel.add(createGeneratorsPanel());

        mainFrame.setVisible(true);
    }

    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(UIConstants.RIGHT_PANEL_PREFERRED_WIDTH, 0));

        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(createCommandPreviewPanel());
        topPanel.add(Box.createVerticalStrut(UIConstants.VERTICAL_STRUT_LARGE));
        topPanel.add(createRunButtonPanel());
        topPanel.add(Box.createVerticalStrut(UIConstants.VERTICAL_STRUT_LARGE));

        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(createOutputPanel(), BorderLayout.CENTER);

        return panel;
    }

    private JPanel createAdvancedDirPanel() {
        JPanel content = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = UIConstants.PANEL_INSETS;
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
        gbc.insets = UIConstants.PANEL_INSETS;
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
        defaultLangField = new JTextField(UIConstants.DEFAULT_LANG);
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
        datadirField.setPreferredSize(UIConstants.TEXT_FIELD_DATADIR_SIZE);
        datadirPanel.add(datadirField);
        JButton browseButton = new JButton(LocaleUtil.getLocaleString("GuiLauncher.Browse", "Browse..."));
        browseButton.addActionListener(this::browseDataDir);
        datadirPanel.add(browseButton);
        panel.add(datadirPanel);
        panel.add(Box.createVerticalStrut(UIConstants.VERTICAL_STRUT_SMALL));

        JPanel encodingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        encodingPanel.add(new JLabel(LocaleUtil.getLocaleString("GuiLauncher.Encoding", "Encoding:")));
        encodingField = new JTextField(UIConstants.DEFAULT_ENCODING);
        encodingField.setPreferredSize(UIConstants.TEXT_FIELD_ENCODING_SIZE);
        encodingField.getDocument().addDocumentListener(new SimpleDocumentListener(this::updateCommandPreview));
        encodingPanel.add(encodingField);
        panel.add(encodingPanel);
        panel.add(Box.createVerticalStrut(UIConstants.VERTICAL_STRUT_SMALL));

        JPanel headRowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headRowPanel.add(new JLabel(LocaleUtil.getLocaleString("GuiLauncher.HeadRow", "Head Row:")));
        headRowField = new JTextField(UIConstants.DEFAULT_HEAD_ROW);
        headRowField.setPreferredSize(UIConstants.TEXT_FIELD_HEADROW_SIZE);
        headRowField.getDocument().addDocumentListener(new SimpleDocumentListener(this::updateCommandPreview));
        headRowPanel.add(headRowField);
        panel.add(headRowPanel);
        panel.add(Box.createVerticalStrut(UIConstants.VERTICAL_STRUT_SMALL));

        JPanel usePoiPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        usePoiPanel.add(new JLabel(LocaleUtil.getLocaleString("GuiLauncher.UsePOI", "Use POI:")));
        usePoiCheckBox = new JCheckBox();
        usePoiCheckBox.addActionListener(e -> updateCommandPreview());
        usePoiPanel.add(usePoiCheckBox);
        panel.add(usePoiPanel);

        panel.add(Box.createVerticalStrut(UIConstants.VERTICAL_STRUT_MEDIUM));
        panel.add(createAdvancedDirPanel());

        panel.add(Box.createVerticalStrut(UIConstants.VERTICAL_STRUT_MEDIUM));
        panel.add(createI18nPanel());

        panel.add(Box.createVerticalStrut(UIConstants.VERTICAL_STRUT_MEDIUM));

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
        JPanel panel = ProviderPanelFactory.createToolsPanel(toolPanels,
                (panels, name) -> addParameterPanel(panels, toolsPanel, "tool", name));
        // 提取toolsPanel引用，用于后续操作
        toolsPanel = (JPanel) ((JPanel) panel.getComponent(0)).getComponent(0);
        return panel;
    }

    private JPanel createGeneratorsPanel() {
        JPanel panel = ProviderPanelFactory.createGeneratorsPanel(generatorPanels,
                (panels, name) -> addParameterPanel(panels, generatorsPanel, "gen", name));
        // 提取generatorsPanel引用，用于后续操作
        generatorsPanel = (JPanel) ((JPanel) panel.getComponent(0)).getComponent(0);
        return panel;
    }

    private JPanel createCommandPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(LocaleUtil.getLocaleString("GuiLauncher.CommandPreview", "Command Preview")));
        panel.setPreferredSize(new Dimension(0, UIConstants.COMMAND_PREVIEW_HEIGHT));

        commandPreview = new JTextArea();
        commandPreview.setEditable(false);
        commandPreview.setBackground(UIConstants.COMMAND_PREVIEW_BG);
        commandPreview.setLineWrap(true);
        commandPreview.setWrapStyleWord(true);
        commandPreview.setFont(new Font(Font.MONOSPACED, Font.PLAIN, UIConstants.COMMAND_PREVIEW_FONT_SIZE));

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
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, UIConstants.OUTPUT_AREA_FONT_SIZE));
        outputArea.setBackground(UIConstants.OUTPUT_AREA_BG);

        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    private void addParameterPanel(List<ParameterPanelItem> panels, JPanel container, String type, String name) {
        ParameterPanelItem panel = new ParameterPanelItem(type, name,
                new SimpleDocumentListener(this::updateCommandPreview));

        panel.setChangeListener(this::updateCommandPreview);
        panel.setDeleteListener(() -> removeParameterPanel(panels, container, panel));

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
        commandPreview.setText(commandBuilder.buildPreviewCommand());
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
        return commandBuilder.buildArgs();
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

    private void removeParameterPanel(List<ParameterPanelItem> panels, JPanel container, ParameterPanelItem panel) {
        panels.remove(panel);
        container.remove(panel.getPanel());
        container.revalidate();
        container.repaint();
        updateCommandPreview();
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

    // Getter方法供CommandLineBuilder和I18nConfig使用
    public JTextField getDatadirField() {
        return datadirField;
    }

    public JTextField getEncodingField() {
        return encodingField;
    }

    public JTextField getHeadRowField() {
        return headRowField;
    }

    public JCheckBox getUsePoiCheckBox() {
        return usePoiCheckBox;
    }

    public JTextField getAsRootField() {
        return asRootField;
    }

    public JTextField getExcelDirsField() {
        return excelDirsField;
    }

    public JTextField getJsonDirsField() {
        return jsonDirsField;
    }

    public JCheckBox getVerboseCheckBox() {
        return verboseCheckBox;
    }

    public JCheckBox getVerbose2CheckBox() {
        return verbose2CheckBox;
    }

    public JCheckBox getProfileCheckBox() {
        return profileCheckBox;
    }

    public JCheckBox getProfileGcCheckBox() {
        return profileGcCheckBox;
    }

    public JCheckBox getNoWarnCheckBox() {
        return noWarnCheckBox;
    }

    public JCheckBox getWeakWarnCheckBox() {
        return weakWarnCheckBox;
    }

    public List<ParameterPanelItem> getToolPanels() {
        return toolPanels;
    }

    public List<ParameterPanelItem> getGeneratorPanels() {
        return generatorPanels;
    }

    // I18n相关的getter方法供CommandLineBuilder使用
    public JRadioButton getI18nFileRadio() {
        return i18nFileRadio;
    }

    public JRadioButton getLangSwitchRadio() {
        return langSwitchRadio;
    }

    public JTextField getI18nfileField() {
        return i18nfileField;
    }

    public JTextField getLangSwitchDirField() {
        return langSwitchDirField;
    }

    public JTextField getDefaultLangField() {
        return defaultLangField;
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
