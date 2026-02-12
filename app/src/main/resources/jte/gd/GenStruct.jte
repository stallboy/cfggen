@import configgen.schema.*
@import configgen.schema.FieldType.*
@import configgen.gengd.StructModel
@param StructModel model
!{InterfaceSchema nullableInterface = model.structural instanceof StructSchema struct ? struct.nullableInterface() : null;}
!{boolean isImpl = nullableInterface != null;}
@if(isImpl)
class_name ${model.name.className} extends ${model.fullName(nullableInterface)}
@else
class_name ${model.name.className}
@endif

## ${model.structural.name()}
@if(!model.structural.comment().isEmpty())
## ${model.structural.comment()}
@endif

# 公开属性
@for(FieldSchema field : model.structural.fields())
var ${model.fieldName(field)}: ${model.type(field.type())}@if(!field.comment().isEmpty())  # ${field.comment()}@endif
@endfor

# 外键引用属性
@for(ForeignKeySchema fk : model.structural.foreignKeys())
var ${model.refName(fk)}: ${model.refType(fk)}
@endfor

# 创建实例
static func create(stream: ConfigStream) -> ${model.name.className}:
	var instance = ${model.name.className}.new()
	@for(FieldSchema field : model.structural.fields())
		!{String n = field.name();}
		!{FieldType t = field.type();}
		@if(t instanceof FList(SimpleType item))
			for c in range(stream.read_int32()):
				instance.${model.fieldName(field)}.append(${model.create(item)})
		@elseif(t instanceof FMap(SimpleType key, SimpleType value))
			for c in range(stream.read_int32()):
				var k = ${model.create(key)}
				var v = ${model.create(value)}
				instance.${model.fieldName(field)}[k] = v
		@else
			instance.${model.fieldName(field)} = ${model.create(t)}
		@endif
	@endfor
	return instance

@if(model._vTable != null)
	!{TableSchema table = model._vTable.schema();}

	# 主键查询
	static func find(id: ${model.keyType()}) -> ${model.name.className}:
		return _data.get(id)

	@for(KeySchema uk : table.uniqueKeys())
		# 唯一键查询
		static func ${model.uniqueKeyGetByName(uk)}(${model.actualParams(uk)}) -> ${model.name.className}:
			return ${model.uniqueKeyMapName(uk)}.get(${model.actualParams(uk)})
	@endfor

	# 获取所有数据
	static func all() -> Array[${model.name.className}]:
		return _data.values()

	# 从流初始化
	static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
		@for(KeySchema uk : table.uniqueKeys())
			${model.uniqueKeyMapName(uk)} = {}
		@endfor

		var count = stream.read_int32()
		for i in range(count):
			var item = create(stream)
			_data[item.${model.primaryKeyFieldName()}] = item
			@for(KeySchema uk : table.uniqueKeys())
				${model.uniqueKeyMapName(uk)}[item.${model.actualParamsKeySelf(uk)}] = item
			@endfor

			@if(table.entry() instanceof EntryType.EntryBase entryBase)
				!{String ef = model.fieldName(entryBase.fieldSchema());}
				if item.${ef}.strip_edges() != "":
					match item.${ef}.strip_edges():
						@for(String enumName : model._vTable.enumNames())
							"${enumName}":
								if ${model.upper1(enumName)} != null:
									_errors.enum_dup("${model.structural.name()}", str(item))
								${model.upper1(enumName)} = item
						@endfor
						_:
							_errors.enum_data_add("${model.structural.name()}", str(item))
			@endif

		@if(table.entry() instanceof EntryType.EntryBase entryBase)
			@for(String enumName : model._vTable.enumNames())
				if ${model.upper1(enumName)} == null:
					_errors.enum_null("${model.structural.name()}", "${enumName}")
			@endfor
		@endif

	# 内部存储
	static var _data: Dictionary[${model.keyType()}, ${model.name.className}] = {}
	@for(KeySchema uk : table.uniqueKeys())
		static var ${model.uniqueKeyMapName(uk)}: ${model.dictionaryType(uk, model.name.className)} = {}
	@endfor

	@if(model._vTable.schema().entry() instanceof EntryType.EntryBase)
		# 静态枚举实例
		@for(String enumName : model._vTable.enumNames())
			static var ${model.upper1(enumName)}: ${model.name.className}
		@endfor
	@endif
@endif

# 解析外键引用
@if(configgen.schema.HasRef.hasRef(model.structural))
	func _resolve(errors: ConfigErrors):
		@for(FieldSchema field : model.structural.fields())
			!{FieldType type = field.type();}
			@if(configgen.schema.HasRef.hasRef(type))
				@if(type instanceof StructRef)
					if ${model.fieldName(field)} != null:
						${model.fieldName(field)}._resolve(errors)
				@elseif(type instanceof FList)
					for item in ${model.fieldName(field)}:
						if item != null:
							item._resolve(errors)
				@elseif(type instanceof FMap)
					for item in ${model.fieldName(field)}.values():
						if item != null:
							item._resolve(errors)
				@endif
			@endif
		@endfor

		@for(ForeignKeySchema fk : model.structural.foreignKeys())
			@if(fk.refKey() instanceof RefKey.RefSimple refSimple)
				!{var firstField = fk.key().fieldSchemas().getFirst();}
				!{var refName = model.refName(fk);}
				!{var fkStr = "\"" + fk.name() + "\"";}

				@if(firstField.type() instanceof SimpleType)
					${refName} = ${model.tableGet(fk.refTableSchema(), refSimple, model.actualParams(fk.key()))}
					@if(!refSimple.nullable())
						if ${refName} == null:
							errors.ref_null("${model.structural.name()}", ${fkStr})
					@endif
				@elseif(firstField.type() instanceof FList)
					for item in ${model.fieldName(firstField)}:
						var r = ${model.tableGet(fk.refTableSchema(), refSimple, "item")}
						if r == null:
							errors.ref_null("${model.structural.name()}", ${fkStr})
						${refName}.append(r)
				@elseif(firstField.type() instanceof FMap)
					for k in ${model.fieldName(firstField)}.keys():
						var v = ${model.tableGet(fk.refTableSchema(), refSimple, model.fieldName(firstField) + "[k]")}
						if v == null:
							errors.ref_null("${model.structural.name()}", ${fkStr})
						${refName}[k] = v
				@endif
			@endif
		@endfor

		@for(ForeignKeySchema fk : model.structural.foreignKeys())
			@if(fk.refKey() instanceof RefKey.RefList refList)
				!{var refName = model.refName(fk);}
				!{var refTable = model.fullName(fk.refTableSchema());}
				for v in ${refTable}.all():
					@if(refList.keyNames().size() == 1)
						!{var keyField = refList.keyNames().getFirst();}
						!{var localField = fk.key().fields().getFirst();}
						if v.${keyField} == ${localField}:
							${refName}.append(v)
					@else
						!{var conditions = new java.util.ArrayList<String>();}
						@for(int i = 0; i < refList.keyNames().size(); i++)
							!{conditions.add("v." + refList.keyNames().get(i) + " == " + fk.key().fields().get(i));}
						@endfor
						if ${String.join(" and ", conditions)}:
							${refName}.append(v)
					@endif
			@endif
		@endfor

	@if(model._vTable != null)
		static func _resolve_refs(errors: ConfigErrors):
			for item in all():
				item._resolve(errors)
	@endif
@endif
