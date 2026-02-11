// noinspection UnnecessaryLocalVariableJS,JSUnusedLocalSymbols,JSUnusedGlobalSymbols,DuplicatedCode,SpellCheckingInspection

import {Stream, LoadErrors, ToStringList, ToStringMap, TextPoolManager} from "./ConfigUtil";
export namespace Config {

export class LevelRank {
    private _Level!: number;
    /* 等级 */
    get Level(): number { return this._Level; }
    private _Rank!: number;
    /* 品质 */
    get Rank(): number { return this._Rank; }

    private _RefRank!: Equip_Rank;
    get RefRank(): Equip_Rank { return this._RefRank; }
    toString() : string {
        return "(" + this._Level + "," + this._Rank + ")";
    }

    static _create(os: Stream) : LevelRank {
        const self = new LevelRank();
        self._Level = os.ReadInt32();
        self._Rank = os.ReadInt32();
        return self;
    }

    _resolve(errors: LoadErrors) {
        const _tmpRefRank = Equip_Rank.Get(this._Rank);
        if (_tmpRefRank === undefined) {
            errors.RefNull("LevelRank", this.toString(), "Rank");
        }
        this._RefRank = _tmpRefRank!;
    }
}

export class Position {
    private _x!: number;
    get X(): number { return this._x; }
    private _y!: number;
    get Y(): number { return this._y; }
    private _z!: number;
    get Z(): number { return this._z; }

    toString() : string {
        return "(" + this._x + "," + this._y + "," + this._z + ")";
    }

    static _create(os: Stream) : Position {
        const self = new Position();
        self._x = os.ReadInt32();
        self._y = os.ReadInt32();
        self._z = os.ReadInt32();
        return self;
    }

}

export class Range {
    private _Min!: number;
    /* 最小 */
    get Min(): number { return this._Min; }
    private _Max!: number;
    /* 最大 */
    get Max(): number { return this._Max; }

    toString() : string {
        return "(" + this._Min + "," + this._Max + ")";
    }

    static _create(os: Stream) : Range {
        const self = new Range();
        self._Min = os.ReadInt32();
        self._Max = os.ReadInt32();
        return self;
    }

}


export abstract class Ai_TriggerTick {
    static _create(os: Stream) : Ai_TriggerTick {
        const typeName = os.ReadStringInPool();
        switch(typeName) {
            case "ConstValue":
                return Ai_TriggerTick_ConstValue._create(os);
            case "ByLevel":
                return Ai_TriggerTick_ByLevel._create(os);
            case "ByServerUpDay":
                return Ai_TriggerTick_ByServerUpDay._create(os);
            default:
                throw new Error("Unknown type: " + typeName);
        }
    }

    abstract toString() : string;
}


export class Ai_TriggerTick_ConstValue extends Ai_TriggerTick {
    private _value!: number;
    get Value(): number { return this._value; }

    toString() : string {
        return "(" + this._value + ")";
    }

    static _create(os: Stream) : Ai_TriggerTick_ConstValue {
        const self = new Ai_TriggerTick_ConstValue();
        self._value = os.ReadInt32();
        return self;
    }

}

export class Ai_TriggerTick_ByLevel extends Ai_TriggerTick {
    private _init!: number;
    get Init(): number { return this._init; }
    private _coefficient!: number;
    get Coefficient(): number { return this._coefficient; }

    toString() : string {
        return "(" + this._init + "," + this._coefficient + ")";
    }

    static _create(os: Stream) : Ai_TriggerTick_ByLevel {
        const self = new Ai_TriggerTick_ByLevel();
        self._init = os.ReadInt32();
        self._coefficient = os.ReadSingle();
        return self;
    }

}

export class Ai_TriggerTick_ByServerUpDay extends Ai_TriggerTick {
    private _init!: number;
    get Init(): number { return this._init; }
    private _coefficient1!: number;
    get Coefficient1(): number { return this._coefficient1; }
    private _coefficient2!: number;
    get Coefficient2(): number { return this._coefficient2; }

    toString() : string {
        return "(" + this._init + "," + this._coefficient1 + "," + this._coefficient2 + ")";
    }

    static _create(os: Stream) : Ai_TriggerTick_ByServerUpDay {
        const self = new Ai_TriggerTick_ByServerUpDay();
        self._init = os.ReadInt32();
        self._coefficient1 = os.ReadSingle();
        self._coefficient2 = os.ReadSingle();
        return self;
    }

}

export class Equip_TestPackBean {
    private _name!: string;
    get Name(): string { return this._name; }
    private _iRange!: Range;
    get IRange(): Range { return this._iRange; }

    toString() : string {
        return "(" + this._name + "," + this.IRange.toString() + ")";
    }

    static _create(os: Stream) : Equip_TestPackBean {
        const self = new Equip_TestPackBean();
        self._name = os.ReadStringInPool();
        self._iRange = Range._create(os);
        return self;
    }

}

export class Other_DropItem {
    private _chance!: number;
    /* 掉落概率 */
    get Chance(): number { return this._chance; }
    private _itemids!: number[];
    /* 掉落物品 */
    get Itemids(): number[] { return this._itemids; }
    private _countmin!: number;
    /* 数量下限 */
    get Countmin(): number { return this._countmin; }
    private _countmax!: number;
    /* 数量上限 */
    get Countmax(): number { return this._countmax; }

    toString() : string {
        return "(" + this._chance + "," + ToStringList(this._itemids) + "," + this._countmin + "," + this._countmax + ")";
    }

    static _create(os: Stream) : Other_DropItem {
        const self = new Other_DropItem();
        self._chance = os.ReadInt32();
        self._itemids = [];
        for (let c = os.ReadInt32(); c > 0; c--)
            self._itemids.push(os.ReadInt32());
        self._countmin = os.ReadInt32();
        self._countmax = os.ReadInt32();
        return self;
    }

}

export class Task_TestDefaultBean {
    private _testInt!: number;
    get TestInt(): number { return this._testInt; }
    private _testBool!: boolean;
    get TestBool(): boolean { return this._testBool; }
    private _testString!: string;
    get TestString(): string { return this._testString; }
    private _testSubBean!: Position;
    get TestSubBean(): Position { return this._testSubBean; }
    private _testList!: number[];
    get TestList(): number[] { return this._testList; }
    private _testList2!: number[];
    get TestList2(): number[] { return this._testList2; }
    private _testMap!: Map<number, string>;
    get TestMap(): Map<number, string> { return this._testMap; }

    toString() : string {
        return "(" + this._testInt + "," + this._testBool + "," + this._testString + "," + this.TestSubBean.toString() + "," + ToStringList(this._testList) + "," + ToStringList(this._testList2) + "," + ToStringMap(this._testMap) + ")";
    }

    static _create(os: Stream) : Task_TestDefaultBean {
        const self = new Task_TestDefaultBean();
        self._testInt = os.ReadInt32();
        self._testBool = os.ReadBool();
        self._testString = os.ReadStringInPool();
        self._testSubBean = Position._create(os);
        self._testList = [];
        for (let c = os.ReadInt32(); c > 0; c--)
            self._testList.push(os.ReadInt32());
        self._testList2 = [];
        for (let c = os.ReadInt32(); c > 0; c--)
            self._testList2.push(os.ReadInt32());
        self._testMap  = new Map<number, string>();
        for (let c = os.ReadInt32(); c > 0; c--) {
            self._testMap.set(os.ReadInt32(), os.ReadStringInPool());
        }
        return self;
    }

}


export abstract class Task_Completecondition {
    abstract type() : Task_Completeconditiontype;
    _resolve(errors: LoadErrors) {
    }
    static _create(os: Stream) : Task_Completecondition {
        const typeName = os.ReadStringInPool();
        switch(typeName) {
            case "KillMonster":
                return Task_Completecondition_KillMonster._create(os);
            case "TalkNpc":
                return Task_Completecondition_TalkNpc._create(os);
            case "TestNoColumn":
                return Task_Completecondition_TestNoColumn._create(os);
            case "Chat":
                return Task_Completecondition_Chat._create(os);
            case "ConditionAnd":
                return Task_Completecondition_ConditionAnd._create(os);
            case "CollectItem":
                return Task_Completecondition_CollectItem._create(os);
            case "aa":
                return Task_Completecondition_Aa._create(os);
            default:
                throw new Error("Unknown type: " + typeName);
        }
    }

    abstract toString() : string;
}


export class Task_Completecondition_KillMonster extends Task_Completecondition {
    type() : Task_Completeconditiontype {
        return Task_Completeconditiontype.KillMonster;
    }

    private _monsterid!: number;
    get Monsterid(): number { return this._monsterid; }
    private _count!: number;
    get Count(): number { return this._count; }

