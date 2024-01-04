import {Button, Card, ColorPicker, Form, Input, Space} from "antd";
import {MinusCircleOutlined, PlusOutlined} from "@ant-design/icons";
import {KeywordColor} from "./model/entityModel.ts";
import {useTranslation} from "react-i18next";

export function KeywordColorSetting({keywordColors, setKeywordColors}: {
    keywordColors: KeywordColor[];
    setKeywordColors: (colors: KeywordColor[]) => void;
}) {
    const {t} = useTranslation();

    function onFinish(values: any) {
        // console.log(values.keywordColors);
        let colors = [];
        for (let keywordColor of values.keywordColors) {
            let color;
            if (typeof keywordColor.color == 'object') {
                color = keywordColor.color.toHexString();
            } else if (typeof keywordColor.color == 'string') {
                color = keywordColor.color;
            } else {
                color = '#1677ff';
            }
            colors.push({keyword: keywordColor.keyword, color})
        }

        setKeywordColors(colors);
    }

    return <Card title={t("keywordColorSetting")}>
        <Form name="keyword color setting" onFinish={onFinish} autoComplete="off">
            <Form.List name="keywordColors" initialValue={keywordColors}>
                {(fields, {add, remove}) => (
                    <>
                        {fields.map(({key, name, ...restField}) => (
                            <Space key={key} style={{display: 'flex', marginBottom: 8}} align="baseline">
                                <Form.Item  {...restField} name={[name, 'keyword']}>
                                    <Input placeholder="keyword"/>
                                </Form.Item>
                                <Form.Item {...restField} name={[name, 'color']}>
                                    <ColorPicker defaultValue='#1677ff'/>
                                </Form.Item>
                                <MinusCircleOutlined onClick={() => remove(name)}/>
                            </Space>
                        ))}
                        <Form.Item>
                            <Button type="dashed" onClick={() => add()} block icon={<PlusOutlined/>}>
                                {t('addKeywordColor')}
                            </Button>
                        </Form.Item>
                    </>
                )}
            </Form.List>
            <Form.Item>
                <Button type="primary" htmlType="submit">
                    {t('setKeywordColors')}
                </Button>
            </Form.Item>
        </Form>
    </Card>;

}