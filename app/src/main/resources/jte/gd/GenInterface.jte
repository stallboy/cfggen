@import configgen.gengd.InterfaceModel
@import configgen.schema.HasRef
@import configgen.schema.StructSchema
@param InterfaceModel model
class_name ${model.name.className}

## ${model.sInterface.name()}
@if(!model.sInterface.comment().isEmpty())
## ${model.sInterface.comment()}
@endif

@if(model.sInterface.nullableEnumRefTable() != null)
	# 获取接口类型
	func get_type():
		push_error("Must be implemented by subclass")
		return null
@endif

@if(HasRef.hasRef(model.sInterface))
	func _resolve(errors: ConfigErrors):
		pass
@endif

# 创建实例（静态工厂方法）
static func create(stream: ConfigStream) -> ${model.name.className}:
	var type_name = stream.get_string()
	match type_name:
		@for(StructSchema impl : model.sInterface.impls())
			"${impl.name()}":
				return ${model.fullName(impl)}.create(stream)
		@endfor
		_:
			push_error("Unknown type: " + type_name)
			return null
