// main.ts
import {Config} from "./Config";
import {Loader, LoadErrors, TextPoolManager} from "./ConfigUtil";
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

    const langNames = stream.GetLangNames();
    console.log("language Count: " + langNames.length);
    for (let i = 0; i < langNames.length; i++) {
        console.log(langNames[i]);
        TextPoolManager.SetGlobalTexts(stream.GetLangTextPools()[i]);
        console.log(Config.Task_Task.Get(1)?.toString());
    }
   

}

main(); // 直接调用