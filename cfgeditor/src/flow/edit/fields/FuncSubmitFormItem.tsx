import {memo} from "react";
import {Button, Form, Space, Tooltip} from "antd";
import {useTranslation} from "react-i18next";
import {FORM_ITEM_LAYOUT_WITHOUT_LABEL, FuncSubmitFormItemProps} from "../shared/constants.ts";

export const FuncSubmitFormItem = memo(function FuncSubmitFormItem({field}: FuncSubmitFormItemProps) {
    const [t] = useTranslation();
    const func = field.value;

    // alt+s 提交热键已移至 EntityForm，按节点表单作用域注册（见 EntityForm），避免全局重复触发。
    return (
        <Form.Item {...FORM_ITEM_LAYOUT_WITHOUT_LABEL}>
            <Space size={50}>
                <Button className="nodrag" type="primary" htmlType="submit" onClick={() => func.funcSubmit()}>
                    <Tooltip title={t("addOrUpdateTooltip")}>{t("addOrUpdate")}</Tooltip>
                </Button>
                <Button className="nodrag" type="default" onClick={() => func.funcClear()}>
                    {t("setDefaultValue")}
                </Button>
            </Space>
        </Form.Item>
    );
});
