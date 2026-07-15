import {memo, useMemo} from "react";
import {Schema} from "@/domain/schema.ts";
import {useMyStore} from "@/store/store.ts";
import {NavList} from "./NavList.tsx";

function getLastSegment(table: string): string {
    const seps = table.split('.');
    return seps[seps.length - 1];
}

class LastAccessedItem {
    constructor(public table: string,
                public id: string,
                public title: string) {
    }
}

export const LastAccessed = memo(function LastAccessed({schema}: {
    schema: Schema | undefined;
}) {
    const {history} = useMyStore();

    const uniqItems: LastAccessedItem[] = useMemo(() => {
        const uniq: LastAccessedItem[] = [];
        for (const item of history.items.toReversed()) {
            if (!uniq.some((it: LastAccessedItem) => it.table === item.table && it.id === item.id)) {
                const title = schema?.getIdTitle(item.table, item.id) || '';
                uniq.push(new LastAccessedItem(item.table, item.id, title));
            }
        }

        return uniq;
    }, [history.items, schema]);

    return <NavList
        items={uniqItems}
        rowKey={item => `${item.table}-${item.id}`}
        toNav={item => ({table: item.table, id: item.id})}
        renderTitle={item => `${getLastSegment(item.table)} ${item.id}-${item.title}`}
        addHistory={false}
    />;
});
