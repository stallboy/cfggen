extends Node

func _ready():
	print("=== GDScript Config Loading Test (Client Mode) ===")

	# 创建错误收集器和处理器
	var errors = ConfigErrors.new()
	var processor = ConfigProcessor.new()

	# 定义处理回调（加载所有配置表）
	var process_func = func(stream: ConfigStream, errors: ConfigErrors):
		processor.load_from_stream(stream, errors)

	# 加载配置并获取 stream
	var stream = ConfigLoader.load_from_file("res://config.bytes", process_func, errors)
	if stream == null:
		print("Failed to load config file")
		return

	# 检查错误
	if errors.get_error_count() > 0:
		print("Errors found:")
		errors.print_all()
		return

	print("Config loaded successfully!")

	# 直接从 stream 获取语言信息
	var lang_names = stream.get_lang_names()
	var lang_pools = stream.get_lang_text_pools()

	print("\nLanguage count: %d" % lang_names.size())
	for i in range(lang_names.size()):
		print("Language %d: %s" % [i, lang_names[i]])

	# 测试多语言切换
	for i in range(lang_pools.size()):
		print("\n=== Switching to language: %s ===" % lang_names[i])
		TextPoolManager.set_global_texts(lang_pools[i])

		# 查询任务数据
		var task = DataTask_Task.find(1)
		print(task);
		if task != null:
			print("Task ID: %d" % task.taskid)
			print("Task Name: %s" % task.name[0].get_text())

	print("\n=== Test completed ===")
