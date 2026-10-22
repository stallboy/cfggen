package configgen.value;

import configgen.naming.GenNaming;
import configgen.schema.*;
import configgen.util.StringUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static configgen.value.CfgValue.*;

public class ValueRefCollector {

    public record RefId(String table,
                        String id) {
    }

    public record FieldRef(
            String firstField,
            String label,   // 链接的名称
            String toTable,
            String toId,
            List<Object> toPath,
            String toRevision) {
        public FieldRef(String firstField, String label, String toTable, String toId) {
            this(firstField, label, toTable, toId, null, null);
        }
    }

    private final CfgValue cfgValue;
    private final ValueReferences pathReferences;
    private final String editorRevision;
    /**
     * 输出：外链到的所有的外部节点（refId-> record）
     */
    private final Map<RefId, VStruct> resultRefIdToRecordMap;
    /**
     * 输出：外链到的所有的链接（field + refId）
     */
    private final List<FieldRef> resultFieldRefs;

    public static List<FieldRef> collectRefs(Value record, CfgValue cfgValue) {
        return collectRefs(record, cfgValue, ValueReferences.forQuery(cfgValue));
    }

    public static List<FieldRef> collectRefs(Value record, CfgValue cfgValue, ValueReferences references) {
        return collectRefs(record, cfgValue, references, null);
    }

    public static List<FieldRef> collectRefs(Value record, CfgValue cfgValue, ValueReferences references, String editorRevision) {
        List<FieldRef> resultFieldRefs = new ArrayList<>();
        Map<RefId, VStruct> newFrontier = new LinkedHashMap<>();
        ValueRefCollector collector = new ValueRefCollector(cfgValue, newFrontier, resultFieldRefs, references, editorRevision);
        collector.collect(record, List.of());
        return resultFieldRefs;
    }

    public ValueRefCollector(CfgValue cfgValue, Map<RefId, VStruct> resultRefIdToRecordMap, List<FieldRef> resultFieldRefs) {
        this(cfgValue, resultRefIdToRecordMap, resultFieldRefs, ValueReferences.forQuery(cfgValue));
    }

    public ValueRefCollector(CfgValue cfgValue, Map<RefId, VStruct> resultRefIdToRecordMap,
                             List<FieldRef> resultFieldRefs, ValueReferences references) {
        this(cfgValue, resultRefIdToRecordMap, resultFieldRefs, references, null);
    }

    public ValueRefCollector(CfgValue cfgValue, Map<RefId, VStruct> resultRefIdToRecordMap,
                             List<FieldRef> resultFieldRefs, ValueReferences references, String editorRevision) {
        this.cfgValue = cfgValue;
        pathReferences = references;
        this.editorRevision = editorRevision;
        this.resultRefIdToRecordMap = resultRefIdToRecordMap;
        this.resultFieldRefs = resultFieldRefs;
    }

    public void collect(Value value, List<String> prefix) {
        switch (value) {
            case VStruct vStruct -> collect(vStruct, prefix);
            case VInterface vInterface -> collect(vInterface, prefix);
            case VList vList -> collect(vList, prefix);
            case VMap vMap -> collect(vMap, prefix);
            default -> {
            }
        }
    }

    public void collect(VStruct vStruct, List<String> prefix) {
        String pre = "";
        if (!prefix.isEmpty()) {
            pre = String.join(".", prefix) + ".";
        }

        collectStructRef(cfgValue, vStruct, resultRefIdToRecordMap, resultFieldRefs, pre, pathReferences, editorRevision);

        int i = 0;
        for (Value value : vStruct.values()) {
            String name = vStruct.schema().fields().get(i).name();
            collect(value, listAddOf(prefix, name));
            i++;
        }
    }

    private static List<String> listAddOf(List<String> old, String e) {
        List<String> res = new ArrayList<>(old.size() + 1);
        res.addAll(old);
        res.add(e);
        return res;
    }

    static List<FieldRef> collectStructRef(CfgValue cfgValue, VStruct vStruct,
                                                   Map<RefId, VStruct> refIdToRecordMap,
                                                   List<FieldRef> fieldRefs, String namePrefix,
                                                   ValueReferences references) {
        return collectStructRef(cfgValue, vStruct, refIdToRecordMap, fieldRefs, namePrefix, references, null);
    }

