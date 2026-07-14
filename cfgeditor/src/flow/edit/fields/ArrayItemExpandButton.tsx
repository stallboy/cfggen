import {useCallback, useState} from "react";
import {Button, Space} from "antd";
import {ArrowDownOutlined, ArrowUpOutlined, LeftOutlined, MinusSquareTwoTone, RightOutlined} from "@ant-design/icons";

interface ArrayItemExpandButtonProps {
    fold: boolean;
    onRemove: () => void;
    onMoveUp?: () => void;
    onMoveDown?: () => void;
}

export function ArrayItemExpandButton({fold, onRemove, onMoveUp, onMoveDown}: ArrayItemExpandButtonProps) {
    const [expand, setExpand] = useState(false);

    const toggleExpand = useCallback(() => {
        setExpand((prev) => !prev);
    }, []);

    const removeButton = <Button className="nodrag" icon={<MinusSquareTwoTone/>} onClick={onRemove}/>;

    // 没有上下移动按钮时，直接返回删除按钮
    if (!onMoveUp && !onMoveDown) {
        return removeButton;
    }

    // 不需要折叠时，显示所有按钮
    if (!fold) {
        return (
            <>
                {removeButton}
                {onMoveUp && <Button className="nodrag" icon={<ArrowUpOutlined/>} onClick={onMoveUp}/>}
                {onMoveDown && <Button className="nodrag" icon={<ArrowDownOutlined/>} onClick={onMoveDown}/>}
            </>
        );
    }

    // 需要折叠时，使用展开/收起按钮
    return (
        <Space size="small">
            <Button className="nodrag" icon={expand ? <LeftOutlined/> : <RightOutlined/>} onClick={toggleExpand}/>
            {expand && (
                <>
                    {removeButton}
                    {onMoveUp && <Button className="nodrag" icon={<ArrowUpOutlined/>} onClick={onMoveUp}/>}
                    {onMoveDown && <Button className="nodrag" icon={<ArrowDownOutlined/>} onClick={onMoveDown}/>}
                </>
            )}
        </Space>
    );
}
