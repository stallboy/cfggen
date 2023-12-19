import {Schema, STable} from "./model/schemaModel.ts";
import {Dispatch} from "react";
import {Badge, Select, Space} from "antd";

class TableWithLastName {
    table: STable;
    lastName: string;

    constructor(table: STable, lastName: string) {
        this.table = table;
        this.lastName = lastName;
    }
}

export function TableList({schema, curTable, setCurTable}: {
    schema: Schema | null, curTable: STable | null, setCurTable: Dispatch<string>
}) {

    if (schema == null) {
        return <Select loading={true}/>
    }

    let group2Tables = new Map<string, TableWithLastName[]>();
    for (let item of schema.itemMap.values()) {
        if (item.type == 'table') {
            let t = item as STable
            let group = ""
            let name = t.name
            let sp = name.split(".")
            if (sp.length > 1) {
                name = sp[sp.length - 1]
                group = sp.slice(0, sp.length - 1).join(".")
            }

            let tables = group2Tables.get(group);
            if (!tables) {
                tables = []
                group2Tables.set(group, tables)
            }
            tables.push(new TableWithLastName(t, name))
        }
    }
    let options = [];
    for (let group2Table of group2Tables.entries()) {
        let grp = group2Table[0]
        let tls = group2Table[1]
        let subOptions = [];

        for (let tl of tls) {
            let style = {backgroundColor: '#bbbbbb'}
            if (tl.table == curTable) {
                style = {backgroundColor: '#52c41a'};
            }

            let badge = <Badge count={tl.table.recordIds.length} overflowCount={999999}
                               style={style}/>

            subOptions.push({
                label: <Space>{tl.lastName}{badge} </Space>,
                value: tl.table.name,
            });
        }

        options.push({
            label: grp,
            value: grp,
            options: subOptions
        })
    }

    return <Select id='table'
                   showSearch
                   options={options}
                   style={{width: 200}}
                   value={curTable?.name}
                   placeholder="search a table"
                   optionFilterProp="children"
                   filterOption={(inputValue, option) => {
                       return !!option?.value.includes(inputValue);
                   }}
                   onChange={(value, _) => {
                       setCurTable(value);
                   }}
    />;
}
