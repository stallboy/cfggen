import {Menu} from "antd";

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
    disabled?: boolean | (() => boolean);
}

export function FlowContextMenu({menuStyle, menuItems, closeMenu}: {
    menuStyle: MenuStyle,
    menuItems: MenuItem[],
    closeMenu: () => void,
}) {

    return <div className='contextMenu' style={{...menuStyle}}>
        <Menu items={menuItems.map(mi => {
            return {
                key: mi.key,
                label: mi.label,
                disabled: typeof mi.disabled === 'function' ? mi.disabled() : mi.disabled,
            };
        })} onClick={(info) => {
            const menuItem = menuItems.find((mi) => mi.key == info.key);
            if (menuItem) {
                menuItem.handler();
                closeMenu();
            }
        }}/>
    </div>;
}
