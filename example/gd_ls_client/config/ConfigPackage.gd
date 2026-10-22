class_name ConfigPackage

const FORMAT_VERSION: int = 1
var metadata: Dictionary[String, String] = {}
var _blocks: Dictionary = {}

# Versioned metadata and named blocks.
static func read(data: PackedByteArray) -> ConfigPackage:
	var input = ConfigStream.new(data)
	var version = input.read_int32()
	if version != FORMAT_VERSION:
		push_error("Unsupported config format version %d; reader supports 1. Regenerate the reader or export compatible data." % version)
		return null
	var package = ConfigPackage.new()
	for i in range(input.read_count(8)):
		var key = input.read_string()
		var value = input.read_string()
		if package.metadata.has(key):
			push_error("Duplicate metadata: " + key)
			return null
		package.metadata[key] = value
	for i in range(input.read_count(8)):
		var name = input.read_string()
		var bytes = input.read_bytes(input.read_int32())
		if package._blocks.has(name):
			push_error("Duplicate block: " + name)
			return null
		package._blocks[name] = bytes
	if not input.require_end():
		return null
	package.metadata.make_read_only()
	return package

static func read_file(path: String) -> ConfigPackage:
	var file = FileAccess.open(path, FileAccess.READ)
	if file == null:
		push_error("Cannot open package: " + path)
		return null
	return read(file.get_buffer(file.get_length()))

func name() -> String:
	var value = metadata.get("pack", "") as String
	if value.is_empty() or value != value.strip_edges() or value.contains(","):
		push_error("Missing or invalid pack name")
		return ""
	return value

func attach(language: ConfigPackage) -> ConfigPackage:
	if language == null or name().is_empty() or name() != language.name():
		push_error("Language resource belongs to another pack")
		return null
	if language._blocks.is_empty():
		push_error("Empty language resource")
		return null
	var result = ConfigPackage.new()
	result.metadata = metadata
	result._blocks = _blocks.duplicate()
	for key in language._blocks:
		if not key.begins_with("text/") or result._blocks.has(key):
			push_error("Invalid or duplicate language block: " + key)
			return null
		result._blocks[key] = language._blocks[key]
	return result

func open_block(key: String) -> ConfigStream:
	if not _blocks.has(key):
		push_error("Missing block: " + key)
		return null
	return ConfigStream.new(_blocks[key])

func open_data(languages: Array[String] = []) -> ConfigStream:
	if not _blocks.has("schema"):
		push_error("Missing schema block")
		return null
	var input = open_block("strings")
	if input == null:
		return null
	input.read_string_pool()
	if not input.require_end():
		return null
	var names: Array[String] = languages.duplicate()
	if names.is_empty():
		names.append(metadata.get("defaultLanguage", "default"))
	input._lang_names = names
	for language in names:
		var text = open_block("text/" + language)
		if text == null:
			return null
		var texts = text.read_text_block()
		if not text.require_end():
			return null
		if not input._lang_text_pools.is_empty() and texts.size() != input._lang_text_pools[0].size():
			push_error("Language text count mismatch")
			return null
		input._lang_text_pools.append(texts)

	for key in _blocks:
		if key.begins_with("."):
			var child = open_block(key)
			child._string_pool = input._string_pool
			child._lang_names = input._lang_names
			child._lang_text_pools = input._lang_text_pools
			input._table_blocks[key.substr(1)] = child
	return input
