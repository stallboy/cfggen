import {Select} from "antd";
import {getIdOptions} from "../table/schemaUtil.tsx";
import {navTo, useMyStore, useLocationData} from "../../store/store.ts";
import {useNavigate} from "react-router-dom";
import {STable} from "../table/schemaModel.ts";
import {memo, useMemo, useCallback} from "react";

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

    return <Select id='id'
                   showSearch={{
                       filterOption: (inputValue, option) =>
                           option?.labelStr.toLowerCase().includes(inputValue.toLowerCase()) ?? false
                   }}
                   options={options}
                   style={{width: 240}}
                   value={curId}
                   placeholder="search a record"
                   onChange={handleChange}/>;
});
