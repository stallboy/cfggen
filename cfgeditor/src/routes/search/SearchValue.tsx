import {memo, useEffect, useState} from "react";
import {App, Empty, Input, Result, Skeleton, Typography} from "antd";
import {SearchResult} from "@/api/searchModel";
import {useTranslation} from "react-i18next";
import {setQuery, useMyStore} from "@/store/store";
import {useQuery} from "@tanstack/react-query";
import {NavList} from "./NavList.tsx";


function getLastSegment(table: string): string {
    const seps = table.split('.');
    return seps[seps.length - 1];
}

export const SearchValue = memo(function SearchValue() {
    const {server, query, searchMax} = useMyStore();
    const {notification} = App.useApp();
    const {t} = useTranslation();
    const [value, setValue] = useState(query);

    const {data: searchResult, isFetching, error} = useQuery({
        queryKey: ['search', value, searchMax, server],
        queryFn: async ({signal}) => {
            const url = `http://${server}/search?q=${encodeURIComponent(value)}&max=${searchMax}`;
            const res = await fetch(url, {signal});
            return (await res.json()) as SearchResult;
        },
        enabled: value.length > 0,
        retry: false,
    });

    useEffect(() => {
        if (error) {
            notification.error({message: `search err: ${error.message}`, placement: 'topRight', duration: 4});
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
            renderExtra={item => <Typography.Text type="secondary" ellipsis
                                                  style={{maxWidth: 120}}>{item.value}</Typography.Text>}
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
