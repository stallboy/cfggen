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

export function getEnumStr(key: string, enums: string[], def: string): string {
    let v = localStorage.getItem(key);
    if (v && enums.includes(v)) {
        return v;
    }
    return def;
}


export function getJsonNullable<T>(key: string, parser: (jsonStr: string) => T): T | null {
    let v = localStorage.getItem(key);
    if (v) {
        try {
            return parser(v);
        } catch (e) {
            console.log(e);
        }
    }
    return null;
}

export function getJson<T>(key: string, parser: (jsonStr: string) => T, def: T): T {
    let v = localStorage.getItem(key);
    if (v) {
        try {
            return parser(v);
        } catch (e) {
            console.log(e);
        }
    }
    return def;
}
