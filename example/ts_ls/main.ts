// main.ts
import {Config} from "./Config";
import {Loader, LoadErrors} from "./ConfigUtil";
import { readFileSync } from 'fs';


function readFileToArrayBufferSync(filePath: string): ArrayBuffer {
    const buffer: Buffer = readFileSync(filePath);
    return buffer.buffer.slice(
        buffer.byteOffset,
        buffer.byteOffset + buffer.byteLength
    );
}


function main(): void {

    const data = readFileToArrayBufferSync('./config.bytes');
    const errors = new LoadErrors();
    const stream = Loader.LoadBytes(data, Config.Processor.Process, errors);


    for(const e of errors.Errors){
        console.error(e);
    }

    console.log(Config.Equip_Rank.Blue)
    const task = Config.Task_Task.Get(1);
    console.log(task?.toString())
    // console.log(task)

    for (const v of Config.Ai_Ai.All().values()){
        console.log(v);
        break;
    }

}

main(); // 直接调用