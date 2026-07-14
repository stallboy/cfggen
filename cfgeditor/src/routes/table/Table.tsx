import {ReadOnlyEntity} from "@/domain/entityModel";
import {SchemaTableType} from "@/app/types";
import {TableEntityCreator, UserData} from "./tableEntityCreator.ts";
import {navTo, useLocationData, useMyStore} from "@/store/store";
import {useNavigate, useOutletContext} from "react-router";
// import {useReactFlow} from "reactflow";
import {MenuItem} from "@/flow/FlowContextMenu";
import {useTranslation} from "react-i18next";
import {fillHandles} from "@/flow/layout/entityToNodeAndEdge";
import {getDefaultIdInTable} from "@/domain/schema";
import {useEntityToGraph} from "@/flow/useEntityToGraph";
import {EntityNode} from "@/flow/FlowGraph";
import {memo, useCallback, useMemo} from "react";


export const Table = memo(function Table() {
    const {schema, notes, curTable} = useOutletContext<SchemaTableType>();
    const {maxImpl} = useMyStore();
    const {pathname, curId} = useLocationData();
    const {t} = useTranslation();
    const navigate = useNavigate();

    const getTableDefaultId = useCallback((tableName: string) =>
        getDefaultIdInTable(schema, tableName, curId), [schema, curId]);

    // entityMap 构建含 fillHandles 副作用，React Compiler 不会 memo，需手动 useMemo
    // 以免每次 render 新建 Map 触发 useEntityToGraph 全量重算。
    const entityMap = useMemo(() => {
        const map = new Map<string, ReadOnlyEntity>();
        const creator = new TableEntityCreator(map, schema, curTable, maxImpl);
        creator.includeSubStructs();
        creator.includeRefTables();
        fillHandles(map);
        return map;
    }, [schema, curTable, maxImpl]);

    const paneMenu = useMemo<MenuItem[]>(() => [
        {
            label: `${curTable.name}\n${t('tableRef')}`,
            key: 'tableRef',
            handler: () => navigate(navTo('tableRef', curTable.name, getTableDefaultId(curTable.name)))
        }
    ], [curTable.name, t, navigate, getTableDefaultId]);

    const nodeMenuFunc = useCallback((entityNode: EntityNode): MenuItem[] => {
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
    }, [curTable.name, t, navigate, getTableDefaultId]);

    useEntityToGraph({type: 'table', pathname, entityMap, notes, nodeMenuFunc, paneMenu});
    return null;
});


