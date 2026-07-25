import {memo, useEffect, useState} from "react";
import {App, Empty, Input, Result, Skeleton, Typography} from "antd";
import {searchServer} from "@/api/apiClient.ts";
import {queryKeys} from "@/services/queryKeys.ts";
import {useTranslation} from "react-i18next";
import {setQuery, useMyStore} from "@/store/store.ts";
import {useQuery} from "@tanstack/react-query";
import {NavList} from "./NavList.tsx";
import {getLastSegment} from "@/domain/strUtils.ts";


export const SearchValue = memo(function SearchValue() {
    const {server, query, searchMax} = useMyStore();
    const {notification} = App.useApp();
    const {t} = useTranslation();
    // 初始化为持久化的上次搜索词，与输入框 defaultValue={query} 保持一致
    const [value, setValue] = useState(query);

    const {data: searchResult, isFetching, error} = useQuery({
        queryKey: queryKeys.search(value, searchMax, server),
        queryFn: ({signal}) => searchServer(server, value, searchMax, signal),
        enabled: value.length > 0,
        retry: false,
    });

    useEffect(() => {
        if (error) {
            notification.error({title: `search err: ${error.message}`, placement: 'topRight', duration: 4});
        }
    }, [error, notification]);

    function onSearch(v: string) {
        setValue(v);
        setQuery(v);
    }

    let content;
    if (!value) {
        content = <Empty/>;
    } else if (searchResult == null) {
        content = isFetching ? <Skeleton/> : <Empty/>;
    } else if (searchResult.resultCode != 'ok') {
        content = <Result status="error" title={searchResult.resultCode}/>;
    } else {
        content = <NavList
            items={searchResult.items}
            rowKey={item => `${item.table}-${item.pk}-${item.fieldChain}`}
            toNav={item => ({table: item.table, id: item.pk})}
            renderTitle={item => `${getLastSegment(item.table)}-${item.pk}`}
            renderExtra={item => <Typography.Text type="secondary" ellipsis style={{maxWidth: 160}}>
                {item.fieldChain}{item.value ? `: ${item.value}` : ''}
            </Typography.Text>}
        />;
    }

    return <>
        <Input.Search placeholder='search value' defaultValue={query}
                      enterButton={t('search')}
                      size='large'
                      loading={isFetching}
                      onSearch={onSearch}/>
        {content}
    </>;
});
