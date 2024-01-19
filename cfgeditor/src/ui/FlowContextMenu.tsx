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

export function FlowContextMenu({menuStyle, menuItems}: {
    menuStyle: MenuStyle,
    menuItems: MenuItem[],
}) {

    return (
        <Flex vertical className='contextMenu' style={{...menuStyle}}>
            {menuItems.map(({handler, key, label}) => <Button key={key} onClick={handler}>{label}</Button>)}
        </Flex>

    );
}
