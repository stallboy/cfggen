@import configgen.genjava.code.StructuralClassModel
@import configgen.genjava.code.StructuralClassModel.FieldInfo
@param StructuralClassModel model
package ${model.pkg};

@if(model.cfg.jspecify() && model.fields.stream().anyMatch(FieldInfo::needsBuilderValue))
import org.jspecify.annotations.Nullable;

@endif
public class ${model.className}Builder {
    @for(FieldInfo field : model.fields)
        public ${field.needsBuilderValue() ? model.cfg.nullable(field.type()) : field.type()} ${field.name()}${field.primitive() || field.needsBuilderValue() ? "" : " = " + field.builderDefault()};
    @endfor

${"\n"}    public ${model.className} build() {
        return new ${model.className}(this);
    }
}
