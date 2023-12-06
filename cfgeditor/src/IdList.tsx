import {STable} from "./model.ts";
import {Dispatch} from "react";
import {Badge, Select, Space, Skeleton} from "antd";
import {MinusOutlined} from "@ant-design/icons";


export function IdList({curTable, curId, setCurId}: {
    curTable: STable | null,
    curId: string | null
    setCurId: Dispatch<string>
}) {
    if (curTable == null) {
        return <Skeleton.Input/>
    }

    let options = [];
    for (let id of curTable.recordIds) {
        options.push({label: <Space><MinusOutlined/> {id.id}</Space>, value: id.id});
    }

    return <Space>
        <Badge count={curTable.recordCount}
               overflowCount={999999}
               style={{backgroundColor: '#52c41a'}}
        />
        <Select
            showSearch
            options={options}
            style={{width: 200}}
            value={curId}
            placeholder="search a record"
            filterOption={(inputValue, option) =>
                option!.value.toUpperCase().includes(inputValue.toUpperCase())
            }
            onChange={(value, _) => {
                setCurId(value);
                // console.log(value);
            }}
        />
    </Space>
}
