using System;
using System.Collections.Generic;

namespace Config.Equip
{
    public partial class DataTestpackbean
    {
        public string Name { get; private set; }
        public Config.DataRange Range { get; private set; }

        public DataTestpackbean() {
        }

        public DataTestpackbean(string name, Config.DataRange range) {
            this.Name = name;
            this.Range = range;
        }

        public override int GetHashCode()
        {
            return Name.GetHashCode() + Range.GetHashCode();
        }

        public override bool Equals(object obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DataTestpackbean;
            return o != null && Name.Equals(o.Name) && Range.Equals(o.Range);
        }

        public override string ToString()
        {
            return "(" + Name + "," + Range + ")";
        }

        internal static DataTestpackbean _create(Config.Stream os)
        {
            var self = new DataTestpackbean();
            self.Name = os.ReadString();
            self.Range = Config.DataRange._create(os);
            return self;
        }

    }
}
