namespace Config.Combat
{
    public partial class DAbilityUse
    {
        internal static DAbilityUse _create(ConfigReader reader)
        {
            var valueBinding = Combat.DBinding._create(reader);
            int Count_valueMachines = reader.ReadCount(0);
            IReadOnlyList<Combat.DMachineUse> valueMachines;
            if (Count_valueMachines == 0) {
                valueMachines = Array.Empty<Combat.DMachineUse>();
            }
            else {
                var values_valueMachines = new List<Combat.DMachineUse>(Count_valueMachines);
                for (int i = 0; i < Count_valueMachines; i++) {
                    values_valueMachines.Add(Combat.DMachineUse._create(reader));
                }
                valueMachines = values_valueMachines.AsReadOnly();
            }
            return new DAbilityUse {
                Binding = valueBinding,
                Machines = valueMachines,
            };
        }

        public override int GetHashCode()
        {
            return Binding.GetHashCode() + Machines.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DAbilityUse o && Binding.Equals(o.Binding) && Machines.Equals(o.Machines);
        }

        public override string ToString()
        {
            return "(" + Binding + "," + StringUtil.ToString(Machines) + ")";
        }

        internal void _resolve(LoadErrors errors, ConfigMgr config)
        {
            foreach(var e in Machines) {
                e._resolve(errors, config);
            }
        }

    }

    public partial class DAnchor
    {
        internal static DAnchor _create(ConfigReader reader)
        {
            return new DAnchor();
        }

        public override int GetHashCode()
        {
            return this.GetType().GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DAnchor;
        }


    }

    public partial class DAnchorUse
    {
        internal static DAnchorUse _create(ConfigReader reader)
        {
            return new DAnchorUse();
        }

        public override int GetHashCode()
        {
            return this.GetType().GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DAnchorUse;
        }

        internal void _resolve(LoadErrors errors, ConfigMgr config)
        {
        }

    }

    public partial class DBinding
    {
        internal static DBinding _create(ConfigReader reader)
        {
            var valueAbilityId = reader.ReadInt32();
            return new DBinding {
                AbilityId = valueAbilityId,
            };
        }

        public override int GetHashCode()
        {
            return AbilityId.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DBinding o && AbilityId.Equals(o.AbilityId);
        }

        public override string ToString()
        {
            return "(" + AbilityId + ")";
        }


    }

    public partial class DBookEntry
    {
        internal static DBookEntry _create(ConfigReader reader)
        {
            var valueName = reader.ReadStringInPool();
            return new DBookEntry {
                Name = valueName,
            };
        }

        public override int GetHashCode()
        {
            return Name.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DBookEntry o && Name.Equals(o.Name);
        }

        public override string ToString()
        {
            return "(" + Name + ")";
        }


    }

    public partial class DCombo
    {
        internal static DCombo _create(ConfigReader reader)
        {
            var valueWeapon = reader.ReadStringInPool();
            var valueTier = reader.ReadInt32();
            var valueAirborne = reader.ReadBool();
            var valueEffect = Combat.DEffect._create(reader);
            return new DCombo {
                Weapon = valueWeapon,
                Tier = valueTier,
                Airborne = valueAirborne,
                Effect = valueEffect,
            };
        }

        public override int GetHashCode()
        {
            return Weapon.GetHashCode() + Tier.GetHashCode() + Airborne.GetHashCode() + Effect.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DCombo o && Weapon.Equals(o.Weapon) && Tier.Equals(o.Tier) && Airborne.Equals(o.Airborne) && Effect.Equals(o.Effect);
        }

        public override string ToString()
        {
            return "(" + Weapon + "," + Tier + "," + Airborne + "," + Effect + ")";
        }


    }

    public partial class DExit
    {
        internal static DExit _create(ConfigReader reader)
        {
            var valueEffect = Combat.DEffect._create(reader);
            return new DExit {
                Effect = valueEffect,
            };
        }

        public override int GetHashCode()
        {
            return Effect.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DExit o && Effect.Equals(o.Effect);
        }

        public override string ToString()
        {
            return "(" + Effect + ")";
        }


    }

    public partial class DFrame
    {
        internal static DFrame _create(ConfigReader reader)
        {
            var valueTick = reader.ReadInt64();
            var valueEffect = Combat.DEffect._create(reader);
            return new DFrame {
                Tick = valueTick,
                Effect = valueEffect,
            };
        }

        public override int GetHashCode()
        {
            return Tick.GetHashCode() + Effect.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DFrame o && Tick.Equals(o.Tick) && Effect.Equals(o.Effect);
        }

        public override string ToString()
        {
            return "(" + Tick + "," + Effect + ")";
        }


    }

    public partial class DImpact
    {
        internal static DImpact _create(ConfigReader reader)
        {
            var valueOrdinal = reader.ReadInt32();
            var valueDamage = reader.ReadInt32();
            return new DImpact {
                Ordinal = valueOrdinal,
                Damage = valueDamage,
            };
        }

