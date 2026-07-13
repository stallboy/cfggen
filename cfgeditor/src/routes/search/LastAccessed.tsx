import {memo, useMemo} from "react";
import {Schema} from "@/domain/schema";
import {useMyStore} from "@/store/store";
import {NavList} from "./NavList.tsx";

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
        renderTitle={item => `${item.id}-${item.title}`}
    />;
});
