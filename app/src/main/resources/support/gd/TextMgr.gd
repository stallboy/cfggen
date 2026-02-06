class_name TextMgr

## 多语言文本管理器，支持运行时语言切换

static var _current_language: String = "zh_CN"
static var _texts: Dictionary = {}  # {textId: {lang: text}}
static var _default_language: String = "zh_CN"

# 设置当前语言
static func set_language(lang: String):
	_current_language = lang

# 获取当前语言
static func get_language() -> String:
	return _current_language

# 设置默认语言
static func set_default_language(lang: String):
	_default_language = lang

# 获取文本（如果当前语言不存在，则使用默认语言）
static func get_text(text_id: String) -> String:
	if not _texts.has(text_id):
		return "<missing: " + text_id + ">"

	var lang_texts = _texts[text_id]
	if lang_texts.has(_current_language):
		return lang_texts[_current_language]
	elif lang_texts.has(_default_language):
		return lang_texts[_default_language]
	else:
		return "<missing: " + text_id + ">"

# 从流加载多语言数据
static func load_from_stream(stream: ConfigStream, languages: Array):
	# 清空现有数据
	_texts.clear()

	var count = stream.get_32()
	for i in range(count):
		var text_id = stream.get_string()
		var lang_texts = {}
		for lang in languages:
			var text = stream.get_string()
			lang_texts[lang] = text
		_texts[text_id] = lang_texts

# 检查文本是否存在
static func has_text(text_id: String) -> bool:
	return _texts.has(text_id)

# 获取所有文本ID
static func get_all_text_ids() -> Array:
	return _texts.keys()