        public override int GetHashCode()
        {
            return Ordinal.GetHashCode() + Damage.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DImpact o && Ordinal.Equals(o.Ordinal) && Damage.Equals(o.Damage);
        }

        public override string ToString()
        {
            return "(" + Ordinal + "," + Damage + ")";
        }


    }

    public partial class DMachine
    {
        internal static DMachine _create(ConfigReader reader)
        {
            var valueCode = reader.ReadStringInPool();
            int Count_valuePhases = reader.ReadCount(0);
            IReadOnlyList<Combat.DPhase> valuePhases;
            if (Count_valuePhases == 0) {
                valuePhases = Array.Empty<Combat.DPhase>();
            }
            else {
                var values_valuePhases = new List<Combat.DPhase>(Count_valuePhases);
                for (int i = 0; i < Count_valuePhases; i++) {
                    values_valuePhases.Add(Combat.DPhase._create(reader));
                }
                valuePhases = values_valuePhases.AsReadOnly();
            }
            int Count_valueImpacts = reader.ReadCount(0);
            IReadOnlyList<Combat.DImpact> valueImpacts;
            if (Count_valueImpacts == 0) {
                valueImpacts = Array.Empty<Combat.DImpact>();
            }
            else {
                var values_valueImpacts = new List<Combat.DImpact>(Count_valueImpacts);
                for (int i = 0; i < Count_valueImpacts; i++) {
                    values_valueImpacts.Add(Combat.DImpact._create(reader));
                }
                valueImpacts = values_valueImpacts.AsReadOnly();
            }
            int Count_valueFrames = reader.ReadCount(0);
            IReadOnlyList<Combat.DFrame> valueFrames;
            if (Count_valueFrames == 0) {
                valueFrames = Array.Empty<Combat.DFrame>();
            }
            else {
                var values_valueFrames = new List<Combat.DFrame>(Count_valueFrames);
                for (int i = 0; i < Count_valueFrames; i++) {
                    values_valueFrames.Add(Combat.DFrame._create(reader));
                }
                valueFrames = values_valueFrames.AsReadOnly();
            }
            int Count_valueStances = reader.ReadCount(0);
            IReadOnlyList<Combat.DStance> valueStances;
            if (Count_valueStances == 0) {
                valueStances = Array.Empty<Combat.DStance>();
            }
            else {
                var values_valueStances = new List<Combat.DStance>(Count_valueStances);
                for (int i = 0; i < Count_valueStances; i++) {
                    values_valueStances.Add(Combat.DStance._create(reader));
                }
                valueStances = values_valueStances.AsReadOnly();
            }
            int Count_valueVariants = reader.ReadCount(0);
            IReadOnlyList<Combat.DPhaseVariant> valueVariants;
            if (Count_valueVariants == 0) {
                valueVariants = Array.Empty<Combat.DPhaseVariant>();
            }
            else {
                var values_valueVariants = new List<Combat.DPhaseVariant>(Count_valueVariants);
                for (int i = 0; i < Count_valueVariants; i++) {
                    values_valueVariants.Add(Combat.DPhaseVariant._create(reader));
                }
                valueVariants = values_valueVariants.AsReadOnly();
            }
            int Count_valueCombos = reader.ReadCount(0);
            IReadOnlyList<Combat.DCombo> valueCombos;
            if (Count_valueCombos == 0) {
                valueCombos = Array.Empty<Combat.DCombo>();
            }
            else {
                var values_valueCombos = new List<Combat.DCombo>(Count_valueCombos);
                for (int i = 0; i < Count_valueCombos; i++) {
                    values_valueCombos.Add(Combat.DCombo._create(reader));
                }
                valueCombos = values_valueCombos.AsReadOnly();
            }
            int Count_valueMarkers = reader.ReadCount(0);
            IReadOnlyList<Combat.DMarker> valueMarkers;
            if (Count_valueMarkers == 0) {
                valueMarkers = Array.Empty<Combat.DMarker>();
            }
            else {
                var values_valueMarkers = new List<Combat.DMarker>(Count_valueMarkers);
                for (int i = 0; i < Count_valueMarkers; i++) {
                    values_valueMarkers.Add(Combat.DMarker._create(reader));
                }
                valueMarkers = values_valueMarkers.AsReadOnly();
            }
            var valueAnchor = Combat.DAnchor._create(reader);
            int Count_valueAnchors = reader.ReadCount(0);
            IReadOnlyList<Combat.DAnchorUse> valueAnchors;
            if (Count_valueAnchors == 0) {
                valueAnchors = Array.Empty<Combat.DAnchorUse>();
            }
            else {
                var values_valueAnchors = new List<Combat.DAnchorUse>(Count_valueAnchors);
                for (int i = 0; i < Count_valueAnchors; i++) {
                    values_valueAnchors.Add(Combat.DAnchorUse._create(reader));
                }
                valueAnchors = values_valueAnchors.AsReadOnly();
            }
            var valueOnExit = Combat.DExit._create(reader);
            var valueInitial = reader.ReadStringInPool();
            var valueImpact = reader.ReadInt32();
            var valueFrame = reader.ReadInt64();
            var valueStance = reader.ReadBool();
            var valueVariant = Combat.DPhaseKey._create(reader);
            var valueWeapon = reader.ReadStringInPool();
            var valueTier = reader.ReadInt32();
            var valueAirborne = reader.ReadBool();
            return new DMachine {
                Code = valueCode,
                Phases = valuePhases,
                Impacts = valueImpacts,
                Frames = valueFrames,
                Stances = valueStances,
                Variants = valueVariants,
                Combos = valueCombos,
                Markers = valueMarkers,
                Anchor = valueAnchor,
                Anchors = valueAnchors,
                OnExit = valueOnExit,
                Initial = valueInitial,
                Impact = valueImpact,
                Frame = valueFrame,
                Stance = valueStance,
                Variant = valueVariant,
                Weapon = valueWeapon,
                Tier = valueTier,
                Airborne = valueAirborne,
            };
        }

