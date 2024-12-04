package configgen.schema.cfg;

import configgen.ctx.DirectoryStructure;
import configgen.util.Logger;
import configgen.schema.*;
import configgen.schema.RefKey.RefList;
import configgen.schema.RefKey.RefPrimary;
import configgen.util.DOMUtil;
import org.w3c.dom.Element;

import java.nio.file.Path;
import java.util.*;

import static configgen.ctx.DirectoryStructure.findConfigFilesFromRecursively;
import static configgen.schema.FieldFormat.AutoOrPack.AUTO;
import static configgen.schema.FieldFormat.AutoOrPack.PACK;
import static configgen.schema.FieldType.Primitive.*;
import static configgen.schema.Metadata.*;

public enum XmlReader {
    INSTANCE;

    public static CfgSchema readFromDir(Path rootDir) {
        CfgSchema destination = CfgSchema.of();
        Map<String, DirectoryStructure.CfgFileInfo> allXmlFiles = new LinkedHashMap<>();
        findConfigFilesFromRecursively(rootDir.resolve("config.xml"), "xml", "",
                rootDir, allXmlFiles);

        for (DirectoryStructure.CfgFileInfo c : allXmlFiles.values()) {
            INSTANCE.readTo(destination, c.path(), c.pkgNameDot());
        }
        return destination;
    }

    public void readTo(CfgSchema destination, Path xml, String pkgNameDot) {
        Element self = DOMUtil.rootElement(xml.toFile());
        for (Element e : DOMUtil.elements(self, "bean")) {
            Fieldable f;
            if (e.hasAttribute("enumRef")) {
                f = parseInterface(e, pkgNameDot);
            } else {
                f = parseStruct(e, pkgNameDot, false);
            }

            destination.items().add(f);
        }
        for (Element e : DOMUtil.elements(self, "table")) {
            TableSchema t = parseTable(e, pkgNameDot);
            destination.items().add(t);
        }
    }

    private TableSchema parseTable(Element self, String pkgNameDot) {
        String name = self.getAttribute("name").trim();
        KeySchema primaryKey = getKeySchema(self, "primaryKey");

        EntryType entry;
        if (self.hasAttribute("enum")) {
            entry = new EntryType.EEnum(self.getAttribute("enum").trim());
        } else if (self.hasAttribute("entry")) {
            entry = new EntryType.EEntry(self.getAttribute("entry").trim());
        } else {
            entry = EntryType.ENo.NO;
        }
        boolean isColumnMode = self.hasAttribute("isColumnMode");
        FieldTagMap fieldTagMap = parseOwn(self, false);
        Metadata meta = fieldTagMap.meta;
        if (self.hasAttribute("extraSplit")) {
            int extraSplit = Integer.parseInt(self.getAttribute("extraSplit"));
            meta.data().put("extraSplit", new MetaInt(extraSplit));
        }

        List<FieldSchema> fields = parseFieldList(self, fieldTagMap.tag2FieldTag);
        List<ForeignKeySchema> foreignKeys = parseForeignKeyList(self);
        List<KeySchema> uniqueKeys = new ArrayList<>();
        for (Element ele : DOMUtil.elements(self, "uniqueKey")) {
            uniqueKeys.add(getKeySchema(ele, "keys"));
        }
        return new TableSchema(pkgNameDot + name, primaryKey, entry, isColumnMode,
                meta, fields, foreignKeys, uniqueKeys);
    }

    private KeySchema getKeySchema(Element self, String attr) {
        String[] keys = DOMUtil.parseStringArray(self, attr);
        return new KeySchema(Arrays.asList(keys));
    }

    private StructSchema parseStruct(Element self, String pkgNameDot, boolean isImpl) {
        String name = self.getAttribute("name").trim();
        FieldTagMap fieldTagMap = parseOwn(self, isImpl);
        Metadata meta = fieldTagMap.meta;
        FieldFormat fmt = parseBeanFmt(self);
        List<FieldSchema> fields = parseFieldList(self, fieldTagMap.tag2FieldTag);
        List<ForeignKeySchema> foreignKeys = parseForeignKeyList(self);
        return new StructSchema(pkgNameDot + name, fmt, meta,
                fields, foreignKeys);
    }

    private Metadata parseOwnToMetadata(Element self) {
        Metadata meta = Metadata.of();
        for (String tag : parseOwnSet(self)) {
            meta.putTag(tag);
        }
        return meta;
    }

