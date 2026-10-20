class_name ConfigLoader

static func load_package(package: ConfigPackage, processor_func: Callable, errors: ConfigErrors, languages: Array[String] = []) -> ConfigStream:
	if package == null:
		return null
	if package.name().is_empty():
		return null
	if not package.metadata.get("requires", "").is_empty():
		errors.error("This loader supports standalone packages; Mod composition requires Java/C#")
		return null
	var stream = package.open_data(languages)
	if stream == null:
		return null
	processor_func.call(stream, errors)
	return stream if stream.valid() else null
