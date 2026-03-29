@import java.util.List
@param String pkg
@param List<String> languages
@if(!pkg.equals("Config"))
    using Config;
@endif

namespace ${pkg};

public partial class Text(int index)
{
    // 对外接口：从全局文本数组获取文本
    public string T => TextPoolManager.GetText(index);

    public override string ToString()
    {
        return T;
    }

    internal static Text _create(Config.Stream os)
    {
        return new Text(os.ReadTextIndex());
    }
}