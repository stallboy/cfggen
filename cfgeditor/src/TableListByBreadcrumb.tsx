import {Schema} from "./schemaModel.ts";
import {Dispatch} from "react";
import {Breadcrumb, Spin} from "antd";
import {TableOutlined} from "@ant-design/icons";
import {ItemType} from "antd/es/breadcrumb/Breadcrumb";


export class TableTreeNode {
    nodeName: string;
    tables: string[] = []; // 终结节点
    parent?: TableTreeNode;
    children: Map<string, TableTreeNode> = new Map<string, TableTreeNode>(); // 中间子节点

    constructor(name: string = "") {
        this.nodeName = name;
    }
}

export class CurSelect {
    node: TableTreeNode;
    table: string;

    constructor(node: TableTreeNode, table: string) {
        this.node = node;
        this.table = table;
    }
}



export function schemaToTree(schema: Schema): TableTreeNode {
    let tree = new TableTreeNode();
    for (let item of schema.itemMap.values()) {
        if (item.type == 'table') {
            let node = tree;
            let sp = item.name.split(".");
            for (let s of sp.slice(0, -1)) {
                let n = node.children.get(s);
                if (n == undefined) {
                    n = new TableTreeNode(s);
                    n.parent = node;
                    node.children.set(s, n);
                }
                node = n;
            }
            node.tables.push(sp[sp.length - 1]);
        }
    }

    return tree;
}

export function getDefaultSelect(fromNode: TableTreeNode): CurSelect | null {
    let node = fromNode;
    while (node) {
        if (node.tables.length > 0) {
            return new CurSelect(node, node.tables[0]);
        }
        if (node.children.size > 0) {
            let k: string = node.children.keys().next().value;
            node = node.children.get(k) as TableTreeNode;
        } else {
            return null;
        }
    }
    return null;
}

export function getCurTableName(curSelect: CurSelect): string {
    let curTable = curSelect.table;
    let node = curSelect.node;
    while (node) {
        if (node.nodeName) {
            curTable = node.nodeName + "." + curTable;
        }
        if (node.parent) {
            node = node.parent;
        } else {
            break;
        }
    }
    return curTable;
}



export function TableListByBreadcrumb({tree, curSelect, setCurSelect}: {
    tree: TableTreeNode | null,
    curSelect: CurSelect | null,
    setCurSelect: Dispatch<CurSelect>
}) {
    if (tree == null) {
        return <Spin/>
    } else {

        if (curSelect == null) {
            return <div> empty </div>
        }

        let items = [];
        let curSelectName = curSelect.table;
        let curSelectNode = curSelect.node;

        while (true) {
            let menu = []
            let i = 0;
            let thisNode = curSelectNode;

            for (let key of thisNode.children.keys()) {
                i++;
                menu.push({
                    key: i.toString() + "#" + key,
                    label: key,
                })
            }

            for (let table of thisNode.tables) {
                i++;
                menu.push({
                    key: "." + i.toString() + "#" + table,
                    label: table,
                    icon: <TableOutlined/>
                })
            }

            let item: ItemType = {
                title: curSelectName,
                menu: {
                    items: menu,
                    onClick: (menuInfo) => {
                        let key = menuInfo.key;
                        let name = key.split("#")[1];
                        let isTable = key.startsWith(".");

                        // console.log(thisNode.nodeName)
                        if (isTable) {
                            setCurSelect(new CurSelect(thisNode, name));
                        } else {
                            let selectedNode = thisNode.children.get(name);
                            if (selectedNode) {
                                let curSel = getDefaultSelect(selectedNode);
                                if (curSel) {
                                    setCurSelect(curSel);
                                }
                            }
                        }
                    }

                },

            };
            items.push(item);

            if (thisNode.parent) {
                curSelectNode = thisNode.parent;
                curSelectName = thisNode.nodeName;
            } else {
                break;
            }
        }

        items.reverse();

        return <Breadcrumb
            items={items}
        />
    }
}