package config

import (
	"encoding/binary"
	"fmt"
	"io"
)

type Stream struct {
	reader    io.Reader
	stringBuf []byte

	// StringPool 和 LangTextPool 字段
	stringPool    []string
	langNames     []string
	langTextPools [][]string // langTextPools[langIndex][textIndex]
}

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
		if err == io.EOF || err == io.ErrUnexpectedEOF {
			return 0
		}
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
	return int(s.ReadInt32())
}

// ReadString 从 io.Reader 中读取格式为 [int32长度][UTF-8内容] 的字符串
func (s *Stream) ReadString() string {
	length := s.ReadInt32() // 先读取字符串长度
	if length <= 0 {
		return ""
	}
	buf := make([]byte, length)
	n, err := io.ReadFull(s.reader, buf)
	if err != nil {
		panic(fmt.Errorf("read string: %w", err))
	}
	if int32(n) != length {
		panic(fmt.Errorf("read string: expected %d bytes, got %d", length, n))
	}
	return string(buf)
}

// ReadStringPool 读取全局字符串池（在读取表数据之前调用）
func (s *Stream) ReadStringPool() {
	count := s.ReadSize()
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

// ReadLangTextPool 读取多语言文本池（在读取表数据之前调用）
func (s *Stream) ReadLangTextPool() {
	langCount := s.ReadSize()
	s.langNames = make([]string, langCount)
	s.langTextPools = make([][]string, langCount)

	for langIdx := 0; langIdx < langCount; langIdx++ {
		langName := s.ReadString()
		s.langNames[langIdx] = langName

		indexCount := s.ReadSize()
		indices := make([]int, indexCount)
		for i := 0; i < indexCount; i++ {
			indices[i] = s.ReadSize()
		}

		// 读取该语言的 StringPool
		poolCount := s.ReadSize()
		pool := make([]string, poolCount)
		for i := 0; i < poolCount; i++ {
			pool[i] = s.ReadString()
		}

		// 构建文本数组：texts[textIndex] = pool[indices[textIndex]]
		s.langTextPools[langIdx] = make([]string, indexCount)
		for i := 0; i < indexCount; i++ {
			s.langTextPools[langIdx][i] = pool[indices[i]]
		}
	}
}

// ReadTextsInPool 从 LangTextPool 读取所有语言文本（多语言 服务器端模式）
func (s *Stream) ReadTextsInPool() []string {
	index := s.ReadInt32()
	if s.langTextPools == nil {
		panic("LangTextPool not initialized")
	}

	texts := make([]string, len(s.langTextPools))
	for i := 0; i < len(s.langTextPools); i++ {
		if index < 0 || int(index) >= len(s.langTextPools[i]) {
			texts[i] = ""
		} else {
			texts[i] = s.langTextPools[i][index]
		}
	}
	return texts
}

// ReadTextInPool 从 LangTextPool 读取 text（单语言模式）
func (s *Stream) ReadTextInPool() string {
	index := s.ReadInt32()
	if s.langTextPools == nil {
		panic("LangTextPool not initialized")
	}
	if len(s.langTextPools) == 0 {
		panic("LangTextPool is empty")
	}
	if index < 0 || int(index) >= len(s.langTextPools[0]) {
		panic(fmt.Sprintf("index %d out of LangTextPool bounds [0, %d)", index, len(s.langTextPools[0])))
	}
	return s.langTextPools[0][index]
}

// ReadTextIndex 从 LangTextPool 读取文本索引（客户端模式）
func (s *Stream) ReadTextIndex() int {
	return s.ReadSize()
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
	buf := make([]byte, count)
	n, err := io.ReadFull(s.reader, buf)
	if err != nil {
		panic(fmt.Errorf("skip bytes: %w", err))
	}
	if n != count {
		panic(fmt.Errorf("skip bytes: expected %d bytes, got %d", count, n))
	}
}
