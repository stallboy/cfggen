import {SItem} from "./model/schemaModel.ts";
import {useRete} from "rete-react-plugin";
import {createEditor} from "./editor.tsx";
import {useCallback} from "react";
import {Entity, EntityGraph, fillInputs} from "./model/entityModel.ts";
import {Item} from "rete-context-menu-plugin/_types/types";
import {SchemaTableType} from "./CfgEditorApp.tsx";
import {useTranslation} from "react-i18next";
import {includeRefTables} from "./func/tableRefEntity.ts";
import {navTo, store} from "./model/store.ts";
import {useNavigate, useOutletContext} from "react-router-dom";


export function TableRef() {
    const {schema, curTable} = useOutletContext<SchemaTableType>();
    const {refIn, refOutDepth, maxNode, nodeShow} = store;
    const {t} = useTranslation();
    const navigate = useNavigate();

    function createGraph(): EntityGraph {
        const entityMap = new Map<string, Entity>();
        includeRefTables(entityMap, curTable, schema, refIn, refOutDepth, maxNode);
        fillInputs(entityMap);

        const menu: Item[] = [{
            label: curTable.name + "\n" + t('table'),
            key: 'table',
            handler() {
                navigate(navTo('table', curTable.name));
            }
        }];

        const entityMenuFunc = (entity: Entity): Item[] => {
            let sItem = entity.userData as SItem;
            return [{
                label: sItem.name + "\n" + t('tableRef'),
                key: `entityTableRef`,
                handler() {
                    navigate(navTo('tableRef', curTable.name));
                }
            }, {
                label: sItem.name + "\n" + t('table'),
                key: `entityTable`,
                handler() {
                    navigate(navTo('table', sItem.name));
                }
            }];
        }

        return {entityMap, menu, entityMenuFunc, nodeShow};
    }

    const create = useCallback(
        (el: HTMLElement) => {
            return createEditor(el, createGraph());
        },
        [schema, curTable, refIn, refOutDepth, maxNode, nodeShow]
    );
    const [ref] = useRete(create);


    return <div ref={ref} style={{height: "100vh", width: "100%"}}></div>
}
