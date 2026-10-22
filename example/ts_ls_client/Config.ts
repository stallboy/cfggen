// noinspection UnnecessaryLocalVariableJS,JSUnusedLocalSymbols,JSUnusedGlobalSymbols,DuplicatedCode,SpellCheckingInspection

import {Stream, LoadErrors, ToStringList, ToStringMap, TextPoolManager, ConfigPackage, Loader} from "./ConfigUtil";
export namespace Config {

export class LevelRank {
    private _Level!: number;
    /* 等级 */
    get Level(): number { return this._Level; }
    private _Rank!: number;
    /* 品质 */
    get Rank(): number { return this._Rank; }
    $relationFields$(): readonly unknown[] {
        return [this._Level, this._Rank];
    }
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
    $relationFields$(): readonly unknown[] {
        return [this._x, this._y, this._z];
    }
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
    $relationFields$(): readonly unknown[] {
        return [this._Min, this._Max];
    }
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
        }
        throw new Error("Unknown type: " + typeName);
    }

    abstract toString() : string;
}


export class Ai_TriggerTick_ConstValue extends Ai_TriggerTick {
    private _value!: number;
    get Value(): number { return this._value; }
    $relationFields$(): readonly unknown[] {
        return [this._value];
    }
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
    $relationFields$(): readonly unknown[] {
        return [this._init, this._coefficient];
    }
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
    $relationFields$(): readonly unknown[] {
        return [this._init, this._coefficient1, this._coefficient2];
    }
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

export class Combat_AbilityUse {
    private _binding!: Combat_Binding;
    get Binding(): Combat_Binding { return this._binding; }
    private _machines!: Combat_MachineUse[];
    get Machines(): Combat_MachineUse[] { return this._machines; }
    $relationFields$(): readonly unknown[] {
        return [this._binding, this._machines];
    }
    private _RefAbility!: Combat_Ability;
    get RefAbility(): Combat_Ability { return this._RefAbility; }
    _bindRefAbility$ (value: Combat_Ability | undefined): void { this._RefAbility = value!; }
    toString() : string {
        return "(" + this.Binding.toString() + "," + ToStringList(this._machines) + ")";
    }

    static _create(os: Stream) : Combat_AbilityUse {
        const self = new Combat_AbilityUse();
        self._binding = Combat_Binding._create(os);
        self._machines = [];
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._machines.push(Combat_MachineUse._create(os));
        }
        return self;
    }

    _resolve(errors: LoadErrors) {
        for (const e of this._machines) {
            e._resolve(errors);
        }
    }
}

export class Combat_Anchor {
    $relationFields$(): readonly unknown[] {
        return [];
    }
    toString() : string {
        return "(" +  + ")";
    }

    static _create(os: Stream) : Combat_Anchor {
        const self = new Combat_Anchor();
        return self;
    }

}

export class Combat_AnchorUse {
    $relationFields$(): readonly unknown[] {
        return [];
    }
    private _RefAnchor!: Combat_Anchor;
    get RefAnchor(): Combat_Anchor { return this._RefAnchor; }
    _bindRefAnchor$ (value: Combat_Anchor | undefined): void { this._RefAnchor = value!; }
    toString() : string {
        return "(" +  + ")";
    }

    static _create(os: Stream) : Combat_AnchorUse {
        const self = new Combat_AnchorUse();
        return self;
    }

    _resolve(errors: LoadErrors) {
    }
}

export class Combat_Binding {
    private _abilityId!: number;
    get AbilityId(): number { return this._abilityId; }
    $relationFields$(): readonly unknown[] {
        return [this._abilityId];
    }
    toString() : string {
        return "(" + this._abilityId + ")";
    }

    static _create(os: Stream) : Combat_Binding {
        const self = new Combat_Binding();
        self._abilityId = os.ReadInt32();
        return self;
    }

}

export class Combat_BookEntry {
    private _name!: string;
    get Name(): string { return this._name; }
    $relationFields$(): readonly unknown[] {
        return [this._name];
    }
    toString() : string {
        return "(" + this._name + ")";
    }

    static _create(os: Stream) : Combat_BookEntry {
        const self = new Combat_BookEntry();
        self._name = os.ReadStringInPool();
        return self;
    }

}

export class Combat_Combo {
    private _weapon!: string;
    get Weapon(): string { return this._weapon; }
    private _tier!: number;
    get Tier(): number { return this._tier; }
    private _airborne!: boolean;
    get Airborne(): boolean { return this._airborne; }
    private _effect!: Combat_Effect;
    get Effect(): Combat_Effect { return this._effect; }
    $relationFields$(): readonly unknown[] {
        return [this._weapon, this._tier, this._airborne, this._effect];
    }
    toString() : string {
        return "(" + this._weapon + "," + this._tier + "," + this._airborne + "," + this.Effect.toString() + ")";
    }

    static _create(os: Stream) : Combat_Combo {
        const self = new Combat_Combo();
        self._weapon = os.ReadStringInPool();
        self._tier = os.ReadInt32();
        self._airborne = os.ReadBool();
        self._effect = Combat_Effect._create(os);
        return self;
    }

}


export abstract class Combat_Command {
    _resolve(errors: LoadErrors) {
    }
    static _create(os: Stream) : Combat_Command {
        const typeName = os.ReadStringInPool();
        switch(typeName) {
            case "StateMachine":
                return Combat_Command_StateMachine._create(os);
            case "Jump":
                return Combat_Command_Jump._create(os);
            case "Noop":
                return Combat_Command_Noop._create(os);
        }
        throw new Error("Unknown type: " + typeName);
    }

    abstract toString() : string;
}


export class Combat_Command_StateMachine extends Combat_Command {
    private _phases!: Combat_NestedPhase[];
    get Phases(): Combat_NestedPhase[] { return this._phases; }
    $relationFields$(): readonly unknown[] {
        return [this._phases];
    }
    toString() : string {
        return "(" + ToStringList(this._phases) + ")";
    }

    static _create(os: Stream) : Combat_Command_StateMachine {
        const self = new Combat_Command_StateMachine();
        self._phases = [];
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._phases.push(Combat_NestedPhase._create(os));
        }
        return self;
    }

    _resolve(errors: LoadErrors) {
        for (const e of this._phases) {
            e._resolve(errors);
        }
    }
}

export class Combat_Command_Jump extends Combat_Command {
    private _key!: string;
    get Key(): string { return this._key; }
    $relationFields$(): readonly unknown[] {
        return [this._key];
    }
    private _NullableRefKey: Combat_NestedPhase | undefined;
    get NullableRefKey(): Combat_NestedPhase | undefined { return this._NullableRefKey; }
    _bindNullableRefKey$ (value: Combat_NestedPhase | undefined): void { this._NullableRefKey = value!; }
    toString() : string {
        return "(" + this._key + ")";
    }

    static _create(os: Stream) : Combat_Command_Jump {
        const self = new Combat_Command_Jump();
        self._key = os.ReadStringInPool();
        return self;
    }

    _resolve(errors: LoadErrors) {
    }
}

export class Combat_Command_Noop extends Combat_Command {
    $relationFields$(): readonly unknown[] {
        return [];
    }
    toString() : string {
        return "(" +  + ")";
    }

    static _create(os: Stream) : Combat_Command_Noop {
        const self = new Combat_Command_Noop();
        return self;
    }

}


export abstract class Combat_Effect {
    static _create(os: Stream) : Combat_Effect {
        const typeName = os.ReadStringInPool();
        switch(typeName) {
            case "Damage":
                return Combat_Effect_Damage._create(os);
            case "Heal":
                return Combat_Effect_Heal._create(os);
            case "None":
                return Combat_Effect_None._create(os);
        }
        throw new Error("Unknown type: " + typeName);
    }

    abstract toString() : string;
}


export class Combat_Effect_Damage extends Combat_Effect {
    private _amount!: number;
    get Amount(): number { return this._amount; }
    $relationFields$(): readonly unknown[] {
        return [this._amount];
    }
    toString() : string {
        return "(" + this._amount + ")";
    }

    static _create(os: Stream) : Combat_Effect_Damage {
        const self = new Combat_Effect_Damage();
        self._amount = os.ReadInt32();
        return self;
    }

}

export class Combat_Effect_Heal extends Combat_Effect {
    private _amount!: number;
    get Amount(): number { return this._amount; }
    $relationFields$(): readonly unknown[] {
        return [this._amount];
    }
    toString() : string {
        return "(" + this._amount + ")";
    }

    static _create(os: Stream) : Combat_Effect_Heal {
        const self = new Combat_Effect_Heal();
        self._amount = os.ReadInt32();
        return self;
    }

}

export class Combat_Effect_None extends Combat_Effect {
    $relationFields$(): readonly unknown[] {
        return [];
    }
    toString() : string {
        return "(" +  + ")";
    }

    static _create(os: Stream) : Combat_Effect_None {
        const self = new Combat_Effect_None();
        return self;
    }

}

export class Combat_Exit {
    private _effect!: Combat_Effect;
    get Effect(): Combat_Effect { return this._effect; }
    $relationFields$(): readonly unknown[] {
        return [this._effect];
    }
    toString() : string {
        return "(" + this.Effect.toString() + ")";
    }

    static _create(os: Stream) : Combat_Exit {
        const self = new Combat_Exit();
        self._effect = Combat_Effect._create(os);
        return self;
    }

}

export class Combat_Frame {
    private _tick!: bigint;
    get Tick(): bigint { return this._tick; }
    private _effect!: Combat_Effect;
    get Effect(): Combat_Effect { return this._effect; }
    $relationFields$(): readonly unknown[] {
        return [this._tick, this._effect];
    }
    toString() : string {
        return "(" + this._tick + "," + this.Effect.toString() + ")";
    }

    static _create(os: Stream) : Combat_Frame {
        const self = new Combat_Frame();
        self._tick = os.ReadInt64BigInt();
        self._effect = Combat_Effect._create(os);
        return self;
    }

}

export class Combat_Impact {
    private _ordinal!: number;
    get Ordinal(): number { return this._ordinal; }
    private _damage!: number;
    get Damage(): number { return this._damage; }
    $relationFields$(): readonly unknown[] {
        return [this._ordinal, this._damage];
    }
    toString() : string {
        return "(" + this._ordinal + "," + this._damage + ")";
    }

    static _create(os: Stream) : Combat_Impact {
        const self = new Combat_Impact();
        self._ordinal = os.ReadInt32();
        self._damage = os.ReadInt32();
        return self;
    }

}

export class Combat_Machine {
    private _code!: string;
    get Code(): string { return this._code; }
    private _phases!: Combat_Phase[];
    get Phases(): Combat_Phase[] { return this._phases; }
    private _impacts!: Combat_Impact[];
    get Impacts(): Combat_Impact[] { return this._impacts; }
    private _frames!: Combat_Frame[];
    get Frames(): Combat_Frame[] { return this._frames; }
    private _stances!: Combat_Stance[];
    get Stances(): Combat_Stance[] { return this._stances; }
    private _variants!: Combat_PhaseVariant[];
    get Variants(): Combat_PhaseVariant[] { return this._variants; }
    private _combos!: Combat_Combo[];
    get Combos(): Combat_Combo[] { return this._combos; }
    private _markers!: Combat_Marker[];
    /* 未被引用也必须保持唯一。 */
    get Markers(): Combat_Marker[] { return this._markers; }
    private _anchor!: Combat_Anchor;
    get Anchor(): Combat_Anchor { return this._anchor; }
    private _anchors!: Combat_AnchorUse[];
    get Anchors(): Combat_AnchorUse[] { return this._anchors; }
    private _onExit!: Combat_Exit;
    get OnExit(): Combat_Exit { return this._onExit; }
    private _initial!: string;
    get Initial(): string { return this._initial; }
    private _impact!: number;
    get Impact(): number { return this._impact; }
    private _frame!: bigint;
    get Frame(): bigint { return this._frame; }
    private _stance!: boolean;
    get Stance(): boolean { return this._stance; }
    private _variant!: Combat_PhaseKey;
    get Variant(): Combat_PhaseKey { return this._variant; }
    private _weapon!: string;
    get Weapon(): string { return this._weapon; }
    private _tier!: number;
    get Tier(): number { return this._tier; }
    private _airborne!: boolean;
    get Airborne(): boolean { return this._airborne; }
    $relationFields$(): readonly unknown[] {
        return [this._code, this._phases, this._impacts, this._frames, this._stances, this._variants, this._combos, this._markers, this._anchor, this._anchors, this._onExit, this._initial, this._impact, this._frame, this._stance, this._variant, this._weapon, this._tier, this._airborne];
    }
    private _RefInitial!: Combat_Phase;
    get RefInitial(): Combat_Phase { return this._RefInitial; }
    _bindRefInitial$ (value: Combat_Phase | undefined): void { this._RefInitial = value!; }
    private _RefImpact!: Combat_Impact;
    get RefImpact(): Combat_Impact { return this._RefImpact; }
    _bindRefImpact$ (value: Combat_Impact | undefined): void { this._RefImpact = value!; }
    private _RefFrame!: Combat_Frame;
    get RefFrame(): Combat_Frame { return this._RefFrame; }
    _bindRefFrame$ (value: Combat_Frame | undefined): void { this._RefFrame = value!; }
    private _RefStance!: Combat_Stance;
    get RefStance(): Combat_Stance { return this._RefStance; }
    _bindRefStance$ (value: Combat_Stance | undefined): void { this._RefStance = value!; }
    private _RefVariant!: Combat_PhaseVariant;
    get RefVariant(): Combat_PhaseVariant { return this._RefVariant; }
    _bindRefVariant$ (value: Combat_PhaseVariant | undefined): void { this._RefVariant = value!; }
    private _RefCombo!: Combat_Combo;
    get RefCombo(): Combat_Combo { return this._RefCombo; }
    _bindRefCombo$ (value: Combat_Combo | undefined): void { this._RefCombo = value!; }
    toString() : string {
        return "(" + this._code + "," + ToStringList(this._phases) + "," + ToStringList(this._impacts) + "," + ToStringList(this._frames) + "," + ToStringList(this._stances) + "," + ToStringList(this._variants) + "," + ToStringList(this._combos) + "," + ToStringList(this._markers) + "," + this.Anchor.toString() + "," + ToStringList(this._anchors) + "," + this.OnExit.toString() + "," + this._initial + "," + this._impact + "," + this._frame + "," + this._stance + "," + this.Variant.toString() + "," + this._weapon + "," + this._tier + "," + this._airborne + ")";
    }

