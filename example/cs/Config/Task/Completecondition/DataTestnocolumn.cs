using System;
using System.Collections.Generic;
using System.IO;

namespace Config.Task.Completecondition
{
    public partial class DataTestnocolumn : Config.Task.DataCompletecondition
    {
        public override Config.Task.DataCompleteconditiontype type() {
            return Config.Task.DataCompleteconditiontype.TestNoColumn;
        }


        public DataTestnocolumn() {
        }

        public override int GetHashCode()
        {
            return this.GetType().GetHashCode();
        }

        public override bool Equals(object obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DataTestnocolumn;
            return o != null;
        }

        internal new static DataTestnocolumn _create(Config.Stream os)
        {
            var self = new DataTestnocolumn();
            return self;
        }

    }
}
