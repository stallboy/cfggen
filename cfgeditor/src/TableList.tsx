import {Schema} from "./model.ts";
import {Dispatch} from "react";
import {Select, Space, Spin} from "antd";
import {TableOutlined} from "@ant-design/icons";

export function getTableNames(schema: Schema): string[] {
    let res: string[] = [];
    for (let item of schema.items) {
        if (item.type == 'table') {
            res.push(item.name);
        }
    }
    return res;
}

export function TableList({tables, curTable, setCurTable}: {
    tables: string[] | null, curTable: string | null, setCurTable: Dispatch<string>
}) {
    if (tables == null) {
        return <Spin/>
    }

    let options = [];
    for (let table of tables) {
        options.push({label: <Space><TableOutlined/> {table}</Space>, value: table});
    }
    return <Select
        showSearch
        options={options}
        style={{width: 300}}
        value={curTable}
        placeholder="search a table"
        optionFilterProp="label"
        filterOption={(inputValue, option) =>
            option!.value.toUpperCase().includes(inputValue.toUpperCase())
        }
        onChange={(value, _) => {
            setCurTable(value);
            // console.log(value);
        }}
    />;
}