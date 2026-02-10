using System;
using System.Collections.Generic;

namespace Config.Equip
{
    public partial class DataTestpackbean
    {
        public string Name { get; private set; }
        public Config.DataRange IRange { get; private set; }

        public DataTestpackbean() {
        }

        public DataTestpackbean(string name, Config.DataRange iRange) {
            this.Name = name;
            this.IRange = iRange;
        }

        public override int GetHashCode()
        {
            return Name.GetHashCode() + IRange.GetHashCode();
        }

        public override bool Equals(object obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DataTestpackbean;
            return o != null && Name.Equals(o.Name) && IRange.Equals(o.IRange);
        }

        public override string ToString()
        {
            return "(" + Name + "," + IRange + ")";
        }

        internal static DataTestpackbean _create(Config.Stream os)
        {
            var self = new DataTestpackbean();
            self.Name = os.ReadStringInPool();
            self.IRange = Config.DataRange._create(os);
            return self;
        }

    }
}