    private Set<String> parseOwnSet(Element self) {
        String own = self.getAttribute("own");
        if (self.hasAttribute("own")) {
            Set<String> tags = new HashSet<>();
            for (String tag : own.split(",")) {
                tag = tag.trim();
                tags.add(tag);
            }
            return tags;
        } else {
            return Set.of();
        }
    }


    private InterfaceSchema parseInterface(Element self, String pkgNameDot) {
        String name = self.getAttribute("name").trim();
        Metadata meta = parseOwnToMetadata(self);
        FieldFormat fmt = parseBeanFmt(self);
        String enumRef = self.getAttribute("enumRef").trim();
        String defaultBeanName = self.getAttribute("defaultBeanName").trim();

        List<StructSchema> impls = new ArrayList<>();
        for (Element subSelf : DOMUtil.elements(self, "bean")) {
            StructSchema impl = parseStruct(subSelf, "", true);
            impls.add(impl);
        }

        return new InterfaceSchema(pkgNameDot + name, enumRef, defaultBeanName,
                fmt, meta, impls);
    }

    private FieldFormat parseBeanFmt(Element self) {
        FieldFormat fmt = AUTO;
        String sep = null;
        if (self.hasAttribute("compress")) { // 改为packSep吧
            sep = self.getAttribute("compress").trim();
        } else if (self.hasAttribute("packSep")) {
            sep = self.getAttribute("packSep").trim();
        }
        if (sep != null) {
            require(sep.length() == 1, "分隔符pack长度必须为1");
            char packSeparator = sep.toCharArray()[0];
            fmt = new FieldFormat.Sep(packSeparator);
        }
        return fmt;
    }

    private List<FieldSchema> parseFieldList(Element self, Map<String, FieldTag> tag2OwnField) {
        List<FieldSchema> fields = new ArrayList<>();
        for (Element ele : DOMUtil.elements(self, "column")) {
            FieldSchema field = parseField(ele, tag2OwnField);
            if (field != null) {
                fields.add(field);
            }
        }
        return fields;
    }


    private enum FieldTagPolicy {
        ALL,
        USE_TAG,
        USE_MINUS_TAG
    }

    private static class FieldTag {
        int count;
        FieldTagPolicy policy = FieldTagPolicy.USE_TAG;

        void resolve(int all) {
            if (count == all) {
                policy = FieldTagPolicy.ALL;
            } else if (count >= 0.7 * all) {
                policy = FieldTagPolicy.USE_MINUS_TAG;
            }
        }
    }

    private record FieldTagMap(Map<String, FieldTag> tag2FieldTag,
                               Metadata meta) {
    }

    private FieldTagMap parseOwn(Element self, boolean isImpl) {
        Map<String, FieldTag> tag2FieldTag = new LinkedHashMap<>();
        int all = 0;
        for (Element ele : DOMUtil.elements(self, "column")) {
            Set<String> tags = parseOwnSet(ele);
            for (String tag : tags) {
                //noinspection unused
                FieldTag ownField = tag2FieldTag.computeIfAbsent(tag, k -> new FieldTag());
                ownField.count++;
            }
            all++;
        }

        for (FieldTag of : tag2FieldTag.values()) {
            of.resolve(all);
        }

        Metadata meta = Metadata.of();
        if (!isImpl) { //impl 刘不加了，实际会在interface处
            for (String tag : tag2FieldTag.keySet()) {
                meta.putTag(tag);
            }
        }
        return new FieldTagMap(tag2FieldTag, meta);
    }


