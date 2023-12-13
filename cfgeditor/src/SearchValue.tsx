import {useState} from "react";
import {Button, Empty, Input, Result, Table} from "antd";
import {SearchResult, SearchResultItem} from "./model/searchModel.ts";
import type {ColumnsType} from "antd/es/table";


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


export function SearchValue({searchMax, setCurTableAndId}: {
    searchMax: number;
    setCurTableAndId: (table: string, id: string) => void;
}) {
    const [loading, setLoading] = useState<boolean>(false);
    const [query, setQuery] = useState<string>('');
    const [searchResult, setSearchResult] = useState<SearchResult | null>(null);

    function onSearch(value: string) {
        setQuery(value);
        setLoading(true);
        const fetchData = async () => {
            const response = await fetch(`http://localhost:3456/search?q=${value}&max=${searchMax}`);
            const recordResult: SearchResult = await response.json();
            setSearchResult(recordResult);
            setLoading(false);
        }
        fetchData().catch((err) => {
            console.log(err)
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
            <Table columns={columns} dataSource={searchResult.items}/>
        </div>
    }

    return <div>
        <Input.Search placeholder='search value' defaultValue={query}
                      enterButton='搜索'
                      size='large'
                      loading={loading}
                      onSearch={onSearch}/>
        {content}
    </div>

}