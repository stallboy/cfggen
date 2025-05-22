package config

import (
	"encoding/binary"
	"fmt"
	"io"
	"os"
)

type Stream struct {
	file      *os.File
	stringBuf []byte
}

func (s *Stream) ReadBool() bool {
	var value int32
	if err := binary.Read(s.file, binary.LittleEndian, &value); err != nil {
		panic(fmt.Errorf("read bool: %w", err))
	}
	return value != 0
}

func (s *Stream) ReadInt32() int32 {
	var value int32
	if err := binary.Read(s.file, binary.LittleEndian, &value); err != nil {
		panic(fmt.Errorf("read int32: %w", err))
	}
	return value
}

func (s *Stream) ReadInt64() int64 {
	var value int64
	if err := binary.Read(s.file, binary.LittleEndian, &value); err != nil {
		panic(fmt.Errorf("read int64: %w", err))
	}
	return value
}

func (s *Stream) ReadFloat32() float32 {
	var value float32
	if err := binary.Read(s.file, binary.LittleEndian, &value); err != nil {
		panic(fmt.Errorf("read float32: %w", err))
	}
	return value
}

// ReadString 从 io.Reader 中读取格式为 [int32长度][UTF-8内容] 的字符串
func (s *Stream) ReadString() string {
	length := s.ReadInt32()

	if length < 0 {
		panic(fmt.Errorf("invalid string length: %d", length))
	}

	if cap(s.stringBuf) < int(length) {
		s.stringBuf = make([]byte, length)
	} else {
		s.stringBuf = s.stringBuf[:length]
	}

	if _, err := io.ReadFull(s.file, s.stringBuf); err != nil {
		panic(fmt.Errorf("read string content: %w", err))
	}

	return string(s.stringBuf)
}
