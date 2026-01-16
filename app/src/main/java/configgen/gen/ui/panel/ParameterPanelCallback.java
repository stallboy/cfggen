package configgen.gen.ui.panel;

import javax.swing.*;
import java.util.List;

/**
 * 参数面板添加回调接口
 */
@FunctionalInterface
public interface ParameterPanelCallback {
    void onAddParameterPanel(List<ParameterPanelItem> panels, JPanel container,
                            String type, String name);
}
