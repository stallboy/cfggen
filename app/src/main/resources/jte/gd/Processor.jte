@import configgen.gengd.ProcessorModel
@import configgen.schema.HasRef
@import configgen.schema.TableSchema
@param ProcessorModel model
class_name ConfigProcessor

# 配置处理器，负责加载所有配置表
# 从流加载所有配置
func load_from_stream(stream: ConfigStream, _errors: ConfigErrors) -> Array[String]:
	var config_tables = {}
	var config_table_names: Array[String] = []
	@for(TableSchema table : model.tableSchemas)
		config_tables["${table.name()}"] = false
		config_table_names.append("${table.name()}")
	@endfor

	while true:
		var csv_name = stream.get_string()
		if csv_name.is_empty():
			break

		match csv_name:
			@for(TableSchema table : model.tableSchemas)
				"${table.name()}":
					config_tables["${table.name()}"] = true
					${model.fullName(table)}._init_from_stream(stream, _errors)
				@endfor
			_:
				_errors.config_unknown(csv_name)
				# 遇到未知表，无法继续处理，直接抛出异常
				assert(false, "Unknown config table: %s" % csv_name)
				break;

	# 检查缺失的配置表
	for table_name in config_tables.keys():
		if not config_tables[table_name]:
			_errors.config_null(table_name)

	# 解析外键引用
	@for(TableSchema table : model.tableSchemas)
		@if(HasRef.hasRef(table))
			${model.fullName(table)}._resolve_refs(_errors)
		@endif
	@endfor

	return config_table_names

