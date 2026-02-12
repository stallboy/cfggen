class_name ConfigLoader

## 配置加载器，用于从字节数组加载配置

# 加载配置的回调函数类型
# processor_func: Callable(stream, errors) -> void
static func load_bytes(data: PackedByteArray, processor_func: Callable, errors: ConfigErrors) -> ConfigStream:
	"""从字节数组加载配置（完全模仿 C# 的 LoadBytes API）"""
	var stream = ConfigStream.new(data)

	# 1. 跳过 Schema（如果有）
	var schema_length = stream.read_32()
	if schema_length > 0:
		stream.skip_bytes(schema_length)

	# 2. 读取 StringPool
	stream.read_string_pool()

	# 3. 读取 LangTextPool
	stream.read_lang_text_pool()

	# 4. 处理表数据（通过回调）
	processor_func.call(stream, errors)

	# 返回 stream，调用者可直接访问语言信息
	return stream

# 便捷方法：从文件加载
static func load_from_file(file_path: String, processor_func: Callable, errors: ConfigErrors) -> ConfigStream:
	"""从文件加载配置（便捷方法）"""
	var file = FileAccess.open(file_path, FileAccess.READ)
	if file == null:
		push_error("Failed to open config file: " + file_path)
		return null

	var bytes = file.get_buffer(file.get_length())
	file.close()

	return load_bytes(bytes, processor_func, errors)
