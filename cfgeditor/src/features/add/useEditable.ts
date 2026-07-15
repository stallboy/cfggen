import {Schema} from "@/domain/schema.ts";
import {useLocationData} from "@/store/store.ts";

/** 当前表是否可编辑（schema 可编辑且当前 table 存在）。Chat / AddJson 共用，避免各写一遍。 */
export function useIsCurTableEditable(schema?: Schema): boolean {
    const {curTableId} = useLocationData();
    return !!(schema?.isEditable && schema.getSTable(curTableId));
}
