import {Entity} from "../../flow/entityModel.ts";
import {SchemaTableType} from "../../CfgEditorApp.tsx";
import {TableEntityCreator, UserData} from "./tableEntityCreator.ts";
import {navTo, useMyStore, useLocationData} from "../setting/store.ts";
import {useNavigate, useOutletContext} from "react-router-dom";
// import {useReactFlow} from "reactflow";
import {MenuItem} from "../../flow/FlowContextMenu.tsx";
import {useTranslation} from "react-i18next";
import {fillHandles} from "../../flow/entityToNodeAndEdge.ts";
import {Schema} from "./schemaUtil.tsx";
import {useEntityToGraph} from "../../flow/useEntityToGraph.tsx";
import {EntityNode} from "../../flow/FlowGraph.tsx";


export function Table() {
    const {schema, notes, curTable} = useOutletContext<SchemaTableType>();
    const {maxImpl} = useMyStore();
    const {pathname, curId} = useLocationData();
    const {t} = useTranslation();
    const navigate = useNavigate();

    // 工具函数，避免重复获取默认ID
    const getTableDefaultId = (tableName: string) => getDefaultIdInTable(schema, tableName, curId);

    const entityMap = new Map<string, Entity>();
    const creator = new TableEntityCreator(entityMap, schema, curTable, maxImpl);
    creator.includeSubStructs();
    creator.includeRefTables();
    fillHandles(entityMap);

    const paneMenu: MenuItem[] = [
        {
            label: `${curTable.name}\n${t('tableRef')}`,
            key: 'tableRef',
            handler: () => navigate(navTo('tableRef', curTable.name, getTableDefaultId(curTable.name)))
        }
    ];

    const nodeMenuFunc = (entityNode: EntityNode): MenuItem[] => {
        const userData = entityNode.data.entity.userData as UserData;
        const menuItems: MenuItem[] = [];
        if (userData.table !== curTable.name) {
            menuItems.push({
                label: `${userData.table}\n${t('table')}`,
                key: 'entityTable',
                handler: () => navigate(navTo('table', userData.table, getTableDefaultId(userData.table)))
            });
        }
        menuItems.push({
            label: `${userData.table}\n${t('tableRef')}`,
            key: 'entityTableRef',
            handler: () => navigate(navTo('tableRef', userData.table, getTableDefaultId(userData.table)))
        });
        return menuItems;
    };

    useEntityToGraph({type: 'table', pathname, entityMap, notes, nodeMenuFunc, paneMenu});
    return null;
}


export function getDefaultIdInTable(schema: Schema, tableId: string, curId: string) {
    const sTable = schema.getSTable(tableId);
    if (sTable && sTable.recordIds.length > 0) {
        return sTable.recordIds[0].id;
    }
    return curId;
}