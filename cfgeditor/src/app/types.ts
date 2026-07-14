import {Schema} from "@/domain/schema";
import {STable} from "@/api/schemaModel";

export type SchemaTableType = {
    schema: Schema,
    notes?: Map<string, string>,
    curTable: STable
};
