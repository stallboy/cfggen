import {Select} from "antd";
import {getIdOptions} from "../table/schemaUtil.tsx";
import {navTo, useMyStore, useLocationData} from "../setting/store.ts";
import {useNavigate} from "react-router-dom";
import {STable} from "../table/schemaModel.ts";
import {memo, useMemo, useCallback} from "react";
import {EntityEditFieldOption} from "../../flow/entityModel.ts";

const SELECT_STYLE = {width: 240} as const;

const FILTER_OPTION = (inputValue: string, option?: EntityEditFieldOption) => 
    option?.labelStr.toLowerCase().includes(inputValue.toLowerCase()) ?? false;

export const IdList = memo(function ({curTable}: {
    curTable: STable,
}) {
    const navigate = useNavigate();
    const {curPage, curTableId, curId} = useLocationData();
    const {isEditMode} = useMyStore();

    const options = useMemo(() => getIdOptions(curTable), [curTable]);
    
    const handleChange = useCallback((value: string) => {
        navigate(navTo(curPage, curTableId, value, isEditMode));
    }, [navigate, curPage, curTableId, isEditMode]);

    return <Select 
        id='id'
        showSearch
        options={options}
        style={SELECT_STYLE}
        value={curId}
        placeholder="search a record"
        filterOption={FILTER_OPTION}
        onChange={handleChange}
    />;
});
