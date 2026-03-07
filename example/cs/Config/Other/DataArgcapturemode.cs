using System;
using System.Collections.Generic;

namespace Config.Other
{
    public partial class DataArgcapturemode
    {
        public static DataArgcapturemode Snapshot { get; private set; }
        public static DataArgcapturemode Dynamic { get; private set; }
        public string Name { get; private set; }
        public string Comment { get; private set; }

        public override int GetHashCode()
        {
            return Name.GetHashCode();
        }

        public override bool Equals(object obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DataArgcapturemode;
            return o != null && Name.Equals(o.Name);
        }

        public override string ToString()
        {
            return "(" + Name + "," + Comment + ")";
        }

        
        static Config.KeyedList<string, DataArgcapturemode> all = null;

        public static DataArgcapturemode Get(string name)
        {
            DataArgcapturemode v;
            return all.TryGetValue(name, out v) ? v : null;
        }

        public static List<DataArgcapturemode> All()
        {
            return all.OrderedValues;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<string, DataArgcapturemode>();
            for (var c = os.ReadInt32(); c > 0; c--)
            {
                var self = _create(os);
                all.Add(self.Name, self);
                if (self.Name.Trim().Length == 0)
                    continue;
                switch(self.Name.Trim())
                {
                    case "Snapshot":
                        if (Snapshot != null)
                            errors.EnumDup("other.ArgCaptureMode", self.ToString());
                        Snapshot = self;
                        break;
                    case "Dynamic":
                        if (Dynamic != null)
                            errors.EnumDup("other.ArgCaptureMode", self.ToString());
                        Dynamic = self;
                        break;
                    default:
                        errors.EnumDataAdd("other.ArgCaptureMode", self.ToString());
                        break;
                }
            }

            if (Snapshot == null)
                errors.EnumNull("other.ArgCaptureMode", "Snapshot");
            if (Dynamic == null)
                errors.EnumNull("other.ArgCaptureMode", "Dynamic");
        }

        internal static DataArgcapturemode _create(Config.Stream os)
        {
            var self = new DataArgcapturemode();
            self.Name = os.ReadStringInPool();
            self.Comment = os.ReadStringInPool();
            return self;
        }

    }
}