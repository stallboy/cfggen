package configgen.gen;

import configgen.util.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.PrintStream;
import java.util.*;
import java.util.List;

/**
 * Swing GUI 启动器
 * 当没有命令行参数时自动启动，提供图形界面配置所有参数
 */
public class GuiLauncher {
    private JFrame mainFrame;
    private JTextArea outputArea;
    private JTextArea commandPreview;

    // 基础参数组件
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
    private JRadioButton i18nNoneRadio;
    private JRadioButton i18nFileRadio;
    private JRadioButton langSwitchRadio;
    private JCheckBox verboseCheckBox;
    private JCheckBox verbose2CheckBox;
    private JCheckBox profileCheckBox;
    private JCheckBox profileGcCheckBox;
    private JCheckBox noWarnCheckBox;
    private JCheckBox weakWarnCheckBox;

    // Tools 和 Generators 面板
    private final List<ToolParameterPanel> toolPanels = new ArrayList<>();
    private final List<GeneratorParameterPanel> generatorPanels = new ArrayList<>();

    private JPanel toolsPanel;
    private JPanel generatorsPanel;

    // 运行按钮
    private JButton runButton;

    // 保存原始的 System.out 和 System.err
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private final Logger.Printer originalPrinter = Logger.getPrinter();

    // 国际化支持
    private final boolean isChineseLocale = Locale.getDefault().getLanguage().equals("zh");

    /**
     * 根据系统locale选择中英文
     */
    private String text(String zh, String en) {
        return isChineseLocale ? zh : en;
    }

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
        // 获取当前工作目录
        String currentDir = System.getProperty("user.dir");
        // 创建标题，包含目录路径
        String title = text("配置生成器", "Configuration Generator") + " - " + currentDir;

        // 创建主窗口
        mainFrame = new JFrame(title);
        mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        mainFrame.setSize(1400, 800);
        mainFrame.setLocationRelativeTo(null);

        // 使用 JSplitPane 创建左右分栏
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(900);
        splitPane.setResizeWeight(0.6);

        // 左侧：可滚动的配置区
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        JScrollPane leftScroll = new JScrollPane(leftPanel);
        leftScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        // 右侧：固定的输出区
        JPanel rightPanel = createRightPanel();

        splitPane.setLeftComponent(leftScroll);
        splitPane.setRightComponent(rightPanel);
        mainFrame.add(splitPane, BorderLayout.CENTER);

        // 按新顺序添加面板到左侧
        leftPanel.add(createToolsPanel());           // 1. Tools 放最上面
        leftPanel.add(Box.createVerticalStrut(10));  // 增加到10像素
        leftPanel.add(createBasicParametersPanel()); // 2. 基础配置
        leftPanel.add(Box.createVerticalStrut(10));  // 增加到10像素
        leftPanel.add(createGeneratorsPanel());      // 3. Generators

