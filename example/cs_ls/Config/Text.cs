namespace Config
{
    public partial class Text
    {
        public string Zh_cn { get; private set; }
        public string En { get; private set; }
        public string Tw { get; private set; }
        private Text() {}

        public override string ToString()
        {
            return "(" + Zh_cn + "," + En + "," + Tw + ")";
        }

        internal static Text _create(Config.Stream os)
        {
            Text self = new Text();
            string[] texts = os.ReadTextsInPool();
            self.Zh_cn = texts[0];
            self.En = texts[1];
            self.Tw = texts[2];
            return self;
        }
    }
}
