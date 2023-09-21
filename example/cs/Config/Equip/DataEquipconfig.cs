using System;
using System.Collections.Generic;
using System.IO;

namespace Config.Equip
{
    public partial class DataEquipconfig
    {
        public static DataEquipconfig Instance { get; private set; }
        public static DataEquipconfig Instance2 { get; private set; }

        public string Entry { get; private set; } /* 入口，程序填*/
        public int Stone_count_for_set { get; private set; } /* 形成套装的音石数量*/
        public string Draw_protect_name { get; private set; } /* 保底策略名称*/
        public int Broadcastid { get; private set; } /* 公告Id*/
        public int Broadcast_least_quality { get; private set; } /* 公告的最低品质*/
        public int Week_reward_mailid { get; private set; } /* 抽卡周奖励的邮件id*/

        public override int GetHashCode()
        {
            return Entry.GetHashCode();
        }

        public override bool Equals(object obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DataEquipconfig;
            return o != null && Entry.Equals(o.Entry);
        }

        public override string ToString()
        {
            return "(" + Entry + "," + Stone_count_for_set + "," + Draw_protect_name + "," + Broadcastid + "," + Broadcast_least_quality + "," + Week_reward_mailid + ")";
        }

        static Config.KeyedList<string, DataEquipconfig> all = null;

        public static DataEquipconfig Get(string entry)
        {
            DataEquipconfig v;
            return all.TryGetValue(entry, out v) ? v : null;
        }

        public static List<DataEquipconfig> All()
        {
            return all.OrderedValues;
        }

        public static List<DataEquipconfig> Filter(Predicate<DataEquipconfig> predicate)
        {
            var r = new List<DataEquipconfig>();
            foreach (var e in all.OrderedValues)
            {
                if (predicate(e))
                    r.Add(e);
            }
            return r;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<string, DataEquipconfig>();
            for (var c = os.ReadInt32(); c > 0; c--) {
                var self = _create(os);
                all.Add(self.Entry, self);
                if (self.Entry.Trim().Length == 0)
                    continue;
                switch(self.Entry.Trim())
                {
                    case "Instance":
                        if (Instance != null)
                            errors.EnumDup("equip.equipconfig", self.ToString());
                        Instance = self;
                        break;
                    case "Instance2":
                        if (Instance2 != null)
                            errors.EnumDup("equip.equipconfig", self.ToString());
                        Instance2 = self;
                        break;
                    default:
                        errors.EnumDataAdd("equip.equipconfig", self.ToString());
                        break;
                }
            }
            if (Instance == null)
                errors.EnumNull("equip.equipconfig", "Instance");
            if (Instance2 == null)
                errors.EnumNull("equip.equipconfig", "Instance2");
        }

        internal static DataEquipconfig _create(Config.Stream os)
        {
            var self = new DataEquipconfig();
            self.Entry = os.ReadString();
            self.Stone_count_for_set = os.ReadInt32();
            self.Draw_protect_name = os.ReadString();
            self.Broadcastid = os.ReadInt32();
            self.Broadcast_least_quality = os.ReadInt32();
            self.Week_reward_mailid = os.ReadInt32();
            return self;
        }

    }
}
