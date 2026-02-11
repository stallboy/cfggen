export class Stream {
    private view: DataView;
    private offset: number;
    private littleEndian: boolean;
    private stringPool: string[] | undefined = undefined;
    private langNames: string[] | undefined = undefined;
    private langTextPools: string[][] | undefined = undefined;

    constructor(buffer: ArrayBuffer, littleEndian: boolean = true) {
        this.view = new DataView(buffer);
        this.offset = 0;
        this.littleEndian = littleEndian;
    }

    ReadStringPool() {
        const count = this.ReadSize();
        this.stringPool = new Array<string>(count);
        for (let i = 0; i < count; i++) {
            this.stringPool[i] = this.ReadString();
        }
    }

    ReadStringInPool(): string {
        const index = this.ReadInt32();
        if (!this.stringPool || index < 0 || index >= this.stringPool.length) {
            return "";
        }
        return this.stringPool[index];
    }

    ReadCfg(): string | null {
        try{
            return this.ReadString();
        }catch (e){
            return null;
        }
    }

    ReadSize(): number {
        return this.ReadInt32();
    }

    ReadString(): string {
        const length = this.ReadSize();
        if (this.offset + length > this.view.byteLength) {
            throw new Error("Buffer overflow while reading string");
        }

        const bytes = new Uint8Array(this.view.buffer, this.offset, length);
        this.offset += length;
        return new TextDecoder().decode(bytes);
    }

    ReadInt32(): number {
        const value = this.view.getInt32(this.offset, this.littleEndian);
        this.offset += 4;
        return value;
    }

    ReadInt64(): number {
        const value = this.view.getFloat64(this.offset, this.littleEndian);
        this.offset += 8;
        return value;
    }

    ReadBool(): boolean {
        const value = this.view.getInt8(this.offset);
        this.offset += 1;
        return value !== 0;
    }

    ReadSingle(): number {
        const value = this.view.getFloat32(this.offset, this.littleEndian);
        this.offset += 4;
        return value;
    }

    ReadLangTextPool(): void {
        const langCount = this.ReadSize();
        this.langNames = new Array<string>(langCount);
        this.langTextPools = new Array<string[]>(langCount);

        for (let langIdx = 0; langIdx < langCount; langIdx++) {
            const langName = this.ReadString();
            this.langNames[langIdx] = langName;

            const indexCount = this.ReadSize();
            const indices = new Array<number>(indexCount);
            for (let i = 0; i < indexCount; i++) {
                indices[i] = this.ReadInt32();
            }

            const poolCount = this.ReadSize();
            const pool = new Array<string>(poolCount);
            for (let i = 0; i < poolCount; i++) {
                pool[i] = this.ReadString();
            }

            this.langTextPools[langIdx] = new Array<string>(indexCount);
            for (let i = 0; i < indexCount; i++) {
                this.langTextPools[langIdx][i] = pool[indices[i]];
            }
        }
    }

    ReadTextsInPool(): string[] {
        const index = this.ReadInt32();
        if (!this.langTextPools) {
            throw new Error("LangTextPool not initialized");
        }

        const texts = new Array<string>(this.langTextPools.length);
        for (let i = 0; i < this.langTextPools.length; i++) {
            const pool = this.langTextPools[i];
            if (index < 0 || index >= pool.length) {
                texts[i] = "";
            } else {
                texts[i] = pool[index];
            }
        }
        return texts;
    }

    // 从 LangTextPool 读取text （单语言模式）
    ReadTextInPool(): string {
        const index = this.ReadInt32();
        if (!this.langTextPools) {
            throw new Error("LangTextPool not initialized");
        }
        if (index < 0 || index >= this.langTextPools[0].Length) {
            throw new Error("index out of LangTextPool");
        }
        return this.langTextPools[0][index];
    }

    ReadTextIndex(): number {
        return this.ReadInt32();
    }

    GetLangNames(): string[] {
        return this.langNames ?? [];
    }

    GetLangTextPools(): string[][] {
        return this.langTextPools ?? [];
    }

    SkipBytes(count: number): void {
        this.offset += count;
    }

}


export enum LoadErrorType {
    ConfigNull,
    ConfigDataAdd,
    EnumDataAdd,
    EnumDup,
    EnumNull,
    RefNull,
}

export class LoadError {
    constructor(public readonly errorType: LoadErrorType,
                public readonly config: string,
                public readonly record: string = "",
                public readonly field: string = "") {
    }
}


export class LoadErrors {
    public readonly Errors: LoadError[];

    constructor() {
        this.Errors = [];
    }

    ConfigsNull(configs: Set<string>) {
        for (const c of configs) {
            this.Errors.push(new LoadError(LoadErrorType.ConfigNull, c));
        }
    }

    ConfigDataAdd(config: string) {
        this.Errors.push(new LoadError(LoadErrorType.ConfigDataAdd, config));
    }

    EnumDataAdd(config: string, record: string,) {
        this.Errors.push(new LoadError(LoadErrorType.EnumDataAdd, config, record));
    }

    EnumDup(config: string, enumName: string) {
        this.Errors.push(new LoadError(LoadErrorType.EnumDup, config, enumName));
    }

    EnumNull(config: string, enumName: string) {
        this.Errors.push(new LoadError(LoadErrorType.EnumNull, config, enumName));
    }

    RefNull(config: string, record: string, field: string) {
        this.Errors.push(new LoadError(LoadErrorType.RefNull, config, record, field));
    }

}

export class TextPoolManager {
    private static globalTexts: string[] | undefined = undefined;

    public static SetGlobalTexts(texts: string[]): void {
        this.globalTexts = texts;
    }

    public static GetText(index: number): string {
        if (!this.globalTexts) {
            return "";
        }
        return this.globalTexts[index];
    }
}


