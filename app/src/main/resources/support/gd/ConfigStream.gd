class_name ConfigStream

## 二进制配置读取流，用于从字节数组读取配置数据

var _data: PackedByteArray
var _pos: int = 0
var _string_pool: Array[String] = []  # 字符串池，用于去重
var _lang_names: Array[String] = []  # 语言名称列表
var _lang_text_pools: Array[Array] = []  # 多语言文本池
var _failed: bool = false
var _table_blocks: Dictionary = {}


func _init(data: PackedByteArray):
	_data = data

func valid() -> bool:
	return not _failed

func remaining() -> int:
	return _data.size() - _pos

func fail(message: String):
	_failed = true
	push_error(message)

func require_bytes(length: int) -> bool:
	if _failed:
		return false
	if length < 0 or length > remaining():
		fail("Read outside input block: %d" % length)
		return false
	return true

func require_end() -> bool:
	if remaining() != 0:
		fail("Unconsumed block bytes")
	return valid()

func read_count(minimum_bytes: int = 4) -> int:
	var count = read_int32()
	if count < 0 or minimum_bytes > 0 and count > remaining() / minimum_bytes:
		fail("Invalid count: %d" % count)
		return 0
	return count

func read_bytes(length: int) -> PackedByteArray:
	if not require_bytes(length):
		return PackedByteArray()
	var result = _data.slice(_pos, _pos + length)
	_pos += length
	return result

func read_tables() -> Dictionary:
	return _table_blocks

func read_text_block() -> Array:
	var indices: Array[int] = []
	for i in range(read_count()):
		indices.append(read_int32())
	var pool: Array[String] = []
	for i in range(read_count()):
		pool.append(read_string())
	var texts: Array[String] = []
	for index in indices:
		if index < 0 or index >= pool.size():
			fail("Invalid text pool index")
			return []
		texts.append(pool[index])
	return texts


# 读取字符串池（可选，如果二进制数据使用了 stringpool）
func read_string_pool():
	"""读取字符串池，必须在读取字符串数据之前调用"""
	var count = read_count()
	_string_pool = []
	for i in range(count):
		_string_pool.append(read_string())

# 读取字符串
func read_string() -> String:
	"""直接读取字符串数据"""
	var length = read_int32()
	return read_bytes(length).get_string_from_utf8()

# 从字符串池读取字符串
func read_string_in_pool() -> String:
	"""从字符串池中读取字符串（通过索引）"""
	var index = read_int32()
	if index < 0 or index >= _string_pool.size():
		fail("String pool index out of bounds: %d" % index)
		return ""
	return _string_pool[index]

# 从多语言文本池读取文本
func read_text_in_pool() -> String:
	"""从多语言文本池中读取文本（通过索引）"""
	if _lang_text_pools.is_empty():
		fail("Text pools not initialized")
		return ""
	var index = read_int32()
	# 服务器模式：直接使用第一个语言的文本
	var current_lang_pool = _lang_text_pools[0]
	if index < 0 or index >= current_lang_pool.size():
		fail("Text index out of bounds: %d" % index)
		return ""
	return current_lang_pool[index]

# 读取32位整数
func read_int32() -> int:
	if not require_bytes(4):
		return 0
	var value = _data.decode_s32(_pos)
	_pos += 4
	return value

# 读取64位整数
func read_int64() -> int:
	if not require_bytes(8):
		return 0
	var value = _data.decode_s64(_pos)
	_pos += 8
	return value

# 读取布尔值
func read_bool() -> bool:
	if not require_bytes(1):
		return false
	var value = _data.decode_s8(_pos) != 0
	_pos += 1
	return value

# 读取浮点数
func read_float() -> float:
	if not require_bytes(4):
		return 0.0
	var value = _data.decode_float(_pos)
	_pos += 4
	return value

# 跳过指定字节数
func skip_bytes(count: int):
	"""跳过指定数量的字节（用于跳过 Schema）"""
	if require_bytes(count):
		_pos += count

# 读取文本索引（客户端模式 TEXT 类型字段）
func read_text_index() -> int:
	"""读取文本索引（用于 ConfigText）"""
	var index = read_int32()
	if _lang_text_pools.is_empty() or index < 0 or index >= _lang_text_pools[0].size():
		fail("Invalid text index: %d" % index)
	return index

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
