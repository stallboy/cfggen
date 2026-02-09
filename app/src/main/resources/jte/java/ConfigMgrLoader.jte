@import configgen.genjava.code.ConfigMgrLoaderModel
@param ConfigMgrLoaderModel model
package ${model.pkg};

import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigMgrLoader {

    public static configgen.genjava.Schema loadSchema(configgen.genjava.ConfigInput input) {
        // 1. 读取 Schema（如果有）
        int schemaLength = input.readInt();
        if (schemaLength <= 0) {
            return null;
        } else {
            return configgen.genjava.SchemaDeserializer.deserialize(input);
        }
    }

    public static ConfigMgr load(configgen.genjava.ConfigInput input) {
        ConfigMgr mgr = new ConfigMgr();
        return load(mgr, input);
    }

    public static ConfigMgr load(ConfigMgr mgr, configgen.genjava.ConfigInput input) {

        // 2. 初始化 StringPool 和 LangTextPool
        input.readStringPool();
        input.readLangTextPool();

        // 3. 读取表数据
        int c = input.readInt();
        if (c < ${model.tables.size()}) {
            throw new IllegalArgumentException();
        }

        Map<String, ConfigLoader> allConfigLoaders = getAllConfigLoaders();
        for (int i = 0; i < c; i++) {
            String tableName = input.readString();
            int tableSize = input.readInt();
            ConfigLoader configLoader = allConfigLoaders.get(tableName);
            if (configLoader != null) {
                configLoader.createAll(mgr, input);
            } else {
                input.skipBytes(tableSize);
            }
        }

        for (ConfigLoader configLoader : allConfigLoaders.values()) {
            configLoader.resolveAll(mgr);
        }

        @for(String fc : model.setAllRefs_FullClassNames)
            ${fc}.setAllRefs(mgr);
        @endfor


        return mgr;
    }

    private static Map<String, ConfigLoader> getAllConfigLoaders() {
        Map<String, ConfigLoader> allConfigLoaders = new LinkedHashMap<>();
        @for(configgen.genjava.code.ConfigMgrLoaderModel.TableInfo t : model.tables)
        allConfigLoaders.put("${t.name()}", new ${t.fullName()}._ConfigLoader());
        @endfor


        return allConfigLoaders;
    }
}
