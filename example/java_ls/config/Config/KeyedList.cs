using System.Collections.Generic;

namespace Config
{
    public class KeyedList<TKey, TValue>
    {
        public List<TKey> OrderedKeys { get; private set; } = new();
        public List<TValue> OrderedValues { get; private set; } = new();
        public Dictionary<TKey, TValue> Map { get; private set; } = new();

        public void Add(TKey key, TValue value)
        {
            OrderedKeys.Add(key);
            OrderedValues.Add(value);
            Map.Add(key, value);
        }

        public bool TryGetValue(TKey key, out TValue val)
        {
            return Map.TryGetValue(key, out val);
        }

        public override string ToString()
        {
            var elements = new string[OrderedKeys.Count];
            var i = 0;
            foreach (var k in OrderedKeys)
            {
                elements[i] = k + "=" + OrderedValues[i];
                i++;
            }
            return "{" + string.Join(", ", elements) + "}";
        }
    }
}