    private _RefMonsterid!: Other_Monster;
    get RefMonsterid(): Other_Monster { return this._RefMonsterid; }
    toString() : string {
        return "(" + this._monsterid + "," + this._count + ")";
    }

    static _create(os: Stream) : Task_Completecondition_KillMonster {
        const self = new Task_Completecondition_KillMonster();
        self._monsterid = os.ReadInt32();
        self._count = os.ReadInt32();
        return self;
    }

    _resolve(errors: LoadErrors) {
        const _tmpRefMonsterid = Other_Monster.Get(this._monsterid);
        if (_tmpRefMonsterid === undefined) {
            errors.RefNull("KillMonster", this.toString(), "monsterid");
        }
        this._RefMonsterid = _tmpRefMonsterid!;
    }
}

export class Task_Completecondition_TalkNpc extends Task_Completecondition {
    type() : Task_Completeconditiontype {
        return Task_Completeconditiontype.TalkNpc;
    }

    private _npcid!: number;
    get Npcid(): number { return this._npcid; }

    toString() : string {
        return "(" + this._npcid + ")";
    }

    static _create(os: Stream) : Task_Completecondition_TalkNpc {
        const self = new Task_Completecondition_TalkNpc();
        self._npcid = os.ReadInt32();
        return self;
    }

}

export class Task_Completecondition_TestNoColumn extends Task_Completecondition {
    type() : Task_Completeconditiontype {
        return Task_Completeconditiontype.TestNoColumn;
    }


    toString() : string {
        return "(" +  + ")";
    }

    static _create(os: Stream) : Task_Completecondition_TestNoColumn {
        const self = new Task_Completecondition_TestNoColumn();
        return self;
    }

}

export class Task_Completecondition_Chat extends Task_Completecondition {
    type() : Task_Completeconditiontype {
        return Task_Completeconditiontype.Chat;
    }

    private _msg!: string;
    get Msg(): string { return this._msg; }

    toString() : string {
        return "(" + this._msg + ")";
    }

    static _create(os: Stream) : Task_Completecondition_Chat {
        const self = new Task_Completecondition_Chat();
        self._msg = os.ReadStringInPool();
        return self;
    }

}

export class Task_Completecondition_ConditionAnd extends Task_Completecondition {
    type() : Task_Completeconditiontype {
        return Task_Completeconditiontype.ConditionAnd;
    }

    private _cond1!: Task_Completecondition;
    get Cond1(): Task_Completecondition { return this._cond1; }
    private _cond2!: Task_Completecondition;
    get Cond2(): Task_Completecondition { return this._cond2; }

    toString() : string {
        return "(" + this.Cond1.toString() + "," + this.Cond2.toString() + ")";
    }

    static _create(os: Stream) : Task_Completecondition_ConditionAnd {
        const self = new Task_Completecondition_ConditionAnd();
        self._cond1 = Task_Completecondition._create(os);
        self._cond2 = Task_Completecondition._create(os);
        return self;
    }

    _resolve(errors: LoadErrors) {
        this._cond1._resolve(errors);
        this._cond2._resolve(errors);
    }
}

export class Task_Completecondition_CollectItem extends Task_Completecondition {
    type() : Task_Completeconditiontype {
        return Task_Completeconditiontype.CollectItem;
    }

    private _itemid!: number;
    get Itemid(): number { return this._itemid; }
    private _count!: number;
    get Count(): number { return this._count; }

    toString() : string {
        return "(" + this._itemid + "," + this._count + ")";
    }

    static _create(os: Stream) : Task_Completecondition_CollectItem {
        const self = new Task_Completecondition_CollectItem();
        self._itemid = os.ReadInt32();
        self._count = os.ReadInt32();
        return self;
    }

}

export class Task_Completecondition_Aa extends Task_Completecondition {
    type() : Task_Completeconditiontype {
        return Task_Completeconditiontype.Aa;
    }


    toString() : string {
        return "(" +  + ")";
    }

    static _create(os: Stream) : Task_Completecondition_Aa {
        const self = new Task_Completecondition_Aa();
        return self;
    }

}


export class Ai_Ai {
    private _ID!: number;
    get ID(): number { return this._ID; }
    private _Desc!: string;
    /* 描述----这里测试下多行效果--再来一行 */
    get Desc(): string { return this._Desc; }
    private _CondID!: string;
    /* 触发公式 */
    get CondID(): string { return this._CondID; }
    private _TrigTick!: Ai_TriggerTick;
    /* 触发间隔(帧) */
    get TrigTick(): Ai_TriggerTick { return this._TrigTick; }
    private _TrigOdds!: number;
    /* 触发几率 */
    get TrigOdds(): number { return this._TrigOdds; }
    private _ActionID!: number[];
    /* 触发行为 */
    get ActionID(): number[] { return this._ActionID; }
    private _DeathRemove!: boolean;
    /* 死亡移除 */
    get DeathRemove(): boolean { return this._DeathRemove; }

    toString() : string {
        return "(" + this._ID + "," + this._Desc + "," + this._CondID + "," + this.TrigTick.toString() + "," + this._TrigOdds + "," + ToStringList(this._ActionID) + "," + this._DeathRemove + ")";
    }

    
    private static all: Map<number, Ai_Ai>;

    static Get(ID: number) : Ai_Ai | undefined {
        return this.all.get(ID)
    }

    static All() : Map<number, Ai_Ai> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<number, Ai_Ai>();
        for (let c = os.ReadInt32(); c > 0; c--)
        {
            let self = this._create(os);
            this.all.set(self._ID, self);
        }

    }

    static _create(os: Stream) : Ai_Ai {
        const self = new Ai_Ai();
        self._ID = os.ReadInt32();
        self._Desc = os.ReadStringInPool();
        self._CondID = os.ReadStringInPool();
        self._TrigTick = Ai_TriggerTick._create(os);
        self._TrigOdds = os.ReadInt32();
        self._ActionID = [];
        for (let c = os.ReadInt32(); c > 0; c--)
            self._ActionID.push(os.ReadInt32());
        self._DeathRemove = os.ReadBool();
        return self;
    }

}

export class Ai_Ai_action {
    private _ID!: number;
    get ID(): number { return this._ID; }
    private _Desc!: string;
    /* 描述 */
    get Desc(): string { return this._Desc; }
    private _FormulaID!: number;
    /* 公式 */
    get FormulaID(): number { return this._FormulaID; }
    private _ArgIList!: number[];
    /* 参数(int)1 */
    get ArgIList(): number[] { return this._ArgIList; }
    private _ArgSList!: number[];
    /* 参数(string)1 */
    get ArgSList(): number[] { return this._ArgSList; }

    toString() : string {
        return "(" + this._ID + "," + this._Desc + "," + this._FormulaID + "," + ToStringList(this._ArgIList) + "," + ToStringList(this._ArgSList) + ")";
    }

    
    private static all: Map<number, Ai_Ai_action>;

    static Get(ID: number) : Ai_Ai_action | undefined {
        return this.all.get(ID)
    }

    static All() : Map<number, Ai_Ai_action> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<number, Ai_Ai_action>();
        for (let c = os.ReadInt32(); c > 0; c--)
        {
            let self = this._create(os);
            this.all.set(self._ID, self);
        }

    }

    static _create(os: Stream) : Ai_Ai_action {
        const self = new Ai_Ai_action();
        self._ID = os.ReadInt32();
        self._Desc = os.ReadStringInPool();
        self._FormulaID = os.ReadInt32();
        self._ArgIList = [];
        for (let c = os.ReadInt32(); c > 0; c--)
            self._ArgIList.push(os.ReadInt32());
        self._ArgSList = [];
        for (let c = os.ReadInt32(); c > 0; c--)
            self._ArgSList.push(os.ReadInt32());
        return self;
    }

}

export class Ai_Ai_condition {
    private _ID!: number;
    get ID(): number { return this._ID; }
    private _Desc!: string;
    /* 描述 */
    get Desc(): string { return this._Desc; }
    private _FormulaID!: number;
    /* 公式 */
    get FormulaID(): number { return this._FormulaID; }
    private _ArgIList!: number[];
    /* 参数(int)1 */
    get ArgIList(): number[] { return this._ArgIList; }
    private _ArgSList!: number[];
    /* 参数(string)1 */
    get ArgSList(): number[] { return this._ArgSList; }

    toString() : string {
        return "(" + this._ID + "," + this._Desc + "," + this._FormulaID + "," + ToStringList(this._ArgIList) + "," + ToStringList(this._ArgSList) + ")";
    }

    
    private static all: Map<number, Ai_Ai_condition>;

    static Get(ID: number) : Ai_Ai_condition | undefined {
        return this.all.get(ID)
    }

