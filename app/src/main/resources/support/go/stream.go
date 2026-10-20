package config

import (
	"bytes"
	"encoding/binary"
	"fmt"
	"io"
)

type Stream struct {
	reader *bytes.Reader

	// StringPool 和 LangTextPool 字段
	stringPool    []string
	langNames     []string
	langTextPools [][]string // langTextPools[langIndex][textIndex]
	tables        []TableBlock
}

type TableBlock struct {
	Name  string
	Input *Stream
}

func (s *Stream) remaining() int {
	return s.reader.Len()
}

func (s *Stream) RequireEnd() {
	if s.remaining() != 0 {
		panic("unconsumed block bytes")
	}
}

func (s *Stream) ReadCount(minimumBytes int) int {
	count := s.ReadSize()
	if minimumBytes > 0 && count > s.remaining()/minimumBytes {
		panic(fmt.Sprintf("invalid count: %d", count))
	}
	return count
}

func (s *Stream) ReadTables() []TableBlock { return s.tables }
func (s *Stream) ReadBool() bool {
	var value byte
	if err := binary.Read(s.reader, binary.LittleEndian, &value); err != nil {
		panic(fmt.Errorf("read bool: %w", err))
	}
	return value != 0
}

func (s *Stream) ReadInt32() int32 {
	var value int32
	if err := binary.Read(s.reader, binary.LittleEndian, &value); err != nil {
		panic(fmt.Errorf("read int32: %w", err))
	}
	return value
}

func (s *Stream) ReadInt64() int64 {
	var value int64
	if err := binary.Read(s.reader, binary.LittleEndian, &value); err != nil {
		panic(fmt.Errorf("read int64: %w", err))
	}
	return value
}

func (s *Stream) ReadFloat32() float32 {
	var value float32
	if err := binary.Read(s.reader, binary.LittleEndian, &value); err != nil {
		panic(fmt.Errorf("read float32: %w", err))
	}
	return value
}

// ReadSize 读取大小（int32 别名）
func (s *Stream) ReadSize() int {
	value := int(s.ReadInt32())
	if value < 0 {
		panic(fmt.Sprintf("negative count or length: %d", value))
	}
	return value
}

// ReadString 从 io.Reader 中读取格式为 [int32长度][UTF-8内容] 的字符串
func (s *Stream) ReadString() string {
	length := s.ReadSize()
	if length > s.remaining() {
		panic("read outside input block")
	}
	if length == 0 {
		return ""
	}
	buf := make([]byte, length)
	if _, err := io.ReadFull(s.reader, buf); err != nil {
		panic(fmt.Errorf("read string: %w", err))
	}
	return string(buf)
}

// ReadStringPool 读取全局字符串池（在读取表数据之前调用）
func (s *Stream) ReadStringPool() {
	count := s.ReadCount(4)
	s.stringPool = make([]string, count)
	for i := 0; i < count; i++ {
		s.stringPool[i] = s.ReadString()
	}
}

// ReadStringInPool 从 StringPool 读取字符串（用于 STRING 类型字段）
func (s *Stream) ReadStringInPool() string {
	index := s.ReadInt32()
	if s.stringPool == nil {
		panic("StringPool not initialized")
	}
	if index < 0 || int(index) >= len(s.stringPool) {
		panic(fmt.Sprintf("index %d out of StringPool bounds [0, %d)", index, len(s.stringPool)))
	}
	return s.stringPool[index]
}

// ReadTextsInPool 从 LangTextPool 读取所有语言文本（多语言 服务器端模式）
func (s *Stream) ReadTextsInPool() []string {
	index := s.ReadTextIndex()
	texts := make([]string, len(s.langTextPools))
	for i := range texts {
		texts[i] = s.langTextPools[i][index]
	}
	return texts
}

// ReadTextInPool 从 LangTextPool 读取 text（单语言模式）
func (s *Stream) ReadTextInPool() string {
	index := s.ReadTextIndex()
	return s.langTextPools[0][index]
}

// ReadTextIndex 从 LangTextPool 读取文本索引（客户端模式）
func (s *Stream) ReadTextIndex() int {
	index := s.ReadSize()
	if len(s.langTextPools) == 0 || index >= len(s.langTextPools[0]) {
		panic("invalid text index")
	}
	return index
}

// GetLangNames 获取语言名称列表
func (s *Stream) GetLangNames() []string {
	return s.langNames
}

// GetLangTextPools 获取所有语言的文本池
func (s *Stream) GetLangTextPools() [][]string {
	return s.langTextPools
}

// SkipBytes 跳过指定字节数（用于跳过未知表的数据）
func (s *Stream) SkipBytes(count int) {
	if count < 0 || count > s.remaining() {
		panic("invalid skip length")
	}
	_, _ = s.reader.Seek(int64(count), io.SeekCurrent)
}
