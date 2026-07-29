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
    const resInfoQuery = useQuery({
        queryKey: queryKeys.resInfo(),
        queryFn: readResInfosAsync,
        enabled: !!data,
    })

    // console.log(isError, _error, data);

    // resInfo 完成前不渲染 CfgEditorApp：readResInfosAsync 会设置 resourceDir/resMap，
    // 提前渲染会让 findAllResInfos 用空 resourceDir 算出错误路径，且高度少算的 layout 结果会被
    // React Query 缓存（queryKey 不含 resourceDir），导致节点重叠最长持续到 staleTime 过期
    if (isError || (data && !resInfoQuery.isPending)) {
        return <CfgEditorApp/>
    }
}
