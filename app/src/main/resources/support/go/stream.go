package config

import (
	"encoding/binary"
	"fmt"
	"io"
)

type Stream struct {
	reader    io.Reader
	stringBuf []byte
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
