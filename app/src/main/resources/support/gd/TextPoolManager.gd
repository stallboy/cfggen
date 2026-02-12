class_name TextPoolManager

## 多语言文本池管理器（客户端模式）
## 运行时动态切换语言，无需重新加载配置

static var _global_texts: Array[String] = []  # 当前语言的文本数组

# 设置全局文本数组（语言切换时调用）
static func set_global_texts(texts: Array[String]):
	"""设置当前语言的文本数组"""
	_global_texts = texts

# 获取文本（通过索引）
static func get_text(index: int) -> String:
	"""获取指定索引的文本"""
	if index < 0 or index >= _global_texts.size():
		return "<missing:" + str(index) + ">"
	return _global_texts[index]
