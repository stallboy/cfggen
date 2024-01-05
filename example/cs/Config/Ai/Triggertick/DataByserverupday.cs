using System;
using System.Collections.Generic;
using System.IO;

namespace Config.Ai.Triggertick
{
    public partial class DataByserverupday : Config.Ai.DataTriggertick
    {
        public int Init { get; private set; }
        public float Coefficient1 { get; private set; }
        public float Coefficient2 { get; private set; }

        public DataByserverupday() {
        }

        public DataByserverupday(int init, float coefficient1, float coefficient2) {
            this.Init = init;
            this.Coefficient1 = coefficient1;
            this.Coefficient2 = coefficient2;
        }

        public override int GetHashCode()
        {
            return Init.GetHashCode() + Coefficient1.GetHashCode() + Coefficient2.GetHashCode();
        }

        public override bool Equals(object obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DataByserverupday;
            return o != null && Init.Equals(o.Init) && Coefficient1.Equals(o.Coefficient1) && Coefficient2.Equals(o.Coefficient2);
        }

        public override string ToString()
        {
            return "(" + Init + "," + Coefficient1 + "," + Coefficient2 + ")";
        }

        internal new static DataByserverupday _create(Config.Stream os)
        {
            var self = new DataByserverupday();
            self.Init = os.ReadInt32();
            self.Coefficient1 = os.ReadSingle();
            self.Coefficient2 = os.ReadSingle();
            return self;
        }

    }
}
