import {DefaultOptionType} from "antd/es/select/index";
import {Select} from "antd";
import {getIdOptions, isPkInteger,} from "../table/schemaUtil.ts";
import {navTo, store, useLocationData} from "../setting/store.ts";
import {useNavigate} from "react-router-dom";
import {STable} from "../table/schemaModel.ts";
import {memo, useMemo} from "react";

function filterOption(inputValue: string, option?: DefaultOptionType) {
    return (option!.label as string).toLowerCase().includes(inputValue.toLowerCase())
}

const intFilterSorts = {
    filterSort: (optionA: DefaultOptionType, optionB: DefaultOptionType) =>
        parseInt(optionA.value as string) - parseInt(optionB.value as string)
};

export const IdList = memo(function IdList({curTable}: {
    curTable: STable,
}) {
    const navigate = useNavigate();
    const {curPage, curTableId, curId} = useLocationData();

    const options = useMemo(() => getIdOptions(curTable), [curTable]);
    const filterSorts = useMemo(() => isPkInteger(curTable) ? intFilterSorts : {}, [curTable]);

    return <Select id='id'
                   showSearch
                   options={options}
                   style={{width: 240}}
                   value={curId}
                   placeholder="search a record"
                   {...filterSorts}
                   filterOption={filterOption}
                   onChange={(value) => {
                       const {isEditMode} = store;
                       navigate(navTo(curPage, curTableId, value, isEditMode));
                   }}/>

});
