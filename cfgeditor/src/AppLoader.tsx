import {useQuery} from "@tanstack/react-query";
import {readPrefAsync} from "./routes/setting/storage.ts";
import {CfgEditorApp} from "./CfgEditorApp.tsx";

export function AppLoader() {
    const {isError, error: _error, data} = useQuery({
        queryKey: ['setting'],
        queryFn: readPrefAsync,
        staleTime: Infinity,
        retry: 0,
    })

    // console.log(isError, _error, data);

    if (isError || data) {
        return <CfgEditorApp/>
    }
}
