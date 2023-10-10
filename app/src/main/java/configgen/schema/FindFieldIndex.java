package configgen.schema;

public class FindFieldIndex {

    public static int[] findFieldIndices(Structural structural, KeySchema key) {
        int size = key.obj().size();
        int[] keyIndices = new int[size];
        int i = 0;
        for (FieldSchema k : key.obj()) {
            keyIndices[i] = findFieldIndex(structural, k);
            i++;
        }
        return keyIndices;
    }

    public static int findFieldIndex(Structural structural, FieldSchema field) {
        int i = 0;
        for (FieldSchema f : structural.fields()) {
            if (f == field) {
                return i;
            }
            i++;
        }
        return i;
    }

}
