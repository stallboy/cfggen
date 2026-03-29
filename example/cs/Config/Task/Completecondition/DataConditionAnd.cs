namespace Config.Task.Completecondition;

public partial class DataConditionAnd : Task.DataCompletecondition
{
    public override Task.DataCompleteconditiontype type() {
        return Task.DataCompleteconditiontype.ConditionAnd;
    }

    public required Task.DataCompletecondition Cond1 { get; init; }
    public required Task.DataCompletecondition Cond2 { get; init; }

    public override int GetHashCode()
    {
        return Cond1.GetHashCode() + Cond2.GetHashCode();
    }

    public override bool Equals(object? obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as DataConditionAnd;
        return o != null && Cond1.Equals(o.Cond1) && Cond2.Equals(o.Cond2);
    }

    public override string ToString()
    {
        return "(" + Cond1 + "," + Cond2 + ")";
    }

    internal new static DataConditionAnd _create(Stream os)
    {
        var cond1 = Task.DataCompletecondition._create(os);
        var cond2 = Task.DataCompletecondition._create(os);
        return new DataConditionAnd {
            Cond1 = cond1,
            Cond2 = cond2,
        };
    }

    internal override void _resolve(LoadErrors errors)
    {
        Cond1._resolve(errors);
        Cond2._resolve(errors);
    }
}
