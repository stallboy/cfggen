export class Stream {
    private view: DataView;
    private offset: number;
    private littleEndian: boolean;
    private stringPool: string[] | undefined = undefined;

    constructor(buffer: ArrayBuffer, littleEndian: boolean = true) {
        this.view = new DataView(buffer);
        this.offset = 0;
        this.littleEndian = littleEndian;
    }

    ReadStringPool() {
        const count = this.ReadSize();
        this.stringPool = new Array<string>(count);
        for (let i = 0; i < count; i++) {
            this.stringPool[i] = this.ReadRealString();
        }
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
        if (this.stringPool) {
            const index = this.ReadInt32();
            return this.stringPool[index];
        } else {
            return this.ReadRealString();
        }
    }

    ReadRealString(): string {
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