    static List<FieldRef> collectStructRef(CfgValue cfgValue, VStruct vStruct,
                                          Map<RefId, VStruct> refIdToRecordMap,
                                          List<FieldRef> fieldRefs, String namePrefix,
                                          ValueReferences references, String editorRevision) {
        if (fieldRefs == null) {
            fieldRefs = new ArrayList<>();
        }

        Structural structural = vStruct.schema();
        for (ForeignKeySchema fk : structural.foreignKeys()) {
            if (fk.pathReference() != null) {
                ValueReferences.Result result = references.result(vStruct, fk);
                ValueReferences.Occurrence at = references.occurrence(vStruct);
                if (result != null && result.target() != null && at != null &&
                        (result.targetRecord() != at.root() || editorRevision != null)) {
                    VStruct record = result.targetRecord();
                    String table = record.schema().name();
                    String id = ValueUtil.extractPrimaryKeyValue(record, (TableSchema) record.schema()).packStr();
                    if (record != at.root()) refIdToRecordMap.put(new RefId(table, id), record);
                    String sourceField = fk.pathReference().sources().stream()
                            .filter(p -> p.start() == ReferencePath.Start.CURRENT)
                            .map(p -> p.fields().getFirst()).findFirst()
                            .orElse("@out"); // Existing editor protocol: node-level source handle.
                    List<Object> path = editorRevision == null ? null : references.jsonPath(result.target());
                    fieldRefs.add(new FieldRef(sourceField, referenceLabel(fk, vStruct, namePrefix), table, id,
                            path, path == null ? null : editorRevision));
                }
                continue;
            }
            RefKey refKey = fk.refKey();
            Map<Value, VStruct> foreignKeyValueMap = ValueUtil.getForeignKeyValueMap(cfgValue, fk);
            if (foreignKeyValueMap == null) {
                continue;
            }
            // 这里没考虑ListRef
            if (refKey instanceof RefKey.RefSimple _) {
                FieldSchema firstField = fk.key().fieldSchemas().getFirst();
                FieldType ft = firstField.type();

                String refName = referenceLabel(fk, vStruct, namePrefix);

                switch (ft) {
                    case FieldType.SimpleType ignored -> {
                        Value localValue = ValueUtil.extractKeyValue(vStruct, fk.keyIndices());
                        VStruct refRecord = foreignKeyValueMap.get(localValue);
                        if (refRecord != null) {
                            addRef(refIdToRecordMap, fieldRefs, fk, refName, localValue.packStr(), refRecord);
                        }
                    }
                    case FieldType.FList ignored -> {
                        VList localList = (VList) vStruct.values().get(fk.keyIndices()[0]);
                        for (SimpleValue item : localList.valueList()) {
                            VStruct refRecord = foreignKeyValueMap.get(item);
                            if (refRecord != null) {
                                addRef(refIdToRecordMap, fieldRefs, fk, refName, item.packStr(), refRecord);
                            }
                        }
                    }
                    case FieldType.FMap ignored -> {
                        VMap localMap = (VMap) vStruct.values().get(fk.keyIndices()[0]);
                        for (SimpleValue val : localMap.valueMap().values()) {
                            VStruct refRecord = foreignKeyValueMap.get(val);
                            if (refRecord != null) {
                                addRef(refIdToRecordMap, fieldRefs, fk, refName, val.packStr(), refRecord);
                            }
                        }
                    }
                }
            }
        }
        return fieldRefs;
    }

    private static String referenceLabel(ForeignKeySchema fk, VStruct source, String prefix) {
        String titleField = fk.meta().getStr("refTitle", null);
        if (titleField != null && ValueUtil.extractFieldValue(source, titleField) instanceof StringValue title) {
            return title.value();
        }
        return prefix + StringUtil.lower1(GenNaming.refFieldName(fk));
    }

    private static void addRef(Map<RefId, VStruct> refIdToRecordMap, List<FieldRef> fieldRefs, ForeignKeySchema fk, String refName, String id, VStruct refRecord) {
        String table = fk.refTableNormalized();
        RefId refId = new RefId(table, id);
        refIdToRecordMap.put(refId, refRecord);
        fieldRefs.add(new FieldRef(fk.key().fieldSchemas().getFirst().name(), refName, table, id));
    }


    public void collect(VInterface vInterface, List<String> prefix) {
        VStruct child = vInterface.child();
        collect(child, prefix);
    }

    public void collect(VList vList, List<String> prefix) {
        int i = 0;
        for (SimpleValue sv : vList.valueList()) {
            collect(sv, listAddOf(prefix, String.valueOf(i)));
            i++;
        }
    }

    public void collect(VMap vMap, List<String> prefix) {
        int i = 0;
        for (Map.Entry<SimpleValue, SimpleValue> e : vMap.valueMap().entrySet()) {
            SimpleValue key = e.getKey();
            SimpleValue value = e.getValue();
            collect(key, listAddOf(prefix, i + "k"));
            collect(value, listAddOf(prefix, i + "v"));
            i++;
        }
    }


}
