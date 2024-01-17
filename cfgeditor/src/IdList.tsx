import {DefaultOptionType} from "antd/es/select/index";
import {Select, Skeleton} from "antd";
import {getIdOptions, isPkInteger} from "./model/schemaUtil.ts";
import {navTo, store, useLocationData} from "./model/store.ts";
import {useNavigate} from "react-router-dom";


export function IdList() {
    const navigate = useNavigate();
    const {schema} = store;
    const {curPage, curTableId, curId} = useLocationData();
    let curTable = schema ? schema.getSTable(curTableId) : null;

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
                       navigate(navTo(curPage, curTableId, value));
                   }}/>

}
