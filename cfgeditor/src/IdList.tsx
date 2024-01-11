import {DefaultOptionType} from "antd/es/select/index";
import {STable} from "./model/schemaModel.ts";
import {Select, Skeleton} from "antd";
import {getIdOptions, isPkInteger} from "./model/schemaUtil.ts";


export function IdList({curTable, curId, setCurId}: {
    curTable: STable | null,
    curId: string | null
    setCurId: (id: string) => void;
}) {
    if (curTable == null) {
        return <Skeleton.Input/>
    }

    let options = getIdOptions(curTable);
    let filterSorts = {};
    if (isPkInteger(curTable)) {
        filterSorts = {
            filterSort: (optionA: DefaultOptionType, optionB: DefaultOptionType) =>
                parseInt(optionA.value as string) - parseInt(optionB.value as string)
        };
    }

    return <Select id='id'
                   showSearch
                   options={options}
                   style={{width: 160}}
                   value={curId}
                   placeholder="search a record"
                   {...filterSorts}
                   filterOption={(inputValue, option) =>
                       option!.label.toUpperCase().includes(inputValue.toUpperCase())
                   }
                   onChange={(value, _) => {
                       setCurId(value);
                       // console.log(value);
                   }}/>

}
