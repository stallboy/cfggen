import {ReadOnlyEntity} from "@/domain/entityModel";
import {SchemaTableType} from "@/CfgEditorApp";
import {TableEntityCreator, UserData} from "./tableEntityCreator.ts";
import {navTo, useLocationData, useMyStore} from "@/store/store";
import {useNavigate, useOutletContext} from "react-router";
// import {useReactFlow} from "reactflow";
import {MenuItem} from "@/flow/FlowContextMenu";
import {useTranslation} from "react-i18next";
import {fillHandles} from "@/flow/entityToNodeAndEdge";
import {getDefaultIdInTable} from "@/domain/schema";
import {useEntityToGraph} from "@/flow/useEntityToGraph";
import {EntityNode} from "@/flow/FlowGraph";


export function Table() {
    const {schema, notes, curTable} = useOutletContext<SchemaTableType>();
    const {maxImpl} = useMyStore();
    const {pathname, curId} = useLocationData();
    const {t} = useTranslation();
    const navigate = useNavigate();

    const getTableDefaultId = (tableName: string) => getDefaultIdInTable(schema, tableName, curId);

    const entityMap = new Map<string, ReadOnlyEntity>();
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


