import {DefaultOptionType} from "antd/es/select/index";
import {Select} from "antd";
import {getIdOptions, isPkInteger,} from "../table/schemaUtil.ts";
import {navTo, store, useLocationData} from "../setting/store.ts";
import {useNavigate} from "react-router-dom";
import {STable} from "../table/schemaModel.ts";
import {memo, useMemo} from "react";


export const IdList = memo(function IdList({curTable}: {
    curTable: STable,
}) {
    const navigate = useNavigate();
    const {curPage, curTableId, curId} = useLocationData();

    let options = useMemo(() => getIdOptions(curTable), [curTable]);
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
                       const {isEditMode} = store;
                       navigate(navTo(curPage, curTableId, value, isEditMode));
                   }}/>

});