        public override int GetHashCode()
        {
            return Code.GetHashCode() + Phases.GetHashCode() + Impacts.GetHashCode() + Frames.GetHashCode() + Stances.GetHashCode() + Variants.GetHashCode() + Combos.GetHashCode() + Markers.GetHashCode() + Anchor.GetHashCode() + Anchors.GetHashCode() + OnExit.GetHashCode() + Initial.GetHashCode() + Impact.GetHashCode() + Frame.GetHashCode() + Stance.GetHashCode() + Variant.GetHashCode() + Weapon.GetHashCode() + Tier.GetHashCode() + Airborne.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DMachine o && Code.Equals(o.Code) && Phases.Equals(o.Phases) && Impacts.Equals(o.Impacts) && Frames.Equals(o.Frames) && Stances.Equals(o.Stances) && Variants.Equals(o.Variants) && Combos.Equals(o.Combos) && Markers.Equals(o.Markers) && Anchor.Equals(o.Anchor) && Anchors.Equals(o.Anchors) && OnExit.Equals(o.OnExit) && Initial.Equals(o.Initial) && Impact.Equals(o.Impact) && Frame.Equals(o.Frame) && Stance.Equals(o.Stance) && Variant.Equals(o.Variant) && Weapon.Equals(o.Weapon) && Tier.Equals(o.Tier) && Airborne.Equals(o.Airborne);
        }

        public override string ToString()
        {
            return "(" + Code + "," + StringUtil.ToString(Phases) + "," + StringUtil.ToString(Impacts) + "," + StringUtil.ToString(Frames) + "," + StringUtil.ToString(Stances) + "," + StringUtil.ToString(Variants) + "," + StringUtil.ToString(Combos) + "," + StringUtil.ToString(Markers) + "," + Anchor + "," + StringUtil.ToString(Anchors) + "," + OnExit + "," + Initial + "," + Impact + "," + Frame + "," + Stance + "," + Variant + "," + Weapon + "," + Tier + "," + Airborne + ")";
        }

        internal void _resolve(LoadErrors errors, ConfigMgr config)
        {
            foreach(var e in Phases) {
                e._resolve(errors, config);
            }
            foreach(var e in Anchors) {
                e._resolve(errors, config);
            }
        }

    }

    public partial class DMachineUse
    {
        internal static DMachineUse _create(ConfigReader reader)
        {
            var valueMachineKey = reader.ReadStringInPool();
            int Count_valueTargets = reader.ReadCount(0);
            IReadOnlyList<Combat.DPhaseUse> valueTargets;
            if (Count_valueTargets == 0) {
                valueTargets = Array.Empty<Combat.DPhaseUse>();
            }
            else {
                var values_valueTargets = new List<Combat.DPhaseUse>(Count_valueTargets);
                for (int i = 0; i < Count_valueTargets; i++) {
                    values_valueTargets.Add(Combat.DPhaseUse._create(reader));
                }
                valueTargets = values_valueTargets.AsReadOnly();
            }
            return new DMachineUse {
                MachineKey = valueMachineKey,
                Targets = valueTargets,
            };
        }

        public override int GetHashCode()
        {
            return MachineKey.GetHashCode() + Targets.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DMachineUse o && MachineKey.Equals(o.MachineKey) && Targets.Equals(o.Targets);
        }

        public override string ToString()
        {
            return "(" + MachineKey + "," + StringUtil.ToString(Targets) + ")";
        }

        internal void _resolve(LoadErrors errors, ConfigMgr config)
        {
            foreach(var e in Targets) {
                e._resolve(errors, config);
            }
        }

    }

    public partial class DMarker
    {
        internal static DMarker _create(ConfigReader reader)
        {
            var valueKey = reader.ReadStringInPool();
            return new DMarker {
                Key = valueKey,
            };
        }

        public override int GetHashCode()
        {
            return Key.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DMarker o && Key.Equals(o.Key);
        }

        public override string ToString()
        {
            return "(" + Key + ")";
        }


    }

