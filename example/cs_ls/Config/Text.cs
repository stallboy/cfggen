namespace Config
{
    public partial class Text
    {
        public string Zh_cn { get; private set; }
        public string En { get; private set; }
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
