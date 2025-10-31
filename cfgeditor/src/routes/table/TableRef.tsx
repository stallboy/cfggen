import { Entity } from "../../flow/entityModel.ts";
import { SchemaTableType } from "../../CfgEditorApp.tsx";
import { includeRefTables } from "./tableRefEntity.ts";
import { navTo, useMyStore, useLocationData } from "../../store/store.ts";
import { useNavigate, useOutletContext } from "react-router-dom";
import { MenuItem } from "../../flow/FlowContextMenu.tsx";
import { useTranslation } from "react-i18next";
import { SItem } from "./schemaModel.ts";
import { fillHandles } from "../../flow/entityToNodeAndEdge.ts";
import { getDefaultIdInTable } from "./Table.tsx";
import { memo, useCallback, useMemo } from "react";
import { useEntityToGraph } from "../../flow/useEntityToGraph.tsx";
import { EntityNode } from "../../flow/FlowGraph.tsx";

export const TableRef = memo(function TableRef() {
    const { schema, notes, curTable } = useOutletContext<SchemaTableType>();
    const { refIn, refOutDepth, maxNode } = useMyStore();
    const { curId, pathname } = useLocationData();
    const { t } = useTranslation();
    const navigate = useNavigate();
    const entityMap = new Map<string, Entity>();
    includeRefTables(entityMap, curTable, schema, refIn, refOutDepth, maxNode);
    fillHandles(entityMap);

    // ��ȡ�ֲ����ߺ����������ظ�
    const getTableDefaultId = useCallback((tableName: string) => getDefaultIdInTable(schema, tableName, curId), [schema, curId]);

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
