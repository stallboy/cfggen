@import java.util.List
@param String pkg
@param List<String> languages
@param boolean unity
@if(unity)
using System;
using System.Collections.Generic;
@endif
@if(!pkg.equals("Config"))
    using Config;
@endif

${unity ? "namespace " + pkg + "\n{" : "namespace " + pkg + ";"}

@if(unity)
public partial class Text
{
    private readonly int _index;

    public Text(int index)
    {
        _index = index;
    }

    // 对外接口：从全局文本数组获取文本
    public string T => TextPoolManager.GetText(_index);
@else
public partial class Text(int index)
{
    // 对外接口：从全局文本数组获取文本
    public string T => TextPoolManager.GetText(index);
@endif

    public override string ToString()
    {
        return T;
    }

    internal static Text _create(ConfigReader reader)
    {
        return new Text(reader.ReadTextIndex());
    }
}
@if(unity)
}
@endif
