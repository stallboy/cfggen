// CFG 语言语法定义用于 Shiki
export const cfgLang = {
	id: 'cfg',
	scopeName: 'source.cfg',
	displayName: 'CFG',
	embeddedLangs: [],
	patterns: [
		{
			name: 'storage.type.primitive.cfg',
			match: '\\b(int|str|bool|long|float|text)\\b'
		},
		{
			name: 'storage.type.class.cfg',
			match: '\\b(struct|interface|table)\\b'
		},
		{
			name: 'string.quoted.double.cfg',
			begin: '"',
			end: '"',
			patterns: [
				{
					name: 'constant.character.escape.cfg',
					match: '\\\\.'
				}
			]
		},
		{
			name: 'string.quoted.single.cfg',
			begin: "'",
			end: "'",
			patterns: [
				{
					name: 'constant.character.escape.cfg',
					match: '\\\\.'
				}
			]
		},
		{
			name: 'constant.numeric.cfg',
			match: '\\b(\\d+(\\.\\d+)?|0[xX][0-9a-fA-F]+|inf|nan|true|false)\\b'
		},
		{
			name: 'comment.line.double-slash.cfg',
			begin: '//',
			end: '$'
		},
		{
			name: 'meta.definition',
			match: '\\b(struct|interface|table)\\s+\\w+'
		},
		{
			name: 'variable.name.cfg',
			match: '\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*:'
		},
		{
			name: 'entity.name.type.cfg',
			match: '\\b[a-zA-Z_][a-zA-Z0-9_]*\\b'
		}
	]
}
