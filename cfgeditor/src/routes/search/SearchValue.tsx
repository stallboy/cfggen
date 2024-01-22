import {useState} from "react";
import {App, Button, Empty, Input, Result, Table} from "antd";
import {SearchResult, SearchResultItem} from "./searchModel.ts";
import {useTranslation} from "react-i18next";
import {getId} from "../record/recordRefEntity.ts";
import {navTo, setQuery, store, useLocationData} from "../setting/store.ts";
import {useNavigate} from "react-router-dom";


function getLabel(table: string, id: string): string {
    let seps = table.split('.');
    return seps[seps.length - 1] + '-' + id;
}

export function SearchValue() {
    const {server, query, searchMax} = store;
    const navigate = useNavigate();

    const [loading, setLoading] = useState<boolean>(false);
    const [searchResult, setSearchResult] = useState<SearchResult | null>(null);
    const {notification} = App.useApp();
    const {t} = useTranslation();
    const {curPage} = useLocationData();

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
            setLoading(false);
        });
    }


    let content;
    if (searchResult == null) {
        content = <Empty/>
    } else if (searchResult.resultCode != 'ok') {
        content = <Result status={'error'} title={searchResult.resultCode}/>
    } else {
        let columns = [
            {
                title: 'id',
                // align: 'left',
                width: 200,
                key: 'id',
                ellipsis: {
                    showTitle: false
                },
                render: (_text: any, item: SearchResultItem, _index: number) => {
                    let label = getLabel(item.table, item.pk);
                    return <Button type={'link'} onClick={() => {
                        navigate(navTo(curPage, item.table, item.pk));
                    }}>
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
