import {useQuery} from "@tanstack/react-query";
import {readPrefAsyncOnce} from "./routes/setting/storage.ts";
import {CfgEditorApp} from "./CfgEditorApp.tsx";
import {readResInfosAsync} from "./routes/setting/readResInfosAsync.ts";

export function AppLoader() {
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
