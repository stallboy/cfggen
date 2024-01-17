import {STable} from "./model/schemaModel.ts";
import {Badge, Select, Space} from "antd";
import {getFixCurIdByTable, navTo, store, useLocationData} from "./model/store.ts";
import {useNavigate} from "react-router-dom";

interface TableWithLastName {
    tableId: string;
    table: STable;
    lastName: string;
}


export function TableList() {
    const {schema} = store;

    const {curPage, curTableId, curId} = useLocationData();
    const navigate = useNavigate();

    if (schema == null) {
        return <Select id='table' loading={true}/>
    }

    let group2Tables = new Map<string, TableWithLastName[]>();
    for (let item of schema.itemMap.values()) {
        if (item.type == 'table') {
            let table = item as STable;
            let tableId = item.name;
            let group = ""
            let lastName = tableId;
            let sp = lastName.split(".")
            if (sp.length > 1) {
                lastName = sp[sp.length - 1]
                group = sp.slice(0, sp.length - 1).join(".")
            }

            let tables = group2Tables.get(group);
            if (!tables) {
                tables = []
                group2Tables.set(group, tables)
            }
            tables.push({tableId, table, lastName})
        }
    }
    let options = [];
    for (let group2Table of group2Tables.entries()) {
        let grp = group2Table[0]
        let tls = group2Table[1]
        let subOptions = [];

        for (let tl of tls) {
            let style = {backgroundColor: '#bbbbbb'}
            if (tl.tableId == curTableId) {
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
                   value={curTableId}
                   placeholder="search a table"
                   optionFilterProp="children"
                   filterOption={(inputValue, option) => {
                       return !!option?.value.includes(inputValue);
                   }}
                   onChange={(tableId, _) => {
                       const id = getFixCurIdByTable(tableId, curId);
                       navigate(navTo(curPage, tableId, id));
                   }}
    />;
}
