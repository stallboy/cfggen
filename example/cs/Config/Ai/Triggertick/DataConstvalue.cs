using System;
using System.Collections.Generic;
using System.IO;

namespace Config.Ai.Triggertick
{
    public partial class DataConstvalue : Config.Ai.DataTriggertick
    {
        public override Config.Ai.DataTriggerticktype type() {
            return Config.Ai.DataTriggerticktype.ConstValue;
        }

        public int Value { get; private set; }

        public DataConstvalue() {
        }

        public DataConstvalue(int value) {
            this.Value = value;
        }

        public override int GetHashCode()
        {
            return Value.GetHashCode();
        }

        public override bool Equals(object obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DataConstvalue;
            return o != null && Value.Equals(o.Value);
        }

        public override string ToString()
        {
            return "(" + Value + ")";
        }

        internal new static DataConstvalue _create(Config.Stream os)
        {
            var self = new DataConstvalue();
            self.Value = os.ReadInt32();
            return self;
        }

    }
}
