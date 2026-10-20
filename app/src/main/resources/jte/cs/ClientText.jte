@import java.util.List
@param String pkg
@param List<String> languages
@param boolean unity

${unity ? "namespace " + pkg + "\n{" : "namespace " + pkg + ";"}

public partial class Text
{
    private readonly int _index;
    private readonly ConfigPools _pool;

    private Text(int index, ConfigPools pool) { _index = index; _pool = pool; }

    public string T => _pool.GetText(_index);

    public override string ToString()
    {
        return T;
    }

    internal static Text _create(ConfigReader reader)
    {
        return new Text(reader.ReadTextIndex(), reader.CaptureTextPool());
    }
}
@if(unity)
}
@endif
