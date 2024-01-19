import {Entity} from "../model/entityModel.ts";
import {SchemaTableType} from "../CfgEditorApp.tsx";
import {includeRefTables} from "../func/tableRefEntity.ts";
import {navTo, store, useLocationData} from "../model/store.ts";
import {useNavigate, useOutletContext} from "react-router-dom";
import {FlowGraph} from "../ui/FlowGraph.tsx";
import {ReactFlowProvider} from "reactflow";
import {MenuItem} from "../ui/FlowContextMenu.tsx";
import {useTranslation} from "react-i18next";
import {SItem} from "../model/schemaModel.ts";
import {convertNodeAndEdges, fillHandles} from "../ui/entityToFlow.ts";


export function TableRef() {
    const {schema, curTable} = useOutletContext<SchemaTableType>();
    const {refIn, refOutDepth, maxNode, nodeShow} = store;
    const {pathname} = useLocationData();
    const {t} = useTranslation();
    const navigate = useNavigate();

    const entityMap = new Map<string, Entity>();
    includeRefTables(entityMap, curTable, schema, refIn, refOutDepth, maxNode);
    fillHandles(entityMap);

    const paneMenu: MenuItem[] = [{
        label: curTable.name + "\n" + t('table'),
        key: 'table',
        handler() {
            navigate(navTo('table', curTable.name));
        }
    }];

    const nodeMenuFunc = (entity: Entity): MenuItem[] => {
        let sItem = entity.userData as SItem;
        return [{
            label: sItem.name + "\n" + t('tableRef'),
            key: `entityTableRef`,
            handler() {
                navigate(navTo('tableRef', sItem.name));
            }
        }, {
            label: sItem.name + "\n" + t('table'),
            key: `entityTable`,
            handler() {
                navigate(navTo('table', sItem.name));
            }
        }];
    }

    const {nodes, edges} = convertNodeAndEdges({entityMap, nodeShow});
    return <ReactFlowProvider>
        <FlowGraph key={pathname} initialNodes={nodes} initialEdges={edges} paneMenu={paneMenu}
                   nodeMenuFunc={nodeMenuFunc}/>
    </ReactFlowProvider>
}