    static _create(os: Stream) : Combat_Machine {
        const self = new Combat_Machine();
        self._code = os.ReadStringInPool();
        self._phases = [];
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._phases.push(Combat_Phase._create(os));
        }
        self._impacts = [];
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._impacts.push(Combat_Impact._create(os));
        }
        self._frames = [];
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._frames.push(Combat_Frame._create(os));
        }
        self._stances = [];
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._stances.push(Combat_Stance._create(os));
        }
        self._variants = [];
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._variants.push(Combat_PhaseVariant._create(os));
        }
        self._combos = [];
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._combos.push(Combat_Combo._create(os));
        }
        self._markers = [];
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._markers.push(Combat_Marker._create(os));
        }
        self._anchor = Combat_Anchor._create(os);
        self._anchors = [];
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._anchors.push(Combat_AnchorUse._create(os));
        }
        self._onExit = Combat_Exit._create(os);
        self._initial = os.ReadStringInPool();
        self._impact = os.ReadInt32();
        self._frame = os.ReadInt64BigInt();
        self._stance = os.ReadBool();
        self._variant = Combat_PhaseKey._create(os);
        self._weapon = os.ReadStringInPool();
        self._tier = os.ReadInt32();
        self._airborne = os.ReadBool();
        return self;
    }

    _resolve(errors: LoadErrors) {
        for (const e of this._phases) {
            e._resolve(errors);
        }
        for (const e of this._anchors) {
            e._resolve(errors);
        }
    }
}

export class Combat_MachineUse {
    private _machineKey!: string;
    get MachineKey(): string { return this._machineKey; }
    private _targets!: Combat_PhaseUse[];
    get Targets(): Combat_PhaseUse[] { return this._targets; }
    $relationFields$(): readonly unknown[] {
        return [this._machineKey, this._targets];
    }
    toString() : string {
        return "(" + this._machineKey + "," + ToStringList(this._targets) + ")";
    }

    static _create(os: Stream) : Combat_MachineUse {
        const self = new Combat_MachineUse();
        self._machineKey = os.ReadStringInPool();
        self._targets = [];
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._targets.push(Combat_PhaseUse._create(os));
        }
        return self;
    }

    _resolve(errors: LoadErrors) {
        for (const e of this._targets) {
            e._resolve(errors);
        }
    }
}

export class Combat_Marker {
    private _key!: string;
    get Key(): string { return this._key; }
    $relationFields$(): readonly unknown[] {
        return [this._key];
    }
    toString() : string {
        return "(" + this._key + ")";
    }

    static _create(os: Stream) : Combat_Marker {
        const self = new Combat_Marker();
        self._key = os.ReadStringInPool();
        return self;
    }

}

export class Combat_NestedPhase {
    private _key!: string;
    get Key(): string { return this._key; }
    private _action!: Combat_Command;
    get Action(): Combat_Command { return this._action; }
    $relationFields$(): readonly unknown[] {
        return [this._key, this._action];
    }
    toString() : string {
        return "(" + this._key + "," + this.Action.toString() + ")";
    }

    static _create(os: Stream) : Combat_NestedPhase {
        const self = new Combat_NestedPhase();
        self._key = os.ReadStringInPool();
        self._action = Combat_Command._create(os);
        return self;
    }

    _resolve(errors: LoadErrors) {
        this._action._resolve(errors);
    }
}

export class Combat_OptionalAim {
    private _abilityId!: number;
    get AbilityId(): number { return this._abilityId; }
    private _machineKey!: string;
    get MachineKey(): string { return this._machineKey; }
    private _phaseKey!: string;
    get PhaseKey(): string { return this._phaseKey; }
    $relationFields$(): readonly unknown[] {
        return [this._abilityId, this._machineKey, this._phaseKey];
    }
    private _NullableRefTarget: Combat_Phase | undefined;
    get NullableRefTarget(): Combat_Phase | undefined { return this._NullableRefTarget; }
    _bindNullableRefTarget$ (value: Combat_Phase | undefined): void { this._NullableRefTarget = value!; }
    toString() : string {
        return "(" + this._abilityId + "," + this._machineKey + "," + this._phaseKey + ")";
    }

    static _create(os: Stream) : Combat_OptionalAim {
        const self = new Combat_OptionalAim();
        self._abilityId = os.ReadInt32();
        self._machineKey = os.ReadStringInPool();
        self._phaseKey = os.ReadStringInPool();
        return self;
    }

    _resolve(errors: LoadErrors) {
    }
}

export class Combat_Origin {
    private _x!: number;
    get X(): number { return this._x; }
    private _y!: number;
    get Y(): number { return this._y; }
    $relationFields$(): readonly unknown[] {
        return [this._x, this._y];
    }
    toString() : string {
        return "(" + this._x + "," + this._y + ")";
    }

    static _create(os: Stream) : Combat_Origin {
        const self = new Combat_Origin();
        self._x = os.ReadInt32();
        self._y = os.ReadInt32();
        return self;
    }

}

export class Combat_Phase {
    private _key!: string;
    get Key(): string { return this._key; }
    private _next!: string;
    get Next(): string { return this._next; }
    $relationFields$(): readonly unknown[] {
        return [this._key, this._next];
    }
    private _NullableRefNext: Combat_Phase | undefined;
    get NullableRefNext(): Combat_Phase | undefined { return this._NullableRefNext; }
    _bindNullableRefNext$ (value: Combat_Phase | undefined): void { this._NullableRefNext = value!; }
    private _RefExit!: Combat_Effect;
    get RefExit(): Combat_Effect { return this._RefExit; }
    _bindRefExit$ (value: Combat_Effect | undefined): void { this._RefExit = value!; }
    toString() : string {
        return "(" + this._key + "," + this._next + ")";
    }

    static _create(os: Stream) : Combat_Phase {
        const self = new Combat_Phase();
        self._key = os.ReadStringInPool();
        self._next = os.ReadStringInPool();
        return self;
    }

    _resolve(errors: LoadErrors) {
    }
}

export class Combat_PhaseKey {
    private _name!: string;
    get Name(): string { return this._name; }
    private _variant!: number;
    get Variant(): number { return this._variant; }
    $relationFields$(): readonly unknown[] {
        return [this._name, this._variant];
    }
    toString() : string {
        return "(" + this._name + "," + this._variant + ")";
    }

    static _create(os: Stream) : Combat_PhaseKey {
        const self = new Combat_PhaseKey();
        self._name = os.ReadStringInPool();
        self._variant = os.ReadInt32();
        return self;
    }

}

export class Combat_PhaseUse {
    private _phaseKey!: string;
    get PhaseKey(): string { return this._phaseKey; }
    $relationFields$(): readonly unknown[] {
        return [this._phaseKey];
    }
    private _RefTarget!: Combat_Phase;
    get RefTarget(): Combat_Phase { return this._RefTarget; }
    _bindRefTarget$ (value: Combat_Phase | undefined): void { this._RefTarget = value!; }
    private _RefByCode!: Combat_Phase;
    get RefByCode(): Combat_Phase { return this._RefByCode; }
    _bindRefByCode$ (value: Combat_Phase | undefined): void { this._RefByCode = value!; }
    private _RefOrigin!: Combat_Origin;
    get RefOrigin(): Combat_Origin { return this._RefOrigin; }
    _bindRefOrigin$ (value: Combat_Origin | undefined): void { this._RefOrigin = value!; }
    toString() : string {
        return "(" + this._phaseKey + ")";
    }

    static _create(os: Stream) : Combat_PhaseUse {
        const self = new Combat_PhaseUse();
        self._phaseKey = os.ReadStringInPool();
        return self;
    }

    _resolve(errors: LoadErrors) {
    }
}

export class Combat_PhaseVariant {
    private _key!: Combat_PhaseKey;
    get Key(): Combat_PhaseKey { return this._key; }
    private _effect!: Combat_Effect;
    get Effect(): Combat_Effect { return this._effect; }
    $relationFields$(): readonly unknown[] {
        return [this._key, this._effect];
    }
    toString() : string {
        return "(" + this.Key.toString() + "," + this.Effect.toString() + ")";
    }

    static _create(os: Stream) : Combat_PhaseVariant {
        const self = new Combat_PhaseVariant();
        self._key = Combat_PhaseKey._create(os);
        self._effect = Combat_Effect._create(os);
        return self;
    }

}

export class Combat_Reward {
    private _count!: number;
    get Count(): number { return this._count; }
    $relationFields$(): readonly unknown[] {
        return [this._count];
    }
    toString() : string {
        return "(" + this._count + ")";
    }

    static _create(os: Stream) : Combat_Reward {
        const self = new Combat_Reward();
        self._count = os.ReadInt32();
        return self;
    }

}

export class Combat_Stance {
    private _airborne!: boolean;
    get Airborne(): boolean { return this._airborne; }
    private _effect!: Combat_Effect;
    get Effect(): Combat_Effect { return this._effect; }
    $relationFields$(): readonly unknown[] {
        return [this._airborne, this._effect];
    }
    toString() : string {
        return "(" + this._airborne + "," + this.Effect.toString() + ")";
    }

    static _create(os: Stream) : Combat_Stance {
        const self = new Combat_Stance();
        self._airborne = os.ReadBool();
        self._effect = Combat_Effect._create(os);
        return self;
    }

}

export class Equip_TestPackBean {
    private _name!: string;
    get Name(): string { return this._name; }
    private _iRange!: Range;
    get IRange(): Range { return this._iRange; }
    $relationFields$(): readonly unknown[] {
        return [this._name, this._iRange];
    }
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
    $relationFields$(): readonly unknown[] {
        return [this._chance, this._itemids, this._countmin, this._countmax];
    }
    toString() : string {
        return "(" + this._chance + "," + ToStringList(this._itemids) + "," + this._countmin + "," + this._countmax + ")";
    }

    static _create(os: Stream) : Other_DropItem {
        const self = new Other_DropItem();
        self._chance = os.ReadInt32();
        self._itemids = [];
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._itemids.push(os.ReadInt32());
        }
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
    $relationFields$(): readonly unknown[] {
        return [this._testInt, this._testBool, this._testString, this._testSubBean, this._testList, this._testList2, this._testMap];
    }
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
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._testList.push(os.ReadInt32());
        }
        self._testList2 = [];
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._testList2.push(os.ReadInt32());
        }
        self._testMap  = new Map<number, string>();
        for (let c = os.ReadCount(0); c > 0; c--) {
            const key$ = os.ReadInt32();
            if (self._testMap.has(key$)) throw Error("Duplicate map key: task.TestDefaultBean.testMap");
            self._testMap.set(key$, os.ReadStringInPool());
        }
        return self;
    }

}


export abstract class Task_Completecondition {
    abstract type() : Task_Complete_condition_type;
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
        }
        throw new Error("Unknown type: " + typeName);
    }

    abstract toString() : string;
}


export class Task_Completecondition_KillMonster extends Task_Completecondition {
    type() : Task_Complete_condition_type {
        return Task_Complete_condition_type.KillMonster;
    }

    private _monsterid!: number;
    get Monsterid(): number { return this._monsterid; }
    private _count!: number;
    get Count(): number { return this._count; }
    $relationFields$(): readonly unknown[] {
        return [this._monsterid, this._count];
    }
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
    type() : Task_Complete_condition_type {
        return Task_Complete_condition_type.TalkNpc;
    }

    private _npcid!: number;
    get Npcid(): number { return this._npcid; }
    $relationFields$(): readonly unknown[] {
        return [this._npcid];
    }
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
    type() : Task_Complete_condition_type {
        return Task_Complete_condition_type.TestNoColumn;
    }

    $relationFields$(): readonly unknown[] {
        return [];
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
    type() : Task_Complete_condition_type {
        return Task_Complete_condition_type.Chat;
    }

    private _msg!: string;
    get Msg(): string { return this._msg; }
    $relationFields$(): readonly unknown[] {
        return [this._msg];
    }
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
    type() : Task_Complete_condition_type {
        return Task_Complete_condition_type.ConditionAnd;
    }

    private _cond1!: Task_Completecondition;
    get Cond1(): Task_Completecondition { return this._cond1; }
    private _cond2!: Task_Completecondition;
    get Cond2(): Task_Completecondition { return this._cond2; }
    $relationFields$(): readonly unknown[] {
        return [this._cond1, this._cond2];
    }
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
    type() : Task_Complete_condition_type {
        return Task_Complete_condition_type.CollectItem;
    }

    private _itemid!: number;
    get Itemid(): number { return this._itemid; }
    private _count!: number;
    get Count(): number { return this._count; }
    $relationFields$(): readonly unknown[] {
        return [this._itemid, this._count];
    }
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
    type() : Task_Complete_condition_type {
        return Task_Complete_condition_type.Aa;
    }

    $relationFields$(): readonly unknown[] {
        return [];
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
    $relationFields$(): readonly unknown[] {
        return [this._ID, this._Desc, this._CondID, this._TrigTick, this._TrigOdds, this._ActionID, this._DeathRemove];
    }
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
        for (let c = os.ReadCount(1); c > 0; c--) {
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
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._ActionID.push(os.ReadInt32());
        }
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
    $relationFields$(): readonly unknown[] {
        return [this._ID, this._Desc, this._FormulaID, this._ArgIList, this._ArgSList];
    }
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
        for (let c = os.ReadCount(1); c > 0; c--) {
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
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._ArgIList.push(os.ReadInt32());
        }
        self._ArgSList = [];
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._ArgSList.push(os.ReadInt32());
        }
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
    $relationFields$(): readonly unknown[] {
        return [this._ID, this._Desc, this._FormulaID, this._ArgIList, this._ArgSList];
    }
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
        for (let c = os.ReadCount(1); c > 0; c--) {
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
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._ArgIList.push(os.ReadInt32());
        }
        self._ArgSList = [];
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._ArgSList.push(os.ReadInt32());
        }
        return self;
    }

}

