@import java.util.List
@param String pkg
@param List<String> languages

export class Text
{
    @for(String lang : languages)
        private _${lang}: string = "";
        get ${lang}(): string { return this._${lang}; }
    @endfor

    static _create(os: Stream): Text
    {
        const self = new Text();
        const texts = os.ReadTextsInPool();
        @for(int i = 0; i < languages.size(); i++)
            self._${languages.get(i)} = texts[${i}];
        @endfor
        return self;
    }

    toString(): string {
        return "(" + ${String.join(" + \",\" + ", languages)} + ")";
    }
}
