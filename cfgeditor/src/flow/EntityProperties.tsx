import {Handle, Position} from "@xyflow/react";
import {DisplayField, EntitySharedSetting} from "@/domain/entityModel";
import {Flex, List, Tooltip, Typography} from "antd";
import {CSSProperties, memo, useMemo} from "react";
import {getFieldBackgroundColor} from "./colors.ts";
import {getReadNodeWidth} from "./dimensions.ts";
import {Highlight} from "./Highlight.tsx";

const {Text} = Typography;

// 字段标签：取 comment 第一个中/英左括号前的部分，截前 6 字符。外层 <Text ellipsis maxWidth:80%>
// 已做 CSS 截断，这里仅预截断避免标签过长撑宽节点——数据层不读 DOM 宽度，避免 measure cycle。
const LABEL_COMMENT_PREFIX_MAX_LEN = 6;
const COMMENT_OPEN_BRACKET = /[（(]/; // 中文/英文左括号：comment 常见「说明（细节）」形式，只取括号前

function buildFieldTooltip({comment, name}: { name: string, comment?: string }) {
    return comment ? `${name}: ${comment}` : name;
}

function buildFieldLabel({comment, name}: { name: string, comment?: string }) {
    if (comment) {
        const c = comment.split(COMMENT_OPEN_BRACKET)[0];
        return `${name} ${c.substring(0, LABEL_COMMENT_PREFIX_MAX_LEN)} `
    }
    return name;
}

const listStyle: CSSProperties = {backgroundColor: '#fff'};
const listItemStyle: CSSProperties = {position: 'relative'};
const flexStyle = {width: '100%'};
const ellipsis = {tooltip: true};
const itemValueStyle = {maxWidth: '70%'};

export const EntityProperties = memo(function EntityProperties({fields, sharedSetting, color}: {
    fields: DisplayField[],
    sharedSetting?: EntitySharedSetting,
    color: string,
}) {

    const itemKeyStyle = useMemo(() => {
        return {color: color, maxWidth: '80%'}
    }, [color]);

    const handleInStyle: CSSProperties = useMemo(() => {
        return {position: 'absolute', left: '-10px', backgroundColor: color}
    }, [color]);
    const handleOutStyle: CSSProperties = useMemo(() => {
        const width = getReadNodeWidth(sharedSetting?.nodeShow);
        return {position: 'absolute', left: `${width - 2}px`, backgroundColor: color}
    }, [color, sharedSetting?.nodeShow]);

    if (fields.length == 0) {
        return <></>;
    }

    const keyword = sharedSetting?.query
    return <List size='small' style={listStyle} bordered dataSource={fields}
                 renderItem={(item) => {
                     const bgColor = getFieldBackgroundColor(item, sharedSetting?.nodeShow)
                     const thisItemStyle: CSSProperties = bgColor ?
                         {position: 'relative', backgroundColor: bgColor} : listItemStyle;

                     return <List.Item key={item.key} style={thisItemStyle}>
                         <Flex justify="space-between" style={flexStyle}>
                             <Tooltip title={buildFieldTooltip(item)}>
                                 <Text style={itemKeyStyle} ellipsis={ellipsis}>
                                     {buildFieldLabel(item)}
                                 </Text>
                             </Tooltip>
                             <Text style={itemValueStyle} ellipsis={ellipsis}>
                                 {keyword ? <Highlight text={item.value} keyword={keyword}/> : item.value}
                             </Text>
                         </Flex>

                         {item.handleIn && <Handle type='target' position={Position.Left} id={`@in_${item.name}`}
                                                   style={handleInStyle}/>}
                         {item.handleOut && <Handle type='source' position={Position.Right} id={item.name}
                                                    style={handleOutStyle}/>}
                     </List.Item>;

                 }}/>;
});
