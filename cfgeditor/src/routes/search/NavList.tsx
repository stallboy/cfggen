import {Empty, List, Typography} from "antd";
import {navTo, useCurPageRecordOrRecordRef, useMyStore} from "@/store/store";
import {useNavigate} from "react-router";
import {CSSProperties, ReactNode} from "react";

interface NavListProps<T> {
    items: T[];
    rowKey: (item: T) => string;
    /** 怎么从条目得到跳转目标 (table,id) */
    toNav: (item: T) => { table: string; id: string };
    /** 主标题（可点击跳转） */
    renderTitle: (item: T) => ReactNode;
    /** 右侧次要信息（TimeAgo / depth / value 等） */
    renderExtra?: (item: T) => ReactNode;
    /** 跳转是否写入导航历史；派生列表（如最近访问）传 false 避免循环污染 alt+c/v 链 */
    addHistory?: boolean;
    empty?: ReactNode;
}

const itemStyle: CSSProperties = {cursor: 'pointer', paddingInline: 8};

/**
 * 泛型导航列表：一组可点击条目，点击按 curPage 跳转到 (table,id)。
 * 用 List（语义即"导航"）替代此前各面板重复的 Table(showHeader=false)。
 *
 * 标题用 Typography.Link 直接挂在 List.Item 上（不包进 List.Item.Meta）：
 * List.Item.Meta 的 title 带 `> a{color: colorText}` 规则，会把 Typography.Link 强制成黑色；
 * 脱离 Meta 后 Typography.Link 才是正常蓝色 link。renderExtra 走 List.Item 的 extra，
 * 靠 List.Item 自身 flex(space-between) 让标题左、次要信息右。
 *
 * 注意：List 在 v6 无原生 virtual，故仅用于条目不多的场景；
 * 长列表（如 RefIdList 可能上百）仍保留 Table virtual。
 */
export function NavList<T>({items, rowKey, toNav, renderTitle, renderExtra, empty, addHistory = true}: NavListProps<T>) {
    const navigate = useNavigate();
    const {curPage} = useCurPageRecordOrRecordRef();
    const {isEditMode} = useMyStore();

    return (
        <List size="small" split dataSource={items} rowKey={rowKey}
              locale={{emptyText: empty ?? <Empty description={false}/>}}
              renderItem={(item) => (
                  <List.Item style={itemStyle}
                             extra={renderExtra ? renderExtra(item) : undefined}
                             onClick={() => navigate(navTo(curPage, toNav(item).table, toNav(item).id, isEditMode, addHistory))}>
                      <Typography.Link>{renderTitle(item)}</Typography.Link>
                  </List.Item>
              )}/>
    );
}
