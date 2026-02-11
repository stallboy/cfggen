// noinspection UnnecessaryLocalVariableJS,JSUnusedLocalSymbols,JSUnusedGlobalSymbols,DuplicatedCode,SpellCheckingInspection

import {Stream, LoadErrors} from "./ConfigUtil";

export namespace Config {

export class LevelRank {
    private _Level: number | undefined;
    /* 等级 */
    get Level(): number { return this._Level as number; }
    private _Rank: number | undefined;
    /* 品质 */
    get Rank(): number { return this._Rank as number; }

    private _RefRank: Equip_Rank | undefined;
    get RefRank(): Equip_Rank { return this._RefRank as Equip_Rank; }
    ToString() : string {
        return "(" + this._Level + "," + this._Rank + ")";
    }

    static _create(os: Stream) : LevelRank {
        const self = new LevelRank();
        self._Level = os.ReadInt32();
        self._Rank = os.ReadInt32();
        return self;
    }

    _resolve(errors: LoadErrors) {
        this._RefRank = Equip_Rank.Get(this._Rank);
        if (this._RefRank === undefined) {
            errors.RefNull("LevelRank", this.ToString(), "Rank");
        }
    }
}

export class Position {
    private _x: number | undefined;
    get X(): number { return this._x as number; }
    private _y: number | undefined;
    get Y(): number { return this._y as number; }
    private _z: number | undefined;
    get Z(): number { return this._z as number; }

    ToString() : string {
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
    private _Min: number | undefined;
    /* 最小 */
    get Min(): number { return this._Min as number; }
    private _Max: number | undefined;
    /* 最大 */
    get Max(): number { return this._Max as number; }

    ToString() : string {
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
        switch(os.ReadStringInPool()) {
            case "ConstValue":
                return Ai_TriggerTick_ConstValue._create(os);
            case "ByLevel":
                return Ai_TriggerTick_ByLevel._create(os);
            case "ByServerUpDay":
                return Ai_TriggerTick_ByServerUpDay._create(os);
        }
    }
}


export class Ai_TriggerTick_ConstValue extends Ai_TriggerTick {
    private _value: number | undefined;
    get Value(): number { return this._value as number; }

    ToString() : string {
        return "(" + this._value + ")";
    }

    static _create(os: Stream) : Ai_TriggerTick_ConstValue {
        const self = new Ai_TriggerTick_ConstValue();
        self._value = os.ReadInt32();
        return self;
    }

}

export class Ai_TriggerTick_ByLevel extends Ai_TriggerTick {
    private _init: number | undefined;
    get Init(): number { return this._init as number; }
    private _coefficient: number | undefined;
    get Coefficient(): number { return this._coefficient as number; }