    static All() : Map<number, Ai_Ai_condition> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<number, Ai_Ai_condition>();
        for (let c = os.ReadInt32(); c > 0; c--)
        {
            let self = this._create(os);
            this.all.set(self._ID, self);
        }

    }

    static _create(os: Stream) : Ai_Ai_condition {
        const self = new Ai_Ai_condition();
        self._ID = os.ReadInt32();
        self._Desc = os.ReadStringInPool();
        self._FormulaID = os.ReadInt32();
        self._ArgIList = [];
        for (let c = os.ReadInt32(); c > 0; c--)
            self._ArgIList.push(os.ReadInt32());
        self._ArgSList = [];
        for (let c = os.ReadInt32(); c > 0; c--)
            self._ArgSList.push(os.ReadInt32());
        return self;
    }

}

export class Equip_Ability {
    private static _attack : Equip_Ability;
    static get Attack() :Equip_Ability { return this._attack; }

    private static _defence : Equip_Ability;
    static get Defence() :Equip_Ability { return this._defence; }

    private static _hp : Equip_Ability;
    static get Hp() :Equip_Ability { return this._hp; }

    private static _critical : Equip_Ability;
    static get Critical() :Equip_Ability { return this._critical; }

    private static _critical_resist : Equip_Ability;
    static get Critical_resist() :Equip_Ability { return this._critical_resist; }

    private static _block : Equip_Ability;
    static get Block() :Equip_Ability { return this._block; }

    private static _break_armor : Equip_Ability;
    static get Break_armor() :Equip_Ability { return this._break_armor; }

    private _id!: number;
    /* 属性类型 */
    get Id(): number { return this._id; }
    private _name!: string;
    /* 程序用名字 */
    get Name(): string { return this._name; }

    toString() : string {
        return "(" + this._id + "," + this._name + ")";
    }

    
    private static all: Map<number, Equip_Ability>;

    static Get(id: number) : Equip_Ability | undefined {
        return this.all.get(id)
    }

    static All() : Map<number, Equip_Ability> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<number, Equip_Ability>();
        for (let c = os.ReadInt32(); c > 0; c--)
        {
            let self = this._create(os);
            this.all.set(self._id, self);
            if (self._name.trim().length === 0) {
                continue;
            }
            switch(self._name.trim()) {
                case "attack":
                    if (this._attack != null)
                        errors.EnumDup("equip.ability", "attack");
                    this._attack = self;
                    break;
                case "defence":
                    if (this._defence != null)
                        errors.EnumDup("equip.ability", "defence");
                    this._defence = self;
                    break;
                case "hp":
                    if (this._hp != null)
                        errors.EnumDup("equip.ability", "hp");
                    this._hp = self;
                    break;
                case "critical":
                    if (this._critical != null)
                        errors.EnumDup("equip.ability", "critical");
                    this._critical = self;
                    break;
                case "critical_resist":
                    if (this._critical_resist != null)
                        errors.EnumDup("equip.ability", "critical_resist");
                    this._critical_resist = self;
                    break;
                case "block":
                    if (this._block != null)
                        errors.EnumDup("equip.ability", "block");
                    this._block = self;
                    break;
                case "break_armor":
                    if (this._break_armor != null)
                        errors.EnumDup("equip.ability", "break_armor");
                    this._break_armor = self;
                    break;
                default:
                    errors.EnumDataAdd("equip.ability", self._name);
                    break;
            }
        }

        if (this._attack == null) {
            errors.EnumNull("equip.ability", "attack");
        }
        if (this._defence == null) {
            errors.EnumNull("equip.ability", "defence");
        }
        if (this._hp == null) {
            errors.EnumNull("equip.ability", "hp");
        }
        if (this._critical == null) {
            errors.EnumNull("equip.ability", "critical");
        }
        if (this._critical_resist == null) {
            errors.EnumNull("equip.ability", "critical_resist");
        }
        if (this._block == null) {
            errors.EnumNull("equip.ability", "block");
        }
        if (this._break_armor == null) {
            errors.EnumNull("equip.ability", "break_armor");
        }
    }

    static _create(os: Stream) : Equip_Ability {
        const self = new Equip_Ability();
        self._id = os.ReadInt32();
        self._name = os.ReadStringInPool();
        return self;
    }

}

export class Equip_Equipconfig {
    private static _Instance : Equip_Equipconfig;
    static get Instance() :Equip_Equipconfig { return this._Instance; }

    private static _Instance2 : Equip_Equipconfig;
    static get Instance2() :Equip_Equipconfig { return this._Instance2; }

    private _entry!: string;
    /* 入口，程序填 */
    get Entry(): string { return this._entry; }
    private _stone_count_for_set!: number;
    /* 形成套装的音石数量 */
    get Stone_count_for_set(): number { return this._stone_count_for_set; }
    private _draw_protect_name!: string;
    /* 保底策略名称 */
    get Draw_protect_name(): string { return this._draw_protect_name; }
    private _broadcastid!: number;
    /* 公告Id */
    get Broadcastid(): number { return this._broadcastid; }
    private _broadcast_least_quality!: number;
    /* 公告的最低品质 */
    get Broadcast_least_quality(): number { return this._broadcast_least_quality; }
    private _week_reward_mailid!: number;
    /* 抽卡周奖励的邮件id */
    get Week_reward_mailid(): number { return this._week_reward_mailid; }

    toString() : string {
        return "(" + this._entry + "," + this._stone_count_for_set + "," + this._draw_protect_name + "," + this._broadcastid + "," + this._broadcast_least_quality + "," + this._week_reward_mailid + ")";
    }

    
    private static all: Map<string, Equip_Equipconfig>;

    static Get(entry: string) : Equip_Equipconfig | undefined {
        return this.all.get(entry)
    }

    static All() : Map<string, Equip_Equipconfig> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<string, Equip_Equipconfig>();
        for (let c = os.ReadInt32(); c > 0; c--)
        {
            let self = this._create(os);
            this.all.set(self._entry, self);
            if (self._entry.trim().length === 0) {
                continue;
            }
            switch(self._entry.trim()) {
                case "Instance":
                    if (this._Instance != null)
                        errors.EnumDup("equip.equipconfig", "Instance");
                    this._Instance = self;
                    break;
                case "Instance2":
                    if (this._Instance2 != null)
                        errors.EnumDup("equip.equipconfig", "Instance2");
                    this._Instance2 = self;
                    break;
                default:
                    errors.EnumDataAdd("equip.equipconfig", self._entry);
                    break;
            }
        }

        if (this._Instance == null) {
            errors.EnumNull("equip.equipconfig", "Instance");
        }
        if (this._Instance2 == null) {
            errors.EnumNull("equip.equipconfig", "Instance2");
        }
    }

    static _create(os: Stream) : Equip_Equipconfig {
        const self = new Equip_Equipconfig();
        self._entry = os.ReadStringInPool();
        self._stone_count_for_set = os.ReadInt32();
        self._draw_protect_name = os.ReadStringInPool();
        self._broadcastid = os.ReadInt32();
        self._broadcast_least_quality = os.ReadInt32();
        self._week_reward_mailid = os.ReadInt32();
        return self;
    }

}

export class Equip_Jewelry {
    private _ID!: number;
    /* 首饰ID */
    get ID(): number { return this._ID; }
    private _Name!: string;
    /* 首饰名称 */
    get Name(): string { return this._Name; }
    private _IconFile!: string;
    /* 图标ID */
    get IconFile(): string { return this._IconFile; }
    private _LvlRank!: LevelRank;
    /* 首饰等级 */
    get LvlRank(): LevelRank { return this._LvlRank; }
    private _JType!: string;
    /* 首饰类型 */
    get JType(): string { return this._JType; }
    private _SuitID!: number;
    /* 套装ID（为0是没有不属于套装，首饰品级为4的首饰该参数为套装id，其余情况为0,引用JewelrySuit.csv） */
    get SuitID(): number { return this._SuitID; }
    private _KeyAbility!: number;
    /* 关键属性类型 */
    get KeyAbility(): number { return this._KeyAbility; }
    private _KeyAbilityValue!: number;
    /* 关键属性数值 */
    get KeyAbilityValue(): number { return this._KeyAbilityValue; }
    private _SalePrice!: number;
    /* 售卖价格 */
    get SalePrice(): number { return this._SalePrice; }
    private _Description!: string;
    /* 描述,根据Lvl和Rank来随机3个属性，第一个属性由Lvl,Rank行随机，剩下2个由Lvl和小于Rank的行里随机。Rank最小的时候都从Lvl，Rank里随机。 */
    get Description(): string { return this._Description; }

