import {Entity} from "../../flow/entityModel.ts";
import {SchemaTableType} from "../../CfgEditorApp.tsx";
import {TableEntityCreator, UserData} from "./tableEntityCreator.ts";
import {navTo, store, useLocationData} from "../setting/store.ts";
import {useNavigate, useOutletContext} from "react-router-dom";
// import {useReactFlow} from "reactflow";
import {MenuItem} from "../../flow/FlowContextMenu.tsx";
import {useTranslation} from "react-i18next";
import {fillHandles} from "../../flow/entityToNodeAndEdge.ts";
import {Schema} from "./schemaUtil.ts";
import {useEntityToGraph} from "../../flow/useEntityToGraph.tsx";
import {EFitView} from "../record/editingObject.ts";


export function Table() {
    const {schema, notes, curTable} = useOutletContext<SchemaTableType>();
    const {maxImpl} = store;
    const {pathname, curId} = useLocationData();
    const {t} = useTranslation();
    const navigate = useNavigate();

    // const flowInstance = useReactFlow();

    const entityMap = new Map<string, Entity>();
    let creator = new TableEntityCreator(entityMap, schema, curTable, maxImpl);
    creator.includeSubStructs();
    creator.includeRefTables();
    fillHandles(entityMap);

    const paneMenu: MenuItem[] = [{
        label: curTable.name + "\n" + t('tableRef'),
        key: 'tableRef',
        handler() {
            navigate(navTo('tableRef', curTable.name, getDefaultIdInTable(schema, curTable.name, curId)));
        }
    }];

    const nodeMenuFunc = (entity: Entity): MenuItem[] => {
        let userData = entity.userData as UserData;
        let mm: MenuItem[] = [];
        if (userData.table != curTable.name) {
            mm.push({
                label: userData.table + "\n" + t('table'),
                key: `entityTable`,
                handler() {
                    navigate(navTo('table', userData.table, getDefaultIdInTable(schema, userData.table, curId)));
                }
            });
        }

        mm.push({
            label: userData.table + "\n" + t('tableRef'),
            key: `entityTableRef`,
            handler() {
                navigate(navTo('tableRef', userData.table, getDefaultIdInTable(schema, userData.table, curId)));
            }
        });
        return mm;
    }

    useEntityToGraph({pathname, entityMap, notes, nodeMenuFunc, paneMenu, fitView: EFitView.FitFull});
    return null;
}


export function getDefaultIdInTable(schema: Schema, tableId: string, curId: string) {
    const sTable = schema.getSTable(tableId);
    if (sTable && sTable.recordIds.length > 0) {
        return sTable.recordIds[0].id;
    }
    return curId;
}