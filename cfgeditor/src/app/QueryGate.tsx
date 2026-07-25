import {ReactNode} from "react";
import {Result, Spin} from "antd";

// useQuery 结果的四连守卫：isLoading → isError → !data → resultCode != 'ok'，
// 全过后才把 data 交给 children 渲染。loading 占位可选（默认 Spin，传 null 表示什么都不渲染）；
// empty 文案各处不同，由调用方给出。
export function QueryGate<T extends { resultCode: string }>({query, loading, emptyTitle, children}: {
    query: {
        isLoading: boolean;
        isError: boolean;
        error: Error | null;
        data: T | undefined;
    };
    loading?: ReactNode;
    emptyTitle: string;
    children: (data: T) => ReactNode;
}) {
    if (query.isLoading) {
        return loading === undefined ? <Spin/> : <>{loading}</>;
    }

    if (query.isError) {
        return <Result status={'error'} title={query.error?.message}/>;
    }

    if (!query.data) {
        return <Result title={emptyTitle}/>;
    }

    if (query.data.resultCode != 'ok') {
        return <Result status={'error'} title={query.data.resultCode}/>;
    }

    return <>{children(query.data)}</>;
}
