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
    empty?: ReactNode;
}

const itemStyle: CSSProperties = {cursor: 'pointer', paddingInline: 8};

/**
 * 泛型导航列表：一组可点击条目，点击按 curPage 跳转到 (table,id)。
 * 用 List（语义即"导航"）替代此前各面板重复的 Table(showHeader=false)。
 *
 * 注意：List 在 v6 无原生 virtual，故仅用于条目不多的场景；
 * 长列表（如 RefIdList 可能上百）仍保留 Table virtual。
 */
export function NavList<T>({items, rowKey, toNav, renderTitle, renderExtra, empty}: NavListProps<T>) {
    const navigate = useNavigate();
    const {curPage} = useCurPageRecordOrRecordRef();
    const {isEditMode} = useMyStore();

    return (
        <List size="small" split dataSource={items} rowKey={rowKey}
              locale={{emptyText: empty ?? <Empty description={false}/>}}
              renderItem={(item) => (
                  <List.Item style={itemStyle}
                             onClick={() => navigate(navTo(curPage, toNav(item).table, toNav(item).id, isEditMode))}>
                      <List.Item.Meta title={<Typography.Link>{renderTitle(item)}</Typography.Link>}/>
                      {renderExtra ? <div>{renderExtra(item)}</div> : null}
                  </List.Item>
              )}/>
    );
}