    private _RefLvlRank!: Equip_Jewelryrandom;
    get RefLvlRank(): Equip_Jewelryrandom { return this._RefLvlRank; }
    private _RefJType!: Equip_Jewelrytype;
    get RefJType(): Equip_Jewelrytype { return this._RefJType; }
    private _NullableRefSuitID: Equip_Jewelrysuit | undefined;
    get NullableRefSuitID(): Equip_Jewelrysuit | undefined { return this._NullableRefSuitID; }
    private _RefKeyAbility!: Equip_Ability;
    get RefKeyAbility(): Equip_Ability { return this._RefKeyAbility; }
    toString() : string {
        return "(" + this._ID + "," + this._Name + "," + this._IconFile + "," + this.LvlRank.toString() + "," + this._JType + "," + this._SuitID + "," + this._KeyAbility + "," + this._KeyAbilityValue + "," + this._SalePrice + "," + this._Description + ")";
    }

    
    private static all: Map<number, Equip_Jewelry>;

    static Get(ID: number) : Equip_Jewelry | undefined {
        return this.all.get(ID)
    }

    static All() : Map<number, Equip_Jewelry> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<number, Equip_Jewelry>();
        for (let c = os.ReadInt32(); c > 0; c--)
        {
            let self = this._create(os);
            this.all.set(self._ID, self);
        }

    }

    static Resolve(errors: LoadErrors) {
        for (const v of this.all.values()) {
            v._resolve(errors);
        }
    }
    static _create(os: Stream) : Equip_Jewelry {
        const self = new Equip_Jewelry();
        self._ID = os.ReadInt32();
        self._Name = os.ReadStringInPool();
        self._IconFile = os.ReadStringInPool();
        self._LvlRank = LevelRank._create(os);
        self._JType = os.ReadStringInPool();
        self._SuitID = os.ReadInt32();
        self._KeyAbility = os.ReadInt32();
        self._KeyAbilityValue = os.ReadInt32();
        self._SalePrice = os.ReadInt32();
        self._Description = os.ReadStringInPool();
        return self;
    }

    _resolve(errors: LoadErrors) {
        this._LvlRank._resolve(errors);
        const _tmpRefLvlRank = Equip_Jewelryrandom.Get(this._LvlRank);
        if (_tmpRefLvlRank === undefined) {
            errors.RefNull("equip.jewelry", this.toString(), "LvlRank");
        }
        this._RefLvlRank = _tmpRefLvlRank!;
        const _tmpRefJType = Equip_Jewelrytype.Get(this._JType);
        if (_tmpRefJType === undefined) {
            errors.RefNull("equip.jewelry", this.toString(), "JType");
        }
        this._RefJType = _tmpRefJType!;
        this._NullableRefSuitID = Equip_Jewelrysuit.Get(this._SuitID);
        const _tmpRefKeyAbility = Equip_Ability.Get(this._KeyAbility);
        if (_tmpRefKeyAbility === undefined) {
            errors.RefNull("equip.jewelry", this.toString(), "KeyAbility");
        }
        this._RefKeyAbility = _tmpRefKeyAbility!;
    }
}

export class Equip_Jewelryrandom {
    private _LvlRank!: LevelRank;
    /* 等级 */
    get LvlRank(): LevelRank { return this._LvlRank; }
    private _AttackRange!: Range;
    /* 最小攻击力 */
    get AttackRange(): Range { return this._AttackRange; }
    private _OtherRange!: Range[];
    /* 最小防御力 */
    get OtherRange(): Range[] { return this._OtherRange; }
    private _TestPack!: Equip_TestPackBean[];
    /* 测试pack */
    get TestPack(): Equip_TestPackBean[] { return this._TestPack; }

    toString() : string {
        return "(" + this.LvlRank.toString() + "," + this.AttackRange.toString() + "," + ToStringList(this._OtherRange) + "," + ToStringList(this._TestPack) + ")";
    }

    
    private static all: Map<number, Equip_Jewelryrandom>;

    static Get(LvlRank: LevelRank) : Equip_Jewelryrandom | undefined {
        return this.all.get(LvlRank.Level + LvlRank.Rank * 100000000)
    }

    static All() : Map<number, Equip_Jewelryrandom> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<number, Equip_Jewelryrandom>();
        for (let c = os.ReadInt32(); c > 0; c--)
        {
            let self = this._create(os);
            this.all.set(self._LvlRank.Level + self._LvlRank.Rank * 100000000, self);
        }

    }

    static Resolve(errors: LoadErrors) {
        for (const v of this.all.values()) {
            v._resolve(errors);
        }
    }
    static _create(os: Stream) : Equip_Jewelryrandom {
        const self = new Equip_Jewelryrandom();
        self._LvlRank = LevelRank._create(os);
        self._AttackRange = Range._create(os);
        self._OtherRange = [];
        for (let c = os.ReadInt32(); c > 0; c--)
            self._OtherRange.push(Range._create(os));
        self._TestPack = [];
        for (let c = os.ReadInt32(); c > 0; c--)
            self._TestPack.push(Equip_TestPackBean._create(os));
        return self;
    }

    _resolve(errors: LoadErrors) {
        this._LvlRank._resolve(errors);
    }
}

export class Equip_Jewelrysuit {
    private static _SpecialSuit : Equip_Jewelrysuit;
    static get SpecialSuit() :Equip_Jewelrysuit { return this._SpecialSuit; }

    private _SuitID!: number;
    /* 饰品套装ID */
    get SuitID(): number { return this._SuitID; }
    private _Ename!: string;
    get Ename(): string { return this._Ename; }
    private _Name!: Text;
    /* 策划用名字 */
    get Name(): Text { return this._Name; }
    private _Ability1!: number;
    /* 套装属性类型1（装备套装中的两件时增加的属性） */
    get Ability1(): number { return this._Ability1; }
    private _Ability1Value!: number;
    /* 套装属性1 */
    get Ability1Value(): number { return this._Ability1Value; }
    private _Ability2!: number;
    /* 套装属性类型2（装备套装中的三件时增加的属性） */
    get Ability2(): number { return this._Ability2; }
    private _Ability2Value!: number;
    /* 套装属性2 */
    get Ability2Value(): number { return this._Ability2Value; }
    private _Ability3!: number;
    /* 套装属性类型3（装备套装中的四件时增加的属性） */
    get Ability3(): number { return this._Ability3; }
    private _Ability3Value!: number;
    /* 套装属性3 */
    get Ability3Value(): number { return this._Ability3Value; }
    private _SuitList!: number[];
    /* 部件1 */
    get SuitList(): number[] { return this._SuitList; }

    toString() : string {
        return "(" + this._SuitID + "," + this._Ename + "," + this._Name + "," + this._Ability1 + "," + this._Ability1Value + "," + this._Ability2 + "," + this._Ability2Value + "," + this._Ability3 + "," + this._Ability3Value + "," + ToStringList(this._SuitList) + ")";
    }

    
    private static all: Map<number, Equip_Jewelrysuit>;

    static Get(SuitID: number) : Equip_Jewelrysuit | undefined {
        return this.all.get(SuitID)
    }

    static All() : Map<number, Equip_Jewelrysuit> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<number, Equip_Jewelrysuit>();
        for (let c = os.ReadInt32(); c > 0; c--)
        {
            let self = this._create(os);
            this.all.set(self._SuitID, self);
            if (self._Ename.trim().length === 0) {
                continue;
            }
            switch(self._Ename.trim()) {
                case "SpecialSuit":
                    if (this._SpecialSuit != null)
                        errors.EnumDup("equip.jewelrysuit", "SpecialSuit");
                    this._SpecialSuit = self;
                    break;
                default:
                    errors.EnumDataAdd("equip.jewelrysuit", self._Ename);
                    break;
            }
        }

        if (this._SpecialSuit == null) {
            errors.EnumNull("equip.jewelrysuit", "SpecialSuit");
        }
    }

    static _create(os: Stream) : Equip_Jewelrysuit {
        const self = new Equip_Jewelrysuit();
        self._SuitID = os.ReadInt32();
        self._Ename = os.ReadStringInPool();
        self._Name = Text._create(os);
        self._Ability1 = os.ReadInt32();
        self._Ability1Value = os.ReadInt32();
        self._Ability2 = os.ReadInt32();
        self._Ability2Value = os.ReadInt32();
        self._Ability3 = os.ReadInt32();
        self._Ability3Value = os.ReadInt32();
        self._SuitList = [];
        for (let c = os.ReadInt32(); c > 0; c--)
            self._SuitList.push(os.ReadInt32());
        return self;
    }

}

export class Equip_Jewelrytype {
    private static _Jade : Equip_Jewelrytype;
    static get Jade() :Equip_Jewelrytype { return this._Jade; }

    private static _Bracelet : Equip_Jewelrytype;
    static get Bracelet() :Equip_Jewelrytype { return this._Bracelet; }