export class Combat_Ability {
    private _id!: number;
    get Id(): number { return this._id; }
    private _code!: string;
    get Code(): string { return this._code; }
    private _origin!: Combat_Origin;
    get Origin(): Combat_Origin { return this._origin; }
    private _machines!: Combat_Machine[];
    get Machines(): Combat_Machine[] { return this._machines; }
    private _startMachine!: string;
    get StartMachine(): string { return this._startMachine; }
    private _startPhase!: string;
    get StartPhase(): string { return this._startPhase; }
    $relationFields$(): readonly unknown[] {
        return [this._id, this._code, this._origin, this._machines, this._startMachine, this._startPhase];
    }
    private _RefInitial!: Combat_Phase;
    get RefInitial(): Combat_Phase { return this._RefInitial; }
    _bindRefInitial$ (value: Combat_Phase | undefined): void { this._RefInitial = value!; }
    private _RefSpawn!: Combat_Origin;
    get RefSpawn(): Combat_Origin { return this._RefSpawn; }
    _bindRefSpawn$ (value: Combat_Origin | undefined): void { this._RefSpawn = value!; }
    toString() : string {
        return "(" + this._id + "," + this._code + "," + this.Origin.toString() + "," + ToStringList(this._machines) + "," + this._startMachine + "," + this._startPhase + ")";
    }

    
    private static all: Map<number, Combat_Ability>;

    static Get(id: number) : Combat_Ability | undefined {
        return this.all.get(id)
    }

    
    private static codeMap: Map<string, Combat_Ability>;

    static GetByCode(code: string) : Combat_Ability | undefined {
        return this.codeMap.get(code)
    }

    static All() : Map<number, Combat_Ability> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<number, Combat_Ability>();
        this.codeMap = new Map<string, Combat_Ability>();
        for (let c = os.ReadCount(1); c > 0; c--) {
            let self = this._create(os);
            this.all.set(self._id, self);
            this.codeMap.set(self._code, self);
        }

    }

    static Resolve(errors: LoadErrors) {
        for (const v of this.all.values()) {
            v._resolve(errors);
        }
    }
    static _create(os: Stream) : Combat_Ability {
        const self = new Combat_Ability();
        self._id = os.ReadInt32();
        self._code = os.ReadStringInPool();
        self._origin = Combat_Origin._create(os);
        self._machines = [];
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._machines.push(Combat_Machine._create(os));
        }
        self._startMachine = os.ReadStringInPool();
        self._startPhase = os.ReadStringInPool();
        return self;
    }

    _resolve(errors: LoadErrors) {
        for (const e of this._machines) {
            e._resolve(errors);
        }
    }
}

export class Combat_Branchreward {
    private _key!: Combat_PhaseKey;
    get Key(): Combat_PhaseKey { return this._key; }
    private _reward!: Combat_Reward;
    get Reward(): Combat_Reward { return this._reward; }
    $relationFields$(): readonly unknown[] {
        return [this._key, this._reward];
    }
    toString() : string {
        return "(" + this.Key.toString() + "," + this.Reward.toString() + ")";
    }

    
    private static all: Map<string, Combat_Branchreward>;

    static Get(key: Combat_PhaseKey) : Combat_Branchreward | undefined {
        return this.all.get($relationKey$([key]))
    }

    static All() : Map<string, Combat_Branchreward> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<string, Combat_Branchreward>();
        for (let c = os.ReadCount(1); c > 0; c--) {
            let self = this._create(os);
            this.all.set($relationKey$([self._key]), self);
        }

    }

    static _create(os: Stream) : Combat_Branchreward {
        const self = new Combat_Branchreward();
        self._key = Combat_PhaseKey._create(os);
        self._reward = Combat_Reward._create(os);
        return self;
    }

}

export class Combat_Cast {
    private _id!: number;
    get Id(): number { return this._id; }
    private _abilityCode!: string;
    get AbilityCode(): string { return this._abilityCode; }
    private _origin!: Combat_Origin;
    get Origin(): Combat_Origin { return this._origin; }
    private _uses!: Combat_AbilityUse[];
    get Uses(): Combat_AbilityUse[] { return this._uses; }
    private _optional!: Combat_OptionalAim[];
    get Optional(): Combat_OptionalAim[] { return this._optional; }
    private _school!: string;
    get School(): string { return this._school; }
    private _tier!: number;
    get Tier(): number { return this._tier; }
    private _airborne!: boolean;
    get Airborne(): boolean { return this._airborne; }
    private _bookCode!: string;
    get BookCode(): string { return this._bookCode; }
    private _edition!: number;
    get Edition(): number { return this._edition; }
    private _rewardKey!: Combat_PhaseKey;
    get RewardKey(): Combat_PhaseKey { return this._rewardKey; }
    $relationFields$(): readonly unknown[] {
        return [this._id, this._abilityCode, this._origin, this._uses, this._optional, this._school, this._tier, this._airborne, this._bookCode, this._edition, this._rewardKey];
    }
    private _RefBook!: Combat_BookEntry;
    get RefBook(): Combat_BookEntry { return this._RefBook; }
    _bindRefBook$ (value: Combat_BookEntry | undefined): void { this._RefBook = value!; }
    private _RefBookByCode!: Combat_BookEntry;
    get RefBookByCode(): Combat_BookEntry { return this._RefBookByCode; }
    _bindRefBookByCode$ (value: Combat_BookEntry | undefined): void { this._RefBookByCode = value!; }
    private _RefBookRecord!: Combat_Phasebook;
    get RefBookRecord(): Combat_Phasebook { return this._RefBookRecord; }
    _bindRefBookRecord$ (value: Combat_Phasebook | undefined): void { this._RefBookRecord = value!; }
    private _RefReward!: Combat_Reward;
    get RefReward(): Combat_Reward { return this._RefReward; }
    _bindRefReward$ (value: Combat_Reward | undefined): void { this._RefReward = value!; }
    toString() : string {
        return "(" + this._id + "," + this._abilityCode + "," + this.Origin.toString() + "," + ToStringList(this._uses) + "," + ToStringList(this._optional) + "," + this._school + "," + this._tier + "," + this._airborne + "," + this._bookCode + "," + this._edition + "," + this.RewardKey.toString() + ")";
    }

    
    private static all: Map<number, Combat_Cast>;

    static Get(id: number) : Combat_Cast | undefined {
        return this.all.get(id)
    }

    static All() : Map<number, Combat_Cast> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<number, Combat_Cast>();
        for (let c = os.ReadCount(1); c > 0; c--) {
            let self = this._create(os);
            this.all.set(self._id, self);
        }

    }

    static Resolve(errors: LoadErrors) {
        for (const v of this.all.values()) {
            v._resolve(errors);
        }
    }
    static _create(os: Stream) : Combat_Cast {
        const self = new Combat_Cast();
        self._id = os.ReadInt32();
        self._abilityCode = os.ReadStringInPool();
        self._origin = Combat_Origin._create(os);
        self._uses = [];
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._uses.push(Combat_AbilityUse._create(os));
        }
        self._optional = [];
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._optional.push(Combat_OptionalAim._create(os));
        }
        self._school = os.ReadStringInPool();
        self._tier = os.ReadInt32();
        self._airborne = os.ReadBool();
        self._bookCode = os.ReadStringInPool();
        self._edition = os.ReadInt32();
        self._rewardKey = Combat_PhaseKey._create(os);
        return self;
    }

    _resolve(errors: LoadErrors) {
        for (const e of this._uses) {
            e._resolve(errors);
        }
        for (const e of this._optional) {
            e._resolve(errors);
        }
    }
}

export class Combat_Encounter {
    private _id!: number;
    get Id(): number { return this._id; }
    private _command!: Combat_Command;
    get Command(): Combat_Command { return this._command; }
    $relationFields$(): readonly unknown[] {
        return [this._id, this._command];
    }
    toString() : string {
        return "(" + this._id + "," + this.Command.toString() + ")";
    }

    
    private static all: Map<number, Combat_Encounter>;

    static Get(id: number) : Combat_Encounter | undefined {
        return this.all.get(id)
    }

    static All() : Map<number, Combat_Encounter> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<number, Combat_Encounter>();
        for (let c = os.ReadCount(1); c > 0; c--) {
            let self = this._create(os);
            this.all.set(self._id, self);
        }

    }

    static Resolve(errors: LoadErrors) {
        for (const v of this.all.values()) {
            v._resolve(errors);
        }
    }
    static _create(os: Stream) : Combat_Encounter {
        const self = new Combat_Encounter();
        self._id = os.ReadInt32();
        self._command = Combat_Command._create(os);
        return self;
    }

    _resolve(errors: LoadErrors) {
        this._command._resolve(errors);
    }
}

export class Combat_Phasebook {
    private _school!: string;
    get School(): string { return this._school; }
    private _tier!: number;
    get Tier(): number { return this._tier; }
    private _airborne!: boolean;
    get Airborne(): boolean { return this._airborne; }
    private _code!: string;
    get Code(): string { return this._code; }
    private _edition!: number;
    get Edition(): number { return this._edition; }
    private _entry!: Combat_BookEntry;
    get Entry(): Combat_BookEntry { return this._entry; }
    $relationFields$(): readonly unknown[] {
        return [this._school, this._tier, this._airborne, this._code, this._edition, this._entry];
    }
    toString() : string {
        return "(" + this._school + "," + this._tier + "," + this._airborne + "," + this._code + "," + this._edition + "," + this.Entry.toString() + ")";
    }

    
    private static all: Map<string, Combat_Phasebook>;

    static Get(school: string, tier: number, airborne: boolean) : Combat_Phasebook | undefined {
        return this.all.get($relationKey$([school, tier, airborne]))
    }

    
    private static codeEditionMap: Map<string, Combat_Phasebook>;

    static GetByCodeEdition(code: string, edition: number) : Combat_Phasebook | undefined {
        return this.codeEditionMap.get($relationKey$([code, edition]))
    }

    static All() : Map<string, Combat_Phasebook> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<string, Combat_Phasebook>();
        this.codeEditionMap = new Map<string, Combat_Phasebook>();
        for (let c = os.ReadCount(1); c > 0; c--) {
            let self = this._create(os);
            this.all.set($relationKey$([self._school, self._tier, self._airborne]), self);
            this.codeEditionMap.set($relationKey$([self._code, self._edition]), self);
        }

    }

    static _create(os: Stream) : Combat_Phasebook {
        const self = new Combat_Phasebook();
        self._school = os.ReadStringInPool();
        self._tier = os.ReadInt32();
        self._airborne = os.ReadBool();
        self._code = os.ReadStringInPool();
        self._edition = os.ReadInt32();
        self._entry = Combat_BookEntry._create(os);
        return self;
    }

}

export class Combat_Timeline {
    private _id!: number;
    get Id(): number { return this._id; }
    private _events!: Map<Combat_PhaseKey, Combat_Effect>;
    get Events(): Map<Combat_PhaseKey, Combat_Effect> { return this._events; }
    private _channels!: Map<string, Combat_Effect>;
    get Channels(): Map<string, Combat_Effect> { return this._channels; }
    private _slots!: Map<number, Combat_Effect>;
    get Slots(): Map<number, Combat_Effect> { return this._slots; }
    private _clock!: Map<bigint, Combat_Effect>;
    get Clock(): Map<bigint, Combat_Effect> { return this._clock; }
    private _modes!: Map<boolean, Combat_Effect>;
    get Modes(): Map<boolean, Combat_Effect> { return this._modes; }
    private _eventKey!: Combat_PhaseKey;
    get EventKey(): Combat_PhaseKey { return this._eventKey; }
    private _channel!: string;
    get Channel(): string { return this._channel; }
    private _slot!: number;
    get Slot(): number { return this._slot; }
    private _frame!: bigint;
    get Frame(): bigint { return this._frame; }
    private _airborne!: boolean;
    get Airborne(): boolean { return this._airborne; }
    private _fallback!: Combat_Effect;
    get Fallback(): Combat_Effect { return this._fallback; }
    $relationFields$(): readonly unknown[] {
        return [this._id, this._events, this._channels, this._slots, this._clock, this._modes, this._eventKey, this._channel, this._slot, this._frame, this._airborne, this._fallback];
    }
    private _RefEventKey!: Combat_Effect;
    get RefEventKey(): Combat_Effect { return this._RefEventKey; }
    _bindRefEventKey$ (value: Combat_Effect | undefined): void { this._RefEventKey = value!; }
    private _RefChannel!: Combat_Effect;
    get RefChannel(): Combat_Effect { return this._RefChannel; }
    _bindRefChannel$ (value: Combat_Effect | undefined): void { this._RefChannel = value!; }
    private _RefSlot!: Combat_Effect;
    get RefSlot(): Combat_Effect { return this._RefSlot; }
    _bindRefSlot$ (value: Combat_Effect | undefined): void { this._RefSlot = value!; }
    private _RefFrame!: Combat_Effect;
    get RefFrame(): Combat_Effect { return this._RefFrame; }
    _bindRefFrame$ (value: Combat_Effect | undefined): void { this._RefFrame = value!; }
    private _RefAirborne!: Combat_Effect;
    get RefAirborne(): Combat_Effect { return this._RefAirborne; }
    _bindRefAirborne$ (value: Combat_Effect | undefined): void { this._RefAirborne = value!; }
    private _RefDefault!: Combat_Effect;
    get RefDefault(): Combat_Effect { return this._RefDefault; }
    _bindRefDefault$ (value: Combat_Effect | undefined): void { this._RefDefault = value!; }
    toString() : string {
        return "(" + this._id + "," + ToStringMap(this._events) + "," + ToStringMap(this._channels) + "," + ToStringMap(this._slots) + "," + ToStringMap(this._clock) + "," + ToStringMap(this._modes) + "," + this.EventKey.toString() + "," + this._channel + "," + this._slot + "," + this._frame + "," + this._airborne + "," + this.Fallback.toString() + ")";
    }

    
    private static all: Map<number, Combat_Timeline>;

