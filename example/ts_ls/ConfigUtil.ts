export class Stream {
    private static readonly decoder = new TextDecoder();
    private view: DataView;
    private offset: number;
    private littleEndian: boolean;
    private stringPool: string[] | undefined = undefined;
    private langNames: string[] | undefined = undefined;
    private langTextPools: string[][] | undefined = undefined;
    private tableBlocks: ReadonlyMap<string, Stream> = new Map();

    constructor(buffer: ArrayBuffer, littleEndian: boolean = true, byteOffset: number = 0, byteLength: number = buffer.byteLength - byteOffset) {
        this.view = new DataView(buffer, byteOffset, byteLength);
        this.offset = 0;
        this.littleEndian = littleEndian;
    }

    Remaining(): number { return this.view.byteLength - this.offset; }
    RequireEnd(): void { if (this.Remaining() !== 0) throw new Error("Unconsumed block bytes"); }
    private Require(length: number): void {
        if (!Number.isInteger(length) || length < 0 || length > this.Remaining()) {
            throw new Error(`Read outside input block: ${length}`);
        }
    }
    ReadCount(minimumBytes: number = 4): number {
        const count = this.ReadInt32();
        if (count < 0 || minimumBytes > 0 && count > this.Remaining() / minimumBytes) {
            throw new Error(`Invalid count: ${count}`);
        }
        return count;
    }
    Slice(length: number): Stream {
        this.Require(length);
        const child = new Stream(this.view.buffer as ArrayBuffer, this.littleEndian, this.view.byteOffset + this.offset, length);
        this.offset += length;
        child.stringPool = this.stringPool; child.langNames = this.langNames; child.langTextPools = this.langTextPools;
        return child;
    }
    Fork(): Stream {
        const child = new Stream(this.view.buffer as ArrayBuffer, this.littleEndian, this.view.byteOffset, this.view.byteLength);
        child.offset = this.offset;
        child.stringPool = this.stringPool; child.langNames = this.langNames; child.langTextPools = this.langTextPools;
        return child;
    }
    SetTableBlocks(blocks: ReadonlyMap<string, Stream>): void { this.tableBlocks = blocks; }
    ReadTables(): ReadonlyMap<string, Stream> {
        const tables = new Map<string, Stream>();
        for (const [name, input] of this.tableBlocks) {
            const child = input.Fork();
            child.stringPool = this.stringPool; child.langNames = this.langNames; child.langTextPools = this.langTextPools;
            tables.set(name, child);
        }
        return tables;
    }
    ReadLanguageBlocks(names: readonly string[], blocks: readonly Stream[]): void {
        this.langNames = [...names];
        this.langTextPools = blocks.map(input => {
            const texts = input.ReadTextBlock(); input.RequireEnd(); return texts;
        });
        if (this.langTextPools.some(pool => pool.length !== this.langTextPools![0].length)) {
            throw new Error("Language text count mismatch");
        }
    }
    private ReadTextBlock(): string[] {
        const indices = Array.from({length: this.ReadCount()}, () => this.ReadInt32());
        const pool = Array.from({length: this.ReadCount()}, () => this.ReadString());
        return indices.map(index => {
            if (index < 0 || index >= pool.length) {
                throw new Error(`Invalid text pool index: ${index}`);
            }
            return pool[index];
        });
    }

    ReadStringPool() {
        const count = this.ReadCount();
        this.stringPool = new Array<string>(count);
        for (let i = 0; i < count; i++) {
            this.stringPool[i] = this.ReadString();
        }
    }

    ReadStringInPool(): string {
        const index = this.ReadInt32();
        if (!this.stringPool || index < 0 || index >= this.stringPool.length) {
            throw new Error(`Invalid string pool index: ${index}`);
        }
        return this.stringPool[index];
    }

    ReadSize(): number {
        const count = this.ReadInt32();
        if (count < 0) {
            throw new Error(`Negative count or length: ${count}`);
        }
        return count;
    }

    ReadString(): string {
        const length = this.ReadSize();
        this.Require(length);
        const bytes = new Uint8Array(this.view.buffer, this.view.byteOffset + this.offset, length);
        this.offset += length;
        return Stream.decoder.decode(bytes);
    }

    ReadInt32(): number {
        const value = this.view.getInt32(this.offset, this.littleEndian);
        this.offset += 4;
        return value;
    }

    ReadInt64(): number {
        const value = this.view.getBigInt64(this.offset, this.littleEndian);
        this.offset += 8;
        return Number(value);
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

    ReadTextsInPool(): string[] {
        const index = this.ReadTextIndex();
        const pools = this.langTextPools!;
        const texts = new Array<string>(pools.length);
        for (let i = 0; i < pools.length; i++) {
            texts[i] = pools[i][index];
        }
        return texts;
    }

    // 从 LangTextPool 读取text （单语言模式）
    ReadTextInPool(): string {
        const index = this.ReadTextIndex();
        return this.langTextPools![0][index];
    }

    ReadTextIndex(): number {
        const index = this.ReadInt32();
        if (!this.langTextPools || this.langTextPools.length === 0 || index < 0 || index >= this.langTextPools[0].length) {
            throw new Error(`Invalid text index: ${index}`);
        }
        return index;
    }

    GetLangNames(): string[] {
        return this.langNames ?? [];
    }

    GetLangTextPools(): string[][] {
        return this.langTextPools ?? [];
    }

    SkipBytes(count: number): void {
        this.Require(count);
        this.offset += count;
    }

}

/** Versioned metadata and named blocks. */
export class ConfigPackage {
    static readonly FormatVersion = 1;
    private constructor(public readonly Metadata: ReadonlyMap<string, string>, private readonly blocks: ReadonlyMap<string, Stream>) {}
    static Read(bytes: ArrayBuffer): ConfigPackage {
        const input = new Stream(bytes);
        const version = input.ReadInt32();
        if (version !== ConfigPackage.FormatVersion) {
            throw new Error(`Unsupported config format version ${version}; reader supports [1]. Regenerate the reader or export compatible data.`);
        }
        const metadata = new Map<string, string>();
        for (let count = input.ReadCount(8); count > 0; count--) {
            const key = input.ReadString(); const value = input.ReadString();
            if (metadata.has(key)) {
                throw new Error(`Duplicate metadata: ${key}`);
            }
            metadata.set(key, value);
        }
        const blocks = new Map<string, Stream>();
        for (let count = input.ReadCount(8); count > 0; count--) {
            const name = input.ReadString(); const block = input.Slice(input.ReadInt32());
            if (blocks.has(name)) {
                throw new Error(`Duplicate block: ${name}`);
            }
            blocks.set(name, block);
        }
        input.RequireEnd(); return new ConfigPackage(metadata, blocks);
    }
    get Name(): string {
        const name = this.Metadata.get("pack");
        if (!name || name.trim() !== name || name.includes(",")) {
            throw new Error("Missing or invalid pack name");
        }
        return name;
    }
    Block(name: string): Stream {
        const block = this.blocks.get(name);
        if (!block) {
            throw new Error(`Missing block: ${name}`);
        }
        return block.Fork();
    }
    Attach(language: ConfigPackage): ConfigPackage {
        if (this.Name !== language.Name) {
            throw new Error("Language resource belongs to another pack");
        }
        const blocks = new Map(this.blocks);
        if (language.blocks.size === 0) {
            throw new Error("Empty language resource");
        }
        for (const [name, block] of language.blocks) {
            if (!name.startsWith("text/") || blocks.has(name)) {
                throw new Error(`Invalid or duplicate language block: ${name}`);
            }
            blocks.set(name, block);
        }
        return new ConfigPackage(this.Metadata, blocks);
    }
    OpenData(languages?: readonly string[]): Stream {
        this.Block("schema");
        const input = this.Block("strings"); input.ReadStringPool(); input.RequireEnd();
        const names = languages?.length ? languages : [this.Metadata.get("defaultLanguage") ?? "default"];
        input.ReadLanguageBlocks(names, names.map(name => this.Block("text/" + name)));
        const tables = new Map<string, Stream>();
        for (const [name, block] of this.blocks) {
            if (name.startsWith(".")) {
                tables.set(name.substring(1), block);
            }
        }
        input.SetTableBlocks(tables); return input;
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

export class Loader {
    static LoadPackage(pack: ConfigPackage, processor: (stream: Stream, errors: LoadErrors) => void,
                       errors: LoadErrors, languages?: readonly string[]): Stream {
        pack.Name;
        // These runtimes load complete single packages; sparse Mod composition is provided by Java/C#.
        if (pack.Metadata.get("requires")) {
            throw new Error("This loader supports standalone packages; Mod composition requires Java/C#");
        }
        const stream = pack.OpenData(languages); processor(stream, errors); return stream;
    }
    static LoadBytes(
        data: ArrayBuffer,
        processor: (stream: Stream, errors: LoadErrors) => void,
        errors: LoadErrors,
        languages?: readonly string[]
    ): Stream {
        return this.LoadPackage(ConfigPackage.Read(data), processor, errors, languages);
    }
}
// 辅助方法：将 List/Array 转换为字符串
export function ToStringList<T>(data: readonly T[]): string {
    const elements = data.map(item => String(item));
    return "[" + elements.join(", ") + "]";
}

// 辅助方法：将 Map 转换为字符串
export function ToStringMap<K, V>(data: ReadonlyMap<K, V>): string {
    const entries: string[] = [];
    for (const [key, value] of data.entries()) {
        entries.push(`${key}=${value}`);
    }
    return "{" + entries.join(", ") + "}";
}
