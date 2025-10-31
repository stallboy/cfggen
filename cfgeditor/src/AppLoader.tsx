import {useQuery} from "@tanstack/react-query";
import {readPrefAsyncOnce} from "./store/storage.ts";
import {CfgEditorApp} from "./CfgEditorApp.tsx";
import {readResInfosAsync} from "./res/readResInfosAsync.ts";

export function AppLoader() {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const {isError, error: _error, data} = useQuery({
        queryKey: ['setting'],
        queryFn: readPrefAsyncOnce,
        staleTime: Infinity,
        retry: 0,
    })
    useQuery({
        queryKey: ['setting', 'resInfo'],
        queryFn: readResInfosAsync,
        enabled: !!data,
    })

    // console.log(isError, _error, data);

    if (isError || data) {
        return <CfgEditorApp/>
    }
}
