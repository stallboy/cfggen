export function getInt(key: string, def: number): number {
    let v = localStorage.getItem(key);
    if (v) {
        let n = parseInt(v);
        if (!isNaN(n)) {
            return n;
        }
    }
    return def;
}

export function getBool(key: string, def: boolean): boolean {
    let v = localStorage.getItem(key);
    if (v) {
        return v == 'true';
    }
    return def;
}

export function getStr(key: string, def: string): string {
    let v = localStorage.getItem(key);
    if (v) {
        return v;
    }
    return def;
}

export function getJson(key: string, def: any): any {
    let v = localStorage.getItem(key);
    if (v) {
        return JSON.parse(v);
    }
    return def;
}
