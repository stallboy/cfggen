package configgen.gen.ui;

import configgen.gen.Generators;
import configgen.gen.Tools;
import configgen.util.LocaleUtil;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * 统一创建Tools和Generators面板的工厂类
 * 直接返回可用的面板，消除不必要的包装层
 */
public class ProviderPanelFactory {

    /**
     * 通用的Provider面板创建方法
     * 直接返回可以使用的面板
     */
    private static JPanel createProviderPanel(
            Map<?, ?> providers,
            List<ParameterPanelItem> panelList,
            String panelTitle,
            String selectPromptKey,
            BiConsumer<List<ParameterPanelItem>, String> onAdd) {

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createTitledBorder(panelTitle));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JComboBox<String> combo = new JComboBox<>();
        String selectPrompt = LocaleUtil.getLocaleString(selectPromptKey, "Select...");
        combo.addItem(selectPrompt);

        for (Object providerName : providers.keySet()) {
            combo.addItem((String) providerName);
        }

        JButton addButton = new JButton(LocaleUtil.getLocaleString("GuiLauncher.Add", "Add"));
        addButton.addActionListener(e -> {
            String selected = (String) combo.getSelectedItem();
            if (selected != null && !selected.equals(selectPrompt)) {
                onAdd.accept(panelList, selected);
                combo.setSelectedItem(selectPrompt);
            }
        });

        buttonPanel.add(combo);
        buttonPanel.add(addButton);
        mainPanel.add(buttonPanel);

        return mainPanel;
    }

    /**
     * 创建Tools面板
     */
    public static JPanel createToolsPanel(List<ParameterPanelItem> toolPanels,
                                          BiConsumer<List<ParameterPanelItem>, String> onAdd) {
        return createProviderPanel(
                Tools.getAllProviders(),
                toolPanels,
                "Tools",
                "GuiLauncher.SelectTool",
                onAdd);
    }

    /**
     * 创建Generators面板
     */
    public static JPanel createGeneratorsPanel(List<ParameterPanelItem> generatorPanels,
                                               BiConsumer<List<ParameterPanelItem>, String> onAdd) {
        return createProviderPanel(
                Generators.getAllProviders(),
                generatorPanels,
                "Generators",
                "GuiLauncher.SelectGenerator",
                onAdd);
    }
}
