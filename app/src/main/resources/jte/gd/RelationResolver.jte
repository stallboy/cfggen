@import configgen.gengd.ProcessorModel
@import configgen.naming.GenNaming
@import configgen.schema.RefKey
@import configgen.schema.FieldType
@param ProcessorModel model
	func _read(value: Object) -> Array:
		var identity: int = value.get_instance_id()
		if not _fields.has(identity):
			var fields: Array = []
			for name in _shapes[value.get_script()].fields:
				fields.append(value.get(name))
			_fields[identity] = fields
		return _fields[identity]

	func _cache_key(owner: Object, field: int) -> String:
		return "%d:%d" % [owner.get_instance_id(), field]

	func _index(identity: String, rows: Array, fields: Array) -> Dictionary:
		var definition: String = identity + ":" + str(fields)
		if _indexes.has(definition):
			return _indexes[definition]
		var index: Dictionary = {}
		_indexes[definition] = index
		for row in rows:
			var values: Array = _read(row)
			var parts: Array = []
			for field in fields:
				parts.append(values[field])
			var key: String = ConfigProcessor._object_key(parts)
			if index.has(key):
				_stream.fail("Duplicate local index key: " + key)
			index[key] = row
		return index

	func _map(identity: String, rows: Dictionary) -> Dictionary:
		if _maps.has(identity):
			return _maps[identity]
		var index: Dictionary = {}
		_maps[identity] = index
		for key in rows:
			var canonical: String = ConfigProcessor._object_key([key])
			if index.has(canonical):
				_stream.fail("Duplicate map key: " + canonical)
			index[canonical] = rows[key]
		return index

	func _walk(value: Variant, root: Object, table: String, parent: Dictionary):
		if value is Object and _shapes.has(value.get_script()):
			var shape: Dictionary = _shapes[value.get_script()]
			var at: Dictionary = { "value": value, "root": root, "table": table, "parent": parent }
			if not shape.plans.is_empty():
				_occurrences.append(at)
			var fields: Array = _read(value)
			for definition in shape.indexes:
				_index(_cache_key(value, definition.field), fields[definition.field], definition.keys)
			for field in shape.maps:
				_map(_cache_key(value, field), fields[field])
			for field in fields:
				_walk(field, root, table, at)
		elif value is Array:
			for item in value:
				_walk(item, root, table, parent)
		elif value is Dictionary:
			for key in value:
				_walk(key, root, table, parent)
				_walk(value[key], root, table, parent)

	func _scope(name: String, at: Dictionary) -> Object:
		if name.is_empty():
			return at.value
		if name == "root":
			return at.root
		while not at.is_empty():
			var value: Object = at.value
			if "ancestor:" + _shapes[value.get_script()].name == name:
				return value
			at = at.parent
		return null

	func resolve():
		for table in _tables:
			for row in _tables[table]:
				_walk(row, row, table, {})
		if not _stream.valid():
			return
		for at in _occurrences:
			for plan in _shapes[at.value.get_script()].plans:
				if plan.root == at.table:
					_bind(at, plan)

	func _bind(at: Dictionary, plan: Dictionary):
		var keys: Array = []
		var valid: bool = true
		for source in plan.sources:
			var value: Variant = _scope(source.scope, at)
			if value != null:
				for field in source.fields:
					value = _read(value)[field]
				keys.append(value)
			else:
				valid = false
		var external: bool = plan.target.begins_with("table:")
		var target: Variant
		if external:
			target = _tables.get(plan.target.substr(6))
		else:
			target = _scope(plan.target, at)
		if not valid or target == null:
			_stream.fail("Missing reference scope: " + str(at.table) + "." + plan.name)
			return
		var used: int = 0
		if external:
			target = _index(plan.target, target, plan.record_key).get(ConfigProcessor._object_key(keys.slice(0, plan.record_key.size())))
			used = plan.record_key.size()
		for step in plan.steps:
			if target == null:
				break
			var member: Variant = _read(target)[step.field]
			if step.kind == 0:
				target = member
			elif step.kind == 1:
				target = _index(_cache_key(target, step.field), member, step.keys).get(ConfigProcessor._object_key(keys.slice(used, used + step.keys.size())))
			else:
				target = _map(_cache_key(target, step.field), member).get(ConfigProcessor._object_key([keys[used]]))
			used += 1 if step.kind == 2 else step.keys.size()
		if target == null and not plan.nullable:
			_errors.ref_null(at.table, plan.name)
			_stream.fail("Missing object reference: " + str(at.table) + "." + plan.name)
		at.value.set(plan.name, target)

