class_name ConfigStream

## 二进制配置读取流，用于从字节数组读取配置数据

var _data: PackedByteArray
var _pos: int = 0
var _string_pool: Array[String] = []  # 字符串池，用于去重
var _lang_names: Array[String] = []  # 语言名称列表
var _lang_text_pools: Array[Array] = []  # 多语言文本池

func _init(data: PackedByteArray):
	_data = data

# 读取字符串（用于表名）
func read_cfg() -> String:
	var length = read_32()
	if length <= 0:
		if _pos >= _data.size():
			return ""
		return ""
	var bytes = _data.slice(_pos, _pos + length)
	_pos += length
	return bytes.get_string_from_utf8()

# 读取字符串池（可选，如果二进制数据使用了 stringpool）
func read_string_pool():
	"""读取字符串池，必须在读取字符串数据之前调用"""
	var count = read_32()
	_string_pool = []
	for i in range(count):
		var length = read_32()
		if length <= 0:
			_string_pool.append("")
		else:
			var bytes = _data.slice(_pos, _pos + length)
			_pos += length
			_string_pool.append(bytes.get_string_from_utf8())

# 读取 LangTextPool（在读取表数据之前调用）
func read_lang_text_pool():
	"""读取多语言文本池"""
	var lang_count = read_32()
	_lang_names = []
	_lang_text_pools = []

	for lang_idx in range(lang_count):
		var length = read_32()
		if length <= 0:
			_lang_names.append("")
		else:
			var bytes = _data.slice(_pos, _pos + length)
			_pos += length
			_lang_names.append(bytes.get_string_from_utf8())

		# 读取索引数组
		var index_count = read_32()
		var indices = []
		for i in range(index_count):
			indices.append(read_32())

		# 读取该语言的字符串池
		var pool_count = read_32()
		var pool = []
		for i in range(pool_count):
			var str_length = read_32()
			if str_length <= 0:
				pool.append("")
			else:
				var bytes = _data.slice(_pos, _pos + str_length)
				_pos += str_length
				pool.append(bytes.get_string_from_utf8())

		# 构建文本数组：texts[textIndex] = pool[indices[textIndex]]
		var texts = []
		texts.resize(index_count)
		for i in range(index_count):
			texts[i] = pool[indices[i]]

		_lang_text_pools.append(texts)

# 读取字符串
func read_string() -> String:
	"""直接读取字符串数据"""
	var length = read_32()
	if length <= 0:
		return ""
	var bytes = _data.slice(_pos, _pos + length)
	_pos += length
	return bytes.get_string_from_utf8()

# 从字符串池读取字符串
func read_string_in_pool() -> String:
	"""从字符串池中读取字符串（通过索引）"""
	if _string_pool.is_empty():
		push_error("字符串池未初始化，请先调用 read_string_pool()")
		return ""
	var index = read_32()
	if index < 0 or index >= _string_pool.size():
		push_error("字符串索引越界: %d" % index)
		return ""
	return _string_pool[index]

# 从多语言文本池读取文本
func read_text_in_pool() -> String:
	"""从多语言文本池中读取文本（通过索引）"""
	if _lang_text_pools.is_empty():
		push_error("文本池未初始化，请先调用 read_lang_text_pool()")
		return ""
	var index = read_32()
	var current_lang_pool = _lang_text_pools[TextPoolManager._current_lang_index]
	if index < 0 or index >= current_lang_pool.size():
		push_error("文本索引越界: %d" % index)
		return ""
	return current_lang_pool[index]

# 读取32位整数
func read_32() -> int:
	var value = _data.decode_s32(_pos)
	_pos += 4
	return value

# 读取64位整数
func read_64() -> int:
	var value = _data.decode_s64(_pos)
	_pos += 8
	return value

# 读取布尔值
func get_bool() -> bool:
	return read_32() != 0

# 读取浮点数
func read_float() -> float:
	var value = _data.decode_float(_pos)
	_pos += 4
	return value

# 跳过指定字节数
func skip_bytes(count: int):
	"""跳过指定数量的字节（用于跳过 Schema）"""
	_pos += count

# 读取文本索引（客户端模式 TEXT 类型字段）
func read_text_index() -> int:
	"""读取文本索引（用于 ConfigText）"""
	return read_32()

# 获取语言名称列表
func get_lang_names() -> Array[String]:
	"""获取所有支持的语言名称"""
	return _lang_names

# 获取多语言文本池
func get_lang_text_pools() -> Array[Array]:
	"""获取所有语言的文本池（用于语言切换）"""
	return _lang_text_pools

# 检查是否已到达末尾
func is_eof() -> bool:
	return _pos >= _data.size()
