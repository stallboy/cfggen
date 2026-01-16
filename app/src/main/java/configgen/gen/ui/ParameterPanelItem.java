package configgen.gen.ui;

import configgen.gen.Generators;
import configgen.gen.ParameterInfoCollector;
import configgen.gen.Tools;
import configgen.util.LocaleUtil;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 参数面板项
 * 负责UI组件的创建、布局和命令构建
 */
public class ParameterPanelItem {
    private final String name;
    private final String type;
    private final JPanel panel;
    private final Map<String, JComponent> paramComponents = new LinkedHashMap<>();

    // 回调监听器
    private Runnable changeListener;
    private Runnable deleteListener;

    public ParameterPanelItem(String type, String name, DocumentListener documentListener) {
        this.type = type;
        this.name = name;
        this.panel = createPanel(documentListener);
    }

    private JPanel createPanel(DocumentListener documentListener) {
        JPanel panel = new JPanel(new GridBagLayout());
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

        buildParameterComponents(panel, info, documentListener);
        return panel;
    }

    private void buildParameterComponents(JPanel panel, ParameterInfoCollector info,
                                         DocumentListener documentListener) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = UIConstants.PARAM_INSETS;
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
            gbc.insets = UIConstants.PARAM_LABEL_INSETS;
            panel.add(new JLabel(paramName + ":"), gbc);

            gbc.gridx = col * 2 + 1;
            gbc.weightx = 1.0;
            gbc.insets = UIConstants.PARAM_VALUE_INSETS;

            JComponent component;
            if (paramInfo.isFlag()) {
                JCheckBox checkBox = new JCheckBox();
                checkBox.addActionListener(e -> notifyChange());
                component = checkBox;
            } else {
                JTextField textField = new JTextField(paramInfo.def() != null ? paramInfo.def() : "");
                textField.getDocument().addDocumentListener(documentListener);
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

        // 添加删除按钮和水平胶水
        gbc.gridx = 0;
        gbc.gridy = row + 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = UIConstants.PARAM_INSETS;

        JButton deleteButton = createDeleteButton();
        panel.add(deleteButton, gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        panel.add(Box.createHorizontalGlue(), gbc);
    }

    private JButton createDeleteButton() {
        JButton button = new JButton(LocaleUtil.getLocaleString("GuiLauncher.Remove", "Remove"));
        button.setPreferredSize(UIConstants.BUTTON_SIZE);
        button.setMaximumSize(UIConstants.BUTTON_SIZE);
        button.setMinimumSize(UIConstants.BUTTON_SIZE);
        button.setForeground(UIConstants.DELETE_BUTTON_FG);
        button.addActionListener(e -> {
            if (deleteListener != null) {
                deleteListener.run();
            }
        });
        return button;
    }

    private void notifyChange() {
        if (changeListener != null) {
            changeListener.run();
        }
    }

    // 设置回调监听器
    public void setChangeListener(Runnable listener) {
        this.changeListener = listener;
    }

    public void setDeleteListener(Runnable listener) {
        this.deleteListener = listener;
    }

    // Getters
    public JPanel getPanel() {
        return panel;
    }

    public Map<String, JComponent> getParamComponents() {
        return paramComponents;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    /**
     * 构建命令行参数字符串
     */
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
