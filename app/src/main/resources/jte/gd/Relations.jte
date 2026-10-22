@import configgen.gengd.ProcessorModel
@import configgen.naming.GenNaming
@import configgen.schema.RefKey
@import configgen.schema.FieldType
@param ProcessorModel model
# Keys contain raw scalar values, never object addresses or derived references.
static func _object_key(parts: Array) -> String:
	var values: Array = []
	for value in parts:
		values.append(_key_value(value))
	return var_to_bytes(values).hex_encode()

static func _key_value(value: Variant) -> Variant:
	if value is Object:
		var fields: Array = value.get_meta(&"_cfggen_key_fields", [])
		var parts: Array = [value.get_meta(&"_cfggen_key_type", "")]
		for field in fields:
			parts.append(_key_value(value.get(field)))
		return parts
	return value

class ${model.relationClass}:
	extends RefCounted
	var _stream: ConfigStream
	var _errors: ConfigErrors
	var _fields: Dictionary = {}
	var _indexes: Dictionary = {}
	var _maps: Dictionary = {}
	var _occurrences: Array = []
	var _shapes: Dictionary = {
		@for(var type : model.relations())
		${model.fullName(type.owner())}: { "name": "${type.owner().fullName()}", "fields": [${model.fields(type.owner())}], "maps": [
			@for(int i = 0; i < type.owner().fields().size(); i++)
				@if(type.owner().fields().get(i).type() instanceof FieldType.FMap)
				${i},
				@endif
			@endfor
		], "indexes": [
			@for(var index : type.indexes())
			{ "field": ${index.field()}, "keys": ${index.keys().toString()} },
			@endfor
		], "plans": [
			@for(var plan : type.plans())
			{ "root": "${plan.root()}", "name": "${GenNaming.refFieldName(plan.reference())}", "nullable": ${((RefKey.RefSimple) plan.reference().refKey()).nullable()}, "sources": [
				@for(var source : plan.sources())
				{ "scope": "${source.scope()}", "fields": ${source.fields().toString()} },
				@endfor
			], "target": "${plan.target()}", "record_key": ${plan.recordKey().toString()}, "steps": [
				@for(var step : plan.steps())
				{ "field": ${step.field()}, "kind": ${step.kind()}, "keys": ${step.keys().toString()} },
				@endfor
			] },
			@endfor
		] },
		@endfor
	}
	var _tables: Dictionary = {}

	func _init(stream: ConfigStream, errors: ConfigErrors):
		_stream = stream
		_errors = errors
		@for(var table : model.tableSchemas)
		_tables["${table.name()}"] = ${model.fullName(table)}.all()
		@endfor

@template.gd.RelationResolver(model = model)

