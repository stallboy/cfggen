import {memo} from "react";
import {Button, Form, Space, Tooltip} from "antd";
import {useTranslation} from "react-i18next";
import {FuncSubmitEditField} from "@/domain/entityModel";
import {FORM_ITEM_LAYOUT_WITHOUT_LABEL} from "../shared/constants.ts";

export interface FuncSubmitFormItemProps {
    field: FuncSubmitEditField;
}

export const FuncSubmitFormItem = memo(function FuncSubmitFormItem({field}: FuncSubmitFormItemProps) {
    const [t] = useTranslation();
    const func = field.value;

    // alt+s「提交」由 CfgEditorApp 全局单点注册、直达 session.submit()（见 EntityForm 注释）；本组件只提供点击按钮入口。
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
