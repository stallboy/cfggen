package configgen.gen.ui.panel;

import configgen.gen.Generators;
import configgen.gen.Tools;
import configgen.util.LocaleUtil;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;

/**
 * 统一创建Tools和Generators面板的工厂类
 * 消除createToolsPanel()和createGeneratorsPanel()的重复代码
 */
public class ProviderPanelFactory {

    /**
     * 创建Tools面板
     */
    public static JPanel createToolsPanel(List<ParameterPanelItem> toolPanels,
                                         java.util.function.BiConsumer<List<ParameterPanelItem>, String> onAdd) {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createTitledBorder("Tools"));

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
                onAdd.accept(toolPanels, selected);
                toolCombo.setSelectedItem(selectPrompt);
            }
        });

        buttonPanel.add(toolCombo);
        buttonPanel.add(addButton);
        mainPanel.add(buttonPanel);

        JPanel container = new JPanel(new BorderLayout());
        container.add(mainPanel, BorderLayout.NORTH);
        return container;
    }

    /**
     * 创建Generators面板
     */
    public static JPanel createGeneratorsPanel(List<ParameterPanelItem> generatorPanels,
                                              java.util.function.BiConsumer<List<ParameterPanelItem>, String> onAdd) {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createTitledBorder("Generators"));

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
                onAdd.accept(generatorPanels, selected);
                genCombo.setSelectedItem(selectPrompt);
            }
        });

        buttonPanel.add(genCombo);
        buttonPanel.add(addButton);
        mainPanel.add(buttonPanel);

        JPanel container = new JPanel(new BorderLayout());
        container.add(mainPanel, BorderLayout.NORTH);
        return container;
    }
}
