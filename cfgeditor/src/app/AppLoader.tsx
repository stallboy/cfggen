import {useQuery} from "@tanstack/react-query";
import {readPrefAsyncOnce} from "@/store/storage";
import {CfgEditorApp} from "./CfgEditorApp";
import {queryKeys} from "@/services/queryKeys.ts";
import {readResInfosAsync} from "@/res/readResInfosAsync";

export function AppLoader() {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const {isError, error: _error, data} = useQuery({
        queryKey: queryKeys.setting(),
        queryFn: readPrefAsyncOnce,
        staleTime: Infinity,
        retry: 0,
    })
    useQuery({
        queryKey: queryKeys.resInfo(),
        queryFn: readResInfosAsync,
        enabled: !!data,
    })

    // console.log(isError, _error, data);

    if (isError || data) {
        return <CfgEditorApp/>
    }
}