    private static _Magic : Equip_Jewelrytype;
    static get Magic() :Equip_Jewelrytype { return this._Magic; }

    private static _Bottle : Equip_Jewelrytype;
    static get Bottle() :Equip_Jewelrytype { return this._Bottle; }

    private _TypeName!: string;
    /* 程序用名字 */
    get TypeName(): string { return this._TypeName; }

    toString() : string {
        return "(" + this._TypeName + ")";
    }

    
    private static all: Map<string, Equip_Jewelrytype>;

    static Get(TypeName: string) : Equip_Jewelrytype | undefined {
        return this.all.get(TypeName)
    }

    static All() : Map<string, Equip_Jewelrytype> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<string, Equip_Jewelrytype>();
        for (let c = os.ReadInt32(); c > 0; c--)
        {
            let self = this._create(os);
            this.all.set(self._TypeName, self);
            if (self._TypeName.trim().length === 0) {
                continue;
            }
            switch(self._TypeName.trim()) {
                case "Jade":
                    if (this._Jade != null)
                        errors.EnumDup("equip.jewelrytype", "Jade");
                    this._Jade = self;
                    break;
                case "Bracelet":
                    if (this._Bracelet != null)
                        errors.EnumDup("equip.jewelrytype", "Bracelet");
                    this._Bracelet = self;
                    break;
                case "Magic":
                    if (this._Magic != null)
                        errors.EnumDup("equip.jewelrytype", "Magic");
                    this._Magic = self;
                    break;
                case "Bottle":
                    if (this._Bottle != null)
                        errors.EnumDup("equip.jewelrytype", "Bottle");
                    this._Bottle = self;
                    break;
                default:
                    errors.EnumDataAdd("equip.jewelrytype", self._TypeName);
                    break;
            }
        }

        if (this._Jade == null) {
            errors.EnumNull("equip.jewelrytype", "Jade");
        }
        if (this._Bracelet == null) {
            errors.EnumNull("equip.jewelrytype", "Bracelet");
        }
        if (this._Magic == null) {
            errors.EnumNull("equip.jewelrytype", "Magic");
        }
        if (this._Bottle == null) {
            errors.EnumNull("equip.jewelrytype", "Bottle");
        }
    }

    static _create(os: Stream) : Equip_Jewelrytype {
        const self = new Equip_Jewelrytype();
        self._TypeName = os.ReadStringInPool();
        return self;
    }

}

export class Equip_Rank {
    private static _white : Equip_Rank;
    static get White() :Equip_Rank { return this._white; }

    private static _green : Equip_Rank;
    static get Green() :Equip_Rank { return this._green; }

    private static _blue : Equip_Rank;
    static get Blue() :Equip_Rank { return this._blue; }

    private static _purple : Equip_Rank;
    static get Purple() :Equip_Rank { return this._purple; }

    private static _yellow : Equip_Rank;
    static get Yellow() :Equip_Rank { return this._yellow; }

    private _RankID!: number;
    /* 稀有度 */
    get RankID(): number { return this._RankID; }
    private _RankName!: string;
    /* 程序用名字 */
    get RankName(): string { return this._RankName; }
    private _RankShowName!: string;
    /* 显示名称 */
    get RankShowName(): string { return this._RankShowName; }

    toString() : string {
        return "(" + this._RankID + "," + this._RankName + "," + this._RankShowName + ")";
    }

    
    private static all: Map<number, Equip_Rank>;

    static Get(RankID: number) : Equip_Rank | undefined {
        return this.all.get(RankID)
    }

    static All() : Map<number, Equip_Rank> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<number, Equip_Rank>();
        for (let c = os.ReadInt32(); c > 0; c--)
        {
            let self = this._create(os);
            this.all.set(self._RankID, self);
            if (self._RankName.trim().length === 0) {
                continue;
            }
            switch(self._RankName.trim()) {
                case "white":
                    if (this._white != null)
                        errors.EnumDup("equip.rank", "white");
                    this._white = self;
                    break;
                case "green":
                    if (this._green != null)
                        errors.EnumDup("equip.rank", "green");
                    this._green = self;
                    break;
                case "blue":
                    if (this._blue != null)
                        errors.EnumDup("equip.rank", "blue");
                    this._blue = self;
                    break;
                case "purple":
                    if (this._purple != null)
                        errors.EnumDup("equip.rank", "purple");
                    this._purple = self;
                    break;
                case "yellow":
                    if (this._yellow != null)
                        errors.EnumDup("equip.rank", "yellow");
                    this._yellow = self;
                    break;
                default:
                    errors.EnumDataAdd("equip.rank", self._RankName);
                    break;
            }
        }

        if (this._white == null) {
            errors.EnumNull("equip.rank", "white");
        }
        if (this._green == null) {
            errors.EnumNull("equip.rank", "green");
        }
        if (this._blue == null) {
            errors.EnumNull("equip.rank", "blue");
        }
        if (this._purple == null) {
            errors.EnumNull("equip.rank", "purple");
        }
        if (this._yellow == null) {
            errors.EnumNull("equip.rank", "yellow");
        }
    }

    static _create(os: Stream) : Equip_Rank {
        const self = new Equip_Rank();
        self._RankID = os.ReadInt32();
        self._RankName = os.ReadStringInPool();
        self._RankShowName = os.ReadStringInPool();
        return self;
    }

}

export class Other_Drop {
    private _dropid!: number;
    /* 序号 */
    get Dropid(): number { return this._dropid; }
    private _name!: Text;
    /* 名字 */
    get Name(): Text { return this._name; }
    private _items!: Other_DropItem[];
    /* 掉落概率 */
    get Items(): Other_DropItem[] { return this._items; }
    private _testmap!: Map<number, number>;
    /* 测试map block */
    get Testmap(): Map<number, number> { return this._testmap; }

    toString() : string {
        return "(" + this._dropid + "," + this._name + "," + ToStringList(this._items) + "," + ToStringMap(this._testmap) + ")";
    }

    
    private static all: Map<number, Other_Drop>;

    static Get(dropid: number) : Other_Drop | undefined {
        return this.all.get(dropid)
    }

    static All() : Map<number, Other_Drop> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<number, Other_Drop>();
        for (let c = os.ReadInt32(); c > 0; c--)
        {
            let self = this._create(os);
            this.all.set(self._dropid, self);
        }

    }

    static _create(os: Stream) : Other_Drop {
        const self = new Other_Drop();
        self._dropid = os.ReadInt32();
        self._name = Text._create(os);
        self._items = [];
        for (let c = os.ReadInt32(); c > 0; c--)
            self._items.push(Other_DropItem._create(os));
        self._testmap  = new Map<number, number>();
        for (let c = os.ReadInt32(); c > 0; c--) {
            self._testmap.set(os.ReadInt32(), os.ReadInt32());
        }
        return self;
    }

}

export class Other_Keytest {
    private _id1!: number;
    get Id1(): number { return this._id1; }
    private _id2!: number;
    get Id2(): number { return this._id2; }
    private _id3!: number;
    get Id3(): number { return this._id3; }
    private _ids!: number[];
    get Ids(): number[] { return this._ids; }

    private _RefIds!: Other_Signin[];
    get RefIds(): Other_Signin[] { return this._RefIds; }
    toString() : string {
        return "(" + this._id1 + "," + this._id2 + "," + this._id3 + "," + ToStringList(this._ids) + ")";
    }

    
    private static all: Map<number, Other_Keytest>;

    static Get(id1: number, id2: number) : Other_Keytest | undefined {
        return this.all.get(id1 + id2 * 100000000)
    }

    
    private static id1Id3Map: Map<number, Other_Keytest>;

    static GetById1Id3(id1: number, id3: number) : Other_Keytest | undefined {
        return this.id1Id3Map.get(id1 + id3 * 100000000)
    }

    
    private static id2Map: Map<number, Other_Keytest>;

    static GetById2(id2: number) : Other_Keytest | undefined {
        return this.id2Map.get(id2)
    }

    
    private static id2Id3Map: Map<number, Other_Keytest>;

    static GetById2Id3(id2: number, id3: number) : Other_Keytest | undefined {
        return this.id2Id3Map.get(id2 + id3 * 100000000)
    }

