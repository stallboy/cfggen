using System;
using System.Collections.Generic;

namespace Config.Task.Completecondition
{
    public partial class DataAa : Config.Task.DataCompletecondition
    {
        public override Config.Task.DataCompleteconditiontype type() {
            return Config.Task.DataCompleteconditiontype.Aa;
        }


        public DataAa() {
        }

        public override int GetHashCode()
        {
            return this.GetType().GetHashCode();
        }

        public override bool Equals(object obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DataAa;
            return o != null;
        }

        internal new static DataAa _create(Config.Stream os)
        {
            var self = new DataAa();
            return self;
        }

    }
}