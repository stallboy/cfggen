namespace Config.Task.Completecondition;

public partial class DataTestNoColumn : Task.DataCompletecondition
{
    public override Task.DataCompleteconditiontype type() {
        return Task.DataCompleteconditiontype.TestNoColumn;
    }


    public override int GetHashCode()
    {
        return this.GetType().GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataTestNoColumn;
        return o != null;
    }

    internal new static DataTestNoColumn _create(Stream os)
    {
        return new DataTestNoColumn {
        };
    }

}
