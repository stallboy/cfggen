import {useState} from "react";
import {App, Button, Empty, Input, Result, Table} from "antd";
import {SearchResult, SearchResultItem} from "./model/searchModel.ts";
import type {ColumnsType} from "antd/es/table";
import {useTranslation} from "react-i18next";
import {getId} from "./func/recordRefEntity.ts";


function getLabel(table: string, id: string): string {
    let seps = table.split('.');
    return seps[seps.length - 1] + '-' + id;
}


function getColumns(onClick: (item: SearchResultItem) => void): ColumnsType<SearchResultItem> {
    return [
        {
            title: 'id',
            align: 'left',
            width: 200,
            key: 'id',
            ellipsis: {
                showTitle: false
            },
            render: (_text: any, item: SearchResultItem, _index: number) => {
                let label = getLabel(item.table, item.pk);
                return <Button type={'link'} onClick={() => onClick(item)}>
                    {label}
                </Button>;
            }
        },
        {
            title: 'fieldChain',
            dataIndex: 'fieldChain',
            width: 200,
            key: 'fieldChain',
            ellipsis: true,
        },
        {
            title: 'value',
            dataIndex: 'value',
            width: 300,
            key: 'value',
            ellipsis: true,
        }
    ];
}


export function SearchValue({searchMax, server, query, setQuery, tryReconnect, setCurTableAndId}: {
    searchMax: number;
    server: string;
    query: string;
    setQuery: (q: string) => void;
    tryReconnect: () => void;
    setCurTableAndId: (table: string, id: string) => void;
}) {
    const [loading, setLoading] = useState<boolean>(false);
    const [searchResult, setSearchResult] = useState<SearchResult | null>(null);
    const {notification} = App.useApp();
    const {t} = useTranslation();

    function onSearch(value: string) {
        setQuery(value);
        setLoading(true);
        let url = `http://${server}/search?q=${value}&max=${searchMax}`;
        const fetchData = async () => {
            const response = await fetch(url);
            const recordResult: SearchResult = await response.json();
            setSearchResult(recordResult);
            setLoading(false);
        }
        fetchData().catch((err) => {
            notification.error({message: `fetch ${url} err: ${err.toString()}`, placement: 'topRight', duration: 4});
            tryReconnect();
            setLoading(false);
        });
    }


    function onClickItem(item: SearchResultItem) {
        setCurTableAndId(item.table, item.pk);
    }

    let content;
    if (searchResult == null) {
        content = <Empty/>
    } else if (searchResult.resultCode != 'ok') {
        content = <Result status={'error'} title={searchResult.resultCode}/>
    } else {
        let columns = getColumns(onClickItem);
        content = <div>q={searchResult.q}&max={searchResult.max}
            <Table columns={columns} dataSource={searchResult.items}
                   rowKey={(item: SearchResultItem) => getId(item.table, item.pk)}/>
        </div>
    }

    return <div>
        <Input.Search placeholder='search value' defaultValue={query}
                      enterButton={t('search')}
                      size='large'
                      loading={loading}
                      onSearch={onSearch}/>
        {content}
    </div>

}
