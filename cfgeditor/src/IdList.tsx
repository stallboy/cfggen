import {RecordId} from "./model.ts";
import {Dispatch} from "react";
import {Badge, Select, Space, Spin} from "antd";
import {MinusOutlined} from "@ant-design/icons";


export function IdList({recordIds, recordCount, curId, setCurId}: {
    recordIds: RecordId[] | null,
    recordCount: number,
    curId: string | null
    setCurId: Dispatch<string>
}) {
    if (recordIds == null) {
        return <Spin/>
    }

    let options = [];
    for (let id of recordIds) {
        options.push({label: <Space><MinusOutlined/> {id.id}</Space>, value: id.id});
    }

    return <Space>
        <Badge count={recordCount}
               overflowCount={999999}
               style={{backgroundColor: '#52c41a'}}
        />
        <Select
            showSearch
            options={options}
            style={{width: 200}}
            value={curId}
            placeholder="search a table"
            optionFilterProp="label"
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
