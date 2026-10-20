@import configgen.gengd.ProcessorModel
@import configgen.schema.HasRef
@import configgen.schema.TableSchema
@param ProcessorModel model
class_name ConfigProcessor

const TABLE_NAMES: Array[String] = [
	@for(TableSchema table : model.tableSchemas)
		"${table.name()}",
	@endfor
]

func load_from_package(package: ConfigPackage, errors: ConfigErrors) -> ConfigStream:
	return ConfigLoader.load_package(package, load_from_stream, errors${model.languageArguments().isEmpty() ? "" : ", [" + model.languageArguments() + "]"})

# 从流加载所有配置（新格式）
func load_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	# 读取表数量
	var tables = stream.read_tables()
	if not stream.valid():
		return
	for table_name in TABLE_NAMES:
		if not tables.has(table_name):
			stream.fail("Missing table in standalone package: " + table_name)
			return
	for table_name in tables:
		var table_input: ConfigStream = tables[table_name]

		match table_name:
			@for(TableSchema table : model.tableSchemas)
				"${table.name()}":
					${model.fullName(table)}._init_from_stream(table_input, _errors)
					if not table_input.require_end():
						stream.fail("Invalid table: " + table_name)
						return
				@endfor
			_:
				# 未知表，跳过
				pass

	# 解析外键引用
	@for(TableSchema table : model.tableSchemas)
		@if(HasRef.hasRef(table))
			${model.fullName(table)}._resolve_refs(_errors)
		@endif
	@endfor

