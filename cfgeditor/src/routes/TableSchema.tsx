import {Entity, fillHandles} from "../model/entityModel.ts";
import {SchemaTableType} from "../CfgEditorApp.tsx";
import {TableEntityCreator, UserData} from "../func/TableEntityCreator.ts";
import {navTo, store, useLocationData} from "../model/store.ts";
import {useNavigate, useOutletContext} from "react-router-dom";
import {convertNodeAndEdges, FlowEntityGraph} from "../ui/FlowEntityNode.tsx";
import {ReactFlowProvider} from "reactflow";
import {MenuItem} from "../ui/FlowContextMenu.tsx";
import {useTranslation} from "react-i18next";


export function TableSchema() {
    const {schema, curTable} = useOutletContext<SchemaTableType>();
    const {maxImpl, nodeShow} = store;
    const {pathname} = useLocationData();
    const {t} = useTranslation();
    const navigate = useNavigate();

    const entityMap = new Map<string, Entity>();
    let creator = new TableEntityCreator(entityMap, schema, curTable, maxImpl);
    creator.includeSubStructs();
    creator.includeRefTables();
    fillHandles(entityMap);

    const paneMenu: MenuItem[] = [{
        label: curTable.name + "\n" + t('tableRef'),
        key: 'tableRef',
        handler() {
            navigate(navTo('tableRef', curTable.name));
        }
    }];

    const nodeMenuFunc = (entity: Entity): MenuItem[] => {
        let userData = entity.userData as UserData;
        let mm : MenuItem[] = [];
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

    const {nodes, edges} = convertNodeAndEdges({entityMap, menu: [], nodeShow});

    return <ReactFlowProvider>
        <FlowEntityGraph key={pathname}
                         initialNodes={nodes}
                         initialEdges={edges}
                         paneMenu ={paneMenu}
                         nodeMenuFunc={nodeMenuFunc}
        />
    </ReactFlowProvider>
}