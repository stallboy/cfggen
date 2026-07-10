/**
 * Folds —— 编辑态折叠记录的不可变包装（ChainFold[] 薄封装，零依赖）。
 *
 * 下沉 domain。注意：fold 状态的"优先级决策"（本地 Folds vs obj.$fold）是上层
 * 业务概念，不在此纯数据结构内——由消费者（recordEditEntityCreator）自行实现。
 */
export interface ChainFold {
    chain: (string | number)[],
    fold: boolean
}

export class Folds {

    constructor(public list: ChainFold[]) {
    }

    setFold(chain: (string | number)[], fold: boolean): Folds {
        const f = this.isFold(chain);
        if (f === fold) {
            return this;
        }

        if (f === undefined) {
            return new Folds([...this.list, {chain, fold}]);
        }

        const newList: ChainFold[] = [];
        for (const c of this.list) {
            if (isChainEqual(c.chain, chain)) {
                newList.push({chain, fold});
            } else {
                newList.push(c);
            }
        }
        return new Folds(newList);
    }

    isFold(chain: (string | number)[]): boolean | undefined {
        for (const c of this.list) {
            if (isChainEqual(c.chain, chain)) {
                return c.fold;
            }
        }
    }
}

function isChainEqual(a: (string | number)[], b: (string | number)[]) {
    if (a === b) {
        return true;
    }
    if (a == null || b == null) {
        return false;
    }
    if (a.length !== b.length) {
        return false;
    }
    for (let i = 0; i < a.length; ++i) {
        if (a[i] !== b[i])
            return false;
    }
    return true;
}
