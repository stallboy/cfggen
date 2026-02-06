@import java.util.List
@param List<String> languages
class_name ConfigText

## 多语言文本支持

var _text_id: String:
	get:
		return _text_id

func _init(text_id: String):
	_text_id = text_id

# 获取当前语言的文本
func get_text() -> String:
	return TextMgr.get_text(_text_id)

# 创建实例
static func create(stream: ConfigStream) -> ConfigText:
	var text_id = stream.get_string()
	return ConfigText.new(text_id)
