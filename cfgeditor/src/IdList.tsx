import {STable} from "./model/schemaModel.ts";
import {Select, Space, Skeleton} from "antd";


export function IdList({curTable, curId, setCurId}: {
    curTable: STable | null,
    curId: string | null
    setCurId: (id: string) => void;
}) {
    if (curTable == null) {
        return <Skeleton.Input/>
    }

    let options = [];
    for (let id of curTable.recordIds) {
        options.push({label: <Space>{id.id}</Space>, value: id.id});
    }

    return <Select
        showSearch
        options={options}
        style={{width: 160}}
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

}
