namespace Config.Task.Completecondition;

public partial class DataAa : Task.DataCompletecondition
{
    public override Task.DataCompleteconditiontype type() {
        return Task.DataCompleteconditiontype.Aa;
    }


    public override int GetHashCode()
    {
        return this.GetType().GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataAa;
        return o != null;
    }

    internal new static DataAa _create(Stream os)
    {
        return new DataAa {
        };
    }

}
