import {useState} from "react";
import {Button, Divider, Empty, Input, Result, Space, Table} from "antd";
import {SearchResult, SearchResultItem} from "./model/searchModel.ts";
import type {ColumnsType} from "antd/es/table";


function getLabel(table: string, id: string): string {
    let seps = table.split('.');
    return seps[seps.length - 1] + '-' + id;
}


function getColumns(onClick: (item: SearchResultItem) => void): ColumnsType<SearchResultItem> {
    const columns: ColumnsType<SearchResultItem> = [
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

    return columns;
}


export function SearchValue({searchMax, setCurTableAndId}: {
    searchMax: number;
    setCurTableAndId: (table: string, id: string) => void;
}) {
    const [query, setQuery] = useState<string>('');
    const [searchResult, setSearchResult] = useState<SearchResult | null>(null);


    function onInputChange(e: React.ChangeEvent<HTMLInputElement>) {
        setQuery(e.target.value);
    }

    function onSearch() {
        const fetchData = async () => {
            const response = await fetch(`http://localhost:3456/search?q=${query}&max=${searchMax}`);
            const recordResult: SearchResult = await response.json();
            setSearchResult(recordResult);
        }
        fetchData().catch(console.error);
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
        content = <div> q={query}&max={searchMax}
            <Table columns={columns} dataSource={searchResult.items} />
        </div>
    }

    return <div>
        <Input placeholder={'search value'} value={query} onChange={onInputChange}></Input>
        <Space/>
        <Button onClick={onSearch}>搜索</Button>
        <Divider/>
        {content}
    </div>

}