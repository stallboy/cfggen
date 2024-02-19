import {Menu} from "antd";
import {useCallback} from "react";

export interface MenuStyle {
    top?: number;
    left?: number;
    right?: number;
    bottom?: number;
}

export interface MenuItem {
    key: string;
    label: string;
    handler: () => void;
}

export function FlowContextMenu({menuStyle, menuItems, closeMenu}: {
    menuStyle: MenuStyle,
    menuItems: MenuItem[],
    closeMenu: () => void,
}) {

    const onClick = useCallback((info: any) => {
        const menuItem = menuItems.find((mi) => mi.key == info.key);
        if (menuItem) {
            menuItem.handler();
            closeMenu();
        }
    }, [menuItems, closeMenu]);

    return <div className='contextMenu' style={{...menuStyle}}>
        <Menu items={menuItems.map(mi => {
            return {
                key: mi.key,
                label: mi.label
            };
        })} onClick={onClick}/>
    </div>;
}
