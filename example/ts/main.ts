// main.ts
import {Config} from "./Config";
import {Stream, LoadErrors} from "./ConfigUtil";
import { readFileSync } from 'fs';


function readFileToArrayBufferSync(filePath: string): ArrayBuffer {
    const buffer: Buffer = readFileSync(filePath);
    return buffer.buffer.slice(
        buffer.byteOffset,
        buffer.byteOffset + buffer.byteLength
    );
}


function main(): void {

    const arrayBuffer = readFileToArrayBufferSync('./config.bytes');
    const errors = new LoadErrors();
    const stream = new Stream(arrayBuffer);
    stream.ReadStringPool();
    Config.Processor.Process(stream, errors);

    console.log("Hello, Config!");
    console.log(Config.Equip_Rank.Blue.ToString())

    for(const e of errors.Errors){
        console.error(e);
    }

    for (const v of Config.Ai_Ai.All().values()){
        console.log(v);
        break;
    }

}

main(); // 直接调用