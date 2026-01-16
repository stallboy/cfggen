package configgen.gen.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;

public final class UIConstants {
    private UIConstants() {} // 工具类，禁止实例化

    // 窗口尺寸
    public static final int WINDOW_WIDTH = 1400;
    public static final int WINDOW_HEIGHT = 800;
    public static final int SPLIT_PANE_DIVIDER_LOCATION = 900;
    public static final double SPLIT_PANE_RESIZE_WEIGHT = 0.6;

    // 右侧面板
    public static final int RIGHT_PANEL_PREFERRED_WIDTH = 450;
    public static final int COMMAND_PREVIEW_HEIGHT = 80;

    // 组件尺寸
    public static final Dimension TEXT_FIELD_DATADIR_SIZE = new Dimension(300, 25);
    public static final Dimension TEXT_FIELD_ENCODING_SIZE = new Dimension(150, 25);
    public static final Dimension TEXT_FIELD_HEADROW_SIZE = new Dimension(100, 25);
    public static final Dimension BUTTON_SIZE = new Dimension(100, 25);

    // 颜色
    public static final Color COMMAND_PREVIEW_BG = new Color(240, 240, 240);
    public static final Color OUTPUT_AREA_BG = new Color(250, 250, 250);
    public static final Color DELETE_BUTTON_FG = new Color(200, 50, 50);

    // 字体
    public static final int COMMAND_PREVIEW_FONT_SIZE = 11;
    public static final int OUTPUT_AREA_FONT_SIZE = 12;

    // 间距
    public static final Insets PANEL_INSETS = new Insets(5, 5, 5, 5);
    public static final Insets PARAM_INSETS = new Insets(3, 3, 3, 3);
    public static final Insets PARAM_LABEL_INSETS = new Insets(3, 3, 3, 0);
    public static final Insets PARAM_VALUE_INSETS = new Insets(3, 2, 3, 3);

    // 垂直间距
    public static final int VERTICAL_STRUT_LARGE = 10;
    public static final int VERTICAL_STRUT_SMALL = 3;
    public static final int VERTICAL_STRUT_MEDIUM = 5;

    // 默认值
    public static final String DEFAULT_ENCODING = "GBK";
    public static final String DEFAULT_HEAD_ROW = "2";
    public static final String DEFAULT_LANG = "zh_cn";
}
