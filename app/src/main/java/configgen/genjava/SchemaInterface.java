package configgen.genjava;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 这个既是总入口，又是多态bean
 */
public final class SchemaInterface implements Schema {

    public final Map<String, Schema> implementations = new LinkedHashMap<>(); //包含SchemaBean和SchemaEnum

    public SchemaInterface() {
    }

    public void addImp(String name, Schema schema) {
        Schema old = implementations.put(name, schema);
        if (old != null) {
            throw new IllegalStateException("implementation duplicate " + name);
        }
    }

    @Override
    public boolean compatible(Schema other) {
        if (!(other instanceof SchemaInterface si)) {
            return false;
        }
        if (implementations.size() > si.implementations.size()) {
            throw new SchemaCompatibleException("size not compatible with data err, code=" + implementations.size() + ", data=" + si.implementations.size());
        }
        for (Map.Entry<String, Schema> entry : implementations.entrySet()) {
            Schema t1 = entry.getValue();
            Schema t2 = si.implementations.get(entry.getKey());
            if (!t1.compatible(t2)) {
                throw new SchemaCompatibleException(entry.getKey() + " bean/table not compatible with data err");
            }
        }
        return true;
    }


}