    ToString() : string {
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
    private _init: number | undefined;
    get Init(): number { return this._init as number; }
    private _coefficient1: number | undefined;
    get Coefficient1(): number { return this._coefficient1 as number; }
    private _coefficient2: number | undefined;
    get Coefficient2(): number { return this._coefficient2 as number; }

    ToString() : string {
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
    private _name: string | undefined;
    get Name(): string { return this._name as string; }
    private _iRange: Range | undefined;
    get IRange(): Range { return this._iRange as Range; }

    ToString() : string {
        return "(" + this._name + "," + this._iRange + ")";
    }

    static _create(os: Stream) : Equip_TestPackBean {
        const self = new Equip_TestPackBean();
        self._name = os.ReadStringInPool();
        self._iRange = Range._create(os);
        return self;
    }

}

export class Other_DropItem {
    private _chance: number | undefined;
    /* 掉落概率 */
    get Chance(): number { return this._chance as number; }
    private _itemids: number[] | undefined;
    /* 掉落物品 */
    get Itemids(): number[] { return this._itemids as number[]; }
    private _countmin: number | undefined;
    /* 数量下限 */
    get Countmin(): number { return this._countmin as number; }
    private _countmax: number | undefined;
    /* 数量上限 */
    get Countmax(): number { return this._countmax as number; }

    ToString() : string {
        return "(" + this._chance + "," + this._itemids + "," + this._countmin + "," + this._countmax + ")";
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
    private _testInt: number | undefined;
    get TestInt(): number { return this._testInt as number; }
    private _testBool: boolean | undefined;
    get TestBool(): boolean { return this._testBool as boolean; }
    private _testString: string | undefined;
    get TestString(): string { return this._testString as string; }
    private _testSubBean: Position | undefined;
    get TestSubBean(): Position { return this._testSubBean as Position; }
    private _testList: number[] | undefined;
    get TestList(): number[] { return this._testList as number[]; }
    private _testList2: number[] | undefined;
    get TestList2(): number[] { return this._testList2 as number[]; }
    private _testMap: Map<number, string> | undefined;
    get TestMap(): Map<number, string> { return this._testMap as Map<number, string>; }

    ToString() : string {
        return "(" + this._testInt + "," + this._testBool + "," + this._testString + "," + this._testSubBean + "," + this._testList + "," + this._testList2 + "," + this._testMap + ")";
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
        switch(os.ReadStringInPool()) {
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
        }
    }
}


export class Task_Completecondition_KillMonster extends Task_Completecondition {
    type() : Task_Completeconditiontype {
        return Task_Completeconditiontype.KillMonster;
    }

    private _monsterid: number | undefined;
    get Monsterid(): number { return this._monsterid as number; }
    private _count: number | undefined;
    get Count(): number { return this._count as number; }

    private _RefMonsterid: Other_Monster | undefined;
    get RefMonsterid(): Other_Monster { return this._RefMonsterid as Other_Monster; }
    ToString() : string {
        return "(" + this._monsterid + "," + this._count + ")";
    }

    static _create(os: Stream) : Task_Completecondition_KillMonster {
        const self = new Task_Completecondition_KillMonster();
        self._monsterid = os.ReadInt32();
        self._count = os.ReadInt32();
        return self;
    }

    _resolve(errors: LoadErrors) {
        this._RefMonsterid = Other_Monster.Get(this._monsterid);
        if (this._RefMonsterid === undefined) {
            errors.RefNull("KillMonster", this.ToString(), "monsterid");
        }
    }
}

export class Task_Completecondition_TalkNpc extends Task_Completecondition {
    type() : Task_Completeconditiontype {
        return Task_Completeconditiontype.TalkNpc;
    }

    private _npcid: number | undefined;
    get Npcid(): number { return this._npcid as number; }

    ToString() : string {
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


    ToString() : string {
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

    private _msg: string | undefined;
    get Msg(): string { return this._msg as string; }

    ToString() : string {
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

    private _cond1: Task_Completecondition | undefined;
    get Cond1(): Task_Completecondition { return this._cond1 as Task_Completecondition; }
    private _cond2: Task_Completecondition | undefined;
    get Cond2(): Task_Completecondition { return this._cond2 as Task_Completecondition; }

    ToString() : string {
        return "(" + this._cond1 + "," + this._cond2 + ")";
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

    private _itemid: number | undefined;
    get Itemid(): number { return this._itemid as number; }
    private _count: number | undefined;
    get Count(): number { return this._count as number; }

    ToString() : string {
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
        return Task_Completeconditiontype.aa;
    }


    ToString() : string {
        return "(" +  + ")";
    }

    static _create(os: Stream) : Task_Completecondition_Aa {
        const self = new Task_Completecondition_Aa();
        return self;
    }

}


export class Ai_Ai {
    private _ID: number | undefined;
    get ID(): number { return this._ID as number; }
    private _Desc: string | undefined;
    /* 描述----这里测试下多行效果--再来一行 */
    get Desc(): string { return this._Desc as string; }
    private _CondID: string | undefined;
    /* 触发公式 */
    get CondID(): string { return this._CondID as string; }
    private _TrigTick: Ai_TriggerTick | undefined;
    /* 触发间隔(帧) */
    get TrigTick(): Ai_TriggerTick { return this._TrigTick as Ai_TriggerTick; }
    private _TrigOdds: number | undefined;
    /* 触发几率 */
    get TrigOdds(): number { return this._TrigOdds as number; }
    private _ActionID: number[] | undefined;
    /* 触发行为 */
    get ActionID(): number[] { return this._ActionID as number[]; }
    private _DeathRemove: boolean | undefined;
    /* 死亡移除 */
    get DeathRemove(): boolean { return this._DeathRemove as boolean; }

    ToString() : string {
        return "(" + this._ID + "," + this._Desc + "," + this._CondID + "," + this._TrigTick + "," + this._TrigOdds + "," + this._ActionID + "," + this._DeathRemove + ")";
    }

    
    private static all : Map<number, Ai_Ai> | undefined;

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
    private _ID: number | undefined;
    get ID(): number { return this._ID as number; }
    private _Desc: string | undefined;
    /* 描述 */
    get Desc(): string { return this._Desc as string; }
    private _FormulaID: number | undefined;
    /* 公式 */
    get FormulaID(): number { return this._FormulaID as number; }
    private _ArgIList: number[] | undefined;
    /* 参数(int)1 */
    get ArgIList(): number[] { return this._ArgIList as number[]; }
    private _ArgSList: number[] | undefined;
    /* 参数(string)1 */
    get ArgSList(): number[] { return this._ArgSList as number[]; }

    ToString() : string {
        return "(" + this._ID + "," + this._Desc + "," + this._FormulaID + "," + this._ArgIList + "," + this._ArgSList + ")";
    }

    
    private static all : Map<number, Ai_Ai_action> | undefined;

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
    private _ID: number | undefined;
    get ID(): number { return this._ID as number; }
    private _Desc: string | undefined;
    /* 描述 */
    get Desc(): string { return this._Desc as string; }
    private _FormulaID: number | undefined;
    /* 公式 */
    get FormulaID(): number { return this._FormulaID as number; }
    private _ArgIList: number[] | undefined;
    /* 参数(int)1 */
    get ArgIList(): number[] { return this._ArgIList as number[]; }
    private _ArgSList: number[] | undefined;
    /* 参数(string)1 */
    get ArgSList(): number[] { return this._ArgSList as number[]; }

    ToString() : string {
        return "(" + this._ID + "," + this._Desc + "," + this._FormulaID + "," + this._ArgIList + "," + this._ArgSList + ")";
    }

    
    private static all : Map<number, Ai_Ai_condition> | undefined;

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

    private _id: number | undefined;
    /* 属性类型 */
    get Id(): number { return this._id as number; }
    private _name: string | undefined;
    /* 程序用名字 */
    get Name(): string { return this._name as string; }

    ToString() : string {
        return "(" + this._id + "," + this._name + ")";
    }

    
    private static all : Map<number, Equip_Ability> | undefined;

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

    private _entry: string | undefined;
    /* 入口，程序填 */
    get Entry(): string { return this._entry as string; }
    private _stone_count_for_set: number | undefined;
    /* 形成套装的音石数量 */
    get Stone_count_for_set(): number { return this._stone_count_for_set as number; }
    private _draw_protect_name: string | undefined;
    /* 保底策略名称 */
    get Draw_protect_name(): string { return this._draw_protect_name as string; }
    private _broadcastid: number | undefined;
    /* 公告Id */
    get Broadcastid(): number { return this._broadcastid as number; }
    private _broadcast_least_quality: number | undefined;
    /* 公告的最低品质 */
    get Broadcast_least_quality(): number { return this._broadcast_least_quality as number; }
    private _week_reward_mailid: number | undefined;
    /* 抽卡周奖励的邮件id */
    get Week_reward_mailid(): number { return this._week_reward_mailid as number; }

    ToString() : string {
        return "(" + this._entry + "," + this._stone_count_for_set + "," + this._draw_protect_name + "," + this._broadcastid + "," + this._broadcast_least_quality + "," + this._week_reward_mailid + ")";
    }

    
    private static all : Map<string, Equip_Equipconfig> | undefined;

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
    private _ID: number | undefined;
    /* 首饰ID */
    get ID(): number { return this._ID as number; }
    private _Name: string | undefined;
    /* 首饰名称 */
    get Name(): string { return this._Name as string; }
    private _IconFile: string | undefined;
    /* 图标ID */
    get IconFile(): string { return this._IconFile as string; }
    private _LvlRank: LevelRank | undefined;
    /* 首饰等级 */
    get LvlRank(): LevelRank { return this._LvlRank as LevelRank; }
    private _JType: string | undefined;
    /* 首饰类型 */
    get JType(): string { return this._JType as string; }
    private _SuitID: number | undefined;
    /* 套装ID（为0是没有不属于套装，首饰品级为4的首饰该参数为套装id，其余情况为0,引用JewelrySuit.csv） */
    get SuitID(): number { return this._SuitID as number; }
    private _KeyAbility: number | undefined;
    /* 关键属性类型 */
    get KeyAbility(): number { return this._KeyAbility as number; }
    private _KeyAbilityValue: number | undefined;
    /* 关键属性数值 */
    get KeyAbilityValue(): number { return this._KeyAbilityValue as number; }
    private _SalePrice: number | undefined;
    /* 售卖价格 */
    get SalePrice(): number { return this._SalePrice as number; }
    private _Description: string | undefined;
    /* 描述,根据Lvl和Rank来随机3个属性，第一个属性由Lvl,Rank行随机，剩下2个由Lvl和小于Rank的行里随机。Rank最小的时候都从Lvl，Rank里随机。 */
    get Description(): string { return this._Description as string; }

    private _RefLvlRank: Equip_Jewelryrandom | undefined;
    get RefLvlRank(): Equip_Jewelryrandom { return this._RefLvlRank as Equip_Jewelryrandom; }
    private _RefJType: Equip_Jewelrytype | undefined;
    get RefJType(): Equip_Jewelrytype { return this._RefJType as Equip_Jewelrytype; }
    private _NullableRefSuitID: Equip_Jewelrysuit | undefined;
    get NullableRefSuitID(): Equip_Jewelrysuit { return this._NullableRefSuitID as Equip_Jewelrysuit; }
    private _RefKeyAbility: Equip_Ability | undefined;
    get RefKeyAbility(): Equip_Ability { return this._RefKeyAbility as Equip_Ability; }
    ToString() : string {
        return "(" + this._ID + "," + this._Name + "," + this._IconFile + "," + this._LvlRank + "," + this._JType + "," + this._SuitID + "," + this._KeyAbility + "," + this._KeyAbilityValue + "," + this._SalePrice + "," + this._Description + ")";
    }

    
    private static all : Map<number, Equip_Jewelry> | undefined;

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
        this._RefLvlRank = Equip_Jewelryrandom.Get(this._LvlRank);
        if (this._RefLvlRank === undefined) {
            errors.RefNull("equip.jewelry", this.ToString(), "LvlRank");
        }
        this._RefJType = Equip_Jewelrytype.Get(this._JType);
        if (this._RefJType === undefined) {
            errors.RefNull("equip.jewelry", this.ToString(), "JType");
        }
        this._NullableRefSuitID = Equip_Jewelrysuit.Get(this._SuitID);
        this._RefKeyAbility = Equip_Ability.Get(this._KeyAbility);
        if (this._RefKeyAbility === undefined) {
            errors.RefNull("equip.jewelry", this.ToString(), "KeyAbility");
        }
    }
}

export class Equip_Jewelryrandom {
    private _LvlRank: LevelRank | undefined;
    /* 等级 */
    get LvlRank(): LevelRank { return this._LvlRank as LevelRank; }
    private _AttackRange: Range | undefined;
    /* 最小攻击力 */
    get AttackRange(): Range { return this._AttackRange as Range; }
    private _OtherRange: Range[] | undefined;
    /* 最小防御力 */
    get OtherRange(): Range[] { return this._OtherRange as Range[]; }
    private _TestPack: Equip_TestPackBean[] | undefined;
    /* 测试pack */
    get TestPack(): Equip_TestPackBean[] { return this._TestPack as Equip_TestPackBean[]; }

    ToString() : string {
        return "(" + this._LvlRank + "," + this._AttackRange + "," + this._OtherRange + "," + this._TestPack + ")";
    }

    
    private static all : Map<number, Equip_Jewelryrandom> | undefined;

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

    private _SuitID: number | undefined;
    /* 饰品套装ID */
    get SuitID(): number { return this._SuitID as number; }
    private _Ename: string | undefined;
    get Ename(): string { return this._Ename as string; }
    private _Name: string | undefined;
    /* 策划用名字 */
    get Name(): string { return this._Name as string; }
    private _Ability1: number | undefined;
    /* 套装属性类型1（装备套装中的两件时增加的属性） */
    get Ability1(): number { return this._Ability1 as number; }
    private _Ability1Value: number | undefined;
    /* 套装属性1 */
    get Ability1Value(): number { return this._Ability1Value as number; }
    private _Ability2: number | undefined;
    /* 套装属性类型2（装备套装中的三件时增加的属性） */
    get Ability2(): number { return this._Ability2 as number; }
    private _Ability2Value: number | undefined;
    /* 套装属性2 */
    get Ability2Value(): number { return this._Ability2Value as number; }
    private _Ability3: number | undefined;
    /* 套装属性类型3（装备套装中的四件时增加的属性） */
    get Ability3(): number { return this._Ability3 as number; }
    private _Ability3Value: number | undefined;
    /* 套装属性3 */
    get Ability3Value(): number { return this._Ability3Value as number; }
    private _SuitList: number[] | undefined;
    /* 部件1 */
    get SuitList(): number[] { return this._SuitList as number[]; }

    ToString() : string {
        return "(" + this._SuitID + "," + this._Ename + "," + this._Name + "," + this._Ability1 + "," + this._Ability1Value + "," + this._Ability2 + "," + this._Ability2Value + "," + this._Ability3 + "," + this._Ability3Value + "," + this._SuitList + ")";
    }

    
    private static all : Map<number, Equip_Jewelrysuit> | undefined;

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
        self._Name = os.ReadTextInPool();
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

    private _TypeName: string | undefined;
    /* 程序用名字 */
    get TypeName(): string { return this._TypeName as string; }

    ToString() : string {
        return "(" + this._TypeName + ")";
    }

    
    private static all : Map<string, Equip_Jewelrytype> | undefined;

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

    private _RankID: number | undefined;
    /* 稀有度 */
    get RankID(): number { return this._RankID as number; }
    private _RankName: string | undefined;
    /* 程序用名字 */
    get RankName(): string { return this._RankName as string; }
    private _RankShowName: string | undefined;
    /* 显示名称 */
    get RankShowName(): string { return this._RankShowName as string; }

    ToString() : string {
        return "(" + this._RankID + "," + this._RankName + "," + this._RankShowName + ")";
    }

    
    private static all : Map<number, Equip_Rank> | undefined;

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
    private _dropid: number | undefined;
    /* 序号 */
    get Dropid(): number { return this._dropid as number; }
    private _name: string | undefined;
    /* 名字 */
    get Name(): string { return this._name as string; }
    private _items: Other_DropItem[] | undefined;
    /* 掉落概率 */
    get Items(): Other_DropItem[] { return this._items as Other_DropItem[]; }
    private _testmap: Map<number, number> | undefined;
    /* 测试map block */
    get Testmap(): Map<number, number> { return this._testmap as Map<number, number>; }

    ToString() : string {
        return "(" + this._dropid + "," + this._name + "," + this._items + "," + this._testmap + ")";
    }

    
    private static all : Map<number, Other_Drop> | undefined;

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
        self._name = os.ReadTextInPool();
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
    private _id1: number | undefined;
    get Id1(): number { return this._id1 as number; }
    private _id2: number | undefined;
    get Id2(): number { return this._id2 as number; }
    private _id3: number | undefined;
    get Id3(): number { return this._id3 as number; }
    private _ids: number[] | undefined;
    get Ids(): number[] { return this._ids as number[]; }

    private _RefIds: Other_Signin[] | undefined;
    get RefIds(): Other_Signin[] { return this._RefIds as Other_Signin[]; }
    ToString() : string {
        return "(" + this._id1 + "," + this._id2 + "," + this._id3 + "," + this._ids + ")";
    }

    
    private static all : Map<number, Other_Keytest> | undefined;

    static Get(id1: number, id2: number) : Other_Keytest | undefined {
        return this.all.get(id1 + id2 * 100000000)
    }

    
    private static id1Id3Map : Map<number, Other_Keytest> | undefined;

    static GetById1Id3(id1: number, id3: number) : Other_Keytest | undefined {
        return this.id1Id3Map.get(id1 + id3 * 100000000)
    }

    
    private static id2Map : Map<number, Other_Keytest> | undefined;

    static GetById2(id2: number) : Other_Keytest | undefined {
        return this.id2Map.get(id2)
    }

    
    private static id2Id3Map : Map<number, Other_Keytest> | undefined;

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
                errors.RefNull("other.keytest", this.ToString(), "ids");
            }
            this._RefIds.push(r);
        }
    }
}

export class Other_Loot {
    private _lootid: number | undefined;
    /* 序号 */
    get Lootid(): number { return this._lootid as number; }
    private _ename: string | undefined;
    get Ename(): string { return this._ename as string; }
    private _name: string | undefined;
    /* 名字 */
    get Name(): string { return this._name as string; }
    private _chanceList: number[] | undefined;
    /* 掉落0件物品的概率 */
    get ChanceList(): number[] { return this._chanceList as number[]; }

    private _ListRefLootid: Other_Lootitem[] | undefined;
    get ListRefLootid(): Other_Lootitem[] { return this._ListRefLootid as Other_Lootitem[]; }
    private _ListRefAnotherWay: Other_Lootitem[] | undefined;
    get ListRefAnotherWay(): Other_Lootitem[] { return this._ListRefAnotherWay as Other_Lootitem[]; }
    ToString() : string {
        return "(" + this._lootid + "," + this._ename + "," + this._name + "," + this._chanceList + ")";
    }

    
    private static all : Map<number, Other_Loot> | undefined;

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
        self._name = os.ReadTextInPool();
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
    private _lootid: number | undefined;
    /* 掉落id */
    get Lootid(): number { return this._lootid as number; }
    private _itemid: number | undefined;
    /* 掉落物品 */
    get Itemid(): number { return this._itemid as number; }
    private _chance: number | undefined;
    /* 掉落概率 */
    get Chance(): number { return this._chance as number; }
    private _countmin: number | undefined;
    /* 数量下限 */
    get Countmin(): number { return this._countmin as number; }
    private _countmax: number | undefined;
    /* 数量上限 */
    get Countmax(): number { return this._countmax as number; }

    ToString() : string {
        return "(" + this._lootid + "," + this._itemid + "," + this._chance + "," + this._countmin + "," + this._countmax + ")";
    }

    
    private static all : Map<number, Other_Lootitem> | undefined;

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
    private _id: number | undefined;
    get Id(): number { return this._id as number; }
    private _posList: Position[] | undefined;
    get PosList(): Position[] { return this._posList as Position[]; }
    private _lootId: number | undefined;
    /* loot */
    get LootId(): number { return this._lootId as number; }
    private _lootItemId: number | undefined;
    /* item */
    get LootItemId(): number { return this._lootItemId as number; }

    private _RefLoot: Other_Lootitem | undefined;
    get RefLoot(): Other_Lootitem { return this._RefLoot as Other_Lootitem; }
    private _RefAllLoot: Other_Loot | undefined;
    get RefAllLoot(): Other_Loot { return this._RefAllLoot as Other_Loot; }
    ToString() : string {
        return "(" + this._id + "," + this._posList + "," + this._lootId + "," + this._lootItemId + ")";
    }

    
    private static all : Map<number, Other_Monster> | undefined;

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
        this._RefLoot = Other_Lootitem.Get(this._lootId, this.lootItemId);
        if (this._RefLoot === undefined) {
            errors.RefNull("other.monster", this.ToString(), "Loot");
        }
        this._RefAllLoot = Other_Loot.Get(this._lootId);
        if (this._RefAllLoot === undefined) {
            errors.RefNull("other.monster", this.ToString(), "AllLoot");
        }
    }
}

export class Other_Signin {
    private _id: number | undefined;
    /* 礼包ID */
    get Id(): number { return this._id as number; }
    private _item2countMap: Map<number, number> | undefined;
    /* 普通奖励 */
    get Item2countMap(): Map<number, number> { return this._item2countMap as Map<number, number>; }
    private _vipitem2vipcountMap: Map<number, number> | undefined;
    /* vip奖励 */
    get Vipitem2vipcountMap(): Map<number, number> { return this._vipitem2vipcountMap as Map<number, number>; }
    private _viplevel: number | undefined;
    /* 领取vip奖励的最低等级 */
    get Viplevel(): number { return this._viplevel as number; }
    private _IconFile: string | undefined;
    /* 礼包图标 */
    get IconFile(): string { return this._IconFile as string; }

    private _RefVipitem2vipcountMap: Map<number, Other_Loot> | undefined;
    get RefVipitem2vipcountMap(): Map<number, Other_Loot> { return this._RefVipitem2vipcountMap as Map<number, Other_Loot>; }
    ToString() : string {
        return "(" + this._id + "," + this._item2countMap + "," + this._vipitem2vipcountMap + "," + this._viplevel + "," + this._IconFile + ")";
    }

    
    private static all : Map<number, Other_Signin> | undefined;

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
                errors.RefNull("other.signin", this.ToString(), "vipitem2vipcountMap");
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

    private _id: number | undefined;
    /* 任务完成条件类型（id的范围为1-100） */
    get Id(): number { return this._id as number; }
    private _name: string | undefined;
    /* 程序用名字 */
    get Name(): string { return this._name as string; }

    ToString() : string {
        return "(" + this._id + "," + this._name + ")";
    }

    
    private static all : Map<number, Task_Completeconditiontype> | undefined;

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
    private _taskid: number | undefined;
    /* 任务完成条件类型（id的范围为1-100） */
    get Taskid(): number { return this._taskid as number; }
    private _name: string[] | undefined;
    /* 程序用名字 */
    get Name(): string[] { return this._name as string[]; }
    private _nexttask: number | undefined;
    get Nexttask(): number { return this._nexttask as number; }
    private _completecondition: Task_Completecondition | undefined;
    get Completecondition(): Task_Completecondition { return this._completecondition as Task_Completecondition; }
    private _exp: number | undefined;
    get Exp(): number { return this._exp as number; }
    private _testDefaultBean: Task_TestDefaultBean | undefined;
    /* 测试 */
    get TestDefaultBean(): Task_TestDefaultBean { return this._testDefaultBean as Task_TestDefaultBean; }

    private _NullableRefTaskid: Task_Taskextraexp | undefined;
    get NullableRefTaskid(): Task_Taskextraexp { return this._NullableRefTaskid as Task_Taskextraexp; }
    private _NullableRefNexttask: Task_Task | undefined;
    get NullableRefNexttask(): Task_Task { return this._NullableRefNexttask as Task_Task; }
    ToString() : string {
        return "(" + this._taskid + "," + this._name + "," + this._nexttask + "," + this._completecondition + "," + this._exp + "," + this._testDefaultBean + ")";
    }

    
    private static all : Map<number, Task_Task> | undefined;

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
            self._name.push(os.ReadTextInPool());
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
    private _taskid: number | undefined;
    /* 任务完成条件类型（id的范围为1-100） */
    get Taskid(): number { return this._taskid as number; }
    private _name: string[] | undefined;
    get Name(): string[] { return this._name as string[]; }
    private _nexttask: number | undefined;
    get Nexttask(): number { return this._nexttask as number; }
    private _completecondition: Task_Completecondition | undefined;
    get Completecondition(): Task_Completecondition { return this._completecondition as Task_Completecondition; }
    private _exp: number | undefined;
    get Exp(): number { return this._exp as number; }
    private _testBool: boolean | undefined;
    get TestBool(): boolean { return this._testBool as boolean; }
    private _testString: string | undefined;
    get TestString(): string { return this._testString as string; }
    private _testStruct: Position | undefined;
    get TestStruct(): Position { return this._testStruct as Position; }
    private _testList: number[] | undefined;
    get TestList(): number[] { return this._testList as number[]; }
    private _testListStruct: Position[] | undefined;
    get TestListStruct(): Position[] { return this._testListStruct as Position[]; }
    private _testListInterface: Ai_TriggerTick[] | undefined;
    get TestListInterface(): Ai_TriggerTick[] { return this._testListInterface as Ai_TriggerTick[]; }

    private _NullableRefTaskid: Task_Taskextraexp | undefined;
    get NullableRefTaskid(): Task_Taskextraexp { return this._NullableRefTaskid as Task_Taskextraexp; }
    private _NullableRefNexttask: Task_Task | undefined;
    get NullableRefNexttask(): Task_Task { return this._NullableRefNexttask as Task_Task; }
    ToString() : string {
        return "(" + this._taskid + "," + this._name + "," + this._nexttask + "," + this._completecondition + "," + this._exp + "," + this._testBool + "," + this._testString + "," + this._testStruct + "," + this._testList + "," + this._testListStruct + "," + this._testListInterface + ")";
    }

    
    private static all : Map<number, Task_Task2> | undefined;

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
            self._name.push(os.ReadTextInPool());
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
    private _taskid: number | undefined;
    /* 任务完成条件类型（id的范围为1-100） */
    get Taskid(): number { return this._taskid as number; }
    private _extraexp: number | undefined;
    /* 额外奖励经验 */
    get Extraexp(): number { return this._extraexp as number; }
    private _test1: string | undefined;
    get Test1(): string { return this._test1 as string; }
    private _test2: string | undefined;
    get Test2(): string { return this._test2 as string; }
    private _fielda: string | undefined;
    get Fielda(): string { return this._fielda as string; }
    private _fieldb: string | undefined;
    get Fieldb(): string { return this._fieldb as string; }
    private _fieldc: string | undefined;
    get Fieldc(): string { return this._fieldc as string; }
    private _fieldd: string | undefined;
    get Fieldd(): string { return this._fieldd as string; }

    ToString() : string {
        return "(" + this._taskid + "," + this._extraexp + "," + this._test1 + "," + this._test2 + "," + this._fielda + "," + this._fieldb + "," + this._fieldc + "," + this._fieldd + ")";
    }

    
    private static all : Map<number, Task_Taskextraexp> | undefined;

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