    public partial class DNestedPhase
    {
        internal static DNestedPhase _create(ConfigReader reader)
        {
            var valueKey = reader.ReadStringInPool();
            var valueAction = Combat.DCommand._create(reader);
            return new DNestedPhase {
                Key = valueKey,
                Action = valueAction,
            };
        }

        public override int GetHashCode()
        {
            return Key.GetHashCode() + Action.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DNestedPhase o && Key.Equals(o.Key) && Action.Equals(o.Action);
        }

        public override string ToString()
        {
            return "(" + Key + "," + Action + ")";
        }

        internal void _resolve(LoadErrors errors, ConfigMgr config)
        {
            Action._resolve(errors, config);
        }

    }

    public partial class DOptionalAim
    {
        internal static DOptionalAim _create(ConfigReader reader)
        {
            var valueAbilityId = reader.ReadInt32();
            var valueMachineKey = reader.ReadStringInPool();
            var valuePhaseKey = reader.ReadStringInPool();
            return new DOptionalAim {
                AbilityId = valueAbilityId,
                MachineKey = valueMachineKey,
                PhaseKey = valuePhaseKey,
            };
        }

        public override int GetHashCode()
        {
            return AbilityId.GetHashCode() + MachineKey.GetHashCode() + PhaseKey.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DOptionalAim o && AbilityId.Equals(o.AbilityId) && MachineKey.Equals(o.MachineKey) && PhaseKey.Equals(o.PhaseKey);
        }

        public override string ToString()
        {
            return "(" + AbilityId + "," + MachineKey + "," + PhaseKey + ")";
        }

        internal void _resolve(LoadErrors errors, ConfigMgr config)
        {
        }

    }

    public partial class DOrigin
    {
        internal static DOrigin _create(ConfigReader reader)
        {
            var valueX = reader.ReadInt32();
            var valueY = reader.ReadInt32();
            return new DOrigin {
                X = valueX,
                Y = valueY,
            };
        }

        public override int GetHashCode()
        {
            return X.GetHashCode() + Y.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DOrigin o && X.Equals(o.X) && Y.Equals(o.Y);
        }

        public override string ToString()
        {
            return "(" + X + "," + Y + ")";
        }


    }

    public partial class DPhase
    {
        internal static DPhase _create(ConfigReader reader)
        {
            var valueKey = reader.ReadStringInPool();
            var valueNext = reader.ReadStringInPool();
            return new DPhase {
                Key = valueKey,
                Next = valueNext,
            };
        }

        public override int GetHashCode()
        {
            return Key.GetHashCode() + Next.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DPhase o && Key.Equals(o.Key) && Next.Equals(o.Next);
        }

        public override string ToString()
        {
            return "(" + Key + "," + Next + ")";
        }

        internal void _resolve(LoadErrors errors, ConfigMgr config)
        {
        }

    }

    public partial class DPhaseKey
    {
        internal static DPhaseKey _create(ConfigReader reader)
        {
            var valueName = reader.ReadStringInPool();
            var valueVariant = reader.ReadInt32();
            return new DPhaseKey {
                Name = valueName,
                Variant = valueVariant,
            };
        }

        public override int GetHashCode()
        {
            return Name.GetHashCode() + Variant.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DPhaseKey o && Name.Equals(o.Name) && Variant.Equals(o.Variant);
        }

        public override string ToString()
        {
            return "(" + Name + "," + Variant + ")";
        }


    }

    public partial class DPhaseUse
    {
        internal static DPhaseUse _create(ConfigReader reader)
        {
            var valuePhaseKey = reader.ReadStringInPool();
            return new DPhaseUse {
                PhaseKey = valuePhaseKey,
            };
        }

        public override int GetHashCode()
        {
            return PhaseKey.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DPhaseUse o && PhaseKey.Equals(o.PhaseKey);
        }

        public override string ToString()
        {
            return "(" + PhaseKey + ")";
        }

        internal void _resolve(LoadErrors errors, ConfigMgr config)
        {
        }

    }

    public partial class DPhaseVariant
    {
        internal static DPhaseVariant _create(ConfigReader reader)
        {
            var valueKey = Combat.DPhaseKey._create(reader);
            var valueEffect = Combat.DEffect._create(reader);
            return new DPhaseVariant {
                Key = valueKey,
                Effect = valueEffect,
            };
        }

        public override int GetHashCode()
        {
            return Key.GetHashCode() + Effect.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DPhaseVariant o && Key.Equals(o.Key) && Effect.Equals(o.Effect);
        }

        public override string ToString()
        {
            return "(" + Key + "," + Effect + ")";
        }


    }

    public partial class DReward
    {
        internal static DReward _create(ConfigReader reader)
        {
            var valueCount = reader.ReadInt32();
            return new DReward {
                Count = valueCount,
            };
        }

        public override int GetHashCode()
        {
            return Count.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DReward o && Count.Equals(o.Count);
        }