    static Get(id: number) : Combat_Timeline | undefined {
        return this.all.get(id)
    }

    static All() : Map<number, Combat_Timeline> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<number, Combat_Timeline>();
        for (let c = os.ReadCount(1); c > 0; c--) {
            let self = this._create(os);
            this.all.set(self._id, self);
        }

    }

    static Resolve(errors: LoadErrors) {
        for (const v of this.all.values()) {
            v._resolve(errors);
        }
    }
    static _create(os: Stream) : Combat_Timeline {
        const self = new Combat_Timeline();
        self._id = os.ReadInt32();
        self._events  = new Map<Combat_PhaseKey, Combat_Effect>();
        for (let c = os.ReadCount(0); c > 0; c--) {
            const key$ = Combat_PhaseKey._create(os);
            if (self._events.has(key$)) throw Error("Duplicate map key: combat.timeline.events");
            self._events.set(key$, Combat_Effect._create(os));
        }
        self._channels  = new Map<string, Combat_Effect>();
        for (let c = os.ReadCount(0); c > 0; c--) {
            const key$ = os.ReadStringInPool();
            if (self._channels.has(key$)) throw Error("Duplicate map key: combat.timeline.channels");
            self._channels.set(key$, Combat_Effect._create(os));
        }
        self._slots  = new Map<number, Combat_Effect>();
        for (let c = os.ReadCount(0); c > 0; c--) {
            const key$ = os.ReadInt32();
            if (self._slots.has(key$)) throw Error("Duplicate map key: combat.timeline.slots");
            self._slots.set(key$, Combat_Effect._create(os));
        }
        self._clock  = new Map<bigint, Combat_Effect>();
        for (let c = os.ReadCount(0); c > 0; c--) {
            const key$ = os.ReadInt64BigInt();
            if (self._clock.has(key$)) throw Error("Duplicate map key: combat.timeline.clock");
            self._clock.set(key$, Combat_Effect._create(os));
        }
        self._modes  = new Map<boolean, Combat_Effect>();
        for (let c = os.ReadCount(0); c > 0; c--) {
            const key$ = os.ReadBool();
            if (self._modes.has(key$)) throw Error("Duplicate map key: combat.timeline.modes");
            self._modes.set(key$, Combat_Effect._create(os));
        }
        self._eventKey = Combat_PhaseKey._create(os);
        self._channel = os.ReadStringInPool();
        self._slot = os.ReadInt32();
        self._frame = os.ReadInt64BigInt();
        self._airborne = os.ReadBool();
        self._fallback = Combat_Effect._create(os);
        return self;
    }

    _resolve(errors: LoadErrors) {
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
    $relationFields$(): readonly unknown[] {
        return [this._id, this._name];
    }
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
        this._attack = undefined!;
        this._defence = undefined!;
        this._hp = undefined!;
        this._critical = undefined!;
        this._critical_resist = undefined!;
        this._block = undefined!;
        this._break_armor = undefined!;
        this.all = new Map<number, Equip_Ability>();
        for (let c = os.ReadCount(1); c > 0; c--) {
            let self = this._create(os);
            this.all.set(self._id, self);
            if (self._name.trim().length === 0) {
                continue;
            }
            switch(self._name.trim()) {
                case "attack":
                    if (this._attack != null) {
                        errors.EnumDup("equip.ability", "attack");
                    }
                    this._attack = self;
                    break;
                case "defence":
                    if (this._defence != null) {
                        errors.EnumDup("equip.ability", "defence");
                    }
                    this._defence = self;
                    break;
                case "hp":
                    if (this._hp != null) {
                        errors.EnumDup("equip.ability", "hp");
                    }
                    this._hp = self;
                    break;
                case "critical":
                    if (this._critical != null) {
                        errors.EnumDup("equip.ability", "critical");
                    }
                    this._critical = self;
                    break;
                case "critical_resist":
                    if (this._critical_resist != null) {
                        errors.EnumDup("equip.ability", "critical_resist");
                    }
                    this._critical_resist = self;
                    break;
                case "block":
                    if (this._block != null) {
                        errors.EnumDup("equip.ability", "block");
                    }
                    this._block = self;
                    break;
                case "break_armor":
                    if (this._break_armor != null) {
                        errors.EnumDup("equip.ability", "break_armor");
                    }
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

export class Equip_Equip_config {
    private static _Instance : Equip_Equip_config;
    static get Instance() :Equip_Equip_config { return this._Instance; }

    private static _Instance2 : Equip_Equip_config;
    static get Instance2() :Equip_Equip_config { return this._Instance2; }

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
    $relationFields$(): readonly unknown[] {
        return [this._entry, this._stone_count_for_set, this._draw_protect_name, this._broadcastid, this._broadcast_least_quality, this._week_reward_mailid];
    }
    toString() : string {
        return "(" + this._entry + "," + this._stone_count_for_set + "," + this._draw_protect_name + "," + this._broadcastid + "," + this._broadcast_least_quality + "," + this._week_reward_mailid + ")";
    }

    
    private static all: Map<string, Equip_Equip_config>;

    static Get(entry: string) : Equip_Equip_config | undefined {
        return this.all.get(entry)
    }

    static All() : Map<string, Equip_Equip_config> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this._Instance = undefined!;
        this._Instance2 = undefined!;
        this.all = new Map<string, Equip_Equip_config>();
        for (let c = os.ReadCount(1); c > 0; c--) {
            let self = this._create(os);
            this.all.set(self._entry, self);
            if (self._entry.trim().length === 0) {
                continue;
            }
            switch(self._entry.trim()) {
                case "Instance":
                    if (this._Instance != null) {
                        errors.EnumDup("equip.equip_config", "Instance");
                    }
                    this._Instance = self;
                    break;
                case "Instance2":
                    if (this._Instance2 != null) {
                        errors.EnumDup("equip.equip_config", "Instance2");
                    }
                    this._Instance2 = self;
                    break;
                default:
                    errors.EnumDataAdd("equip.equip_config", self._entry);
                    break;
            }
        }

        if (this._Instance == null) {
            errors.EnumNull("equip.equip_config", "Instance");
        }
        if (this._Instance2 == null) {
            errors.EnumNull("equip.equip_config", "Instance2");
        }
    }

    static _create(os: Stream) : Equip_Equip_config {
        const self = new Equip_Equip_config();
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
    $relationFields$(): readonly unknown[] {
        return [this._ID, this._Name, this._IconFile, this._LvlRank, this._JType, this._SuitID, this._KeyAbility, this._KeyAbilityValue, this._SalePrice, this._Description];
    }
    private _RefLvlRank!: Equip_Jewelry_random;
    get RefLvlRank(): Equip_Jewelry_random { return this._RefLvlRank; }
    private _RefJType!: Equip_Jewelry_type;
    get RefJType(): Equip_Jewelry_type { return this._RefJType; }
    private _NullableRefSuitID: Equip_Jewelry_suit | undefined;
    get NullableRefSuitID(): Equip_Jewelry_suit | undefined { return this._NullableRefSuitID; }
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
        for (let c = os.ReadCount(1); c > 0; c--) {
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
        const _tmpRefLvlRank = Equip_Jewelry_random.Get(this._LvlRank);
        if (_tmpRefLvlRank === undefined) {
            errors.RefNull("equip.jewelry", this.toString(), "LvlRank");
        }
        this._RefLvlRank = _tmpRefLvlRank!;
        const _tmpRefJType = Equip_Jewelry_type.Get(this._JType);
        if (_tmpRefJType === undefined) {
            errors.RefNull("equip.jewelry", this.toString(), "JType");
        }
        this._RefJType = _tmpRefJType!;
        this._NullableRefSuitID = Equip_Jewelry_suit.Get(this._SuitID);
        const _tmpRefKeyAbility = Equip_Ability.Get(this._KeyAbility);
        if (_tmpRefKeyAbility === undefined) {
            errors.RefNull("equip.jewelry", this.toString(), "KeyAbility");
        }
        this._RefKeyAbility = _tmpRefKeyAbility!;
    }
}

export class Equip_Jewelry_random {
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
    $relationFields$(): readonly unknown[] {
        return [this._LvlRank, this._AttackRange, this._OtherRange, this._TestPack];
    }
    toString() : string {
        return "(" + this.LvlRank.toString() + "," + this.AttackRange.toString() + "," + ToStringList(this._OtherRange) + "," + ToStringList(this._TestPack) + ")";
    }

    
    private static all: Map<string, Equip_Jewelry_random>;

    static Get(LvlRank: LevelRank) : Equip_Jewelry_random | undefined {
        return this.all.get($relationKey$([LvlRank]))
    }

    static All() : Map<string, Equip_Jewelry_random> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<string, Equip_Jewelry_random>();
        for (let c = os.ReadCount(1); c > 0; c--) {
            let self = this._create(os);
            this.all.set($relationKey$([self._LvlRank]), self);
        }

    }

    static Resolve(errors: LoadErrors) {
        for (const v of this.all.values()) {
            v._resolve(errors);
        }
    }
    static _create(os: Stream) : Equip_Jewelry_random {
        const self = new Equip_Jewelry_random();
        self._LvlRank = LevelRank._create(os);
        self._AttackRange = Range._create(os);
        self._OtherRange = [];
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._OtherRange.push(Range._create(os));
        }
        self._TestPack = [];
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._TestPack.push(Equip_TestPackBean._create(os));
        }
        return self;
    }

    _resolve(errors: LoadErrors) {
        this._LvlRank._resolve(errors);
    }
}

export class Equip_Jewelry_suit {
    private static _SpecialSuit : Equip_Jewelry_suit;
    static get SpecialSuit() :Equip_Jewelry_suit { return this._SpecialSuit; }

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
    $relationFields$(): readonly unknown[] {
        return [this._SuitID, this._Ename, this._Name, this._Ability1, this._Ability1Value, this._Ability2, this._Ability2Value, this._Ability3, this._Ability3Value, this._SuitList];
    }
    toString() : string {
        return "(" + this._SuitID + "," + this._Ename + "," + this._Name + "," + this._Ability1 + "," + this._Ability1Value + "," + this._Ability2 + "," + this._Ability2Value + "," + this._Ability3 + "," + this._Ability3Value + "," + ToStringList(this._SuitList) + ")";
    }

    
    private static all: Map<number, Equip_Jewelry_suit>;

    static Get(SuitID: number) : Equip_Jewelry_suit | undefined {
        return this.all.get(SuitID)
    }

    static All() : Map<number, Equip_Jewelry_suit> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this._SpecialSuit = undefined!;
        this.all = new Map<number, Equip_Jewelry_suit>();
        for (let c = os.ReadCount(1); c > 0; c--) {
            let self = this._create(os);
            this.all.set(self._SuitID, self);
            if (self._Ename.trim().length === 0) {
                continue;
            }
            switch(self._Ename.trim()) {
                case "SpecialSuit":
                    if (this._SpecialSuit != null) {
                        errors.EnumDup("equip.jewelry_suit", "SpecialSuit");
                    }
                    this._SpecialSuit = self;
                    break;
                default:
                    errors.EnumDataAdd("equip.jewelry_suit", self._Ename);
                    break;
            }
        }

        if (this._SpecialSuit == null) {
            errors.EnumNull("equip.jewelry_suit", "SpecialSuit");
        }
    }

    static _create(os: Stream) : Equip_Jewelry_suit {
        const self = new Equip_Jewelry_suit();
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
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._SuitList.push(os.ReadInt32());
        }
        return self;
    }

}

export class Equip_Jewelry_type {
    private static _Jade : Equip_Jewelry_type;
    static get Jade() :Equip_Jewelry_type { return this._Jade; }

    private static _Bracelet : Equip_Jewelry_type;
    static get Bracelet() :Equip_Jewelry_type { return this._Bracelet; }

    private static _Magic : Equip_Jewelry_type;
    static get Magic() :Equip_Jewelry_type { return this._Magic; }

    private static _Bottle : Equip_Jewelry_type;
    static get Bottle() :Equip_Jewelry_type { return this._Bottle; }

    private _TypeName!: string;
    /* 程序用名字 */
    get TypeName(): string { return this._TypeName; }
    $relationFields$(): readonly unknown[] {
        return [this._TypeName];
    }
    toString() : string {
        return "(" + this._TypeName + ")";
    }

    
    private static all: Map<string, Equip_Jewelry_type>;

    static Get(TypeName: string) : Equip_Jewelry_type | undefined {
        return this.all.get(TypeName)
    }

