@import configgen.gengd.ProcessorModel
@import configgen.schema.HasRef
@import configgen.schema.TableSchema
@param ProcessorModel model
class_name ConfigProcessor

# 从流加载所有配置（新格式）
func load_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	var config_nulls: Array[String] = []
	@for(TableSchema table : model.tableSchemas)
		config_nulls.append("${table.name()}")
	@endfor

	# 读取表数量
	var table_count = stream.read_int32()

	for i in range(table_count):
		# 读取表名
		var table_name = stream.read_string()
		# 读取表大小
		var table_size = stream.read_int32()

		match table_name:
			@for(TableSchema table : model.tableSchemas)
				"${table.name()}":
					config_nulls.erase("${table.name()}")
					${model.fullName(table)}._init_from_stream(stream, _errors)
				@endfor
			_:
				# 未知表，跳过
				stream.skip_bytes(table_size)

	# 检查缺失的配置表
	for table_name in config_nulls:
		_errors.config_null(table_name)

	# 解析外键引用
	@for(TableSchema table : model.tableSchemas)
		@if(HasRef.hasRef(table))
			${model.fullName(table)}._resolve_refs(_errors)
		@endif
	@endfor