    static All() : Map<number, Other_Keytest> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<number, Other_Keytest>();
        this.id1Id3Map = new Map<number, Other_Keytest>();
        this.id2Map = new Map<number, Other_Keytest>();
        this.id2Id3Map = new Map<number, Other_Keytest>();
        for (let c = os.ReadInt32(); c > 0; c--)
        {
            let self = this._create(os);
            this.all.set(self._id1 + self._id2 * 100000000, self);
            this.id1Id3Map.set(self._id1 + self._id3 * 100000000, self);
            this.id2Map.set(self._id2, self);
            this.id2Id3Map.set(self._id2 + self._id3 * 100000000, self);
        }

    }

    static Resolve(errors: LoadErrors) {
        for (const v of this.all.values()) {
            v._resolve(errors);
        }
    }
    static _create(os: Stream) : Other_Keytest {
        const self = new Other_Keytest();
        self._id1 = os.ReadInt32();
        self._id2 = os.ReadInt64();
        self._id3 = os.ReadInt32();
        self._ids = [];
        for (let c = os.ReadInt32(); c > 0; c--)
            self._ids.push(os.ReadInt32());
        return self;
    }

    _resolve(errors: LoadErrors) {
        this._RefIds = [];
        for (const e of this._ids) {
            const r = Other_Signin.Get(e);
            if (r === undefined) {
                errors.RefNull("other.keytest", this.toString(), "ids");
            }
            this._RefIds.push(r);
        }
    }
}

export class Other_Loot {
    private _lootid!: number;
    /* 序号 */
    get Lootid(): number { return this._lootid; }
    private _ename!: string;
    get Ename(): string { return this._ename; }
    private _name!: Text;
    /* 名字 */
    get Name(): Text { return this._name; }
    private _chanceList!: number[];
    /* 掉落0件物品的概率 */
    get ChanceList(): number[] { return this._chanceList; }

    private _ListRefLootid!: Other_Lootitem[];
    get ListRefLootid(): Other_Lootitem[] { return this._ListRefLootid; }
    private _ListRefAnotherWay!: Other_Lootitem[];
    get ListRefAnotherWay(): Other_Lootitem[] { return this._ListRefAnotherWay; }
    toString() : string {
        return "(" + this._lootid + "," + this._ename + "," + this._name + "," + ToStringList(this._chanceList) + ")";
    }

    
    private static all: Map<number, Other_Loot>;

    static Get(lootid: number) : Other_Loot | undefined {
        return this.all.get(lootid)
    }

    static All() : Map<number, Other_Loot> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<number, Other_Loot>();
        for (let c = os.ReadInt32(); c > 0; c--)
        {
            let self = this._create(os);
            this.all.set(self._lootid, self);
        }

    }

    static Resolve(errors: LoadErrors) {
        for (const v of this.all.values()) {
            v._resolve(errors);
        }
    }
    static _create(os: Stream) : Other_Loot {
        const self = new Other_Loot();
        self._lootid = os.ReadInt32();
        self._ename = os.ReadStringInPool();
        self._name = Text._create(os);
        self._chanceList = [];
        for (let c = os.ReadInt32(); c > 0; c--)
            self._chanceList.push(os.ReadInt32());
        return self;
    }

    _resolve(errors: LoadErrors) {
        this._ListRefLootid = [];
        for (const v of Other_Lootitem.All().values())
        {
            if (v.Lootid === this._lootid)
                this._ListRefLootid.push(v);
        }
        this._ListRefAnotherWay = [];
        for (const v of Other_Lootitem.All().values())
        {
            if (v.Lootid === this._lootid)
                this._ListRefAnotherWay.push(v);
        }
    }
}

export class Other_Lootitem {
    private _lootid!: number;
    /* 掉落id */
    get Lootid(): number { return this._lootid; }
    private _itemid!: number;
    /* 掉落物品 */
    get Itemid(): number { return this._itemid; }
    private _chance!: number;
    /* 掉落概率 */
    get Chance(): number { return this._chance; }
    private _countmin!: number;
    /* 数量下限 */
    get Countmin(): number { return this._countmin; }
    private _countmax!: number;
    /* 数量上限 */
    get Countmax(): number { return this._countmax; }

    toString() : string {
        return "(" + this._lootid + "," + this._itemid + "," + this._chance + "," + this._countmin + "," + this._countmax + ")";
    }

    
    private static all: Map<number, Other_Lootitem>;

    static Get(lootid: number, itemid: number) : Other_Lootitem | undefined {
        return this.all.get(lootid + itemid * 100000000)
    }

    static All() : Map<number, Other_Lootitem> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<number, Other_Lootitem>();
        for (let c = os.ReadInt32(); c > 0; c--)
        {
            let self = this._create(os);
            this.all.set(self._lootid + self._itemid * 100000000, self);
        }

    }

    static _create(os: Stream) : Other_Lootitem {
        const self = new Other_Lootitem();
        self._lootid = os.ReadInt32();
        self._itemid = os.ReadInt32();
        self._chance = os.ReadInt32();
        self._countmin = os.ReadInt32();
        self._countmax = os.ReadInt32();
        return self;
    }

}

export class Other_Monster {
    private _id!: number;
    get Id(): number { return this._id; }
    private _posList!: Position[];
    get PosList(): Position[] { return this._posList; }
    private _lootId!: number;
    /* loot */
    get LootId(): number { return this._lootId; }
    private _lootItemId!: number;
    /* item */
    get LootItemId(): number { return this._lootItemId; }

    private _RefLoot!: Other_Lootitem;
    get RefLoot(): Other_Lootitem { return this._RefLoot; }
    private _RefAllLoot!: Other_Loot;
    get RefAllLoot(): Other_Loot { return this._RefAllLoot; }
    toString() : string {
        return "(" + this._id + "," + ToStringList(this._posList) + "," + this._lootId + "," + this._lootItemId + ")";
    }

    
    private static all: Map<number, Other_Monster>;

    static Get(id: number) : Other_Monster | undefined {
        return this.all.get(id)
    }

    static All() : Map<number, Other_Monster> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<number, Other_Monster>();
        for (let c = os.ReadInt32(); c > 0; c--)
        {
            let self = this._create(os);
            this.all.set(self._id, self);
        }

    }

    static Resolve(errors: LoadErrors) {
        for (const v of this.all.values()) {
            v._resolve(errors);
        }
    }
    static _create(os: Stream) : Other_Monster {
        const self = new Other_Monster();
        self._id = os.ReadInt32();
        self._posList = [];
        for (let c = os.ReadInt32(); c > 0; c--)
            self._posList.push(Position._create(os));
        self._lootId = os.ReadInt32();
        self._lootItemId = os.ReadInt32();
        return self;
    }

    _resolve(errors: LoadErrors) {
        const _tmpRefLoot = Other_Lootitem.Get(this._lootId, this._lootItemId);
        if (_tmpRefLoot === undefined) {
            errors.RefNull("other.monster", this.toString(), "Loot");
        }
        this._RefLoot = _tmpRefLoot!;
        const _tmpRefAllLoot = Other_Loot.Get(this._lootId);
        if (_tmpRefAllLoot === undefined) {
            errors.RefNull("other.monster", this.toString(), "AllLoot");
        }
        this._RefAllLoot = _tmpRefAllLoot!;
    }
}

export class Other_Signin {
    private _id!: number;
    /* 礼包ID */
    get Id(): number { return this._id; }
    private _item2countMap!: Map<number, number>;
    /* 普通奖励 */
    get Item2countMap(): Map<number, number> { return this._item2countMap; }
    private _vipitem2vipcountMap!: Map<number, number>;
    /* vip奖励 */
    get Vipitem2vipcountMap(): Map<number, number> { return this._vipitem2vipcountMap; }
    private _viplevel!: number;
    /* 领取vip奖励的最低等级 */
    get Viplevel(): number { return this._viplevel; }
    private _IconFile!: string;
    /* 礼包图标 */
    get IconFile(): string { return this._IconFile; }

    private _RefVipitem2vipcountMap!: Map<number, Other_Loot>;
    get RefVipitem2vipcountMap(): Map<number, Other_Loot> { return this._RefVipitem2vipcountMap; }
    toString() : string {
        return "(" + this._id + "," + ToStringMap(this._item2countMap) + "," + ToStringMap(this._vipitem2vipcountMap) + "," + this._viplevel + "," + this._IconFile + ")";
    }

    
    private static all: Map<number, Other_Signin>;

    static Get(id: number) : Other_Signin | undefined {
        return this.all.get(id)
    }

