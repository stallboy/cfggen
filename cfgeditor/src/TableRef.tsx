import {Schema, SItem, STable} from "./model/schemaModel.ts";
import {useRete} from "rete-react-plugin";
import {createEditor} from "./editor.tsx";
import {useCallback} from "react";
import {Entity, EntityGraph, fillInputs, NodePlacementStrategyType} from "./model/entityModel.ts";
import {Item} from "rete-context-menu-plugin/_types/types";
import {pageTable} from "./CfgEditorApp.tsx";
import {useTranslation} from "react-i18next";
import {includeRefTables} from "./func/tableRefEntity.ts";


export function TableRef({
                             schema,
                             curTable,
                             setCurTable,
                             refIn,
                             refOutDepth,
                             maxNode,
                             setCurPage,
                             nodePlacementStrategy
                         }: {
    schema: Schema;
    curTable: STable;
    setCurTable: (cur: string) => void;
    refIn: boolean;
    refOutDepth: number;
    maxNode: number;
    setCurPage: (page: string) => void;
    nodePlacementStrategy: NodePlacementStrategyType;
}) {
    const {t} = useTranslation();

    function createGraph(): EntityGraph {
        const entityMap = new Map<string, Entity>();
        includeRefTables(entityMap, curTable, schema, refIn, refOutDepth, maxNode);
        fillInputs(entityMap);

        const menu: Item[] = [{
            label: curTable.name + "\n" + t('table'),
            key: 'table',
            handler() {
                setCurPage(pageTable);
            }
        }];

        const entityMenuFunc = (entity: Entity): Item[] => {
            let sItem = entity.userData as SItem;
            return [{
                label: sItem.name + "\n" + t('tableRef'),
                key: `entityTableRef`,
                handler() {
                    setCurTable(sItem.name);
                }
            }, {
                label: sItem.name + "\n" + t('table'),
                key: `entityTable`,
                handler() {
                    setCurTable(sItem.name);
                    setCurPage(pageTable);
                }
            }];
        }

        return {entityMap, menu, entityMenuFunc, nodePlacementStrategy};
    }

    const create = useCallback(
        (el: HTMLElement) => {
            return createEditor(el, createGraph());
        },
        [schema, curTable, refIn, refOutDepth, maxNode, nodePlacementStrategy]
    );
    const [ref] = useRete(create);


    return <div ref={ref} style={{height: "100vh", width: "100%"}}></div>
}
