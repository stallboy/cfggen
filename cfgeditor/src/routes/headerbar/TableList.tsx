import {STable} from "../table/schemaModel.ts";
import {Select} from "antd";
import {getLastOpenIdByTable, navTo, store, useLocationData} from "../setting/store.ts";
import {useNavigate} from "react-router-dom";
import {Schema} from "../table/schemaUtil.tsx";
import {memo} from "react";

interface TableWithLastName {
    tableId: string;
    table: STable;
    lastName: string;
}


export const TableList = memo(function ({schema}: { schema: Schema }) {
    const {curPage, curTableId} = useLocationData();
    const navigate = useNavigate();
    const {isEditMode} = store;

    if (!schema) {
        return <Select id='table' loading={true}/>
    }

    const options= [];
    const group2Tables = new Map<string, TableWithLastName[]>();
    for (const item of schema.itemMap.values()) {
        if (item.type == 'table') {
            const table = item as STable;
            const tableId = item.name;
            let group = ""
            let lastName = tableId;
            const sp = lastName.split(".")
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
    for (const group2Table of group2Tables.entries()) {
        const grp = group2Table[0]
        const tls = group2Table[1]
        const subOptions = [];

        for (const tl of tls) {
            subOptions.push({
                label: <>{tl.lastName} <i style={{fontSize: '0.85em'}}> {tl.table.recordIds.length}</i></>,
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
                   onChange={(tableId) => {
                       const id = getLastOpenIdByTable(schema, tableId);
                       navigate(navTo(curPage, tableId, id || '', isEditMode));
                   }}
    />;
});
