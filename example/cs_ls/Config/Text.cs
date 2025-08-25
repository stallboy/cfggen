namespace Config
{
    public partial class Text
    {
        public string Zh_cn { get; private set; }
        public string Backup { get; private set; }
        public string Langs { get; private set; }
        private Text() {}

        public override string ToString()
        {
            return "(" + Zh_cn + "," + Backup + "," + Langs + ")";
        }

        internal static Text _create(Config.Stream os)
        {
            Text self = new Text();
            self.Zh_cn = os.ReadString();
            self.Backup = os.ReadString();
            self.Langs = os.ReadString();
            return self;
        }
    }
}
