import {Select} from "antd";
import {getIdOptionsWithNew, NEW_RECORD_ID} from "../table/schemaUtil.tsx";
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

    const options = useMemo(() => getIdOptionsWithNew(curTable), [curTable]);

    // 当只有new record时，选中它
    const defaultCurId = useMemo(() => {
        if (!curId && options.length > 0 && options[0].value === NEW_RECORD_ID) {
            return NEW_RECORD_ID;
        }
        return curId;
    }, [curId, options]);

    const handleChange = useCallback((value: string) => {
        navigate(navTo(curPage, curTableId, value, isEditMode));
    }, [navigate, curPage, curTableId, isEditMode]);

    return <Select id='id'
                   showSearch={{
                       filterOption: (inputValue, option) =>
                           option?.labelstr.toLowerCase().includes(inputValue.toLowerCase()) ?? false
                   }}
                   options={options}
                   style={{width: 240}}
                   value={defaultCurId}
                   placeholder="search a record"
                   onChange={handleChange}/>;
});
