@import java.util.List
@param String pkg
@param List<String> languages

public class Text
{
    @for(String lang : languages)
        private _${lang}:string=""
        get ${lang}(): string {return this._${lang}};}
    @endfor

    static _create(Stream os) : Text
    {
        const self = new Text();
        @for(String lang : languages)
            self._${lang} = os.ReadString();
        @endfor
        return self;
    }
}