    private FieldSchema parseField(Element self, Map<String, FieldTag> tag2OwnField) {
        Set<String> ownSet = parseOwnSet(self);
        Metadata meta = Metadata.of();
        for (Map.Entry<String, FieldTag> e : tag2OwnField.entrySet()) {
            String tag = e.getKey();
            FieldTag ownField = e.getValue();

            switch (ownField.policy) {
                case ALL -> {
                }
                case USE_TAG -> {
                    if (ownSet.contains(tag)) {
                        meta.putTag(tag);
                    }
                }
                case USE_MINUS_TAG -> {
                    if (!ownSet.contains(tag)) {
                        meta.putTag("-" + tag);
                    }
                }
            }
        }

        String name = self.getAttribute("name").trim();
        if (!CfgUtil.isIdentifier(name)) {
            Logger.log("%s not identifier, ignore!", name);
            return null;
        }

        if (self.hasAttribute("range")) {
            String range = self.getAttribute("range").trim();
            if (!range.isEmpty()) {
                meta.data().put("range", new MetaStr(range));
            }
        }

        String comment = self.getAttribute("desc").trim();
        if (!comment.isEmpty() && !comment.equalsIgnoreCase(name)) {
            meta.putComment(comment);
        }

        FieldType type;
        FieldFormat fmt = AUTO;

        require(self.hasAttribute("type"), "column必须设置type");
        String typ = self.getAttribute("type").trim();

        if (typ.startsWith("list,")) {
            String[] sp = typ.split(",");
            String v = sp[1].trim();
            SimpleType item = parseSimpleType(v);
            type = new FList(item);

            if (sp.length > 2) {
                int c = Integer.parseInt(sp[2].trim());
                fmt = new FieldFormat.Fix(c);
            }

        } else if (typ.startsWith("map,")) {
            String[] sp = typ.split(",");
            String k = sp[1].trim();
            String v = sp[2].trim();
            SimpleType key = parseSimpleType(k);
            SimpleType value = parseSimpleType(v);
            type = new FMap(key, value);

            if (sp.length > 3) {
                int c = Integer.parseInt(sp[3].trim());
                fmt = new FieldFormat.Fix(c);
            }

        } else {
            type = parseSimpleType(typ);
        }

        if (self.hasAttribute("block")) {
            fmt = new FieldFormat.Block(1); //block不允许和pack一起配置，简单点
        } else if (self.hasAttribute("pack") || self.hasAttribute("compressAsOne")) {
            fmt = PACK;
        } else if (self.hasAttribute("packSep") || self.hasAttribute("compress")) {  // compress改为packSep
            String sep = self.hasAttribute("packSep") ?
                    self.getAttribute("packSep") : self.getAttribute("compress");
            require(sep.length() == 1, "packSep字符串长度必须是1, " + sep);
            char packSeparator = sep.toCharArray()[0];
            fmt = new FieldFormat.Sep(packSeparator);
        }

        return new FieldSchema(name, type, fmt, meta);
    }

    private List<ForeignKeySchema> parseForeignKeyList(Element self) {
        List<ForeignKeySchema> foreignKeys = new ArrayList<>();
        for (Element ele : DOMUtil.elements(self, "column")) {
            if (ele.hasAttribute("ref")) {
                ForeignKeySchema fk = parseForeignKey(ele, true);
                foreignKeys.add(fk);
            }
        }

        for (Element ele : DOMUtil.elements(self, "foreignKey")) {
            ForeignKeySchema fk = parseForeignKey(ele, false);
            foreignKeys.add(fk);
        }

        return foreignKeys;
    }


    private ForeignKeySchema parseForeignKey(Element self, boolean isFromColumnTag) {
        String name = self.getAttribute("name").trim();
        KeySchema localKey;
        if (isFromColumnTag) {
            localKey = new KeySchema(List.of(name));
        } else {
            localKey = getKeySchema(self, "keys");
        }

        String refStr = self.getAttribute("ref").trim();
        String[] r = refStr.split("\\s*,\\s*");
        String refTable = r[0].trim();
        RefKey refKey;
        boolean nullable = false;
        boolean isList = false;
        if (self.hasAttribute("refType")) {
            String rt = self.getAttribute("refType").trim();
            if (rt.equalsIgnoreCase("nullable")) {
                nullable = true;
            } else if (rt.equalsIgnoreCase("list")) {
                isList = true;
            }
        }
        if (r.length > 1) {
            String[] rs = Arrays.copyOfRange(r, 1, r.length);
            KeySchema keySchema = new KeySchema(Arrays.asList(rs));

            if (isList) {
                refKey = new RefList(keySchema);
            } else {
                refKey = new RefKey.RefUniq(keySchema, nullable);
            }
        } else {
            refKey = new RefPrimary(nullable);
        }

        return new ForeignKeySchema(name, localKey, refTable, refKey, Metadata.of());
    }

    private SimpleType parseSimpleType(String typ) {
        return switch (typ) {
            case "int" -> INT;
            case "long" -> LONG;
            case "bool" -> BOOL;
            case "float" -> FLOAT;
            case "string" -> STRING;
            case "text" -> TEXT;
            default -> new StructRef(typ);
        };
    }

    private void require(boolean cond, Object detailMessage) {
        if (!cond)
            throw new AssertionError(detailMessage);
    }
}
