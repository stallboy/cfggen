using System;
using System.Collections.Generic;

namespace Config.Ai.Triggertick
{
    public partial class DataBylevel : Config.Ai.DataTriggertick
    {
        public int Init { get; private set; }
        public float Coefficient { get; private set; }

        public DataBylevel() {
        }

        public DataBylevel(int init, float coefficient) {
            this.Init = init;
            this.Coefficient = coefficient;
        }

        public override int GetHashCode()
        {
            return Init.GetHashCode() + Coefficient.GetHashCode();
        }

        public override bool Equals(object obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DataBylevel;
            return o != null && Init.Equals(o.Init) && Coefficient.Equals(o.Coefficient);
        }

        public override string ToString()
        {
            return "(" + Init + "," + Coefficient + ")";
        }

        internal new static DataBylevel _create(Config.Stream os)
        {
            var self = new DataBylevel();
            self.Init = os.ReadInt32();
            self.Coefficient = os.ReadSingle();
            return self;
        }

    }
}