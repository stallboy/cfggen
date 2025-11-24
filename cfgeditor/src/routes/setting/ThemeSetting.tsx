import {memo, useState, useEffect} from "react";
import {Form, Input, Button, message, Space, Typography, Divider} from "antd";
import {useTranslation} from "react-i18next";
import {useMyStore, setThemeConfig} from "../../store/store.ts";
import {themeService} from "./themeService.ts";
import {FlowVisualizationSetting} from "./FlowVisualizationSetting.tsx";
import Title from "antd/lib/typography/Title";

const {Text} = Typography;

export const ThemeSetting = memo(function ThemeSetting() {
    const {t} = useTranslation();
    const {themeConfig} = useMyStore();
    const [loading, setLoading] = useState(false);
    const [themeExists, setThemeExists] = useState<boolean | null>(null);

    // 检查当前主题文件是否存在
    useEffect(() => {
        const checkThemeFile = async () => {
            if (themeConfig.themeFile) {
                const exists = await themeService.themeExists(themeConfig.themeFile);
                setThemeExists(exists);
            } else {
                setThemeExists(null);
            }
        };

        checkThemeFile();
    }, [themeConfig.themeFile]);

    const handleThemeChange = async (values: { themeFile: string }) => {
        setLoading(true);
        try {
            const newThemeConfig = {
                ...themeConfig,
                themeFile: values.themeFile.trim() || '',
            };

            // 如果设置了主题文件，验证文件是否存在
            if (newThemeConfig.themeFile) {
                const exists = await themeService.themeExists(newThemeConfig.themeFile);
                if (!exists) {
                    message.warning(t('themeFileNotFound'));
                    setThemeExists(false);
                    setLoading(false);
                    return;
                }
                setThemeExists(true);
            } else {
                setThemeExists(null);
            }

            // 保存主题配置
            setThemeConfig(newThemeConfig);
            message.success(t('themeSettingSaved'));

            // 提示用户可能需要刷新页面
            message.info(t('themeChangeHint'));
        } catch (error) {
            console.error('设置主题失败:', error);
            message.error(t('themeSettingFailed'));
        } finally {
            setLoading(false);
        }
    };

    const testTheme = async () => {
        if (!themeConfig.themeFile) {
            message.warning(t('pleaseSetThemeFile'));
            return;
        }

        setLoading(true);
        try {
            const theme = await themeService.loadTheme(themeConfig.themeFile);
            if (theme) {
                message.success(t('themeFileValid'));
            } else {
                message.error(t('themeFileInvalid'));
            }
        } catch (error) {
            console.error('测试主题失败:', error);
            message.error(t('themeTestFailed'));
        } finally {
            setLoading(false);
        }
    };

    return (
        <Form layout="vertical" size={"small"}
              initialValues={themeConfig}
              onFinish={handleThemeChange}>

            <Title level={4} style={{marginTop: -4}}>{t('themeSetting')}</Title>
            <Form.Item label={t('themeFile')}
                       name="themeFile"
                       help={
                           themeExists === false ? (
                               <Text type="danger">{t('themeFileNotFound')}</Text>
                           ) : themeExists === true ? (
                               <Text type="success">{t('themeFileExists')}</Text>
                           ) : (
                               t('themeFileHelp')
                           )
                       }>
                <Input placeholder="colourpurple.json" allowClear/>
            </Form.Item>

            <Form.Item>
                <Space>
                    <Button type="primary" htmlType="submit" loading={loading}>
                        {t('save')}
                    </Button>
                    <Button onClick={testTheme} loading={loading} disabled={!themeConfig.themeFile}>
                        {t('testTheme')}
                    </Button>
                </Space>
            </Form.Item>

            <Divider/>

            <FlowVisualizationSetting/>
        </Form>
    );
});