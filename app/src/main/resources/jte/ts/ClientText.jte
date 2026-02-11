@import java.util.List
@param String pkg
@param List<String> languages

export class Text
{
    private index: number = 0;

    get T(): string {
        return TextPoolManager.GetText(this.index);
    }

    static _create(os: Stream): Text
    {
        const self = new Text();
        self.index = os.ReadTextIndex();
        return self;
    }

    toString(): string {
        return this.T;
    }
}
