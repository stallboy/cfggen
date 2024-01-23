import {Button, Flex} from "antd";

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

    return (
        <Flex vertical className='contextMenu' style={{...menuStyle}}>
            {menuItems.map(({handler, key, label}) => <Button key={key} onClick={() => {
                handler();
                closeMenu();
            }}>{label}</Button>)}
        </Flex>

    );
}
