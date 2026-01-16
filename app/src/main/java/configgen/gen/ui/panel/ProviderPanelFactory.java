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
                                         ParameterPanelCallback callback) {
        return createProviderPanel(
            toolPanels,
            "Tools",
            "GuiLauncher.SelectTool",
            "Select tool...",
            Tools.getAllProviders(),
            "tool",
            callback
        );
    }

    /**
     * 创建Generators面板
     */
    public static JPanel createGeneratorsPanel(List<ParameterPanelItem> generatorPanels,
                                              ParameterPanelCallback callback) {
        return createProviderPanel(
            generatorPanels,
            "Generators",
            "GuiLauncher.SelectGenerator",
            "Select generator...",
            Generators.getAllProviders(),
            "gen",
            callback
        );
    }

    /**
     * 统一的Provider面板创建逻辑
     */
    private static JPanel createProviderPanel(
            List<ParameterPanelItem> panels,
            String title,
            String selectKey,
            String defaultSelect,
            Map<String, ?> providers,
            String type,
            ParameterPanelCallback callback) {

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createTitledBorder(title));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JComboBox<String> providerCombo = new JComboBox<>();
        String selectPrompt = LocaleUtil.getLocaleString(selectKey, defaultSelect);
        providerCombo.addItem(selectPrompt);

        for (String providerName : providers.keySet()) {
            providerCombo.addItem(providerName);
        }

        JButton addButton = new JButton(LocaleUtil.getLocaleString("GuiLauncher.Add", "Add"));
        addButton.addActionListener(e -> {
            String selected = (String) providerCombo.getSelectedItem();
            if (selected != null && !selected.equals(selectPrompt)) {
                callback.onAddParameterPanel(panels, mainPanel, type, selected);
                providerCombo.setSelectedItem(selectPrompt);
            }
        });

        buttonPanel.add(providerCombo);
        buttonPanel.add(addButton);
        mainPanel.add(buttonPanel);

        JPanel container = new JPanel(new BorderLayout());
        container.add(mainPanel, BorderLayout.NORTH);
        return container;
    }
}
