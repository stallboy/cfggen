import {Entity, fillHandles} from "./model/entityModel.ts";
import {SchemaTableType} from "./CfgEditorApp.tsx";
import {TableEntityCreator} from "./func/TableEntityCreator.ts";
import {store, useLocationData} from "./model/store.ts";
import {useOutletContext} from "react-router-dom";
import {convertNodeAndEdges, FlowEntityGraph} from "./ui/FlowEntityNode.tsx";
import {ReactFlowProvider} from "reactflow";


export function TableSchema() {
    const {schema, curTable} = useOutletContext<SchemaTableType>();
    const {maxImpl, nodeShow} = store;
    const {pathname} = useLocationData();
    // const {t} = useTranslation();
    // const navigate = useNavigate();

    // function createGraph(): EntityGraph {
    //     const entityMap = new Map<string, Entity>();
    //     let creator = new TableEntityCreator(entityMap, schema, curTable, maxImpl);
    //     creator.includeSubStructs();
    //     creator.includeRefTables();
    //     fillInputs(entityMap);
    //
    //     const menu: Item[] = [{
    //         label: curTable.name + "\n" + t('tableRef'),
    //         key: 'tableRef',
    //         handler() {
    //             navigate(navTo('tableRef', curTable.name));
    //         }
    //     }];
    //
    //     const entityMenuFunc = (entity: Entity): Item[] => {
    //         let userData = entity.userData as UserData;
    //         let mm = [];
    //         if (userData.table != curTable.name) {
    //             mm.push({
    //                 label: userData.table + "\n" + t('table'),
    //                 key: `entityTable`,
    //                 handler() {
    //                     navigate(navTo('table', userData.table));
    //                 }
    //             });
    //         }
    //
    //         mm.push({
    //             label: userData.table + "\n" + t('tableRef'),
    //             key: `entityTableRef`,
    //             handler() {
    //                 navigate(navTo('tableRef', userData.table));
    //             }
    //         });
    //         return mm;
    //     }
    //
    //     return {entityMap, menu, entityMenuFunc, nodeShow};
    // }
    //
    //
    // const create = useCallback(
    //     (el: HTMLElement) => {
    //         return createEditor(el, createGraph());
    //     },
    //     [schema, curTable, maxImpl, nodeShow]
    // );
    // const [ref] = useRete(create);
    //
    // return <div ref={ref} style={{height: "100vh", width: "100%"}}></div>

    const entityMap = new Map<string, Entity>();
    let creator = new TableEntityCreator(entityMap, schema, curTable, maxImpl);
    creator.includeSubStructs();
    creator.includeRefTables();
    fillHandles(entityMap);

    const {nodes, edges} = convertNodeAndEdges({entityMap, menu: [], nodeShow});

    return <ReactFlowProvider>
        <FlowEntityGraph key={pathname} initialNodes={nodes} initialEdges={edges}/>
    </ReactFlowProvider>
}
