import React from 'react';
import {Table} from 'antd';
import type {ColumnsType} from 'antd/es/table';

interface DataType {
    name: string;
    value: string | number | boolean;
}

const columns: ColumnsType<DataType> = [
    {
        title: 'name',
        dataIndex: 'name',
        align: 'right',
        width: 100,

    },
    {
        title: 'value',
        dataIndex: 'value',
        width: 300,
        // render: (_, { tags }) => (
        //     <>
        //         {tags.map((tag) => {
        //             let color = tag.length > 5 ? 'geekblue' : 'green';
        //             if (tag === 'loser') {
        //                 color = 'volcano';
        //             }
        //             return (
        //                 <Tag color={color} key={tag}>
        //                     {tag.toUpperCase()}
        //                 </Tag>
        //             );
        //         })}
        //     </>
        // ),

    },
];

const data: DataType[] = [
    {
        name: 'Name:',
        value: 'John Brown',
    },
    {
        name: 'Age:',
        value: 32,
    },
    {
        name: 'Address:',
        value: 'New York',
    },

    {
        name: 'Tags:',
        value: true,
    },

];

const App: React.FC = () => <Table bordered showHeader={false} columns={columns} dataSource={data} pagination={false}/>;

export default App;