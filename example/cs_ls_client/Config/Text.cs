namespace Config;

public partial class Text(int index)
{
    // 对外接口：从全局文本数组获取文本
    public string T => TextPoolManager.GetText(index);

    public override string ToString()
    {
        return T;
    }

    internal static Text _create(ConfigReader reader)
    {
        return new Text(reader.ReadTextIndex());
    }
}