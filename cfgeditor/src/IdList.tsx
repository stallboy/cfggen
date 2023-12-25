import {STable} from "./model/schemaModel.ts";
import {Select, Skeleton} from "antd";


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
        options.push({label: id.title ? `${id.id}-${id.title}` : id.id, value: id.id});
    }

    return <Select id='id'
                   showSearch
                   options={options}
                   style={{width: 160}}
                   value={curId}
                   placeholder="search a record"
                   filterOption={(inputValue, option) =>
                       option!.label.toUpperCase().includes(inputValue.toUpperCase())
                   }
                   onChange={(value, _) => {
                       setCurId(value);
                       // console.log(value);
                   }}/>

}
