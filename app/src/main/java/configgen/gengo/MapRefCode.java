package configgen.gengo;

public class MapRefCode {
    public final String codeGetByDefine;
    public MapRefCode(GoName name, String keyType, String valueType) {
        codeGetByDefine = """
                func (t *${className}) GetRef${MapName}Map() map[int32]*${className} {
                	if t.ref${MapName}Map == nil {
                		t.ref${MapName}Map = make(map[int32]*OtherLoot, len(t.${mapName}Map))
                		for k, v := range t.${mapName}Map {
                			t.ref${MapName}Map[k] = Get${MapValueType}Mgr().Get(v)
                		}
                	}
                	return t.ref${MapName}Map
                }
                """.replace("${className}", name.className)
                .replace("${mapName}", name.mapName)
                .replace("${MapName}", name.mapName)
                .replace("${keyType}", keyType)
                .replace("${keyVar}", name.keyVar);
    }
}
