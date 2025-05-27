@import java.util.List
@param String pkg
@param List<String> languages
@if(!pkg.equals("Config"))
    using Config;
@endif

namespace ${pkg}
{
    public partial class Text
    {
        @for(String lang : languages)
            public string ${lang} { get; private set; }
        @endfor
        private Text() {}

        public override string ToString()
        {
            return "(" + ${String.join(" + \",\" + ", languages)} + ")";
        }

        internal static Text _create(Config.Stream os)
        {
            Text self = new Text();
            @for(String lang : languages)
                self.${lang} = os.ReadString();
            @endfor
            return self;
        }
    }
}
