export class HistoryItem {
    constructor(public table: string, public id: string) {
    }
}

export class History {
    constructor(public items: HistoryItem[] = [], public index: number = -1) {
    }

    addItem(table: string, id: string): History {
        if (this.items.length > 0) {
            let item = this.items[this.index];
            if (item.table == table && item.id == id) {
                return this;
            } else {
                const maxHistory = 10;
                let startIdx = this.index - maxHistory;
                if (startIdx < 0) {
                    startIdx = 0;
                }
                let itemsCopy = this.items.slice(startIdx, this.index + 1)
                itemsCopy.push(new HistoryItem(table, id));
                return new History(itemsCopy, itemsCopy.length - 1);
            }
        } else {
            return new History([new HistoryItem(table, id)], 0);
        }
    }

    canPrev(): boolean {
        return this.items.length > 0 && this.index > 0;
    }

    prev(): History {
        if (this.canPrev()) {
            return new History(this.items, this.index - 1);
        } else {
            return this;
        }
    }

    canNext(): boolean {
        return this.items.length > 0 && this.index < this.items.length - 1;
    }

    next(): History {
        if (this.canNext()) {
            return new History(this.items, this.index + 1);
        } else {
            return this;
        }
    }

    cur(): HistoryItem | null {
        if (this.items.length > 0) {
            return this.items[this.index];
        }
        return null;
    }
}
