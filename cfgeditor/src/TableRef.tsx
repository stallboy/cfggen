import {Entity, fillHandles, } from "./model/entityModel.ts";
import {SchemaTableType} from "./CfgEditorApp.tsx";
import {includeRefTables} from "./func/tableRefEntity.ts";
import {store, useLocationData} from "./model/store.ts";
import {useOutletContext} from "react-router-dom";
import {convertNodeAndEdges, FlowEntityGraph} from "./ui/FlowEntityNode.tsx";
import {ReactFlowProvider} from "reactflow";


export function TableRef() {
    const {schema, curTable} = useOutletContext<SchemaTableType>();
    const {refIn, refOutDepth, maxNode, nodeShow} = store;
    const {pathname} = useLocationData();
    // const {t} = useTranslation();
    // const navigate = useNavigate();

    // function createGraph(): EntityGraph {
    //     const entityMap = new Map<string, Entity>();
    //     includeRefTables(entityMap, curTable, schema, refIn, refOutDepth, maxNode);
    //     fillHandles(entityMap);
    //
    //     const menu: Item[] = [{
    //         label: curTable.name + "\n" + t('table'),
    //         key: 'table',
    //         handler() {
    //             navigate(navTo('table', curTable.name));
    //         }
    //     }];
    //
    //     const entityMenuFunc = (entity: Entity): Item[] => {
    //         let sItem = entity.userData as SItem;
    //         return [{
    //             label: sItem.name + "\n" + t('tableRef'),
    //             key: `entityTableRef`,
    //             handler() {
    //                 navigate(navTo('tableRef', curTable.name));
    //             }
    //         }, {
    //             label: sItem.name + "\n" + t('table'),
    //             key: `entityTable`,
    //             handler() {
    //                 navigate(navTo('table', sItem.name));
    //             }
    //         }];
    //     }
    //
    //     return {entityMap, menu, entityMenuFunc, nodeShow};
    // }
    //
    // const create = useCallback(
    //     (el: HTMLElement) => {
    //         return createEditor(el, createGraph());
    //     },
    //     [schema, curTable, refIn, refOutDepth, maxNode, nodeShow]
    // );
    // const [ref] = useRete(create);



    // return <div ref={ref} style={{height: "100vh", width: "100%"}}></div>

    const entityMap = new Map<string, Entity>();
    includeRefTables(entityMap, curTable, schema, refIn, refOutDepth, maxNode);
    fillHandles(entityMap);
    const {nodes, edges} = convertNodeAndEdges({entityMap, menu: [], nodeShow});

    return <ReactFlowProvider>
        <FlowEntityGraph key={pathname} initialNodes={nodes} initialEdges={edges}/>
    </ReactFlowProvider>
}