    static All() : Map<string, Equip_Jewelry_type> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this._Jade = undefined!;
        this._Bracelet = undefined!;
        this._Magic = undefined!;
        this._Bottle = undefined!;
        this.all = new Map<string, Equip_Jewelry_type>();
        for (let c = os.ReadCount(1); c > 0; c--) {
            let self = this._create(os);
            this.all.set(self._TypeName, self);
            if (self._TypeName.trim().length === 0) {
                continue;
            }
            switch(self._TypeName.trim()) {
                case "Jade":
                    if (this._Jade != null) {
                        errors.EnumDup("equip.jewelry_type", "Jade");
                    }
                    this._Jade = self;
                    break;
                case "Bracelet":
                    if (this._Bracelet != null) {
                        errors.EnumDup("equip.jewelry_type", "Bracelet");
                    }
                    this._Bracelet = self;
                    break;
                case "Magic":
                    if (this._Magic != null) {
                        errors.EnumDup("equip.jewelry_type", "Magic");
                    }
                    this._Magic = self;
                    break;
                case "Bottle":
                    if (this._Bottle != null) {
                        errors.EnumDup("equip.jewelry_type", "Bottle");
                    }
                    this._Bottle = self;
                    break;
                default:
                    errors.EnumDataAdd("equip.jewelry_type", self._TypeName);
                    break;
            }
        }

        if (this._Jade == null) {
            errors.EnumNull("equip.jewelry_type", "Jade");
        }
        if (this._Bracelet == null) {
            errors.EnumNull("equip.jewelry_type", "Bracelet");
        }
        if (this._Magic == null) {
            errors.EnumNull("equip.jewelry_type", "Magic");
        }
        if (this._Bottle == null) {
            errors.EnumNull("equip.jewelry_type", "Bottle");
        }
    }

    static _create(os: Stream) : Equip_Jewelry_type {
        const self = new Equip_Jewelry_type();
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

    private static _red : Equip_Rank;
    static get Red() :Equip_Rank { return this._red; }

    private _RankID!: number;
    /* 稀有度 */
    get RankID(): number { return this._RankID; }
    private _RankName!: string;
    /* 程序用名字 */
    get RankName(): string { return this._RankName; }
    private _RankShowName!: string;
    /* 显示名称 */
    get RankShowName(): string { return this._RankShowName; }
    $relationFields$(): readonly unknown[] {
        return [this._RankID, this._RankName, this._RankShowName];
    }
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
        this._white = undefined!;
        this._green = undefined!;
        this._blue = undefined!;
        this._purple = undefined!;
        this._yellow = undefined!;
        this._red = undefined!;
        this.all = new Map<number, Equip_Rank>();
        for (let c = os.ReadCount(1); c > 0; c--) {
            let self = this._create(os);
            this.all.set(self._RankID, self);
            if (self._RankName.trim().length === 0) {
                continue;
            }
            switch(self._RankName.trim()) {
                case "white":
                    if (this._white != null) {
                        errors.EnumDup("equip.rank", "white");
                    }
                    this._white = self;
                    break;
                case "green":
                    if (this._green != null) {
                        errors.EnumDup("equip.rank", "green");
                    }
                    this._green = self;
                    break;
                case "blue":
                    if (this._blue != null) {
                        errors.EnumDup("equip.rank", "blue");
                    }
                    this._blue = self;
                    break;
                case "purple":
                    if (this._purple != null) {
                        errors.EnumDup("equip.rank", "purple");
                    }
                    this._purple = self;
                    break;
                case "yellow":
                    if (this._yellow != null) {
                        errors.EnumDup("equip.rank", "yellow");
                    }
                    this._yellow = self;
                    break;
                case "red":
                    if (this._red != null) {
                        errors.EnumDup("equip.rank", "red");
                    }
                    this._red = self;
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
        if (this._red == null) {
            errors.EnumNull("equip.rank", "red");
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

export class Other_ArgCaptureMode {
    private static _Snapshot : Other_ArgCaptureMode;
    static get Snapshot() :Other_ArgCaptureMode { return this._Snapshot; }

    private static _Dynamic : Other_ArgCaptureMode;
    static get Dynamic() :Other_ArgCaptureMode { return this._Dynamic; }

    private _name!: string;
    get Name(): string { return this._name; }
    private _id!: number;
    get Id(): number { return this._id; }
    private _comment!: string;
    get Comment(): string { return this._comment; }
    $relationFields$(): readonly unknown[] {
        return [this._name, this._id, this._comment];
    }
    toString() : string {
        return "(" + this._name + "," + this._id + "," + this._comment + ")";
    }

    
    private static all: Map<string, Other_ArgCaptureMode>;

    static Get(name: string) : Other_ArgCaptureMode | undefined {
        return this.all.get(name)
    }

    
    private static idMap: Map<number, Other_ArgCaptureMode>;

    static GetById(id: number) : Other_ArgCaptureMode | undefined {
        return this.idMap.get(id)
    }

    static All() : Map<string, Other_ArgCaptureMode> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this._Snapshot = undefined!;
        this._Dynamic = undefined!;
        this.all = new Map<string, Other_ArgCaptureMode>();
        this.idMap = new Map<number, Other_ArgCaptureMode>();
        for (let c = os.ReadCount(1); c > 0; c--) {
            let self = this._create(os);
            this.all.set(self._name, self);
            this.idMap.set(self._id, self);
            if (self._name.trim().length === 0) {
                continue;
            }
            switch(self._name.trim()) {
                case "Snapshot":
                    if (this._Snapshot != null) {
                        errors.EnumDup("other.ArgCaptureMode", "Snapshot");
                    }
                    this._Snapshot = self;
                    break;
                case "Dynamic":
                    if (this._Dynamic != null) {
                        errors.EnumDup("other.ArgCaptureMode", "Dynamic");
                    }
                    this._Dynamic = self;
                    break;
                default:
                    errors.EnumDataAdd("other.ArgCaptureMode", self._name);
                    break;
            }
        }

        if (this._Snapshot == null) {
            errors.EnumNull("other.ArgCaptureMode", "Snapshot");
        }
        if (this._Dynamic == null) {
            errors.EnumNull("other.ArgCaptureMode", "Dynamic");
        }
    }

    static _create(os: Stream) : Other_ArgCaptureMode {
        const self = new Other_ArgCaptureMode();
        self._name = os.ReadStringInPool();
        self._id = os.ReadInt32();
        self._comment = os.ReadStringInPool();
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
    $relationFields$(): readonly unknown[] {
        return [this._dropid, this._name, this._items, this._testmap];
    }
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
        for (let c = os.ReadCount(1); c > 0; c--) {
            let self = this._create(os);
            this.all.set(self._dropid, self);
        }

    }

    static _create(os: Stream) : Other_Drop {
        const self = new Other_Drop();
        self._dropid = os.ReadInt32();
        self._name = Text._create(os);
        self._items = [];
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._items.push(Other_DropItem._create(os));
        }
        self._testmap  = new Map<number, number>();
        for (let c = os.ReadCount(0); c > 0; c--) {
            const key$ = os.ReadInt32();
            if (self._testmap.has(key$)) throw Error("Duplicate map key: other.drop.testmap");
            self._testmap.set(key$, os.ReadInt32());
        }
        return self;
    }

}

export class Other_Key_test {
    private _id1!: number;
    get Id1(): number { return this._id1; }
    private _id2!: bigint;
    get Id2(): bigint { return this._id2; }
    private _id3!: number;
    get Id3(): number { return this._id3; }
    private _ids!: number[];
    get Ids(): number[] { return this._ids; }
    private _enumTest!: string;
    get EnumTest(): string { return this._enumTest; }
    private _enumList!: string[];
    get EnumList(): string[] { return this._enumList; }
    $relationFields$(): readonly unknown[] {
        return [this._id1, this._id2, this._id3, this._ids, this._enumTest, this._enumList];
    }
    private _RefIds!: Other_Sign_in[];
    get RefIds(): Other_Sign_in[] { return this._RefIds; }
    private _RefEnumTest!: Other_ArgCaptureMode;
    get RefEnumTest(): Other_ArgCaptureMode { return this._RefEnumTest; }
    private _RefEnumList!: Other_ArgCaptureMode[];
    get RefEnumList(): Other_ArgCaptureMode[] { return this._RefEnumList; }
    toString() : string {
        return "(" + this._id1 + "," + this._id2 + "," + this._id3 + "," + ToStringList(this._ids) + "," + this._enumTest + "," + ToStringList(this._enumList) + ")";
    }

    
    private static all: Map<string, Other_Key_test>;

    static Get(id1: number, id2: bigint) : Other_Key_test | undefined {
        return this.all.get($relationKey$([id1, id2]))
    }

    
    private static id1Id3Map: Map<string, Other_Key_test>;

    static GetById1Id3(id1: number, id3: number) : Other_Key_test | undefined {
        return this.id1Id3Map.get($relationKey$([id1, id3]))
    }

    
    private static id2Map: Map<bigint, Other_Key_test>;

    static GetById2(id2: bigint) : Other_Key_test | undefined {
        return this.id2Map.get(id2)
    }

    
    private static id2Id3Map: Map<string, Other_Key_test>;

    static GetById2Id3(id2: bigint, id3: number) : Other_Key_test | undefined {
        return this.id2Id3Map.get($relationKey$([id2, id3]))
    }

    static All() : Map<string, Other_Key_test> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<string, Other_Key_test>();
        this.id1Id3Map = new Map<string, Other_Key_test>();
        this.id2Map = new Map<bigint, Other_Key_test>();
        this.id2Id3Map = new Map<string, Other_Key_test>();
        for (let c = os.ReadCount(1); c > 0; c--) {
            let self = this._create(os);
            this.all.set($relationKey$([self._id1, self._id2]), self);
            this.id1Id3Map.set($relationKey$([self._id1, self._id3]), self);
            this.id2Map.set(self._id2, self);
            this.id2Id3Map.set($relationKey$([self._id2, self._id3]), self);
        }

    }

    static Resolve(errors: LoadErrors) {
        for (const v of this.all.values()) {
            v._resolve(errors);
        }
    }
    static _create(os: Stream) : Other_Key_test {
        const self = new Other_Key_test();
        self._id1 = os.ReadInt32();
        self._id2 = os.ReadInt64BigInt();
        self._id3 = os.ReadInt32();
        self._ids = [];
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._ids.push(os.ReadInt32());
        }
        self._enumTest = os.ReadStringInPool();
        self._enumList = [];
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._enumList.push(os.ReadStringInPool());
        }
        return self;
    }

    _resolve(errors: LoadErrors) {
        this._RefIds = [];
        for (const e of this._ids) {
            const r = Other_Sign_in.Get(e);
            if (r === undefined) {
                errors.RefNull("other.key_test", this.toString(), "ids");
                continue;
            }
            this._RefIds.push(r);
        }
        const _tmpRefEnumTest = Other_ArgCaptureMode.Get(this._enumTest);
        if (_tmpRefEnumTest === undefined) {
            errors.RefNull("other.key_test", this.toString(), "enumTest");
        }
        this._RefEnumTest = _tmpRefEnumTest!;
        this._RefEnumList = [];
        for (const e of this._enumList) {
            const r = Other_ArgCaptureMode.Get(e);
            if (r === undefined) {
                errors.RefNull("other.key_test", this.toString(), "enumList");
                continue;
            }
            this._RefEnumList.push(r);
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
    $relationFields$(): readonly unknown[] {
        return [this._lootid, this._ename, this._name, this._chanceList];
    }
    private _ListRefLootid!: Other_Loot_item[];
    get ListRefLootid(): Other_Loot_item[] { return this._ListRefLootid; }
    private _ListRefAnotherWay!: Other_Loot_item[];
    get ListRefAnotherWay(): Other_Loot_item[] { return this._ListRefAnotherWay; }
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
        for (let c = os.ReadCount(1); c > 0; c--) {
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
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._chanceList.push(os.ReadInt32());
        }
        return self;
    }

    _resolve(errors: LoadErrors) {
        this._ListRefLootid = [];
        for (const v of Other_Loot_item.All().values()) {
            if (v.Lootid === this._lootid) {
                this._ListRefLootid.push(v);
            }
        }
        this._ListRefAnotherWay = [];
        for (const v of Other_Loot_item.All().values()) {
            if (v.Lootid === this._lootid) {
                this._ListRefAnotherWay.push(v);
            }
        }
    }
}

export class Other_Loot_item {
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
    $relationFields$(): readonly unknown[] {
        return [this._lootid, this._itemid, this._chance, this._countmin, this._countmax];
    }
    toString() : string {
        return "(" + this._lootid + "," + this._itemid + "," + this._chance + "," + this._countmin + "," + this._countmax + ")";
    }

    
    private static all: Map<string, Other_Loot_item>;

    static Get(lootid: number, itemid: number) : Other_Loot_item | undefined {
        return this.all.get($relationKey$([lootid, itemid]))
    }

