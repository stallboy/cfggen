import {Entity} from "@/domain/entityModel";
import {SchemaTableType} from "@/CfgEditorApp";
import {includeRefTables} from "./tableRefEntity.ts";
import {navTo, useMyStore, useLocationData} from "@/store/store";
import {useNavigate, useOutletContext} from "react-router";
import {MenuItem} from "@/flow/FlowContextMenu";
import {useTranslation} from "react-i18next";
import {SItem} from "@/api/schemaModel";
import {fillHandles} from "@/flow/entityToNodeAndEdge";
import {memo, useCallback, useMemo} from "react";
import {useEntityToGraph} from "@/flow/useEntityToGraph";
import {EntityNode} from "@/flow/FlowGraph";
import {getDefaultIdInTable} from "@/domain/schema";

export const TableRef = memo(function TableRef() {
    const {schema, notes, curTable} = useOutletContext<SchemaTableType>();
    const {refIn, refOutDepth, maxNode} = useMyStore();
    const {curId, pathname} = useLocationData();
    const {t} = useTranslation();
    const navigate = useNavigate();
    // entityMap 构建含 fillHandles 副作用，React Compiler 不会 memo，需手动 useMemo
    const entityMap = useMemo(() => {
        const map = new Map<string, Entity>();
        includeRefTables(map, curTable, schema, refIn, refOutDepth, maxNode);
        fillHandles(map);
        return map;
    }, [curTable, schema, refIn, refOutDepth, maxNode]);

    const getTableDefaultId = useCallback(
        (tableName: string) => getDefaultIdInTable(schema, tableName, curId), [schema, curId]);

    const paneMenu: MenuItem[] = useMemo(() => [{
        label: `${curTable.name}\n${t('table')}`,
        key: 'table',
        handler: () => navigate(navTo('table', curTable.name, getTableDefaultId(curTable.name)))
    }], [navigate, curTable, getTableDefaultId, t]);

    const nodeDoubleClickFunc = useCallback((entityNode: EntityNode): void => {
        const sItem = entityNode.data.entity.userData as SItem;
        navigate(navTo('table', sItem.name, getTableDefaultId(sItem.name)));
    }, [navigate, getTableDefaultId]);

    const nodeMenuFunc = useCallback((entityNode: EntityNode): MenuItem[] => {
        const sItem = entityNode.data.entity.userData as SItem;
        return [{
            label: `${sItem.name}\n${t('table')}`,
            key: 'entityTable',
            handler: () => navigate(navTo('table', sItem.name, getTableDefaultId(sItem.name)))
        }, {
            label: `${sItem.name}\n${t('tableRef')}`,
            key: 'entityTableRef',
            handler: () => navigate(navTo('tableRef', sItem.name, getTableDefaultId(sItem.name)))
        }];
    }, [navigate, getTableDefaultId, t]);

    useEntityToGraph({
        type: 'tableRef',
        pathname,
        entityMap,
        notes,
        nodeMenuFunc,
        paneMenu,
        nodeDoubleClickFunc
    });

    return null;
});