    static All() : Map<number, Other_Signin> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<number, Other_Signin>();
        for (let c = os.ReadInt32(); c > 0; c--)
        {
            let self = this._create(os);
            this.all.set(self._id, self);
        }

    }

    static Resolve(errors: LoadErrors) {
        for (const v of this.all.values()) {
            v._resolve(errors);
        }
    }
    static _create(os: Stream) : Other_Signin {
        const self = new Other_Signin();
        self._id = os.ReadInt32();
        self._item2countMap  = new Map<number, number>();
        for (let c = os.ReadInt32(); c > 0; c--) {
            self._item2countMap.set(os.ReadInt32(), os.ReadInt32());
        }
        self._vipitem2vipcountMap  = new Map<number, number>();
        for (let c = os.ReadInt32(); c > 0; c--) {
            self._vipitem2vipcountMap.set(os.ReadInt32(), os.ReadInt32());
        }
        self._viplevel = os.ReadInt32();
        self._IconFile = os.ReadStringInPool();
        return self;
    }

    _resolve(errors: LoadErrors) {
        this._RefVipitem2vipcountMap = new Map<number, Other_Loot>();
        for (const e of this._vipitem2vipcountMap.entries()) {
            const v = Other_Loot.Get(e[1]);
            if (v === undefined) {
                errors.RefNull("other.signin", this.toString(), "vipitem2vipcountMap");
            }
            this._RefVipitem2vipcountMap.set(e[0], v);
        }
    }
}

export class Task_Completeconditiontype {
    private static _KillMonster : Task_Completeconditiontype;
    static get KillMonster() :Task_Completeconditiontype { return this._KillMonster; }

    private static _TalkNpc : Task_Completeconditiontype;
    static get TalkNpc() :Task_Completeconditiontype { return this._TalkNpc; }

    private static _CollectItem : Task_Completeconditiontype;
    static get CollectItem() :Task_Completeconditiontype { return this._CollectItem; }

    private static _ConditionAnd : Task_Completeconditiontype;
    static get ConditionAnd() :Task_Completeconditiontype { return this._ConditionAnd; }

    private static _Chat : Task_Completeconditiontype;
    static get Chat() :Task_Completeconditiontype { return this._Chat; }

    private static _TestNoColumn : Task_Completeconditiontype;
    static get TestNoColumn() :Task_Completeconditiontype { return this._TestNoColumn; }

    private static _aa : Task_Completeconditiontype;
    static get Aa() :Task_Completeconditiontype { return this._aa; }

    private _id!: number;
    /* 任务完成条件类型（id的范围为1-100） */
    get Id(): number { return this._id; }
    private _name!: string;
    /* 程序用名字 */
    get Name(): string { return this._name; }

    toString() : string {
        return "(" + this._id + "," + this._name + ")";
    }

    
    private static all: Map<number, Task_Completeconditiontype>;

    static Get(id: number) : Task_Completeconditiontype | undefined {
        return this.all.get(id)
    }

    static All() : Map<number, Task_Completeconditiontype> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<number, Task_Completeconditiontype>();
        for (let c = os.ReadInt32(); c > 0; c--)
        {
            let self = this._create(os);
            this.all.set(self._id, self);
            if (self._name.trim().length === 0) {
                continue;
            }
            switch(self._name.trim()) {
                case "KillMonster":
                    if (this._KillMonster != null)
                        errors.EnumDup("task.completeconditiontype", "KillMonster");
                    this._KillMonster = self;
                    break;
                case "TalkNpc":
                    if (this._TalkNpc != null)
                        errors.EnumDup("task.completeconditiontype", "TalkNpc");
                    this._TalkNpc = self;
                    break;
                case "CollectItem":
                    if (this._CollectItem != null)
                        errors.EnumDup("task.completeconditiontype", "CollectItem");
                    this._CollectItem = self;
                    break;
                case "ConditionAnd":
                    if (this._ConditionAnd != null)
                        errors.EnumDup("task.completeconditiontype", "ConditionAnd");
                    this._ConditionAnd = self;
                    break;
                case "Chat":
                    if (this._Chat != null)
                        errors.EnumDup("task.completeconditiontype", "Chat");
                    this._Chat = self;
                    break;
                case "TestNoColumn":
                    if (this._TestNoColumn != null)
                        errors.EnumDup("task.completeconditiontype", "TestNoColumn");
                    this._TestNoColumn = self;
                    break;
                case "aa":
                    if (this._aa != null)
                        errors.EnumDup("task.completeconditiontype", "aa");
                    this._aa = self;
                    break;
                default:
                    errors.EnumDataAdd("task.completeconditiontype", self._name);
                    break;
            }
        }

        if (this._KillMonster == null) {
            errors.EnumNull("task.completeconditiontype", "KillMonster");
        }
        if (this._TalkNpc == null) {
            errors.EnumNull("task.completeconditiontype", "TalkNpc");
        }
        if (this._CollectItem == null) {
            errors.EnumNull("task.completeconditiontype", "CollectItem");
        }
        if (this._ConditionAnd == null) {
            errors.EnumNull("task.completeconditiontype", "ConditionAnd");
        }
        if (this._Chat == null) {
            errors.EnumNull("task.completeconditiontype", "Chat");
        }
        if (this._TestNoColumn == null) {
            errors.EnumNull("task.completeconditiontype", "TestNoColumn");
        }
        if (this._aa == null) {
            errors.EnumNull("task.completeconditiontype", "aa");
        }
    }

    static _create(os: Stream) : Task_Completeconditiontype {
        const self = new Task_Completeconditiontype();
        self._id = os.ReadInt32();
        self._name = os.ReadStringInPool();
        return self;
    }

}

export class Task_Task {
    private _taskid!: number;
    /* 任务完成条件类型（id的范围为1-100） */
    get Taskid(): number { return this._taskid; }
    private _name!: Text[];
    /* 程序用名字 */
    get Name(): Text[] { return this._name; }
    private _nexttask!: number;
    get Nexttask(): number { return this._nexttask; }
    private _completecondition!: Task_Completecondition;
    get Completecondition(): Task_Completecondition { return this._completecondition; }
    private _exp!: number;
    get Exp(): number { return this._exp; }
    private _testDefaultBean!: Task_TestDefaultBean;
    /* 测试 */
    get TestDefaultBean(): Task_TestDefaultBean { return this._testDefaultBean; }

    private _NullableRefTaskid: Task_Taskextraexp | undefined;
    get NullableRefTaskid(): Task_Taskextraexp | undefined { return this._NullableRefTaskid; }
    private _NullableRefNexttask: Task_Task | undefined;
    get NullableRefNexttask(): Task_Task | undefined { return this._NullableRefNexttask; }
    toString() : string {
        return "(" + this._taskid + "," + ToStringList(this._name) + "," + this._nexttask + "," + this.Completecondition.toString() + "," + this._exp + "," + this.TestDefaultBean.toString() + ")";
    }

    
    private static all: Map<number, Task_Task>;

    static Get(taskid: number) : Task_Task | undefined {
        return this.all.get(taskid)
    }

    static All() : Map<number, Task_Task> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<number, Task_Task>();
        for (let c = os.ReadInt32(); c > 0; c--)
        {
            let self = this._create(os);
            this.all.set(self._taskid, self);
        }

    }

    static Resolve(errors: LoadErrors) {
        for (const v of this.all.values()) {
            v._resolve(errors);
        }
    }
    static _create(os: Stream) : Task_Task {
        const self = new Task_Task();
        self._taskid = os.ReadInt32();
        self._name = [];
        for (let c = os.ReadInt32(); c > 0; c--)
            self._name.push(Text._create(os));
        self._nexttask = os.ReadInt32();
        self._completecondition = Task_Completecondition._create(os);
        self._exp = os.ReadInt32();
        self._testDefaultBean = Task_TestDefaultBean._create(os);
        return self;
    }

    _resolve(errors: LoadErrors) {
        this._completecondition._resolve(errors);
        this._NullableRefTaskid = Task_Taskextraexp.Get(this._taskid);
        this._NullableRefNexttask = Task_Task.Get(this._nexttask);
    }
}

export class Task_Task2 {
    private _taskid!: number;
    /* 任务完成条件类型（id的范围为1-100） */
    get Taskid(): number { return this._taskid; }
    private _name!: Text[];
    get Name(): Text[] { return this._name; }
    private _nexttask!: number;
    get Nexttask(): number { return this._nexttask; }
    private _completecondition!: Task_Completecondition;
    get Completecondition(): Task_Completecondition { return this._completecondition; }
    private _exp!: number;
    get Exp(): number { return this._exp; }
    private _testBool!: boolean;
    get TestBool(): boolean { return this._testBool; }
    private _testString!: string;
    get TestString(): string { return this._testString; }
    private _testStruct!: Position;
    get TestStruct(): Position { return this._testStruct; }
    private _testList!: number[];
    get TestList(): number[] { return this._testList; }
    private _testListStruct!: Position[];
    get TestListStruct(): Position[] { return this._testListStruct; }
    private _testListInterface!: Ai_TriggerTick[];
    get TestListInterface(): Ai_TriggerTick[] { return this._testListInterface; }

