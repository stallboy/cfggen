import {memo, useMemo} from "react";
import {Schema} from "@/domain/schema";
import {NavList} from "./NavList.tsx";

function getLastSegment(table: string): string {
    const seps = table.split('.');
    return seps[seps.length - 1];
}
import TimeAgo from 'react-timeago'

class LastModifiedItem {
    constructor(public table: string,
                public id: string,
                public title: string,
                public lastModified: number) {
    }
}

export const LastModified = memo(function LastModified({schema}: {
    schema: Schema | undefined;
}) {

    const orderedItems: LastModifiedItem[] = useMemo(() => {
        if (schema == undefined) {
            return [];
        }
        const ordered: LastModifiedItem[] = [];
        for (const e of schema.lastModifiedMap.entries()) {
            const table: string = e[0]
            const idMap = e[1];
            const sTable = schema.getSTable(table);
            if (sTable == null) {
                continue;
            }
            for (const it of idMap.entries()) {
                const id: string = it[0];
                const lastModified: number = it[1]
                const title: string = sTable.idMap?.get(id)?.title || '';
                ordered.push(new LastModifiedItem(table, id, title, lastModified));
            }
        }
        ordered.sort((a: LastModifiedItem, b: LastModifiedItem) => b.lastModified - a.lastModified);
        return ordered;
    }, [schema]);


    if (!schema) return <></>;

    return <NavList
        items={orderedItems}
        rowKey={item => `${item.table}-${item.id}`}
        toNav={item => ({table: item.table, id: item.id})}
        renderTitle={item => `${getLastSegment(item.table)} ${item.id}-${item.title}`}
        renderExtra={item => <TimeAgo date={item.lastModified}/>}
    />;
});
