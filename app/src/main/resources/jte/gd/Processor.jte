@import configgen.gengd.ProcessorModel
@import configgen.schema.HasRef
@import configgen.schema.TableSchema
@param ProcessorModel model
class_name ConfigProcessor

## 配置处理器，负责加载所有配置表

var _errors: ConfigErrors

func _init():
	_errors = ConfigErrors.new()

func get_errors() -> ConfigErrors:
	return _errors

# 从流加载所有配置
func load_from_stream(stream: ConfigStream):
	var config_tables = {}
	@for(TableSchema table : model.tableSchemas)
		config_tables["${table.name()}"] = false
	@endfor

	while true:
		var csv_name = stream.get_string()
		if csv_name.is_empty():
			break

		match csv_name:
			@for(TableSchema table : model.tableSchemas)
				"${table.name()}":
					config_tables["${table.name()}"] = true
					${model.fullName(table)}._init_from_stream(stream)
				@endfor
			_:
				_errors.error("未知配置表: " + csv_name)
				# 跳过未知表的数据
				_skip_table_data(stream)

	# 检查缺失的配置表
	for table_name in config_tables.keys():
		if not config_tables[table_name]:
			_errors.error("配置表缺失: " + table_name)

	# 解析外键引用
	@for(TableSchema table : model.tableSchemas)
		@if(HasRef.hasRef(table))
			${model.fullName(table)}._resolve_refs(_errors)
		@endif
	@endfor

# 跳过表数据（用于未知表）
func _skip_table_data(stream: ConfigStream):
	var count = stream.get_32()
	for i in range(count):
		# 跳过一行数据（简化处理，假设只有一列）
		stream.get_string()

# 检查是否有错误
func has_errors() -> bool:
	return _errors.has_errors()

# 获取错误消息
func get_error_messages() -> Array:
	return _errors.get_messages()
