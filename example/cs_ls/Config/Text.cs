namespace Config
{
    public partial class Text
    {
        public String Zh_cn { get; private set; }
        public String En { get; private set; }
        private Text() {}

        public override string ToString()
        {
            return "(" + Zh_cn + "," + En + ")";
        }

        internal static Text _create(Config.Stream os)
        {
            Text self = new Text();
            self.Zh_cn = os.ReadString();
            self.En = os.ReadString();
            return self;
        }
    }
}