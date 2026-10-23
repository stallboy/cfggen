@import configgen.genjava.code.ConfigMgrLoaderModel
@param ConfigMgrLoaderModel model
package ${model.pkg};

import configgen.genjava.ConfigLoader;
import configgen.genjava.ConfigPackage;
import configgen.genjava.LoadValueErrs;
import configgen.genjava.SchemaEnum;
@if(!model.relationBinding.isEmpty())
import configgen.genjava.RelationResolver;
import configgen.genjava.RelationResolver.Plan;
@endif
import java.util.*;

public class ConfigMgrLoader {
    @if(!model.languageLiterals.isEmpty())
        private static final String[] LANGUAGES = { ${model.languageLiterals} };
    @endif

    public static ConfigMgr loadPackagesAndSetMgr(ConfigPackage... packages) {
        ConfigMgr config = loadPackages(packages);
        ConfigMgr.setMgr(config);
        return config;
    }

    public static ConfigMgr loadPackages(ConfigPackage... packages) {
        ConfigPackage.validateOrder(List.of(packages));
        var errors = new LoadValueErrs();
        ConfigMgr mgr = new ConfigMgr();
        var loaders = newLoaders(mgr, errors);
        var expected = ConfigCodeSchema.getPackageSchema();
        for (var pack : packages) {
            pack.validateSchema(expected, errors);
            for (String block : pack.blockNames()) {
                if (block.startsWith(".")) {
                    String table = block.substring(1);
                    if (!loaders.containsKey(table) && !(expected.implementations.get(table) instanceof SchemaEnum symbol && !symbol.isEnumPart)) {
                        errors.unknownTableBlock(pack.name(), block);
                    }
                }
            }
        }
        errors.checkErrors();
        for (var pack : packages) {
            String packName = pack.name();
            for (String block : pack.blockNames()) {
                if (!block.startsWith(".")) {
                    continue;
                }
                String table = block.substring(1);
                var loader = loaders.get(table);
                if (loader != null) {
                    loader.read(pack.block(block${model.languageLiterals.isEmpty() ? "" : ", LANGUAGES"}), packName, pack == packages[0]);
                }
            }
        }
        return finish(mgr, loaders, errors);
    }

    private static ConfigMgr finish(ConfigMgr mgr, Map<String, ConfigLoader<?, ?>> loaders, LoadValueErrs errors) {
        for (var entry : loaders.entrySet()) {
            mgr.setOrigins(entry.getKey(), entry.getValue().origins());
            entry.getValue().index();
        }
        errors.checkErrors();
        @for(String fc : model.setAllRefs_FullClassNames)
        ${fc}.setAllRefs(mgr, errors);
        @endfor
        for (var loader : loaders.values()) {
            loader.resolveAll();
        }
        @if(!model.relationBinding.isEmpty())
        resolveRelations(mgr, errors);
        @endif
        errors.checkErrors();
        @if(!model.extensionRows.isEmpty())
        var seen = Collections.newSetFromMap(new IdentityHashMap<ConfigExtension, Boolean>());
        var extensions = new ArrayList<ConfigExtension>();
        java.util.function.Consumer<ConfigExtension> collect = extension -> {
            if (seen.add(extension)) {
                extension._bind(mgr);
                extensions.add(extension);
            }
        };
        @for(String rows : model.extensionRows)
        for (var row : ${rows}) {
            row._collectExtensions(collect);
        }
        @endfor
        for (var extension : extensions) {
            extension.onConfigReady();
        }
        @endif
        mgr.markReady();
        return mgr;
    }

    private static Map<String, ConfigLoader<?, ?>> newLoaders(ConfigMgr mgr, LoadValueErrs errors) {
        var loaders = new LinkedHashMap<String, ConfigLoader<?, ?>>();
        @for(configgen.genjava.code.ConfigMgrLoaderModel.TableInfo t : model.tables)
        loaders.put("${t.name()}", new ConfigLoader<>("${t.name()}", errors, ${t.fullName()}::_create,
                ${t.key()}, ${t.index()}, ${t.resolve()}));
        @endfor
        return loaders;
    }

${model.relationBinding}
}