        public override string ToString()
        {
            return "(" + Count + ")";
        }


    }

    public partial class DStance
    {
        internal static DStance _create(ConfigReader reader)
        {
            var valueAirborne = reader.ReadBool();
            var valueEffect = Combat.DEffect._create(reader);
            return new DStance {
                Airborne = valueAirborne,
                Effect = valueEffect,
            };
        }

        public override int GetHashCode()
        {
            return Airborne.GetHashCode() + Effect.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DStance o && Airborne.Equals(o.Airborne) && Effect.Equals(o.Effect);
        }

        public override string ToString()
        {
            return "(" + Airborne + "," + Effect + ")";
        }


    }

    public partial class DAbility
    {
        internal static int PrimaryKey(DAbility self) => self.Id;
        internal static void InitializeAll(List<DAbility> list, LoadErrors errors, ConfigMgr config)
        {
            int count = list.Count;
            var s_all = new Dictionary<int, DAbility>(count);
            var s_codeMap = new Dictionary<string, DAbility>(count);
            foreach (var self in list) {
                var key = PrimaryKey(self);
                s_all.Add(key, self);
                {
                    var uniqueKey = self.Code;
                    if (!s_codeMap.TryAdd(uniqueKey, self)) {
                        errors.DuplicateUniqueKey("combat.ability", "code", key, uniqueKey, config.Explain("combat.ability", key));
                    }
                }
            }
            config._combat_dability_All = new System.Collections.ObjectModel.ReadOnlyDictionary<int, DAbility>(s_all);
            config._combat_dability_CodeMap = new System.Collections.ObjectModel.ReadOnlyDictionary<string, DAbility>(s_codeMap);
            config._combat_dability_Rows = list.AsReadOnly();
        }

        internal static DAbility _create(ConfigReader reader)
        {
            var valueId = reader.ReadInt32();
            var valueCode = reader.ReadStringInPool();
            var valueOrigin = Combat.DOrigin._create(reader);
            int Count_valueMachines = reader.ReadCount(0);
            IReadOnlyList<Combat.DMachine> valueMachines;
            if (Count_valueMachines == 0) {
                valueMachines = Array.Empty<Combat.DMachine>();
            }
            else {
                var values_valueMachines = new List<Combat.DMachine>(Count_valueMachines);
                for (int i = 0; i < Count_valueMachines; i++) {
                    values_valueMachines.Add(Combat.DMachine._create(reader));
                }
                valueMachines = values_valueMachines.AsReadOnly();
            }
            var valueStartMachine = reader.ReadStringInPool();
            var valueStartPhase = reader.ReadStringInPool();
            return new DAbility {
                Id = valueId,
                Code = valueCode,
                Origin = valueOrigin,
                Machines = valueMachines,
                StartMachine = valueStartMachine,
                StartPhase = valueStartPhase,
            };
        }

        public override int GetHashCode()
        {
            return Id.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DAbility o && Id.Equals(o.Id);
        }

        public override string ToString()
        {
            return "(" + Id + "," + Code + "," + Origin + "," + StringUtil.ToString(Machines) + "," + StartMachine + "," + StartPhase + ")";
        }

        internal void _resolve(LoadErrors errors, ConfigMgr config)
        {
            foreach(var e in Machines) {
                e._resolve(errors, config);
            }
        }

    }

    public partial class DBranchreward
    {
        internal static Combat.DPhaseKey PrimaryKey(DBranchreward self) => self.Key;
        internal static void InitializeAll(List<DBranchreward> list, LoadErrors errors, ConfigMgr config)
        {
            int count = list.Count;
            var s_all = new Dictionary<Combat.DPhaseKey, DBranchreward>(count);
            foreach (var self in list) {
                var key = PrimaryKey(self);
                s_all.Add(key, self);
            }
            config._combat_dbranchreward_All = new System.Collections.ObjectModel.ReadOnlyDictionary<Combat.DPhaseKey, DBranchreward>(s_all);
            config._combat_dbranchreward_Rows = list.AsReadOnly();
        }

        internal static DBranchreward _create(ConfigReader reader)
        {
            var valueKey = Combat.DPhaseKey._create(reader);
            var valueReward = Combat.DReward._create(reader);
            return new DBranchreward {
                Key = valueKey,
                Reward = valueReward,
            };
        }

        public override int GetHashCode()
        {
            return Key.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DBranchreward o && Key.Equals(o.Key);
        }

        public override string ToString()
        {
            return "(" + Key + "," + Reward + ")";
        }


    }

    public partial class DCast
    {
        internal static int PrimaryKey(DCast self) => self.Id;
        internal static void InitializeAll(List<DCast> list, LoadErrors errors, ConfigMgr config)
        {
            int count = list.Count;
            var s_all = new Dictionary<int, DCast>(count);
            foreach (var self in list) {
                var key = PrimaryKey(self);
                s_all.Add(key, self);
            }
            config._combat_dcast_All = new System.Collections.ObjectModel.ReadOnlyDictionary<int, DCast>(s_all);
            config._combat_dcast_Rows = list.AsReadOnly();
        }

