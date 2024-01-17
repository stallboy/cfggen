import {useRete} from "rete-react-plugin";
import {createEditor} from "./editor.tsx";
import {useCallback} from "react";
import {Entity, EntityGraph, fillInputs} from "./model/entityModel.ts";
import {Item} from "rete-context-menu-plugin/_types/types";
import {SchemaTableType} from "./CfgEditorApp.tsx";
import {useTranslation} from "react-i18next";
import {TableEntityCreator, UserData} from "./func/TableEntityCreator.ts";
import {navTo, store} from "./model/store.ts";
import {useNavigate, useOutletContext} from "react-router-dom";


export function TableSchema() {
    const {schema, curTable} = useOutletContext<SchemaTableType>();
    const {maxImpl, nodeShow} = store;
    const {t} = useTranslation();
    const navigate = useNavigate();

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
                navigate(navTo('tableRef', curTable.name));
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
                        navigate(navTo('table', userData.table));
                    }
                });
            }

            mm.push({
                label: userData.table + "\n" + t('tableRef'),
                key: `entityTableRef`,
                handler() {
                    navigate(navTo('tableRef', userData.table));
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
