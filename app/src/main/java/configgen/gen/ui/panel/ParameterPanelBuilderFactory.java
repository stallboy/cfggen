package configgen.gen.ui.panel;

import javax.swing.*;
import java.util.Map;

/**
 * 参数面板的命令构建器
 */
public final class ParameterPanelBuilderFactory {

    private ParameterPanelBuilderFactory() {} // 工具类，禁止实例化

    /**
     * 构建参数面板的命令行参数字符串
     */
    public static String buildCommand(String name, Map<String, JComponent> paramComponents) {
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