        internal static DCast _create(ConfigReader reader)
        {
            var valueId = reader.ReadInt32();
            var valueAbilityCode = reader.ReadStringInPool();
            var valueOrigin = Combat.DOrigin._create(reader);
            int Count_valueUses = reader.ReadCount(0);
            IReadOnlyList<Combat.DAbilityUse> valueUses;
            if (Count_valueUses == 0) {
                valueUses = Array.Empty<Combat.DAbilityUse>();
            }
            else {
                var values_valueUses = new List<Combat.DAbilityUse>(Count_valueUses);
                for (int i = 0; i < Count_valueUses; i++) {
                    values_valueUses.Add(Combat.DAbilityUse._create(reader));
                }
                valueUses = values_valueUses.AsReadOnly();
            }
            int Count_valueOptional = reader.ReadCount(0);
            IReadOnlyList<Combat.DOptionalAim> valueOptional;
            if (Count_valueOptional == 0) {
                valueOptional = Array.Empty<Combat.DOptionalAim>();
            }
            else {
                var values_valueOptional = new List<Combat.DOptionalAim>(Count_valueOptional);
                for (int i = 0; i < Count_valueOptional; i++) {
                    values_valueOptional.Add(Combat.DOptionalAim._create(reader));
                }
                valueOptional = values_valueOptional.AsReadOnly();
            }
            var valueSchool = reader.ReadStringInPool();
            var valueTier = reader.ReadInt32();
            var valueAirborne = reader.ReadBool();
            var valueBookCode = reader.ReadStringInPool();
            var valueEdition = reader.ReadInt32();
            var valueRewardKey = Combat.DPhaseKey._create(reader);
            return new DCast {
                Id = valueId,
                AbilityCode = valueAbilityCode,
                Origin = valueOrigin,
                Uses = valueUses,
                Optional = valueOptional,
                School = valueSchool,
                Tier = valueTier,
                Airborne = valueAirborne,
                BookCode = valueBookCode,
                Edition = valueEdition,
                RewardKey = valueRewardKey,
            };
        }

        public override int GetHashCode()
        {
            return Id.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DCast o && Id.Equals(o.Id);
        }

        public override string ToString()
        {
            return "(" + Id + "," + AbilityCode + "," + Origin + "," + StringUtil.ToString(Uses) + "," + StringUtil.ToString(Optional) + "," + School + "," + Tier + "," + Airborne + "," + BookCode + "," + Edition + "," + RewardKey + ")";
        }

        internal void _resolve(LoadErrors errors, ConfigMgr config)
        {
            foreach(var e in Uses) {
                e._resolve(errors, config);
            }
            foreach(var e in Optional) {
                e._resolve(errors, config);
            }
        }

    }

    public partial class DEncounter
    {
        internal static int PrimaryKey(DEncounter self) => self.Id;
        internal static void InitializeAll(List<DEncounter> list, LoadErrors errors, ConfigMgr config)
        {
            int count = list.Count;
            var s_all = new Dictionary<int, DEncounter>(count);
            foreach (var self in list) {
                var key = PrimaryKey(self);
                s_all.Add(key, self);
            }
            config._combat_dencounter_All = new System.Collections.ObjectModel.ReadOnlyDictionary<int, DEncounter>(s_all);
            config._combat_dencounter_Rows = list.AsReadOnly();
        }

        internal static DEncounter _create(ConfigReader reader)
        {
            var valueId = reader.ReadInt32();
            var valueCommand = Combat.DCommand._create(reader);
            return new DEncounter {
                Id = valueId,
                Command = valueCommand,
            };
        }

        public override int GetHashCode()
        {
            return Id.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DEncounter o && Id.Equals(o.Id);
        }

        public override string ToString()
        {
            return "(" + Id + "," + Command + ")";
        }

        internal void _resolve(LoadErrors errors, ConfigMgr config)
        {
            Command._resolve(errors, config);
        }

    }

    public partial class DPhasebook
    {
        internal static SchoolTierAirborneKey PrimaryKey(DPhasebook self) => new SchoolTierAirborneKey(self.School, self.Tier, self.Airborne);
        internal static void InitializeAll(List<DPhasebook> list, LoadErrors errors, ConfigMgr config)
        {
            int count = list.Count;
            var s_all = new Dictionary<SchoolTierAirborneKey, DPhasebook>(count);
            var s_codeEditionMap = new Dictionary<CodeEditionKey, DPhasebook>(count);
            foreach (var self in list) {
                var key = PrimaryKey(self);
                s_all.Add(key, self);
                {
                    var uniqueKey = new CodeEditionKey(self.Code, self.Edition);
                    if (!s_codeEditionMap.TryAdd(uniqueKey, self)) {
                        errors.DuplicateUniqueKey("combat.phasebook", "code,edition", key, uniqueKey, config.Explain("combat.phasebook", key));
                    }
                }
            }
            config._combat_dphasebook_All = new System.Collections.ObjectModel.ReadOnlyDictionary<SchoolTierAirborneKey, DPhasebook>(s_all);
            config._combat_dphasebook_CodeEditionMap = new System.Collections.ObjectModel.ReadOnlyDictionary<CodeEditionKey, DPhasebook>(s_codeEditionMap);
            config._combat_dphasebook_Rows = list.AsReadOnly();
        }

