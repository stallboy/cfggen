export class HistoryItem {
    constructor(public table: string,
                public id: string) {
    }
}

export class History {

    constructor(public items: HistoryItem[] = [],
                public index: number = -1,
                public lastOpenIdMap: Map<string, string> = new Map<string, string>(),
    ) {
    }

    addItem(table: string, id: string): History {
        if (this.items.length > 0) {
            const item = this.items[this.index];
            if (item.table == table && item.id == id) {
                return this;
            } else {
                const maxHistory = 20;
                let startIdx = this.index - maxHistory;
                if (startIdx < 0) {
                    startIdx = 0;
                }
                const itemsCopy = this.items.slice(startIdx, this.index + 1)
                itemsCopy.push(new HistoryItem(table, id));

                let map = this.lastOpenIdMap;
                const oldId = map.get(table);
                if (oldId != id) {
                    map = new Map<string, string>([...map, [table, id]])
                }
                return new History(itemsCopy, itemsCopy.length - 1, map);
            }
        } else {
            return new History([new HistoryItem(table, id)], 0, new Map<string, string>());
        }
    }

    canPrev(): boolean {
        return this.items.length > 0 && this.index > 0;
    }

    prev(): History {
        if (this.canPrev()) {
            // 透传 lastOpenIdMap：prev/next 只移动导航栈游标，不清空「每表最后打开 id」的记忆
            return new History(this.items, this.index - 1, this.lastOpenIdMap);
        } else {
            return this;
        }
    }

    canNext(): boolean {
        return this.items.length > 0 && this.index < this.items.length - 1;
    }

    next(): History {
        if (this.canNext()) {
            return new History(this.items, this.index + 1, this.lastOpenIdMap);
        } else {
            return this;
        }
    }

    cur(): HistoryItem | undefined {
        if (this.items.length > 0) {
            return this.items[this.index];
        }
    }

    findLastOpenId(table: string): string | undefined {
        return this.lastOpenIdMap.get(table);
    }
}
