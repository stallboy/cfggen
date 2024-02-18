import {Entity} from "../../flow/entityModel.ts";
import {SchemaTableType} from "../../CfgEditorApp.tsx";
import {includeRefTables} from "./tableRefEntity.ts";
import {navTo, store, useLocationData} from "../setting/store.ts";
import {useNavigate, useOutletContext} from "react-router-dom";
import {MenuItem} from "../../flow/FlowContextMenu.tsx";
import {useTranslation} from "react-i18next";
import {SItem} from "./schemaModel.ts";
import {fillHandles} from "../../flow/entityToNodeAndEdge.ts";
import {getDefaultIdInTable} from "./Table.tsx";
import {useEntityToGraph} from "../../flow/FlowGraph.tsx";
import {memo, useCallback, useMemo} from "react";


export const TableRef = memo(function TableRef() {
    const {schema, curTable} = useOutletContext<SchemaTableType>();
    const {refIn, refOutDepth, maxNode} = store;
    const {curId, pathname} = useLocationData();
    const {t} = useTranslation();
    const navigate = useNavigate();
    const entityMap = new Map<string, Entity>();
    includeRefTables(entityMap, curTable, schema, refIn, refOutDepth, maxNode);
    fillHandles(entityMap);

    const paneMenu: MenuItem[] = useMemo(() => [{
        label: curTable.name + "\n" + t('table'),
        key: 'table',
        handler() {
            navigate(navTo('table', curTable.name, getDefaultIdInTable(schema, curTable.name, curId)));
        }
    }], [navigate, schema, curTable, curId]);

    const nodeDoubleClickFunc = useCallback((entity: Entity): void => {
        let sItem = entity.userData as SItem;
        navigate(navTo('table', sItem.name, getDefaultIdInTable(schema, sItem.name, curId)));
    }, [navigate, schema, curId]);

    const nodeMenuFunc = useCallback((entity: Entity): MenuItem[] => {
        let sItem = entity.userData as SItem;
        return [{
            label: sItem.name + "\n" + t('tableRef'),
            key: `entityTableRef`,
            handler() {
                navigate(navTo('tableRef', sItem.name, getDefaultIdInTable(schema, sItem.name, curId)));
            }
        }, {
            label: sItem.name + "\n" + t('table'),
            key: `entityTable`,
            handler() {
                navigate(navTo('table', sItem.name, getDefaultIdInTable(schema, sItem.name, curId)));
            }
        }];
    }, [navigate, schema, curId]);

    useEntityToGraph(pathname, entityMap, nodeMenuFunc, paneMenu, true, undefined, nodeDoubleClickFunc);

    return <></>;
});
