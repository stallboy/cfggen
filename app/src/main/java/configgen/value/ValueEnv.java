package configgen.value;

import org.jspecify.annotations.Nullable;

import configgen.data.CfgData;
import configgen.data.HeadRow;
import configgen.i18n.TranslationCatalog;
import configgen.layout.JsonFileInfo;
import configgen.schema.CfgSchema;

import java.util.Collection;
import java.util.function.Function;

/// 值解析所需的全局环境（原直接持有ctx.Context，为解value→ctx依赖环改为参数对象）。
/// 由Context.makeValue在锁内构建快照，保证与解析时刻的数据一致。
public record ValueEnv(CfgSchema fullSchema,
                       CfgData cfgData,
                       HeadRow headRow,
                       @Nullable TranslationCatalog nullableTranslationCatalog,
                       Function<String, Collection<JsonFileInfo>> jsonFilesByTable) {
}
