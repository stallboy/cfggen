package equip

type Jewelrytype struct {
    typeName string //程序用名字
}

//entries
var (
    jade Jewelrytype
    bracelet Jewelrytype
    magic Jewelrytype
    bottle Jewelrytype
)

//getters
func (t *Jewelrytype) GetTypeName() string {
    return t.typeName
}

var all []Jewelrytype
func GetAll() []Jewelrytype {
    return all[:len(all)]:len(all)]
}

var allMap map[string]Jewelrytype
func Get(key string) (Jewelrytype,bool){
    return allMap[key]
}
        public static Jewelrytype Get(string typeName)
        {
            Jewelrytype v;
            return all.TryGetValue(typeName, out v) ? v : null;
        }

        public static List<Jewelrytype> All()
        {
            return all.OrderedValues;
        }

        public static List<Jewelrytype> Filter(Predicate<Jewelrytype> predicate)
        {
            var r = new List<Jewelrytype>();
            foreach (var e in all.OrderedValues)
            {
                if (predicate(e))
                    r.Add(e);
            }
            return r;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<string, Jewelrytype>();
            for (var c = os.ReadInt32(); c > 0; c--) {
                var self = _create(os);
                all.Add(self.TypeName, self);
                if (self.TypeName.Trim().Length == 0)
                    continue;
                switch(self.TypeName.Trim())
                {
                    case "Jade":
                        if (Jade != null)
                            errors.EnumDup("equip.jewelrytype", self.ToString());
                        Jade = self;
                        break;
                    case "Bracelet":
                        if (Bracelet != null)
                            errors.EnumDup("equip.jewelrytype", self.ToString());
                        Bracelet = self;
                        break;
                    case "Magic":
                        if (Magic != null)
                            errors.EnumDup("equip.jewelrytype", self.ToString());
                        Magic = self;
                        break;
                    case "Bottle":
                        if (Bottle != null)
                            errors.EnumDup("equip.jewelrytype", self.ToString());
                        Bottle = self;
                        break;
                    default:
                        errors.EnumDataAdd("equip.jewelrytype", self.ToString());
                        break;
                }
            }
            if (Jade == null)
                errors.EnumNull("equip.jewelrytype", "Jade");
            if (Bracelet == null)
                errors.EnumNull("equip.jewelrytype", "Bracelet");
            if (Magic == null)
                errors.EnumNull("equip.jewelrytype", "Magic");
            if (Bottle == null)
                errors.EnumNull("equip.jewelrytype", "Bottle");
        }