        internal static DPhasebook _create(ConfigReader reader)
        {
            var valueSchool = reader.ReadStringInPool();
            var valueTier = reader.ReadInt32();
            var valueAirborne = reader.ReadBool();
            var valueCode = reader.ReadStringInPool();
            var valueEdition = reader.ReadInt32();
            var valueEntry = Combat.DBookEntry._create(reader);
            return new DPhasebook {
                School = valueSchool,
                Tier = valueTier,
                Airborne = valueAirborne,
                Code = valueCode,
                Edition = valueEdition,
                Entry = valueEntry,
            };
        }

        public override int GetHashCode()
        {
            return School.GetHashCode() + Tier.GetHashCode() + Airborne.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DPhasebook o && School.Equals(o.School) && Tier.Equals(o.Tier) && Airborne.Equals(o.Airborne);
        }

        public override string ToString()
        {
            return "(" + School + "," + Tier + "," + Airborne + "," + Code + "," + Edition + "," + Entry + ")";
        }


    }

    public partial class DTimeline
    {
        internal static int PrimaryKey(DTimeline self) => self.Id;
        internal static void InitializeAll(List<DTimeline> list, LoadErrors errors, ConfigMgr config)
        {
            int count = list.Count;
            var s_all = new Dictionary<int, DTimeline>(count);
            foreach (var self in list) {
                var key = PrimaryKey(self);
                s_all.Add(key, self);
            }
            config._combat_dtimeline_All = new System.Collections.ObjectModel.ReadOnlyDictionary<int, DTimeline>(s_all);
            config._combat_dtimeline_Rows = list.AsReadOnly();
        }

        internal static DTimeline _create(ConfigReader reader)
        {
            var valueId = reader.ReadInt32();
            int Count_valueEvents = reader.ReadCount(0);
            var valueEvents = Count_valueEvents == 0 ? ConfigMap<Combat.DPhaseKey, Combat.DEffect>.Empty : new ConfigMap<Combat.DPhaseKey, Combat.DEffect>(Count_valueEvents);
            for (int i = 0; i < Count_valueEvents; i++) {
                var key = Combat.DPhaseKey._create(reader);
                var value = Combat.DEffect._create(reader);
                if (!valueEvents.TryAdd(key, value)) {
                    reader.Errors.DuplicateKey("combat.timeline.events", key);
                }
            }
            int Count_valueChannels = reader.ReadCount(0);
            var valueChannels = Count_valueChannels == 0 ? ConfigMap<string, Combat.DEffect>.Empty : new ConfigMap<string, Combat.DEffect>(Count_valueChannels);
            for (int i = 0; i < Count_valueChannels; i++) {
                var key = reader.ReadStringInPool();
                var value = Combat.DEffect._create(reader);
                if (!valueChannels.TryAdd(key, value)) {
                    reader.Errors.DuplicateKey("combat.timeline.channels", key);
                }
            }
            int Count_valueSlots = reader.ReadCount(0);
            var valueSlots = Count_valueSlots == 0 ? ConfigMap<int, Combat.DEffect>.Empty : new ConfigMap<int, Combat.DEffect>(Count_valueSlots);
            for (int i = 0; i < Count_valueSlots; i++) {
                var key = reader.ReadInt32();
                var value = Combat.DEffect._create(reader);
                if (!valueSlots.TryAdd(key, value)) {
                    reader.Errors.DuplicateKey("combat.timeline.slots", key);
                }
            }
            int Count_valueClock = reader.ReadCount(0);
            var valueClock = Count_valueClock == 0 ? ConfigMap<long, Combat.DEffect>.Empty : new ConfigMap<long, Combat.DEffect>(Count_valueClock);
            for (int i = 0; i < Count_valueClock; i++) {
                var key = reader.ReadInt64();
                var value = Combat.DEffect._create(reader);
                if (!valueClock.TryAdd(key, value)) {
                    reader.Errors.DuplicateKey("combat.timeline.clock", key);
                }
            }
            int Count_valueModes = reader.ReadCount(0);
            var valueModes = Count_valueModes == 0 ? ConfigMap<bool, Combat.DEffect>.Empty : new ConfigMap<bool, Combat.DEffect>(Count_valueModes);
            for (int i = 0; i < Count_valueModes; i++) {
                var key = reader.ReadBool();
                var value = Combat.DEffect._create(reader);
                if (!valueModes.TryAdd(key, value)) {
                    reader.Errors.DuplicateKey("combat.timeline.modes", key);
                }
            }
            var valueEventKey = Combat.DPhaseKey._create(reader);
            var valueChannel = reader.ReadStringInPool();
            var valueSlot = reader.ReadInt32();
            var valueFrame = reader.ReadInt64();
            var valueAirborne = reader.ReadBool();
            var valueFallback = Combat.DEffect._create(reader);
            return new DTimeline {
                Id = valueId,
                Events = valueEvents,
                Channels = valueChannels,
                Slots = valueSlots,
                Clock = valueClock,
                Modes = valueModes,
                EventKey = valueEventKey,
                Channel = valueChannel,
                Slot = valueSlot,
                Frame = valueFrame,
                Airborne = valueAirborne,
                Fallback = valueFallback,
            };
        }

