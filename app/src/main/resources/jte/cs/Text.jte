@import configgen.gencs.GenText.TextModel
@param TextModel model
@if(!model.pkg().equals("Config"))using Config;@endifnamespace ${model.pkg()}
{
    public partial class Text
    {@for(String lang : model.languages())
        public string ${lang} { get; private set; }@endfor
        private Text() {}

        public override string ToString()
        {
            return "(" + ${String.join(" + \",\" + ", model.languages())} + ")";
        }

        internal static Text _create(Config.Stream os)
        {
            Text self = new Text();@for(String lang : model.languages())
            self.${lang} = os.ReadString();@endfor
            return self;
        }
    }
}
