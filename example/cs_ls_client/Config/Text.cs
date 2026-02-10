namespace Config
{
    public partial class Text
    {
        private int index;

        private Text() {}

        // 对外接口：从全局文本数组获取文本
        public string T
        {
            get
            {
                return TextPoolManager.GetText(index);
            }
            private set
            {
                // 不允许设置
            }
        }

        public override string ToString()
        {
            return T;
        }

        internal static Text _create(Config.Stream os)
        {
            Text self = new Text();
            self.index = os.ReadTextIndex();
            return self;
        }
    }
}