    static All() : Map<string, Other_Loot_item> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<string, Other_Loot_item>();
        for (let c = os.ReadCount(1); c > 0; c--) {
            let self = this._create(os);
            this.all.set($relationKey$([self._lootid, self._itemid]), self);
        }

    }

    static _create(os: Stream) : Other_Loot_item {
        const self = new Other_Loot_item();
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
    private _enumMap1!: Map<string, number>;
    get EnumMap1(): Map<string, number> { return this._enumMap1; }
    private _enumMap2!: Map<number, string>;
    get EnumMap2(): Map<number, string> { return this._enumMap2; }
    $relationFields$(): readonly unknown[] {
        return [this._id, this._posList, this._lootId, this._lootItemId, this._enumMap1, this._enumMap2];
    }
    private _RefLoot!: Other_Loot_item;
    get RefLoot(): Other_Loot_item { return this._RefLoot; }
    private _RefAllLoot!: Other_Loot;
    get RefAllLoot(): Other_Loot { return this._RefAllLoot; }
    private _RefEnumMap2!: Map<number, Other_ArgCaptureMode>;
    get RefEnumMap2(): Map<number, Other_ArgCaptureMode> { return this._RefEnumMap2; }
    toString() : string {
        return "(" + this._id + "," + ToStringList(this._posList) + "," + this._lootId + "," + this._lootItemId + "," + ToStringMap(this._enumMap1) + "," + ToStringMap(this._enumMap2) + ")";
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
        for (let c = os.ReadCount(1); c > 0; c--) {
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
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._posList.push(Position._create(os));
        }
        self._lootId = os.ReadInt32();
        self._lootItemId = os.ReadInt32();
        self._enumMap1  = new Map<string, number>();
        for (let c = os.ReadCount(0); c > 0; c--) {
            const key$ = os.ReadStringInPool();
            if (self._enumMap1.has(key$)) throw Error("Duplicate map key: other.monster.enumMap1");
            self._enumMap1.set(key$, os.ReadInt32());
        }
        self._enumMap2  = new Map<number, string>();
        for (let c = os.ReadCount(0); c > 0; c--) {
            const key$ = os.ReadInt32();
            if (self._enumMap2.has(key$)) throw Error("Duplicate map key: other.monster.enumMap2");
            self._enumMap2.set(key$, os.ReadStringInPool());
        }
        return self;
    }

    _resolve(errors: LoadErrors) {
        const _tmpRefLoot = Other_Loot_item.Get(this._lootId, this._lootItemId);
        if (_tmpRefLoot === undefined) {
            errors.RefNull("other.monster", this.toString(), "Loot");
        }
        this._RefLoot = _tmpRefLoot!;
        const _tmpRefAllLoot = Other_Loot.Get(this._lootId);
        if (_tmpRefAllLoot === undefined) {
            errors.RefNull("other.monster", this.toString(), "AllLoot");
        }
        this._RefAllLoot = _tmpRefAllLoot!;
        this._RefEnumMap2 = new Map<number, Other_ArgCaptureMode>();
        for (const e of this._enumMap2.entries()) {
            const v = Other_ArgCaptureMode.Get(e[1]);
            if (v === undefined) {
                errors.RefNull("other.monster", this.toString(), "enumMap2");
                continue;
            }
            this._RefEnumMap2.set(e[0], v);
        }
    }
}

export class Other_Sign_in {
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
    $relationFields$(): readonly unknown[] {
        return [this._id, this._item2countMap, this._vipitem2vipcountMap, this._viplevel, this._IconFile];
    }
    private _RefVipitem2vipcountMap!: Map<number, Other_Loot>;
    get RefVipitem2vipcountMap(): Map<number, Other_Loot> { return this._RefVipitem2vipcountMap; }
    toString() : string {
        return "(" + this._id + "," + ToStringMap(this._item2countMap) + "," + ToStringMap(this._vipitem2vipcountMap) + "," + this._viplevel + "," + this._IconFile + ")";
    }

    
    private static all: Map<number, Other_Sign_in>;

    static Get(id: number) : Other_Sign_in | undefined {
        return this.all.get(id)
    }

    static All() : Map<number, Other_Sign_in> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<number, Other_Sign_in>();
        for (let c = os.ReadCount(1); c > 0; c--) {
            let self = this._create(os);
            this.all.set(self._id, self);
        }

    }

    static Resolve(errors: LoadErrors) {
        for (const v of this.all.values()) {
            v._resolve(errors);
        }
    }
    static _create(os: Stream) : Other_Sign_in {
        const self = new Other_Sign_in();
        self._id = os.ReadInt32();
        self._item2countMap  = new Map<number, number>();
        for (let c = os.ReadCount(0); c > 0; c--) {
            const key$ = os.ReadInt32();
            if (self._item2countMap.has(key$)) throw Error("Duplicate map key: other.sign_in.item2countMap");
            self._item2countMap.set(key$, os.ReadInt32());
        }
        self._vipitem2vipcountMap  = new Map<number, number>();
        for (let c = os.ReadCount(0); c > 0; c--) {
            const key$ = os.ReadInt32();
            if (self._vipitem2vipcountMap.has(key$)) throw Error("Duplicate map key: other.sign_in.vipitem2vipcountMap");
            self._vipitem2vipcountMap.set(key$, os.ReadInt32());
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
                errors.RefNull("other.sign_in", this.toString(), "vipitem2vipcountMap");
                continue;
            }
            this._RefVipitem2vipcountMap.set(e[0], v);
        }
    }
}

export class Task_Complete_condition_type {
    private static _KillMonster : Task_Complete_condition_type;
    static get KillMonster() :Task_Complete_condition_type { return this._KillMonster; }

    private static _TalkNpc : Task_Complete_condition_type;
    static get TalkNpc() :Task_Complete_condition_type { return this._TalkNpc; }

    private static _CollectItem : Task_Complete_condition_type;
    static get CollectItem() :Task_Complete_condition_type { return this._CollectItem; }

    private static _ConditionAnd : Task_Complete_condition_type;
    static get ConditionAnd() :Task_Complete_condition_type { return this._ConditionAnd; }

    private static _Chat : Task_Complete_condition_type;
    static get Chat() :Task_Complete_condition_type { return this._Chat; }

    private static _TestNoColumn : Task_Complete_condition_type;
    static get TestNoColumn() :Task_Complete_condition_type { return this._TestNoColumn; }

    private static _aa : Task_Complete_condition_type;
    static get Aa() :Task_Complete_condition_type { return this._aa; }

    private _id!: number;
    /* 任务完成条件类型（id的范围为1-100） */
    get Id(): number { return this._id; }
    private _name!: string;
    /* 程序用名字 */
    get Name(): string { return this._name; }
    $relationFields$(): readonly unknown[] {
        return [this._id, this._name];
    }
    toString() : string {
        return "(" + this._id + "," + this._name + ")";
    }

    
    private static all: Map<number, Task_Complete_condition_type>;

    static Get(id: number) : Task_Complete_condition_type | undefined {
        return this.all.get(id)
    }

    static All() : Map<number, Task_Complete_condition_type> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this._KillMonster = undefined!;
        this._TalkNpc = undefined!;
        this._CollectItem = undefined!;
        this._ConditionAnd = undefined!;
        this._Chat = undefined!;
        this._TestNoColumn = undefined!;
        this._aa = undefined!;
        this.all = new Map<number, Task_Complete_condition_type>();
        for (let c = os.ReadCount(1); c > 0; c--) {
            let self = this._create(os);
            this.all.set(self._id, self);
            if (self._name.trim().length === 0) {
                continue;
            }
            switch(self._name.trim()) {
                case "KillMonster":
                    if (this._KillMonster != null) {
                        errors.EnumDup("task.complete_condition_type", "KillMonster");
                    }
                    this._KillMonster = self;
                    break;
                case "TalkNpc":
                    if (this._TalkNpc != null) {
                        errors.EnumDup("task.complete_condition_type", "TalkNpc");
                    }
                    this._TalkNpc = self;
                    break;
                case "CollectItem":
                    if (this._CollectItem != null) {
                        errors.EnumDup("task.complete_condition_type", "CollectItem");
                    }
                    this._CollectItem = self;
                    break;
                case "ConditionAnd":
                    if (this._ConditionAnd != null) {
                        errors.EnumDup("task.complete_condition_type", "ConditionAnd");
                    }
                    this._ConditionAnd = self;
                    break;
                case "Chat":
                    if (this._Chat != null) {
                        errors.EnumDup("task.complete_condition_type", "Chat");
                    }
                    this._Chat = self;
                    break;
                case "TestNoColumn":
                    if (this._TestNoColumn != null) {
                        errors.EnumDup("task.complete_condition_type", "TestNoColumn");
                    }
                    this._TestNoColumn = self;
                    break;
                case "aa":
                    if (this._aa != null) {
                        errors.EnumDup("task.complete_condition_type", "aa");
                    }
                    this._aa = self;
                    break;
                default:
                    errors.EnumDataAdd("task.complete_condition_type", self._name);
                    break;
            }
        }

        if (this._KillMonster == null) {
            errors.EnumNull("task.complete_condition_type", "KillMonster");
        }
        if (this._TalkNpc == null) {
            errors.EnumNull("task.complete_condition_type", "TalkNpc");
        }
        if (this._CollectItem == null) {
            errors.EnumNull("task.complete_condition_type", "CollectItem");
        }
        if (this._ConditionAnd == null) {
            errors.EnumNull("task.complete_condition_type", "ConditionAnd");
        }
        if (this._Chat == null) {
            errors.EnumNull("task.complete_condition_type", "Chat");
        }
        if (this._TestNoColumn == null) {
            errors.EnumNull("task.complete_condition_type", "TestNoColumn");
        }
        if (this._aa == null) {
            errors.EnumNull("task.complete_condition_type", "aa");
        }
    }

    static _create(os: Stream) : Task_Complete_condition_type {
        const self = new Task_Complete_condition_type();
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
    $relationFields$(): readonly unknown[] {
        return [this._taskid, this._name, this._nexttask, this._completecondition, this._exp, this._testDefaultBean];
    }
    private _NullableRefTaskid: Task_Task_extra_exp | undefined;
    get NullableRefTaskid(): Task_Task_extra_exp | undefined { return this._NullableRefTaskid; }
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
        for (let c = os.ReadCount(1); c > 0; c--) {
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
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._name.push(Text._create(os));
        }
        self._nexttask = os.ReadInt32();
        self._completecondition = Task_Completecondition._create(os);
        self._exp = os.ReadInt32();
        self._testDefaultBean = Task_TestDefaultBean._create(os);
        return self;
    }

    _resolve(errors: LoadErrors) {
        this._completecondition._resolve(errors);
        this._NullableRefTaskid = Task_Task_extra_exp.Get(this._taskid);
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
    $relationFields$(): readonly unknown[] {
        return [this._taskid, this._name, this._nexttask, this._completecondition, this._exp, this._testBool, this._testString, this._testStruct, this._testList, this._testListStruct, this._testListInterface];
    }
    private _NullableRefTaskid: Task_Task_extra_exp | undefined;
    get NullableRefTaskid(): Task_Task_extra_exp | undefined { return this._NullableRefTaskid; }
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
        for (let c = os.ReadCount(1); c > 0; c--) {
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
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._name.push(Text._create(os));
        }
        self._nexttask = os.ReadInt32();
        self._completecondition = Task_Completecondition._create(os);
        self._exp = os.ReadInt32();
        self._testBool = os.ReadBool();
        self._testString = os.ReadStringInPool();
        self._testStruct = Position._create(os);
        self._testList = [];
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._testList.push(os.ReadInt32());
        }
        self._testListStruct = [];
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._testListStruct.push(Position._create(os));
        }
        self._testListInterface = [];
        for (let c = os.ReadCount(0); c > 0; c--) {
            self._testListInterface.push(Ai_TriggerTick._create(os));
        }
        return self;
    }

    _resolve(errors: LoadErrors) {
        this._completecondition._resolve(errors);
        this._NullableRefTaskid = Task_Task_extra_exp.Get(this._taskid);
        this._NullableRefNexttask = Task_Task.Get(this._nexttask);
    }
}

export class Task_Task_extra_exp {
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
    $relationFields$(): readonly unknown[] {
        return [this._taskid, this._extraexp, this._test1, this._test2, this._fielda, this._fieldb, this._fieldc, this._fieldd];
    }
    toString() : string {
        return "(" + this._taskid + "," + this._extraexp + "," + this._test1 + "," + this._test2 + "," + this._fielda + "," + this._fieldb + "," + this._fieldc + "," + this._fieldd + ")";
    }

    
    private static all: Map<number, Task_Task_extra_exp>;

    static Get(taskid: number) : Task_Task_extra_exp | undefined {
        return this.all.get(taskid)
    }

    static All() : Map<number, Task_Task_extra_exp> {
        return this.all;
    }

    static Initialize(os: Stream, errors: LoadErrors) {
        this.all = new Map<number, Task_Task_extra_exp>();
        for (let c = os.ReadCount(1); c > 0; c--) {
            let self = this._create(os);
            this.all.set(self._taskid, self);
        }

    }

