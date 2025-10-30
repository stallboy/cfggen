import {Handle, Position} from "@xyflow/react";
import {EntityField, EntitySharedSetting} from "./entityModel.ts";
import {Flex, List, Tooltip, Typography} from "antd";
import {CSSProperties, memo, useMemo} from "react";
import {getFieldBackgroundColor} from "./colors.ts";
import {Highlight} from "./EntityCard.tsx";

const {Text} = Typography;

function tooltip({comment, name}: { name: string, comment?: string }) {
    return comment ? `${name}: ${comment}` : name;
}

const re = /[（(]/;

function text({comment, name}: { name: string, comment?: string }) {
    if (comment) {
        const c = comment.split(re)[0];
        return `${name} ${c.substring(0, 6)} `
    }
    return name;
}

const listStyle: CSSProperties = {backgroundColor: '#fff'};
const listItemStyle: CSSProperties = {position: 'relative'};
const flexStyle = {width: '100%'};
const ellipsis = {tooltip: true};
const itemValueStyle = {maxWidth: '70%'};

export const EntityProperties = memo(function EntityProperties({fields, sharedSetting, color}: {
    fields: EntityField[],
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
        const width = sharedSetting?.nodeShow?.nodeWidth ?? 240;
        return {position: 'absolute', left: `${width - 2}px`, backgroundColor: color}
    }, [color, sharedSetting?.nodeShow?.nodeWidth]);

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
                             <Tooltip title={tooltip(item)}>
                                 <Text style={itemKeyStyle} ellipsis={ellipsis}>
                                     {text(item)}
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
