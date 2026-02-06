class_name ConfigLoader

## 配置加载器，用于从字节数组加载配置

static var _processor: ConfigProcessor = null

# 设置配置处理器
static func set_processor(processor: ConfigProcessor):
	_processor = processor

# 从字节数组加载配置
static func load_from_bytes(data: PackedByteArray):
	if _processor == null:
		_processor = ConfigProcessor.new()

	var stream = ConfigStream.new(data)
	_processor.load_from_stream(stream)

# 检查是否有错误
static func has_errors() -> bool:
	return _processor != null and _processor.has_errors()

# 获取错误消息
static func get_error_messages() -> Array:
	if _processor == null:
		return []
	return _processor.get_error_messages()

# 打印所有消息
static func print_errors():
	if _processor != null:
		_processor.get_errors().print_all()