    static _create(os: Stream) : Task_Task_extra_exp {
        const self = new Task_Task_extra_exp();
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
    private static readonly TableNames = [
        "ai.ai",
        "ai.ai_action",
        "ai.ai_condition",
        "combat.ability",
        "combat.branchreward",
        "combat.cast",
        "combat.encounter",
        "combat.phasebook",
        "combat.timeline",
        "equip.ability",
        "equip.equip_config",
        "equip.jewelry",
        "equip.jewelry_random",
        "equip.jewelry_suit",
        "equip.jewelry_type",
        "equip.rank",
        "other.ArgCaptureMode",
        "other.drop",
        "other.key_test",
        "other.loot",
        "other.loot_item",
        "other.monster",
        "other.sign_in",
        "task.complete_condition_type",
        "task.task",
        "task.task2",
        "task.task_extra_exp",
    ];

    static ProcessPackage(pack: ConfigPackage, errors: LoadErrors): Stream {
        return Loader.LoadPackage(pack, Processor.Process, errors, ["zh_cn", "en", "tw"]);
    }

    // 从 bytes 文件加载（新格式）
    static Process(os: Stream, errors: LoadErrors): void {
        // 读取表数量
        const tables = os.ReadTables();
        for (const name of Processor.TableNames) {
            if (!tables.has(name)) {
                throw new Error("Missing table in standalone package: " + name);
            }
        }
        for (const [tableName, tableInput] of tables) {

            // 根据表名分发到对应的 Initialize 方法
            switch(tableName) {
                case "ai.ai":
                    Ai_Ai.Initialize(tableInput, errors);
                    tableInput.RequireEnd();
                    break;
                case "ai.ai_action":
                    Ai_Ai_action.Initialize(tableInput, errors);
                    tableInput.RequireEnd();
                    break;
                case "ai.ai_condition":
                    Ai_Ai_condition.Initialize(tableInput, errors);
                    tableInput.RequireEnd();
                    break;
                case "combat.ability":
                    Combat_Ability.Initialize(tableInput, errors);
                    tableInput.RequireEnd();
                    break;
                case "combat.branchreward":
                    Combat_Branchreward.Initialize(tableInput, errors);
                    tableInput.RequireEnd();
                    break;
                case "combat.cast":
                    Combat_Cast.Initialize(tableInput, errors);
                    tableInput.RequireEnd();
                    break;
                case "combat.encounter":
                    Combat_Encounter.Initialize(tableInput, errors);
                    tableInput.RequireEnd();
                    break;
                case "combat.phasebook":
                    Combat_Phasebook.Initialize(tableInput, errors);
                    tableInput.RequireEnd();
                    break;
                case "combat.timeline":
                    Combat_Timeline.Initialize(tableInput, errors);
                    tableInput.RequireEnd();
                    break;
                case "equip.ability":
                    Equip_Ability.Initialize(tableInput, errors);
                    tableInput.RequireEnd();
                    break;
                case "equip.equip_config":
                    Equip_Equip_config.Initialize(tableInput, errors);
                    tableInput.RequireEnd();
                    break;
                case "equip.jewelry":
                    Equip_Jewelry.Initialize(tableInput, errors);
                    tableInput.RequireEnd();
                    break;
                case "equip.jewelry_random":
                    Equip_Jewelry_random.Initialize(tableInput, errors);
                    tableInput.RequireEnd();
                    break;
                case "equip.jewelry_suit":
                    Equip_Jewelry_suit.Initialize(tableInput, errors);
                    tableInput.RequireEnd();
                    break;
                case "equip.jewelry_type":
                    Equip_Jewelry_type.Initialize(tableInput, errors);
                    tableInput.RequireEnd();
                    break;
                case "equip.rank":
                    Equip_Rank.Initialize(tableInput, errors);
                    tableInput.RequireEnd();
                    break;
                case "other.ArgCaptureMode":
                    Other_ArgCaptureMode.Initialize(tableInput, errors);
                    tableInput.RequireEnd();
                    break;
                case "other.drop":
                    Other_Drop.Initialize(tableInput, errors);
                    tableInput.RequireEnd();
                    break;
                case "other.key_test":
                    Other_Key_test.Initialize(tableInput, errors);
                    tableInput.RequireEnd();
                    break;
                case "other.loot":
                    Other_Loot.Initialize(tableInput, errors);
                    tableInput.RequireEnd();
                    break;
                case "other.loot_item":
                    Other_Loot_item.Initialize(tableInput, errors);
                    tableInput.RequireEnd();
                    break;
                case "other.monster":
                    Other_Monster.Initialize(tableInput, errors);
                    tableInput.RequireEnd();
                    break;
                case "other.sign_in":
                    Other_Sign_in.Initialize(tableInput, errors);
                    tableInput.RequireEnd();
                    break;
                case "task.complete_condition_type":
                    Task_Complete_condition_type.Initialize(tableInput, errors);
                    tableInput.RequireEnd();
                    break;
                case "task.task":
                    Task_Task.Initialize(tableInput, errors);
                    tableInput.RequireEnd();
                    break;
                case "task.task2":
                    Task_Task2.Initialize(tableInput, errors);
                    tableInput.RequireEnd();
                    break;
                case "task.task_extra_exp":
                    Task_Task_extra_exp.Initialize(tableInput, errors);
                    tableInput.RequireEnd();
                    break;
                default:
                    // 未知表，跳过
                    break;
            }
        }

        // 解析外键引用
        Combat_Ability.Resolve(errors);
        Combat_Cast.Resolve(errors);
        Combat_Encounter.Resolve(errors);
        Combat_Timeline.Resolve(errors);
        Equip_Jewelry.Resolve(errors);
        Equip_Jewelry_random.Resolve(errors);
        Other_Key_test.Resolve(errors);
        Other_Loot.Resolve(errors);
        Other_Monster.Resolve(errors);
        Other_Sign_in.Resolve(errors);
        Task_Task.Resolve(errors);
        Task_Task2.Resolve(errors);
        $resolveRelations$(errors);
    }
}


function $resolveRelations$(errors: LoadErrors): void {
    const resolver = new $RelationResolver$(errors, [
        { name: "LevelRank", ctor: LevelRank, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "Position", ctor: Position, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "Range", ctor: Range, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "ai.TriggerTick.ByLevel", ctor: Ai_TriggerTick_ByLevel, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "ai.TriggerTick.ByServerUpDay", ctor: Ai_TriggerTick_ByServerUpDay, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "ai.TriggerTick.ConstValue", ctor: Ai_TriggerTick_ConstValue, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "ai.ai", ctor: Ai_Ai, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "ai.ai_action", ctor: Ai_Ai_action, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "ai.ai_condition", ctor: Ai_Ai_condition, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "combat.AbilityUse", ctor: Combat_AbilityUse, indexes: [
        ], plans: [
            { root: "combat.cast", slot: 0, name: "ability", nullable: false, sources: [
                { scope: "", fields: [0, 0] },
            ], target: "table:combat.ability", recordKey: [0], steps: [
            ] },
        ], bind: (value, refs) => {
            const row = value as Combat_AbilityUse;
            row._bindRefAbility$(refs[0] as Combat_Ability | undefined);
        } },
        { name: "combat.Anchor", ctor: Combat_Anchor, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "combat.AnchorUse", ctor: Combat_AnchorUse, indexes: [
        ], plans: [
            { root: "combat.ability", slot: 0, name: "anchor", nullable: false, sources: [
            ], target: "ancestor:combat.Machine", recordKey: [], steps: [
                { field: 8, kind: 0, keys: [] },
            ] },
        ], bind: (value, refs) => {
            const row = value as Combat_AnchorUse;
            row._bindRefAnchor$(refs[0] as Combat_Anchor | undefined);
        } },
        { name: "combat.Binding", ctor: Combat_Binding, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "combat.BookEntry", ctor: Combat_BookEntry, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "combat.Combo", ctor: Combat_Combo, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "combat.Command.Jump", ctor: Combat_Command_Jump, indexes: [
        ], plans: [
            { root: "combat.encounter", slot: 0, name: "key", nullable: true, sources: [
                { scope: "", fields: [0] },
            ], target: "ancestor:combat.Command.StateMachine", recordKey: [], steps: [
                { field: 0, kind: 1, keys: [0] },
            ] },
        ], bind: (value, refs) => {
            const row = value as Combat_Command_Jump;
            row._bindNullableRefKey$(refs[0] as Combat_NestedPhase | undefined);
        } },
        { name: "combat.Command.Noop", ctor: Combat_Command_Noop, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "combat.Command.StateMachine", ctor: Combat_Command_StateMachine, indexes: [
            { field: 0, keys: [0] },
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "combat.Effect.Damage", ctor: Combat_Effect_Damage, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "combat.Effect.Heal", ctor: Combat_Effect_Heal, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "combat.Effect.None", ctor: Combat_Effect_None, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "combat.Exit", ctor: Combat_Exit, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "combat.Frame", ctor: Combat_Frame, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "combat.Impact", ctor: Combat_Impact, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "combat.Machine", ctor: Combat_Machine, indexes: [
            { field: 1, keys: [0] },
            { field: 2, keys: [0] },
            { field: 3, keys: [0] },
            { field: 4, keys: [0] },
            { field: 5, keys: [0] },
            { field: 6, keys: [0, 1, 2] },
            { field: 7, keys: [0] },
        ], plans: [
            { root: "combat.ability", slot: 0, name: "initial", nullable: false, sources: [
                { scope: "", fields: [11] },
            ], target: "ancestor:combat.Machine", recordKey: [], steps: [
                { field: 1, kind: 1, keys: [0] },
            ] },
            { root: "combat.ability", slot: 1, name: "impact", nullable: false, sources: [
                { scope: "", fields: [12] },
            ], target: "ancestor:combat.Machine", recordKey: [], steps: [
                { field: 2, kind: 1, keys: [0] },
            ] },
            { root: "combat.ability", slot: 2, name: "frame", nullable: false, sources: [
                { scope: "", fields: [13] },
            ], target: "ancestor:combat.Machine", recordKey: [], steps: [
                { field: 3, kind: 1, keys: [0] },
            ] },
            { root: "combat.ability", slot: 3, name: "stance", nullable: false, sources: [
                { scope: "", fields: [14] },
            ], target: "ancestor:combat.Machine", recordKey: [], steps: [
                { field: 4, kind: 1, keys: [0] },
            ] },
            { root: "combat.ability", slot: 4, name: "variant", nullable: false, sources: [
                { scope: "", fields: [15] },
            ], target: "ancestor:combat.Machine", recordKey: [], steps: [
                { field: 5, kind: 1, keys: [0] },
            ] },
            { root: "combat.ability", slot: 5, name: "combo", nullable: false, sources: [
                { scope: "", fields: [16] },
                { scope: "", fields: [17] },
                { scope: "", fields: [18] },
            ], target: "ancestor:combat.Machine", recordKey: [], steps: [
                { field: 6, kind: 1, keys: [0, 1, 2] },
            ] },
        ], bind: (value, refs) => {
            const row = value as Combat_Machine;
            row._bindRefInitial$(refs[0] as Combat_Phase | undefined);
            row._bindRefImpact$(refs[1] as Combat_Impact | undefined);
            row._bindRefFrame$(refs[2] as Combat_Frame | undefined);
            row._bindRefStance$(refs[3] as Combat_Stance | undefined);
            row._bindRefVariant$(refs[4] as Combat_PhaseVariant | undefined);
            row._bindRefCombo$(refs[5] as Combat_Combo | undefined);
        } },
        { name: "combat.MachineUse", ctor: Combat_MachineUse, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "combat.Marker", ctor: Combat_Marker, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "combat.NestedPhase", ctor: Combat_NestedPhase, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "combat.OptionalAim", ctor: Combat_OptionalAim, indexes: [
        ], plans: [
            { root: "combat.cast", slot: 0, name: "target", nullable: true, sources: [
                { scope: "", fields: [0] },
                { scope: "", fields: [1] },
                { scope: "", fields: [2] },
            ], target: "table:combat.ability", recordKey: [0], steps: [
                { field: 3, kind: 1, keys: [0] },
                { field: 1, kind: 1, keys: [0] },
            ] },
        ], bind: (value, refs) => {
            const row = value as Combat_OptionalAim;
            row._bindNullableRefTarget$(refs[0] as Combat_Phase | undefined);
        } },
        { name: "combat.Origin", ctor: Combat_Origin, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "combat.Phase", ctor: Combat_Phase, indexes: [
        ], plans: [
            { root: "combat.ability", slot: 0, name: "next", nullable: true, sources: [
                { scope: "", fields: [1] },
            ], target: "ancestor:combat.Machine", recordKey: [], steps: [
                { field: 1, kind: 1, keys: [0] },
            ] },
            { root: "combat.ability", slot: 1, name: "exit", nullable: false, sources: [
            ], target: "ancestor:combat.Machine", recordKey: [], steps: [
                { field: 10, kind: 0, keys: [] },
                { field: 0, kind: 0, keys: [] },
            ] },
        ], bind: (value, refs) => {
            const row = value as Combat_Phase;
            row._bindNullableRefNext$(refs[0] as Combat_Phase | undefined);
            row._bindRefExit$(refs[1] as Combat_Effect | undefined);
        } },
        { name: "combat.PhaseKey", ctor: Combat_PhaseKey, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "combat.PhaseUse", ctor: Combat_PhaseUse, indexes: [
        ], plans: [
            { root: "combat.cast", slot: 0, name: "target", nullable: false, sources: [
                { scope: "ancestor:combat.AbilityUse", fields: [0, 0] },
                { scope: "ancestor:combat.MachineUse", fields: [0] },
                { scope: "", fields: [0] },
            ], target: "table:combat.ability", recordKey: [0], steps: [
                { field: 3, kind: 1, keys: [0] },
                { field: 1, kind: 1, keys: [0] },
            ] },
            { root: "combat.cast", slot: 1, name: "byCode", nullable: false, sources: [
                { scope: "root", fields: [1] },
                { scope: "ancestor:combat.MachineUse", fields: [0] },
                { scope: "", fields: [0] },
            ], target: "table:combat.ability", recordKey: [1], steps: [
                { field: 3, kind: 1, keys: [0] },
                { field: 1, kind: 1, keys: [0] },
            ] },
            { root: "combat.cast", slot: 2, name: "origin", nullable: false, sources: [
            ], target: "root", recordKey: [], steps: [
                { field: 2, kind: 0, keys: [] },
            ] },
        ], bind: (value, refs) => {
            const row = value as Combat_PhaseUse;
            row._bindRefTarget$(refs[0] as Combat_Phase | undefined);
            row._bindRefByCode$(refs[1] as Combat_Phase | undefined);
            row._bindRefOrigin$(refs[2] as Combat_Origin | undefined);
        } },
        { name: "combat.PhaseVariant", ctor: Combat_PhaseVariant, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "combat.Reward", ctor: Combat_Reward, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "combat.Stance", ctor: Combat_Stance, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "combat.ability", ctor: Combat_Ability, indexes: [
            { field: 3, keys: [0] },
        ], plans: [
            { root: "combat.ability", slot: 0, name: "initial", nullable: false, sources: [
                { scope: "", fields: [4] },
                { scope: "", fields: [5] },
            ], target: "root", recordKey: [], steps: [
                { field: 3, kind: 1, keys: [0] },
                { field: 1, kind: 1, keys: [0] },
            ] },
            { root: "combat.ability", slot: 1, name: "spawn", nullable: false, sources: [
            ], target: "root", recordKey: [], steps: [
                { field: 2, kind: 0, keys: [] },
            ] },
        ], bind: (value, refs) => {
            const row = value as Combat_Ability;
            row._bindRefInitial$(refs[0] as Combat_Phase | undefined);
            row._bindRefSpawn$(refs[1] as Combat_Origin | undefined);
        } },
        { name: "combat.branchreward", ctor: Combat_Branchreward, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "combat.cast", ctor: Combat_Cast, indexes: [
        ], plans: [
            { root: "combat.cast", slot: 0, name: "book", nullable: false, sources: [
                { scope: "", fields: [5] },
                { scope: "", fields: [6] },
                { scope: "", fields: [7] },
            ], target: "table:combat.phasebook", recordKey: [0, 1, 2], steps: [
                { field: 5, kind: 0, keys: [] },
            ] },
            { root: "combat.cast", slot: 1, name: "bookByCode", nullable: false, sources: [
                { scope: "", fields: [8] },
                { scope: "", fields: [9] },
            ], target: "table:combat.phasebook", recordKey: [3, 4], steps: [
                { field: 5, kind: 0, keys: [] },
            ] },
            { root: "combat.cast", slot: 2, name: "bookRecord", nullable: false, sources: [
                { scope: "root", fields: [5] },
                { scope: "root", fields: [6] },
                { scope: "root", fields: [7] },
            ], target: "table:combat.phasebook", recordKey: [0, 1, 2], steps: [
            ] },
            { root: "combat.cast", slot: 3, name: "reward", nullable: false, sources: [
                { scope: "", fields: [10] },
            ], target: "table:combat.branchreward", recordKey: [0], steps: [
                { field: 1, kind: 0, keys: [] },
            ] },
        ], bind: (value, refs) => {
            const row = value as Combat_Cast;
            row._bindRefBook$(refs[0] as Combat_BookEntry | undefined);
            row._bindRefBookByCode$(refs[1] as Combat_BookEntry | undefined);
            row._bindRefBookRecord$(refs[2] as Combat_Phasebook | undefined);
            row._bindRefReward$(refs[3] as Combat_Reward | undefined);
        } },
        { name: "combat.encounter", ctor: Combat_Encounter, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "combat.phasebook", ctor: Combat_Phasebook, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "combat.timeline", ctor: Combat_Timeline, indexes: [
        ], plans: [
            { root: "combat.timeline", slot: 0, name: "eventKey", nullable: false, sources: [
                { scope: "", fields: [6] },
            ], target: "root", recordKey: [], steps: [
                { field: 1, kind: 2, keys: [] },
            ] },
            { root: "combat.timeline", slot: 1, name: "channel", nullable: false, sources: [
                { scope: "", fields: [7] },
            ], target: "root", recordKey: [], steps: [
                { field: 2, kind: 2, keys: [] },
            ] },
            { root: "combat.timeline", slot: 2, name: "slot", nullable: false, sources: [
                { scope: "", fields: [8] },
            ], target: "root", recordKey: [], steps: [
                { field: 3, kind: 2, keys: [] },
            ] },
            { root: "combat.timeline", slot: 3, name: "frame", nullable: false, sources: [
                { scope: "", fields: [9] },
            ], target: "root", recordKey: [], steps: [
                { field: 4, kind: 2, keys: [] },
            ] },
            { root: "combat.timeline", slot: 4, name: "airborne", nullable: false, sources: [
                { scope: "", fields: [10] },
            ], target: "root", recordKey: [], steps: [
                { field: 5, kind: 2, keys: [] },
            ] },
            { root: "combat.timeline", slot: 5, name: "default", nullable: false, sources: [
            ], target: "root", recordKey: [], steps: [
                { field: 11, kind: 0, keys: [] },
            ] },
        ], bind: (value, refs) => {
            const row = value as Combat_Timeline;
            row._bindRefEventKey$(refs[0] as Combat_Effect | undefined);
            row._bindRefChannel$(refs[1] as Combat_Effect | undefined);
            row._bindRefSlot$(refs[2] as Combat_Effect | undefined);
            row._bindRefFrame$(refs[3] as Combat_Effect | undefined);
            row._bindRefAirborne$(refs[4] as Combat_Effect | undefined);
            row._bindRefDefault$(refs[5] as Combat_Effect | undefined);
        } },
        { name: "equip.TestPackBean", ctor: Equip_TestPackBean, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "equip.ability", ctor: Equip_Ability, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "equip.equip_config", ctor: Equip_Equip_config, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "equip.jewelry", ctor: Equip_Jewelry, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "equip.jewelry_random", ctor: Equip_Jewelry_random, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "equip.jewelry_suit", ctor: Equip_Jewelry_suit, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "equip.jewelry_type", ctor: Equip_Jewelry_type, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "equip.rank", ctor: Equip_Rank, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "other.ArgCaptureMode", ctor: Other_ArgCaptureMode, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "other.DropItem", ctor: Other_DropItem, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "other.drop", ctor: Other_Drop, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "other.key_test", ctor: Other_Key_test, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "other.loot", ctor: Other_Loot, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "other.loot_item", ctor: Other_Loot_item, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "other.monster", ctor: Other_Monster, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "other.sign_in", ctor: Other_Sign_in, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "task.TestDefaultBean", ctor: Task_TestDefaultBean, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "task.complete_condition_type", ctor: Task_Complete_condition_type, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "task.completecondition.Chat", ctor: Task_Completecondition_Chat, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "task.completecondition.CollectItem", ctor: Task_Completecondition_CollectItem, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "task.completecondition.ConditionAnd", ctor: Task_Completecondition_ConditionAnd, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "task.completecondition.KillMonster", ctor: Task_Completecondition_KillMonster, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "task.completecondition.TalkNpc", ctor: Task_Completecondition_TalkNpc, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "task.completecondition.TestNoColumn", ctor: Task_Completecondition_TestNoColumn, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "task.completecondition.aa", ctor: Task_Completecondition_Aa, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "task.task", ctor: Task_Task, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "task.task2", ctor: Task_Task2, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
        { name: "task.task_extra_exp", ctor: Task_Task_extra_exp, indexes: [
        ], plans: [
        ], bind: (value, refs) => {
        } },
    ]);
    resolver.table("ai.ai", [...Ai_Ai.All().values()]);
    resolver.table("ai.ai_action", [...Ai_Ai_action.All().values()]);
    resolver.table("ai.ai_condition", [...Ai_Ai_condition.All().values()]);
    resolver.table("combat.ability", [...Combat_Ability.All().values()]);
    resolver.table("combat.branchreward", [...Combat_Branchreward.All().values()]);
    resolver.table("combat.cast", [...Combat_Cast.All().values()]);
    resolver.table("combat.encounter", [...Combat_Encounter.All().values()]);
    resolver.table("combat.phasebook", [...Combat_Phasebook.All().values()]);
    resolver.table("combat.timeline", [...Combat_Timeline.All().values()]);
    resolver.table("equip.ability", [...Equip_Ability.All().values()]);
    resolver.table("equip.equip_config", [...Equip_Equip_config.All().values()]);
    resolver.table("equip.jewelry", [...Equip_Jewelry.All().values()]);
    resolver.table("equip.jewelry_random", [...Equip_Jewelry_random.All().values()]);
    resolver.table("equip.jewelry_suit", [...Equip_Jewelry_suit.All().values()]);
    resolver.table("equip.jewelry_type", [...Equip_Jewelry_type.All().values()]);
    resolver.table("equip.rank", [...Equip_Rank.All().values()]);
    resolver.table("other.ArgCaptureMode", [...Other_ArgCaptureMode.All().values()]);
    resolver.table("other.drop", [...Other_Drop.All().values()]);
    resolver.table("other.key_test", [...Other_Key_test.All().values()]);
    resolver.table("other.loot", [...Other_Loot.All().values()]);
    resolver.table("other.loot_item", [...Other_Loot_item.All().values()]);
    resolver.table("other.monster", [...Other_Monster.All().values()]);
    resolver.table("other.sign_in", [...Other_Sign_in.All().values()]);
    resolver.table("task.complete_condition_type", [...Task_Complete_condition_type.All().values()]);
    resolver.table("task.task", [...Task_Task.All().values()]);
    resolver.table("task.task2", [...Task_Task2.All().values()]);
    resolver.table("task.task_extra_exp", [...Task_Task_extra_exp.All().values()]);
    resolver.resolve();
}

type $RelationSource$ = { scope: string; fields: readonly number[] };
type $RelationStep$ = { field: number; kind: number; keys: readonly number[] };
type $RelationPlan$ = { root: string; slot: number; name: string; nullable: boolean; sources: readonly $RelationSource$[]; target: string; recordKey: readonly number[]; steps: readonly $RelationStep$[] };
interface $RelationValue$ { $relationFields$(): readonly unknown[] }
type $RelationType$ = { name: string; ctor: Function; indexes: readonly { field: number; keys: readonly number[] }[]; plans: readonly $RelationPlan$[]; bind: (value: object, refs: readonly unknown[]) => void };
type $RelationOccurrence$ = { value: object; root: object; table: string; parent: $RelationOccurrence$ | undefined };

function $relationKey$(parts: readonly unknown[]): string {
    return JSON.stringify(parts, (_key, value: unknown) => {
        if (typeof value === "bigint") return { $long: value.toString() };
        return value !== null && typeof value === "object" && "$relationFields$" in value
            ? (value as $RelationValue$).$relationFields$() : value;
    });
}

class $RelationResolver$ {
    private readonly types = new Map<Function, $RelationType$>();
    private readonly tables = new Map<string, readonly object[]>();
    private readonly fields = new Map<object, readonly unknown[]>();
    private readonly indexes = new Map<readonly unknown[], Map<string, Map<string, object>>>();
    private readonly maps = new Map<Map<unknown, unknown>, Map<string, unknown>>();
    private readonly occurrences: $RelationOccurrence$[] = [];
    constructor(private readonly errors: LoadErrors, types: readonly $RelationType$[]) {
        for (const type of types) this.types.set(type.ctor, type);
    }
    table(name: string, rows: readonly object[]): void { this.tables.set(name, rows); }
    private read(value: object): readonly unknown[] {
        let fields = this.fields.get(value);
        if (!fields) { fields = (value as $RelationValue$).$relationFields$(); this.fields.set(value, fields); }
        return fields;
    }
    resolve(): void {
        for (const [table, rows] of this.tables) for (const row of rows) this.walk(row, row, table);
        for (const at of this.occurrences) {
            const type = this.types.get(at.value.constructor)!;
            const refs: unknown[] = [];
            for (const plan of type.plans) if (plan.root === at.table) refs[plan.slot] = this.bind(at, type, plan);
            type.bind(at.value, refs);
        }
    }
    private bind(at: $RelationOccurrence$, type: $RelationType$, plan: $RelationPlan$): unknown {
        const keys: unknown[] = [];
        let valid = true;
        for (const source of plan.sources) {
            let value: unknown = this.scope(source.scope, at);
            if (value === undefined) { valid = false; continue; }
            for (const field of source.fields) value = this.read(value as object)[field];
            keys.push(value);
        }
        const external = plan.target.startsWith("table:");
        let target: unknown = external ? this.tables.get(plan.target.substring(6)) : this.scope(plan.target, at);
        if (!valid || target === undefined) throw Error("Missing reference scope: " + type.name + "." + plan.name);
        let used = 0;
        if (external) { target = this.index(target as readonly object[], plan.recordKey).get($relationKey$(keys.slice(0, plan.recordKey.length))); used = plan.recordKey.length; }
        for (const step of plan.steps) {
            if (target === undefined) break;
            const member = this.read(target as object)[step.field];
            if (step.kind === 0) target = member;
            else if (step.kind === 1) target = this.index(member as readonly object[], step.keys).get($relationKey$(keys.slice(used, used + step.keys.length)));
            else target = this.map(member as Map<unknown, unknown>).get($relationKey$([keys[used]]));
            used += step.kind === 2 ? 1 : step.keys.length;
        }
        if (target === undefined && !plan.nullable) this.errors.RefNull(type.name, at.table, plan.name);
        return target;
    }
    private scope(scope: string, at: $RelationOccurrence$): object | undefined {
        if (!scope) return at.value;
        if (scope === "root") return at.root;
        for (let current: $RelationOccurrence$ | undefined = at; current; current = current.parent) {
            const value = current.value;
            if ("ancestor:" + this.types.get(value.constructor)!.name === scope) return value;
        }
        return undefined;
    }
    private walk(value: unknown, root: object, table: string, parent?: $RelationOccurrence$): void {
        if (value === null || typeof value !== "object") return;
        const type = this.types.get(value.constructor);
        if (type) {
            const at = {value, root, table, parent};
            if (type.plans.length) this.occurrences.push(at);
            const fields = this.read(value);
            for (const index of type.indexes) this.index(fields[index.field] as readonly object[], index.keys);
            for (const field of fields) this.walk(field, root, table, at);
        } else if (Array.isArray(value)) {
            for (const item of value) this.walk(item, root, table, parent);
        } else if (value instanceof Map) {
            this.map(value);
            for (const [key, item] of value) { this.walk(key, root, table, parent); this.walk(item, root, table, parent); }
        }
    }
    private index(rows: readonly object[], keys: readonly number[]): Map<string, object> {
        let definitions = this.indexes.get(rows);
        if (!definitions) { definitions = new Map(); this.indexes.set(rows, definitions); }
        const definition = keys.join(",");
        let index = definitions.get(definition);
        if (!index) {
            index = new Map(); definitions.set(definition, index);
            for (const row of rows) {
                const fields = this.read(row); const key = $relationKey$(keys.map(field => fields[field]));
                if (index.has(key)) throw Error("Duplicate local index key: " + key);
                index.set(key, row);
            }
        }
        return index;
    }
    private map(rows: Map<unknown, unknown>): Map<string, unknown> {
        let index = this.maps.get(rows);
        if (!index) {
            index = new Map(); this.maps.set(rows, index);
            for (const [key, value] of rows) {
                const canonical = $relationKey$([key]);
                if (index.has(canonical)) throw Error("Duplicate map key: " + canonical);
                index.set(canonical, value);
            }
        }
        return index;
    }
}




}


