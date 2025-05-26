
class @{keyClassName}
{
    @for(f : keySchema.fieldSchemas()) {
    readonly @{type(f.type())} @{fieldName};
    }

    public @{keyClassName}(@formalParams)
    {
        @for(f : keySchema.fieldSchemas()) {
        this.@{fieldName} = @{lower1(f.name())};
        }
    }

    public override int GetHashCode()
    {
        return @{hashCodes};
    }

    public override bool Equals(object obj)
    {
        if (obj == null) return false;
        if (obj == this) return true;
        var o = obj as @{keyClassName};
        return o != null && @{equals};
    }
}
