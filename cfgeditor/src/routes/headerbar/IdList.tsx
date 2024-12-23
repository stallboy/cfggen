import {Select} from "antd";
import {getIdOptions} from "../table/schemaUtil.tsx";
import {navTo, store, useLocationData} from "../setting/store.ts";
import {useNavigate} from "react-router-dom";
import {STable} from "../table/schemaModel.ts";
import {memo, useMemo} from "react";
import {EntityEditFieldOption} from "../../flow/entityModel.ts";

function filterOption(inputValue: string, option?: EntityEditFieldOption) {
    return option!.labelStr.toLowerCase().includes(inputValue.toLowerCase())
}


export const IdList = memo(function ({curTable}: {
    curTable: STable,
}) {
    const navigate = useNavigate();
    const {curPage, curTableId, curId} = useLocationData();

    const options = useMemo(() => getIdOptions(curTable),
        [curTable]);

    return <Select id='id'
                   showSearch
                   options={options}
                   style={{width: 240}}
                   value={curId}
                   placeholder="search a record"
                   filterOption={filterOption}
                   onChange={(value) => {
                       const {isEditMode} = store;
                       navigate(navTo(curPage, curTableId, value, isEditMode));
                   }}/>

});
