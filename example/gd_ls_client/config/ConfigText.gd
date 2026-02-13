class_name ConfigText

## 多语言文本支持（客户端模式）

var _text_index: int

func _init(text_index: int):
	_text_index = text_index

# 获取当前语言的文本（通过 TextPoolManager）
func get_text() -> String:
	return TextPoolManager.get_text(_text_index)

# 创建实例
static func _create(stream: ConfigStream) -> ConfigText:
	var text_index = stream.read_text_index()
	return ConfigText.new(text_index)

# 字符串表示
func _to_string() -> String:
	return get_text()
