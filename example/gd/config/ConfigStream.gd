class_name ConfigStream

## 二进制配置读取流，用于从字节数组读取配置数据

var _data: PackedByteArray
var _pos: int = 0
var _string_pool: Array[String] = []  # 字符串池，用于去重

func _init(data: PackedByteArray):
	_data = data

# 读取字符串（用于表名）
func read_cfg() -> String:
	var s = read_string()
	if s.is_empty() and _pos >= _data.size():
		return ""
	return s

# 读取字符串池（可选，如果二进制数据使用了 stringpool）
func read_string_pool():
	"""读取字符串池，必须在 read_string() 之前调用"""
	var count = read_32()
	_string_pool = []
	for i in range(count):
		_string_pool.append(read_string_impl())

# 读取字符串
func read_string() -> String:
	"""读取字符串，如果已读取 stringpool 则从池中获取索引"""
	if not _string_pool.is_empty():
		var index = read_32()
		return _string_pool[index]
	else:
		return read_string_impl()

# 实际的字符串读取实现
func read_string_impl() -> String:
	"""实际的字符串读取实现"""
	var length = read_32()
	if length <= 0:
		return ""
	var bytes = _data.slice(_pos, _pos + length)
	_pos += length
	return bytes.get_string_from_utf8()

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

# 检查是否已到达末尾
func is_eof() -> bool:
	return _pos >= _data.size()
