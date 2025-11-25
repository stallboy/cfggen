import {STable} from "../table/schemaModel.ts";
import {Select} from "antd";
import {getLastOpenIdByTable, navTo, useMyStore, useLocationData} from "../../store/store.ts";
import {useNavigate} from "react-router-dom";
import {Schema} from "../table/schemaUtil.tsx";
import {memo, useMemo, useCallback} from "react";

interface TableWithLastName {
    tableId: string;
    table: STable;
    lastName: string;
}

const SELECT_STYLE = {width: 200} as const;
const LABEL_COUNT_STYLE = {fontSize: '0.85em'} as const;

function generateTableOptions(schema: Schema) {
    const group2Tables = new Map<string, TableWithLastName[]>();

    // Group tables by their namespace
    for (const item of schema.itemMap.values()) {
        if (item.type === 'table') {
            const table = item as STable;
            const tableId = item.name;
            const [group, lastName] = parseTableId(tableId);

            const tables = group2Tables.get(group) ?? [];
            if (!tables.length) {
                group2Tables.set(group, tables);
            }
            tables.push({tableId, table, lastName});
        }
    }

    // Generate options with groups
    return Array.from(group2Tables.entries()).map(([group, tables]) => ({
        label: group,
        value: group,
        options: tables.map(tl => ({
            label: <>{tl.lastName} <i style={LABEL_COUNT_STYLE}>{tl.table.recordIds.length}</i></>,
            value: tl.table.name,
        }))
    }));
}

function parseTableId(tableId: string): [string, string] {
    const parts = tableId.split('.');
    if (parts.length > 1) {
        return [
            parts.slice(0, -1).join('.'),
            parts[parts.length - 1]
        ];
    }
    return ['', tableId];
}

export const TableList = memo(function ({schema}: { schema: Schema }) {
    const {curPage, curTableId} = useLocationData();
    const navigate = useNavigate();
    const {isEditMode} = useMyStore();

    const options = useMemo(() =>
            schema ? generateTableOptions(schema) : [],
        [schema]
    );

    const handleChange = useCallback((tableId: string) => {
        const id = getLastOpenIdByTable(schema, tableId);
        navigate(navTo(curPage, tableId, id || '', isEditMode));
    }, [schema, navigate, curPage, isEditMode]);

    if (!schema) {
        return <Select id='table' loading={true}/>;
    }

    return (
        <Select id='table'
                showSearch={{
                    optionFilterProp: "children",
                    filterOption: (input, option) =>
                        option?.value.includes(input) ?? false
                }}
                options={options}
                style={SELECT_STYLE}
                value={curTableId}
                placeholder="search a table"
                onChange={handleChange}
        />
    );
});
