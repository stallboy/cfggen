import {DefaultOptionType} from "antd/es/select/index";
import {Select} from "antd";
import {getIdOptions, isPkInteger,} from "./model/schemaUtil.ts";
import {navTo, useLocationData} from "./model/store.ts";
import {useNavigate} from "react-router-dom";
import {STable} from "./model/schemaModel.ts";


export function IdList({curTable}: {
    curTable: STable,
}) {
    const navigate = useNavigate();
    const {curPage, curTableId, curId} = useLocationData();

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
                       navigate(navTo(curPage, curTableId, value));
                   }}/>

}
