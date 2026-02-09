using System.Collections.Generic;

namespace Config
{

    public enum LoadErrorType
    {
        //Warn
        ConfigNull,
        ConfigDataAdd,
        EnumDataAdd,

        //Error
        EnumDup,
        EnumNull,
        RefNull,
    }

    public class LoadError
    {
        public LoadErrorType Type { get; private set; }
        public string Config { get; private set; }
        public string Record { get; private set; }
        public string Field { get; private set; }

        public LoadError(LoadErrorType type, string config, string record, string field)
        {
            Type = type;
            Config = config;
            Record = record;
            Field = field;
        }

        public override string ToString()
        {
            return Type + ", " + Config + ", " + Record + ", " + Field;
        }
    }


    public class LoadErrors
    {
        public List<LoadError> Errors { get; private set; }
        public List<LoadError> Warns  { get; private set; }

        public LoadErrors()
        {
            Errors = new List<LoadError>();
            Warns = new List<LoadError>(); 
        }

        public void ConfigNull(string config)
        {
            Warns.Add( new LoadError(LoadErrorType.ConfigNull, config, "", ""));
        }

        public void ConfigDataAdd(string config)
        {
            Warns.Add(new LoadError(LoadErrorType.ConfigDataAdd, config, "", ""));
        }

        public void EnumDataAdd(string config, string record)
        {
            Warns.Add(new LoadError(LoadErrorType.EnumDataAdd, config, record, ""));
        }


        public void EnumDup(string config, string record)
        {
            Errors.Add(new LoadError(LoadErrorType.EnumDup, config, record, ""));
        }

        public void EnumNull(string config, string record)
        {
            Errors.Add(new LoadError(LoadErrorType.EnumNull, config, record, ""));
        }

        public void RefNull(string config, string record, string field)
        {
            Errors.Add(new LoadError(LoadErrorType.RefNull, config, record, field));
        }
    }

  

}