        public override int GetHashCode()
        {
            return Id.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DTimeline o && Id.Equals(o.Id);
        }

        public override string ToString()
        {
            return "(" + Id + "," + Events + "," + Channels + "," + Slots + "," + Clock + "," + Modes + "," + EventKey + "," + Channel + "," + Slot + "," + Frame + "," + Airborne + "," + Fallback + ")";
        }

        internal void _resolve(LoadErrors errors, ConfigMgr config)
        {
        }

    }

    public partial interface DCommand
    {
        void _resolve(LoadErrors errors, ConfigMgr config) { }
        internal static DCommand _create(ConfigReader reader)
        {
            return reader.ReadStringInPool() switch {
                "StateMachine" => Combat.Command.DStateMachine._create(reader),
                "Jump" => Combat.Command.DJump._create(reader),
                "Noop" => Combat.Command.DNoop._create(reader),
                var impl => throw reader.Errors.UnknownVariant("combat.Command", impl)
            };
        }
    }

    public partial interface DEffect
    {
        internal static DEffect _create(ConfigReader reader)
        {
            return reader.ReadStringInPool() switch {
                "Damage" => Combat.Effect.DDamage._create(reader),
                "Heal" => Combat.Effect.DHeal._create(reader),
                "None" => Combat.Effect.DNone._create(reader),
                var impl => throw reader.Errors.UnknownVariant("combat.Effect", impl)
            };
        }
    }

}

namespace Config.Combat.Command
{
    public partial class DStateMachine
    {
        internal static DStateMachine _create(ConfigReader reader)
        {
            int Count_valuePhases = reader.ReadCount(0);
            IReadOnlyList<Combat.DNestedPhase> valuePhases;
            if (Count_valuePhases == 0) {
                valuePhases = Array.Empty<Combat.DNestedPhase>();
            }
            else {
                var values_valuePhases = new List<Combat.DNestedPhase>(Count_valuePhases);
                for (int i = 0; i < Count_valuePhases; i++) {
                    values_valuePhases.Add(Combat.DNestedPhase._create(reader));
                }
                valuePhases = values_valuePhases.AsReadOnly();
            }
            return new DStateMachine {
                Phases = valuePhases,
            };
        }

        public override int GetHashCode()
        {
            return Phases.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DStateMachine o && Phases.Equals(o.Phases);
        }

        public override string ToString()
        {
            return "(" + StringUtil.ToString(Phases) + ")";
        }

        public void _resolve(LoadErrors errors, ConfigMgr config)
        {
            foreach(var e in Phases) {
                e._resolve(errors, config);
            }
        }

    }

    public partial class DJump
    {
        internal static DJump _create(ConfigReader reader)
        {
            var valueKey = reader.ReadStringInPool();
            return new DJump {
                Key = valueKey,
            };
        }

        public override int GetHashCode()
        {
            return Key.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DJump o && Key.Equals(o.Key);
        }

        public override string ToString()
        {
            return "(" + Key + ")";
        }

        public void _resolve(LoadErrors errors, ConfigMgr config)
        {
        }

    }

    public partial class DNoop
    {
        internal static DNoop _create(ConfigReader reader)
        {
            return new DNoop();
        }

        public override int GetHashCode()
        {
            return this.GetType().GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DNoop;
        }


    }

}

namespace Config.Combat.Effect
{
    public partial class DDamage
    {
        internal static DDamage _create(ConfigReader reader)
        {
            var valueAmount = reader.ReadInt32();
            return new DDamage {
                Amount = valueAmount,
            };
        }

        public override int GetHashCode()
        {
            return Amount.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DDamage o && Amount.Equals(o.Amount);
        }

        public override string ToString()
        {
            return "(" + Amount + ")";
        }


    }

    public partial class DHeal
    {
        internal static DHeal _create(ConfigReader reader)
        {
            var valueAmount = reader.ReadInt32();
            return new DHeal {
                Amount = valueAmount,
            };
        }

        public override int GetHashCode()
        {
            return Amount.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DHeal o && Amount.Equals(o.Amount);
        }

        public override string ToString()
        {
            return "(" + Amount + ")";
        }


    }

    public partial class DNone
    {
        internal static DNone _create(ConfigReader reader)
        {
            return new DNone();
        }

        public override int GetHashCode()
        {
            return this.GetType().GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DNone;
        }


    }

}