        // 显示窗口
        mainFrame.setVisible(true);
    }

    /**
     * 创建右侧固定面板
     */
    private JPanel createRightPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setPreferredSize(new Dimension(450, 0));

        // 顶部区域：命令预览 + 运行按钮
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(createCommandPreviewPanel());
        topPanel.add(Box.createVerticalStrut(10));
        topPanel.add(createRunButtonPanel());
        topPanel.add(Box.createVerticalStrut(10));

        // 添加到顶部（固定大小）
        panel.add(topPanel, BorderLayout.NORTH);

        // 输出面板：添加到中间，自动填充剩余空间
        panel.add(createOutputPanel(), BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建可折叠的高级目录配置面板
     */
    private JPanel createAdvancedDirPanel() {
        JPanel content = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // asRoot
        gbc.gridx = 0;
        gbc.gridy = row;
        content.add(new JLabel("As Root:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        asRootField = createTextField();
        content.add(asRootField, gbc);
        row++;

        // excelDirs
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        content.add(new JLabel("Excel Dirs:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        excelDirsField = createTextField();
        content.add(excelDirsField, gbc);
        row++;

        // jsonDirs
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        content.add(new JLabel("Json Dirs:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        jsonDirsField = createTextField();
        content.add(jsonDirsField, gbc);

        JButton toggleButton = new JButton("▶ " + text("高级目录配置", "Advanced Directories"));
        toggleButton.addActionListener(e -> {
            boolean visible = !content.isVisible();
            content.setVisible(visible);
            toggleButton.setText(visible ? "▼ " + text("高级目录配置", "Advanced Directories")
                                       : "▶ " + text("高级目录配置", "Advanced Directories"));
        });

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(toggleButton, BorderLayout.NORTH);
        wrapper.add(content, BorderLayout.CENTER);
        content.setVisible(false); // 默认折叠

        return wrapper;
    }

    /**
     * 创建可折叠的国际化选项面板
     */
    private JPanel createI18nPanel() {
        JPanel content = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        int row = 0;

        // 选项0：None (无需国际化)
        i18nNoneRadio = new JRadioButton(text("无需", "None"));
        i18nNoneRadio.setSelected(true);  // 默认选中
        i18nNoneRadio.addActionListener(e -> onI18nModeChanged());

        gbc.gridx = 0;
        gbc.gridy = row;
        content.add(i18nNoneRadio, gbc);
        row++;

        // 选项1：I18n File
        i18nFileRadio = new JRadioButton(text("I18n 文件:", "I18n File:"));
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

        // 选项2：Lang Switch
        langSwitchRadio = new JRadioButton(text("语言切换:", "Lang Switch:"));
        langSwitchRadio.addActionListener(e -> onI18nModeChanged());

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        content.add(langSwitchRadio, gbc);
        row++;

        // Dir 和 Default Lang 在同一行
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0;
        content.add(new JLabel(text("  目录:", "  Dir:")), gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.5;
        langSwitchDirField = createTextField();
        langSwitchDirField.setEnabled(false);
        content.add(langSwitchDirField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        content.add(new JLabel(text("默认语言:", "Default Lang:")), gbc);

        gbc.gridx = 3;
        gbc.weightx = 0.5;
        defaultLangField = new JTextField("zh_cn");
        defaultLangField.getDocument().addDocumentListener(new SimpleDocumentListener(this::updateCommandPreview));
        defaultLangField.setEnabled(false);
        content.add(defaultLangField, gbc);

        // 互斥分组
        ButtonGroup group = new ButtonGroup();
        group.add(i18nNoneRadio);   // 新增
        group.add(i18nFileRadio);
        group.add(langSwitchRadio);

        JButton toggleButton = new JButton("▶ " + text("国际化配置", "I18n Configuration"));
        toggleButton.addActionListener(e -> {
            boolean visible = !content.isVisible();
            content.setVisible(visible);
            toggleButton.setText(visible ? "▼ " + text("国际化配置", "I18n Configuration")
                                       : "▶ " + text("国际化配置", "I18n Configuration"));
        });

        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(toggleButton, BorderLayout.NORTH);
        wrapper.add(content, BorderLayout.CENTER);
        content.setVisible(false); // 默认折叠

        return wrapper;
    }

    /**
     * 国际化模式切换处理
     */
    private void onI18nModeChanged() {
        boolean useI18nNone = i18nNoneRadio.isSelected();
        boolean useI18nFile = i18nFileRadio.isSelected();
        boolean useLangSwitch = langSwitchRadio.isSelected();

        i18nfileField.setEnabled(useI18nFile);
        langSwitchDirField.setEnabled(useLangSwitch);
        defaultLangField.setEnabled(useLangSwitch);
        updateCommandPreview();
    }

    /**
     * 创建基础参数面板
     */
    private JPanel createBasicParametersPanel() {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder(text("基础配置", "Basic Configuration")));

        // 数据目录
        JPanel datadirPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        datadirPanel.add(new JLabel(text("数据目录:*", "Data Dir:*")));
        datadirField = createTextField();
        datadirField.setPreferredSize(new Dimension(300, 25));
        datadirPanel.add(datadirField);
        JButton browseButton = new JButton(text("浏览...", "Browse..."));
        browseButton.addActionListener(this::browseDataDir);
        datadirPanel.add(browseButton);
        panel.add(datadirPanel);
        panel.add(Box.createVerticalStrut(3));  // 3像素小间隔

        // 编码
        JPanel encodingPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        encodingPanel.add(new JLabel(text("编码:", "Encoding:")));
        encodingField = new JTextField("GBK");
        encodingField.setPreferredSize(new Dimension(150, 25));
        encodingField.getDocument().addDocumentListener(new SimpleDocumentListener(this::updateCommandPreview));
        encodingPanel.add(encodingField);
        panel.add(encodingPanel);
        panel.add(Box.createVerticalStrut(3));  // 3像素小间隔

        // 表头行
        JPanel headRowPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        headRowPanel.add(new JLabel(text("表头行:", "Head Row:")));
        headRowField = new JTextField("2");
        headRowField.setPreferredSize(new Dimension(100, 25));
        headRowField.getDocument().addDocumentListener(new SimpleDocumentListener(this::updateCommandPreview));
        headRowPanel.add(headRowField);
        panel.add(headRowPanel);
        panel.add(Box.createVerticalStrut(3));  // 3像素小间隔

        // usePoi
        JPanel usePoiPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        usePoiPanel.add(new JLabel(text("使用 POI:", "Use POI:")));
        usePoiCheckBox = new JCheckBox();
        usePoiCheckBox.addActionListener(e -> updateCommandPreview());
        usePoiPanel.add(usePoiCheckBox);
        panel.add(usePoiPanel);

        // 添加折叠面板和间隔
        panel.add(Box.createVerticalStrut(5));  // 5像素间隔
        panel.add(createAdvancedDirPanel());

        panel.add(Box.createVerticalStrut(5));  // 高级目录配置和国际化配置之间的间隔
        panel.add(createI18nPanel());

        panel.add(Box.createVerticalStrut(5));  // 国际化配置和选项之间的间隔

        // 选项复选框
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        verboseCheckBox = new JCheckBox(text("详细", "Verbose") + " (-v)");
        verboseCheckBox.addActionListener(e -> updateCommandPreview());
        verbose2CheckBox = new JCheckBox(text("详细2", "Verbose2") + " (-vv)");
        verbose2CheckBox.addActionListener(e -> updateCommandPreview());
        profileCheckBox = new JCheckBox(text("性能分析", "Profile") + " (-p)");
        profileCheckBox.addActionListener(e -> updateCommandPreview());
        profileGcCheckBox = new JCheckBox(text("性能分析+GC", "Profile+GC") + " (-pp)");
        profileGcCheckBox.addActionListener(e -> updateCommandPreview());
        noWarnCheckBox = new JCheckBox(text("无警告", "No Warn"));
        noWarnCheckBox.addActionListener(e -> updateCommandPreview());
        weakWarnCheckBox = new JCheckBox(text("弱警告", "Weak Warn"));
        weakWarnCheckBox.addActionListener(e -> updateCommandPreview());

        optionsPanel.add(verboseCheckBox);
        optionsPanel.add(verbose2CheckBox);
        optionsPanel.add(profileCheckBox);
        optionsPanel.add(profileGcCheckBox);
        optionsPanel.add(noWarnCheckBox);
        optionsPanel.add(weakWarnCheckBox);

        panel.add(optionsPanel);

        // 设置默认数据目录（必须在所有字段初始化完成后）
        datadirField.setText(".");

        return panel;
    }

    /**
     * 创建 Tools 面板
     */
    private JPanel createToolsPanel() {
        toolsPanel = new JPanel();
        toolsPanel.setLayout(new BoxLayout(toolsPanel, BoxLayout.Y_AXIS));
        toolsPanel.setBorder(BorderFactory.createTitledBorder("Tools"));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // 创建添加菜单
        Map<String, Tools.ToolProvider> toolProviders = Tools.getAllProviders();
        JComboBox<String> toolCombo = new JComboBox<>();
        toolCombo.addItem(text("选择工具...", "Select tool..."));
        for (String toolName : toolProviders.keySet()) {
            toolCombo.addItem(toolName);
        }

        JButton addButton = new JButton(text("添加", "Add"));
        addButton.addActionListener(e -> {
            String selected = (String) toolCombo.getSelectedItem();
            if (selected != null && !selected.equals(text("选择工具...", "Select tool..."))) {
                addToolPanel(selected);
                toolCombo.setSelectedItem(text("选择工具...", "Select tool..."));
            }
        });

        buttonPanel.add(toolCombo);
        buttonPanel.add(addButton);
        toolsPanel.add(buttonPanel);

        JPanel container = new JPanel(new BorderLayout());
        container.add(toolsPanel, BorderLayout.NORTH);
        return container;
    }

    /**
     * 创建 Generators 面板
     */
    private JPanel createGeneratorsPanel() {
        generatorsPanel = new JPanel();
        generatorsPanel.setLayout(new BoxLayout(generatorsPanel, BoxLayout.Y_AXIS));
        generatorsPanel.setBorder(BorderFactory.createTitledBorder("Generators"));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // 创建添加菜单
        Map<String, Generators.GeneratorProvider> genProviders = Generators.getAllProviders();
        JComboBox<String> genCombo = new JComboBox<>();
        genCombo.addItem(text("选择生成器...", "Select generator..."));
        for (String genName : genProviders.keySet()) {
            genCombo.addItem(genName);
        }

        JButton addButton = new JButton(text("添加", "Add"));
        addButton.addActionListener(e -> {
            String selected = (String) genCombo.getSelectedItem();
            if (selected != null && !selected.equals(text("选择生成器...", "Select generator..."))) {
                addGeneratorPanel(selected);
                genCombo.setSelectedItem(text("选择生成器...", "Select generator..."));
            }
        });

        buttonPanel.add(genCombo);
        buttonPanel.add(addButton);
        generatorsPanel.add(buttonPanel);

        JPanel container = new JPanel(new BorderLayout());
        container.add(generatorsPanel, BorderLayout.NORTH);
        return container;
    }

    /**
     * 创建命令预览面板
     */
    private JPanel createCommandPreviewPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(text("命令预览", "Command Preview")));
        panel.setPreferredSize(new Dimension(0, 80));  // 设置合适的高度

        // 使用JTextArea替代JTextField，支持多行和自动换行
        commandPreview = new JTextArea();
        commandPreview.setEditable(false);
        commandPreview.setBackground(new Color(240, 240, 240));
        commandPreview.setLineWrap(true);           // 自动换行
        commandPreview.setWrapStyleWord(true);      // 按单词换行
        commandPreview.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));

        JScrollPane scrollPane = new JScrollPane(commandPreview);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 创建运行按钮面板
     */
    private JPanel createRunButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        // 复制按钮：放在最前面
        JButton copyButton = new JButton(text("复制", "Copy"));
        copyButton.addActionListener(e -> {
            String command = commandPreview.getText();
            if (!command.isEmpty()) {
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard()
                    .setContents(new java.awt.datatransfer.StringSelection(command), null);
            }
        });
        panel.add(copyButton);

        // 运行按钮：重命名
        runButton = new JButton(text("运行", "Run"));
        runButton.addActionListener(this::runGeneration);
        panel.add(runButton);

        // 清空输出按钮
        JButton clearButton = new JButton(text("清空输出", "Clear Output"));
        clearButton.addActionListener(e -> outputArea.setText(""));
        panel.add(clearButton);

        return panel;
    }

    /**
     * 创建输出面板
     */
    private JPanel createOutputPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createTitledBorder(text("输出", "Output")));
        // 移除固定高度设置，让它自适应

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        outputArea.setBackground(new Color(250, 250, 250));

        JScrollPane scrollPane = new JScrollPane(outputArea);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        panel.add(scrollPane, BorderLayout.CENTER);

        return panel;
    }

    /**
     * 添加工具面板
     */
    private void addToolPanel(String toolName) {
        ToolParameterPanel panel = new ToolParameterPanel(toolName);
        toolPanels.add(panel);
        toolsPanel.add(panel.getPanel());
        toolsPanel.revalidate();
        toolsPanel.repaint();
        updateCommandPreview();
    }

    /**
     * 添加生成器面板
     */
    private void addGeneratorPanel(String genName) {
        GeneratorParameterPanel panel = new GeneratorParameterPanel(genName);
        generatorPanels.add(panel);
        generatorsPanel.add(panel.getPanel());
        generatorsPanel.revalidate();
        generatorsPanel.repaint();
        updateCommandPreview();
    }

    /**
     * 浏览数据目录
     */
    private void browseDataDir(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("选择数据目录 (Select Data Directory)");
        if (chooser.showOpenDialog(mainFrame) == JFileChooser.APPROVE_OPTION) {
            datadirField.setText(chooser.getSelectedFile().getAbsolutePath());
            updateCommandPreview();
        }
    }

    /**
     * 更新命令预览
     */
    private void updateCommandPreview() {
        StringBuilder cmd = new StringBuilder("java -jar cfggen.jar");

        // 添加基础参数
        String datadir = datadirField.getText().trim();
        if (!datadir.isEmpty()) {
            cmd.append(" -datadir \"").append(datadir).append("\"");
        }

        String encoding = encodingField.getText().trim();
        if (!encoding.isEmpty() && !encoding.equals("GBK")) {
            cmd.append(" -encoding ").append(encoding);
        }

        String headRow = headRowField.getText().trim();
        if (!headRow.isEmpty() && !headRow.equals("2")) {
            cmd.append(" -headrow ").append(headRow);
        }

        if (usePoiCheckBox.isSelected()) {
            cmd.append(" -usepoi");
        }

        String asRoot = asRootField.getText().trim();
        if (!asRoot.isEmpty()) {
            cmd.append(" -asroot \"").append(asRoot).append("\"");
        }

        String excelDirs = excelDirsField.getText().trim();
        if (!excelDirs.isEmpty()) {
            cmd.append(" -exceldirs \"").append(excelDirs).append("\"");
        }

        String jsonDirs = jsonDirsField.getText().trim();
        if (!jsonDirs.isEmpty()) {
            cmd.append(" -jsondirs \"").append(jsonDirs).append("\"");
        }

        // 国际化互斥处理
        if (i18nNoneRadio.isSelected()) {
            // 无需国际化，不添加任何参数
        } else if (i18nFileRadio.isSelected()) {
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

        // 添加 tools
        for (ToolParameterPanel panel : toolPanels) {
            String toolCmd = panel.buildCommand();
            if (!toolCmd.isEmpty()) {
                cmd.append(" -tool ").append(toolCmd);
            }
        }

        // 添加 generators
        for (GeneratorParameterPanel panel : generatorPanels) {
            String genCmd = panel.buildCommand();
            if (!genCmd.isEmpty()) {
                cmd.append(" -gen ").append(genCmd);
            }
        }

        commandPreview.setText(cmd.toString());
    }

    /**
     * 运行生成
     */
    private void runGeneration(ActionEvent e) {
        // 检查数据目录
        if (datadirField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, text("请设置数据目录", "Please set data directory"),
                text("错误", "Error"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        if (generatorPanels.isEmpty()) {
            JOptionPane.showMessageDialog(mainFrame, text("请至少添加一个生成器", "Please add at least one generator"),
                text("错误", "Error"), JOptionPane.ERROR_MESSAGE);
            return;
        }

        // 禁用运行按钮
        runButton.setEnabled(false);
        outputArea.setText("");

        // 重定向输出
        redirectOutput();

        // 在后台线程运行
        SwingWorker<Integer, String> worker = new SwingWorker<>() {
            @Override
            protected Integer doInBackground() {
                try {
                    // 构建命令行参数
                    List<String> args = buildCommandLineArgs();
                    return Main.runWithCatch(args.toArray(new String[0]));
                } catch (Throwable t) {
                    t.printStackTrace();
                    return 1;
                }
            }

            @Override
            protected void done() {
                // 恢复输出
                restoreOutput();
                runButton.setEnabled(true);
            }
        };
        worker.execute();
    }

    /**
     * 构建命令行参数列表
     */
    private List<String> args = new ArrayList<>();

    private List<String> buildCommandLineArgs() {
        args.clear();

        String datadir = datadirField.getText().trim();
        if (!datadir.isEmpty()) {
            args.add("-datadir");
            args.add(datadir);
        }

        String encoding = encodingField.getText().trim();
        if (!encoding.isEmpty()) {
            args.add("-encoding");
            args.add(encoding);
        }

        String headRow = headRowField.getText().trim();
        if (!headRow.isEmpty()) {
            args.add("-headrow");
            args.add(headRow);
        }

        if (usePoiCheckBox.isSelected()) {
            args.add("-usepoi");
        }

        String asRoot = asRootField.getText().trim();
        if (!asRoot.isEmpty()) {
            args.add("-asroot");
            args.add(asRoot);
        }

        String excelDirs = excelDirsField.getText().trim();
        if (!excelDirs.isEmpty()) {
            args.add("-exceldirs");
            args.add(excelDirs);
        }

        String jsonDirs = jsonDirsField.getText().trim();
        if (!jsonDirs.isEmpty()) {
            args.add("-jsondirs");
            args.add(jsonDirs);
        }

        // 国际化互斥处理
        if (i18nNoneRadio.isSelected()) {
            // 无需国际化，不添加任何参数
        } else if (i18nFileRadio.isSelected()) {
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

        // 添加 tools
        for (ToolParameterPanel panel : toolPanels) {
            String toolCmd = panel.buildCommand();
            if (!toolCmd.isEmpty()) {
                args.add("-tool");
                args.add(toolCmd);
            }
        }

        // 添加 generators
        for (GeneratorParameterPanel panel : generatorPanels) {
            String genCmd = panel.buildCommand();
            if (!genCmd.isEmpty()) {
                args.add("-gen");
                args.add(genCmd);
            }
        }

        return args;
    }

    /**
     * 重定向输出到 GUI
     */
    private void redirectOutput() {
        // 重定向 System.out 和 System.err
        System.setOut(new PrintStream(new TextAreaOutputStream(outputArea), true));
        System.setErr(new PrintStream(new TextAreaOutputStream(outputArea), true));

        // 设置 Logger 的 Printer
        Logger.setPrinter(new GuiPrinter());
    }

    /**
     * 恢复原始输出
     */
    private void restoreOutput() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        Logger.setPrinter(originalPrinter);
    }

    /**
     * 创建文本框并添加监听器
     */
    private JTextField createTextField() {
        JTextField field = new JTextField();
        field.getDocument().addDocumentListener(new SimpleDocumentListener(this::updateCommandPreview));
        return field;
    }

    /**
     * 创建统一样式的删除按钮
     */
    private JButton createDeleteButton(Runnable action) {
        JButton button = new JButton(text("删除", "Remove"));

        // 计算：3倍文字宽度 + 边距
        // "删除"2个字符，每个约14像素，3倍约84像素
        // 加上按钮边距约10-15像素，设置为95-100像素
        int buttonWidth = isChineseLocale ? 100 : 90;  // 中文稍宽
        button.setPreferredSize(new Dimension(buttonWidth, 25));
        button.setMaximumSize(new Dimension(buttonWidth, 25));
        button.setMinimumSize(new Dimension(buttonWidth, 25));

        button.setForeground(new Color(200, 50, 50)); // 红色文字
        button.addActionListener(e -> action.run());
        return button;
    }

    // ==================== 内部类 ====================

    /**
     * Tool 参数面板
     */
    private class ToolParameterPanel {
        private final String toolName;
        private final JPanel panel;
        private final Map<String, JComponent> paramComponents = new LinkedHashMap<>();

        public ToolParameterPanel(String toolName) {
            this.toolName = toolName;
            this.panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createTitledBorder(toolName));

            // 收集参数信息
            ParameterInfoCollector info = new ParameterInfoCollector("tool", toolName);
            Tools.ToolProvider provider = Tools.getAllProviders().get(toolName);
            if (provider != null) {
                provider.create(info);
            }

            // 动态生成参数输入组件
            buildParameterComponents(info);
        }

        private void buildParameterComponents(ParameterInfoCollector info) {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(3, 3, 3, 3);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            int row = 0;
            int columns = 3; // 每行3个参数
            int col = 0;

            for (var entry : info.getInfos().entrySet()) {
                String paramName = entry.getKey();
                var paramInfo = entry.getValue();

                // 标签：紧挨着输入框
                gbc.gridx = col * 2;
                gbc.gridy = row;
                gbc.weightx = 0;
                gbc.insets = new Insets(3, 3, 3, 0);  // 右边距为0，紧贴输入框
                panel.add(new JLabel(paramName + ":"), gbc);

                // 输入框：紧挨着标签
                gbc.gridx = col * 2 + 1;
                gbc.weightx = 1.0;
                gbc.insets = new Insets(3, 2, 3, 3);  // 左边距2像素，与标签稍微分开

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

            // 添加删除按钮
            gbc.gridx = 0;
            gbc.gridy = row + 1;
            gbc.gridwidth = 1;
            gbc.weightx = 0;  // 重置为0，避免水平拉伸
            gbc.fill = GridBagConstraints.NONE;  // 不填充
            gbc.anchor = GridBagConstraints.WEST;  // 靠左对齐
            gbc.insets = new Insets(3, 3, 3, 3);  // 恢复默认边距
            JButton deleteButton = createDeleteButton(() -> removeToolPanel(this));
            panel.add(deleteButton, gbc);

            // 添加水平胶占据剩余空间，确保按钮靠左
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(Box.createHorizontalGlue(), gbc);
        }

        public JPanel getPanel() {
            return panel;
        }

        public String buildCommand() {
            StringBuilder cmd = new StringBuilder(toolName);
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

    /**
     * Generator 参数面板
     */
    private class GeneratorParameterPanel {
        private final String genName;
        private final JPanel panel;
        private final Map<String, JComponent> paramComponents = new LinkedHashMap<>();

        public GeneratorParameterPanel(String genName) {
            this.genName = genName;
            this.panel = new JPanel(new GridBagLayout());
            panel.setBorder(BorderFactory.createTitledBorder(genName));

            // 收集参数信息
            ParameterInfoCollector info = new ParameterInfoCollector("gen", genName);
            Generators.GeneratorProvider provider = Generators.getAllProviders().get(genName);
            if (provider != null) {
                provider.create(info);
            }

            // 动态生成参数输入组件
            buildParameterComponents(info);
        }

        private void buildParameterComponents(ParameterInfoCollector info) {
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(3, 3, 3, 3);
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;

            int row = 0;
            int columns = 3; // 每行3个参数
            int col = 0;

            for (var entry : info.getInfos().entrySet()) {
                String paramName = entry.getKey();
                var paramInfo = entry.getValue();

                // 标签：紧挨着输入框
                gbc.gridx = col * 2;
                gbc.gridy = row;
                gbc.weightx = 0;
                gbc.insets = new Insets(3, 3, 3, 0);  // 右边距为0，紧贴输入框
                panel.add(new JLabel(paramName + ":"), gbc);

                // 输入框：紧挨着标签
                gbc.gridx = col * 2 + 1;
                gbc.weightx = 1.0;
                gbc.insets = new Insets(3, 2, 3, 3);  // 左边距2像素，与标签稍微分开

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

            // 添加删除按钮
            gbc.gridx = 0;
            gbc.gridy = row + 1;
            gbc.gridwidth = 1;
            gbc.weightx = 0;  // 重置为0，避免水平拉伸
            gbc.fill = GridBagConstraints.NONE;  // 不填充
            gbc.anchor = GridBagConstraints.WEST;  // 靠左对齐
            gbc.insets = new Insets(3, 3, 3, 3);  // 恢复默认边距
            JButton deleteButton = createDeleteButton(() -> removeGeneratorPanel(this));
            panel.add(deleteButton, gbc);

            // 添加水平胶占据剩余空间，确保按钮靠左
            gbc.gridx = 1;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            panel.add(Box.createHorizontalGlue(), gbc);
        }

        public JPanel getPanel() {
            return panel;
        }

        public String buildCommand() {
            StringBuilder cmd = new StringBuilder(genName);
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

    /**
     * 移除工具面板
     */
    private void removeToolPanel(ToolParameterPanel panel) {
        toolPanels.remove(panel);
        toolsPanel.remove(panel.getPanel());
        toolsPanel.revalidate();
        toolsPanel.repaint();
        updateCommandPreview();
    }

    /**
     * 移除生成器面板
     */
    private void removeGeneratorPanel(GeneratorParameterPanel panel) {
        generatorPanels.remove(panel);
        generatorsPanel.remove(panel.getPanel());
        generatorsPanel.revalidate();
        generatorsPanel.repaint();
        updateCommandPreview();
    }

    /**
     * GUI Printer 实现
     */
    private class GuiPrinter implements Logger.Printer {
        @Override
        public void printf(String fmt, Object... args) {
            // Logger.log 会将 lineSeparator 添加到格式字符串中
            // 所以这里直接格式化，不需要再添加换行符
            try {
                String message = String.format(fmt, args);
                SwingUtilities.invokeLater(() -> {
                    outputArea.append(message);
                    outputArea.setCaretPosition(outputArea.getDocument().getLength());
                });
            } catch (Exception e) {
                // 如果格式化失败，直接输出原始信息
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

    /**
     * 文本区域输出流
     */
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

    /**
     * 简单的文档监听器
     */
    private static class SimpleDocumentListener implements javax.swing.event.DocumentListener {
        private final Runnable callback;

        public SimpleDocumentListener(Runnable callback) {
            this.callback = callback;
        }

        @Override
        public void insertUpdate(javax.swing.event.DocumentEvent e) {
            callback.run();
        }

        @Override
        public void removeUpdate(javax.swing.event.DocumentEvent e) {
            callback.run();
        }

        @Override
        public void changedUpdate(javax.swing.event.DocumentEvent e) {
            callback.run();
        }
    }
}
