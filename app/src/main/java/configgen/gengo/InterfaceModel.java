package configgen.gengo;

import configgen.schema.InterfaceSchema;

public record InterfaceModel(String pkg,
                             GoName name,
                             InterfaceSchema sInterface) {
}
