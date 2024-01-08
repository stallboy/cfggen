import {Schema, STable} from "./model/schemaModel.ts";
import {useRete} from "rete-react-plugin";
import {createEditor} from "./editor.tsx";
import {useCallback} from "react";
import {Entity, EntityGraph, fillInputs, NodeShowType} from "./model/entityModel.ts";
import {Item} from "rete-context-menu-plugin/_types/types";
import {pageTableRef} from "./CfgEditorApp.tsx";
import {useTranslation} from "react-i18next";
import {TableEntityCreator, UserData} from "./func/TableEntityCreator.ts";


export function TableSchema({schema, curTable, maxImpl, setCurTable, setCurPage, nodeShow}: {
    schema: Schema;
    curTable: STable;
    maxImpl: number;
    setCurTable: (cur: string) => void;
    setCurPage: (page: string) => void;
    nodeShow: NodeShowType
}) {
    const {t} = useTranslation();

    function createGraph(): EntityGraph {
        const entityMap = new Map<string, Entity>();
        let creator = new TableEntityCreator(entityMap, schema, curTable, maxImpl);
        creator.includeSubStructs();
        creator.includeRefTables();
        fillInputs(entityMap);

        const menu: Item[] = [{
            label: curTable.name + "\n" + t('tableRef'),
            key: 'tableRef',
            handler() {
                setCurPage(pageTableRef);
            }
        }];

        const entityMenuFunc = (entity: Entity): Item[] => {
            let userData = entity.userData as UserData;
            let mm = [];
            if (userData.table != curTable.name) {
                mm.push({
                    label: userData.table + "\n" + t('table'),
                    key: `entityTable`,
                    handler() {
                        setCurTable(userData.table);
                    }
                });
            }

            mm.push({
                label: userData.table + "\n" + t('tableRef'),
                key: `entityTableRef`,
                handler() {
                    setCurTable(userData.table);
                    setCurPage(pageTableRef);
                }
            });
            return mm;
        }

        return {entityMap, menu, entityMenuFunc, nodeShow};
    }


    const create = useCallback(
        (el: HTMLElement) => {
            return createEditor(el, createGraph());
        },
        [schema, curTable, maxImpl, nodeShow]
    );
    const [ref] = useRete(create);

    return <div ref={ref} style={{height: "100vh", width: "100%"}}></div>
}
