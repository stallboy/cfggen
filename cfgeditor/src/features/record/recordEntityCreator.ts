import {Entity, ReadOnlyEntity, DisplayField, classifyJsonValue, EntitySourceEdge, EntityType, makeChildEdge} from "@/domain/entityModel.ts";
import {SField, SStruct, STable} from "@/api/schemaModel.ts";
import {BriefRecord, JSONArray, JSONObject, JSONValue, RefId, Refs} from "@/api/recordModel.ts";
import {createRefs, getLabel} from "./recordRefUtils.ts";
import {getField, Schema} from "@/domain/schema.ts";
import {getNote} from "@/domain/persistedKeys.ts";
import {findAllResInfos} from "@/res/findAllResInfos.ts";
import {TauriConf} from "@/domain/storageJson.ts";
import {ResInfo} from "@/domain/resInfo.ts";


export class RecordEntityCreator {
    constructor(public entityMap: Map<string, Entity>,
                public schema: Schema,
                public refId: RefId,
                public refs: BriefRecord[],
                public tauriConf: TauriConf,
                public resourceDir: string,
                public resMap: Map<string, ResInfo[]>) {
    }

    createRecordEntity(id: string,
                       obj: JSONObject & Refs,
                       label?: string,
                       arrayIndex?: number): ReadOnlyEntity<RefId> | null {

        const fields: DisplayField[] = [];
        const type: string = obj['$type'] as string;
        if (type == null) {
            console.error('$type missing');
            return null;
        }
        const note = getNote(obj);

        let sItem: STable | SStruct | null = null;
        if (!type.startsWith("$")) {
            const item = this.schema.getItemIncludeImpl(type);
            if (item == null) {
                console.error(type + ' not found!');
                return null;
            }
            if (item.type === 'interface') {
                // 记录视图的 $type 只会是 table/struct/impl 名；interface 名出现即脏数据，不再 as 强转后崩溃
                console.error(type + ' is an interface, not structural (dirty $type?)');
                return null;
            }
            sItem = item;
        }

        const sourceEdges: EntitySourceEdge[] = [];

        for (const fieldKey in obj) {
            if (fieldKey.startsWith("$")) {
                continue;
            }
            const fieldValue: JSONValue = obj[fieldKey];

            let sField: SField | null = null;
            if (sItem) {
                sField = getField(sItem, fieldKey);
            }
            const comment = sField?.comment ?? fieldKey;

            const field = {
                key: fieldKey,
                name: fieldKey,
                comment: comment,
                value: '',
            }
            fields.push(field);

            const kind = classifyJsonValue(fieldValue);
            if (kind === 'objectList') {  // list of struct/interface, or map (map is list of $entry)
                const fArr: JSONArray = fieldValue as JSONArray;
                let i = 0;
                for (const e of fArr) {
                    const fObj: JSONObject & Refs = e as JSONObject & Refs;
                    const childId: string = `${id}-${fieldKey}[${i}]`;
                    const childEntity = this.createRecordEntity(childId, fObj, undefined, i + 1);
                    i++;

                    if (childEntity) {
                        sourceEdges.push(makeChildEdge(fieldKey, childEntity.id));
                    }
                }
                field.value = `[]*${i}`

            } else if (kind === 'primitiveList') {  // list of primitive value（含空数组）
                const fArr: JSONArray = fieldValue as JSONArray;
                field.value = fArr.length == 0 ? '[]' : fArr.join(',')

            } else if (kind === 'object') { // struct or interface
                const fObj: JSONObject & Refs = fieldValue as JSONObject & Refs;
                const childId: string = id + "-" + fieldKey;
                const childEntity = this.createRecordEntity(childId, fObj);
                if (childEntity) {
                    sourceEdges.push(makeChildEdge(fieldKey, childEntity.id));
                }
                field.value = '<>';
            } else { // primitive
                let valueStr: string = fieldValue.toString();
                if (typeof fieldValue === 'boolean') {
                    valueStr = fieldValue ? '✔️' : '✘';
                }
                field.value = valueStr
            }
        }

        let thisLabel = label ?? getLabel(type);
        thisLabel = arrayIndex === undefined ? thisLabel : thisLabel + '.' + arrayIndex;

        const entity: ReadOnlyEntity<RefId> = {
            id: id,
            label: thisLabel,
            type: 'readonly',
            fields: fields as DisplayField[],
            sourceEdges: sourceEdges,
            entityType: EntityType.Normal,
            note: note,
            userData: this.refId,
            assets: findAllResInfos({
                label: thisLabel,
                refs: obj,
                tauriConf: this.tauriConf,
                resourceDir: this.resourceDir,
                resMap: this.resMap,
            }),
        };

        this.entityMap.set(id, entity);
        createRefs(entity, obj, this.refs);
        return entity;
    }

}
