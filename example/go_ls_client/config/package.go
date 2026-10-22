package config

import (
	"bytes"
	"fmt"
	"strings"
)

const ConfigFormatVersion = 1

// ConfigPackage contains versioned metadata and bounded named blocks. ReadPackage panics on malformed input, like Stream.
type ConfigPackage struct {
	metadata map[string]string
	blocks   map[string][]byte
	order    []string
}

func ReadPackage(data []byte) *ConfigPackage {
	input := &Stream{reader: bytes.NewReader(data)}
	version := input.ReadInt32()
	if version != ConfigFormatVersion {
		panic(fmt.Sprintf("unsupported config format version %d; reader supports 1. Regenerate the reader or export compatible data.", version))
	}
	pack := &ConfigPackage{metadata: make(map[string]string), blocks: make(map[string][]byte)}
	for count := input.ReadCount(8); count > 0; count-- {
		key, value := input.ReadString(), input.ReadString()
		if _, exists := pack.metadata[key]; exists {
			panic("duplicate metadata: " + key)
		}
		pack.metadata[key] = value
	}
	for count := input.ReadCount(8); count > 0; count-- {
		name, size := input.ReadString(), input.ReadSize()
		if _, exists := pack.blocks[name]; exists {
			panic("duplicate block: " + name)
		}
		if size > input.remaining() {
			panic("read outside input block")
		}
		start := len(data) - input.remaining()
		pack.blocks[name] = data[start : start+size]
		pack.order = append(pack.order, name)
		input.SkipBytes(size)
	}
	input.RequireEnd()
	return pack
}

func (p *ConfigPackage) Metadata() map[string]string {
	result := make(map[string]string, len(p.metadata))
	for k, v := range p.metadata {
		result[k] = v
	}
	return result
}

func (p *ConfigPackage) Name() string {
	name := p.metadata["pack"]
	if name == "" || name != strings.TrimSpace(name) || strings.Contains(name, ",") {
		panic("missing or invalid pack name")
	}
	return name
}

func (p *ConfigPackage) Block(name string) *Stream {
	data, exists := p.blocks[name]
	if !exists {
		panic("missing block: " + name)
	}
	return &Stream{reader: bytes.NewReader(data)}
}

func (p *ConfigPackage) Attach(language *ConfigPackage) *ConfigPackage {
	if p.Name() != language.Name() {
		panic("language resource belongs to another pack")
	}
	result := &ConfigPackage{metadata: p.metadata, blocks: make(map[string][]byte), order: append([]string{}, p.order...)}
	for name, block := range p.blocks {
		result.blocks[name] = block
	}
	if len(language.blocks) == 0 {
		panic("empty language resource")
	}
	for _, name := range language.order {
		if !strings.HasPrefix(name, "text/") {
			panic("unexpected language resource block: " + name)
		}
		if _, exists := result.blocks[name]; exists {
			panic("duplicate language block: " + name)
		}
		result.blocks[name] = language.blocks[name]
		result.order = append(result.order, name)
	}
	return result
}

func (p *ConfigPackage) OpenData(languages ...string) *Stream {
	p.Block("schema")
	input := p.Block("strings")
	input.ReadStringPool()
	input.RequireEnd()
	if len(languages) == 0 {
		language := p.metadata["defaultLanguage"]
		if language == "" {
			language = "default"
		}
		languages = []string{language}
	}
	input.langNames = append([]string{}, languages...)
	input.langTextPools = make([][]string, len(languages))
	for i, language := range languages {
		block := p.Block("text/" + language)
		indices := make([]int, block.ReadCount(4))
		for j := range indices {
			indices[j] = block.ReadSize()
		}
		pool := make([]string, block.ReadCount(4))
		for j := range pool {
			pool[j] = block.ReadString()
		}
		block.RequireEnd()
		texts := make([]string, len(indices))
		for j, index := range indices {
			if index >= len(pool) {
				panic("invalid text pool index")
			}
			texts[j] = pool[index]
		}
		if i > 0 && len(texts) != len(input.langTextPools[0]) {
			panic("language text count mismatch")
		}
		input.langTextPools[i] = texts
	}
	input.tables = make([]TableBlock, 0)
	for _, name := range p.order {
		if strings.HasPrefix(name, ".") {
			child := p.Block(name)
			child.stringPool = input.stringPool
			child.langNames = input.langNames
			child.langTextPools = input.langTextPools
			input.tables = append(input.tables, TableBlock{strings.TrimPrefix(name, "."), child})
		}
	}
	return input
}