    private _NullableRefTaskid: Task_Taskextraexp | undefined;
    get NullableRefTaskid(): Task_Taskextraexp | undefined { return this._NullableRefTaskid; }
    private _NullableRefNexttask: Task_Task | undefined;
    get NullableRefNexttask(): Task_Task | undefined { return this._NullableRefNexttask; }
    toString() : string {
        return "(" + this._taskid + "," + ToStringList(this._name) + "," + this._nexttask + "," + this.Completecondition.toString() + "," + this._exp + "," + this._testBool + "," + this._testString + "," + this.TestStruct.toString() + "," + ToStringList(this._testList) + "," + ToStringList(this._testListStruct) + "," + ToStringList(this._testListInterface) + ")";
    }

    
    private static all: Map<number, Task_Task2>;

    static Get(taskid: number) : Task_Task2 | undefined {
        return this.all.get(taskid)
    }

    static All() : Map<number, Task_Task2> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<number, Task_Task2>();
        for (let c = os.ReadInt32(); c > 0; c--)
        {
            let self = this._create(os);
            this.all.set(self._taskid, self);
        }

    }

    static Resolve(errors: LoadErrors) {
        for (const v of this.all.values()) {
            v._resolve(errors);
        }
    }
    static _create(os: Stream) : Task_Task2 {
        const self = new Task_Task2();
        self._taskid = os.ReadInt32();
        self._name = [];
        for (let c = os.ReadInt32(); c > 0; c--)
            self._name.push(Text._create(os));
        self._nexttask = os.ReadInt32();
        self._completecondition = Task_Completecondition._create(os);
        self._exp = os.ReadInt32();
        self._testBool = os.ReadBool();
        self._testString = os.ReadStringInPool();
        self._testStruct = Position._create(os);
        self._testList = [];
        for (let c = os.ReadInt32(); c > 0; c--)
            self._testList.push(os.ReadInt32());
        self._testListStruct = [];
        for (let c = os.ReadInt32(); c > 0; c--)
            self._testListStruct.push(Position._create(os));
        self._testListInterface = [];
        for (let c = os.ReadInt32(); c > 0; c--)
            self._testListInterface.push(Ai_TriggerTick._create(os));
        return self;
    }

    _resolve(errors: LoadErrors) {
        this._completecondition._resolve(errors);
        this._NullableRefTaskid = Task_Taskextraexp.Get(this._taskid);
        this._NullableRefNexttask = Task_Task.Get(this._nexttask);
    }
}

export class Task_Taskextraexp {
    private _taskid!: number;
    /* 任务完成条件类型（id的范围为1-100） */
    get Taskid(): number { return this._taskid; }
    private _extraexp!: number;
    /* 额外奖励经验 */
    get Extraexp(): number { return this._extraexp; }
    private _test1!: string;
    get Test1(): string { return this._test1; }
    private _test2!: string;
    get Test2(): string { return this._test2; }
    private _fielda!: string;
    get Fielda(): string { return this._fielda; }
    private _fieldb!: string;
    get Fieldb(): string { return this._fieldb; }
    private _fieldc!: string;
    get Fieldc(): string { return this._fieldc; }
    private _fieldd!: string;
    get Fieldd(): string { return this._fieldd; }

    toString() : string {
        return "(" + this._taskid + "," + this._extraexp + "," + this._test1 + "," + this._test2 + "," + this._fielda + "," + this._fieldb + "," + this._fieldc + "," + this._fieldd + ")";
    }

    
    private static all: Map<number, Task_Taskextraexp>;

    static Get(taskid: number) : Task_Taskextraexp | undefined {
        return this.all.get(taskid)
    }

    static All() : Map<number, Task_Taskextraexp> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<number, Task_Taskextraexp>();
        for (let c = os.ReadInt32(); c > 0; c--)
        {
            let self = this._create(os);
            this.all.set(self._taskid, self);
        }

    }

    static _create(os: Stream) : Task_Taskextraexp {
        const self = new Task_Taskextraexp();
        self._taskid = os.ReadInt32();
        self._extraexp = os.ReadInt32();
        self._test1 = os.ReadStringInPool();
        self._test2 = os.ReadStringInPool();
        self._fielda = os.ReadStringInPool();
        self._fieldb = os.ReadStringInPool();
        self._fieldc = os.ReadStringInPool();
        self._fieldd = os.ReadStringInPool();
        return self;
    }

}


export class Text
{
    private index: number = 0;

    get T(): string {
        return TextPoolManager.GetText(this.index);
    }

    static _create(os: Stream): Text
    {
        const self = new Text();
        self.index = os.ReadTextIndex();
        return self;
    }

    toString(): string {
        return this.T;
    }
}


export class Processor {

    // 从 bytes 文件加载（新格式）
    static Process(os: Stream, errors: LoadErrors): void {
        const configNulls = new Set<string>([
            "ai.ai",
            "ai.ai_action",
            "ai.ai_condition",
            "equip.ability",
            "equip.equipconfig",
            "equip.jewelry",
            "equip.jewelryrandom",
            "equip.jewelrysuit",
            "equip.jewelrytype",
            "equip.rank",
            "other.drop",
            "other.keytest",
            "other.loot",
            "other.lootitem",
            "other.monster",
            "other.signin",
            "task.completeconditiontype",
            "task.task",
            "task.task2",
            "task.taskextraexp",
        ]);

        // 读取表数量
        const tableCount = os.ReadSize();

        for (let i = 0; i < tableCount; i++) {
            // 读取表名
            const tableName = os.ReadString();
            // 读取表大小
            const tableSize = os.ReadSize();

            // 根据表名分发到对应的 Initialize 方法
            switch(tableName) {
                case "ai.ai":
                    configNulls.delete(tableName);
                    Ai_Ai.Initialize(os, errors);
                    break;
                case "ai.ai_action":
                    configNulls.delete(tableName);
                    Ai_Ai_action.Initialize(os, errors);
                    break;
                case "ai.ai_condition":
                    configNulls.delete(tableName);
                    Ai_Ai_condition.Initialize(os, errors);
                    break;
                case "equip.ability":
                    configNulls.delete(tableName);
                    Equip_Ability.Initialize(os, errors);
                    break;
                case "equip.equipconfig":
                    configNulls.delete(tableName);
                    Equip_Equipconfig.Initialize(os, errors);
                    break;
                case "equip.jewelry":
                    configNulls.delete(tableName);
                    Equip_Jewelry.Initialize(os, errors);
                    break;
                case "equip.jewelryrandom":
                    configNulls.delete(tableName);
                    Equip_Jewelryrandom.Initialize(os, errors);
                    break;
                case "equip.jewelrysuit":
                    configNulls.delete(tableName);
                    Equip_Jewelrysuit.Initialize(os, errors);
                    break;
                case "equip.jewelrytype":
                    configNulls.delete(tableName);
                    Equip_Jewelrytype.Initialize(os, errors);
                    break;
                case "equip.rank":
                    configNulls.delete(tableName);
                    Equip_Rank.Initialize(os, errors);
                    break;
                case "other.drop":
                    configNulls.delete(tableName);
                    Other_Drop.Initialize(os, errors);
                    break;
                case "other.keytest":
                    configNulls.delete(tableName);
                    Other_Keytest.Initialize(os, errors);
                    break;
                case "other.loot":
                    configNulls.delete(tableName);
                    Other_Loot.Initialize(os, errors);
                    break;
                case "other.lootitem":
                    configNulls.delete(tableName);
                    Other_Lootitem.Initialize(os, errors);
                    break;
                case "other.monster":
                    configNulls.delete(tableName);
                    Other_Monster.Initialize(os, errors);
                    break;
                case "other.signin":
                    configNulls.delete(tableName);
                    Other_Signin.Initialize(os, errors);
                    break;
                case "task.completeconditiontype":
                    configNulls.delete(tableName);
                    Task_Completeconditiontype.Initialize(os, errors);
                    break;
                case "task.task":
                    configNulls.delete(tableName);
                    Task_Task.Initialize(os, errors);
                    break;
                case "task.task2":
                    configNulls.delete(tableName);
                    Task_Task2.Initialize(os, errors);
                    break;
                case "task.taskextraexp":
                    configNulls.delete(tableName);
                    Task_Taskextraexp.Initialize(os, errors);
                    break;
                default:
                    // 未知表，跳过
                    os.SkipBytes(tableSize);
                    break;
            }
        }

        // 检查缺失的表
        for (const t of configNulls) {
            errors.ConfigNull(t);
        }

        // 解析外键引用
        Equip_Jewelry.Resolve(errors);
        Equip_Jewelryrandom.Resolve(errors);
        Other_Keytest.Resolve(errors);
        Other_Loot.Resolve(errors);
        Other_Monster.Resolve(errors);
        Other_Signin.Resolve(errors);
        Task_Task.Resolve(errors);
        Task_Task2.Resolve(errors);
    }
}


}


