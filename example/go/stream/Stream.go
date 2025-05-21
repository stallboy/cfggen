package stream

import (
	"encoding/binary"
	"fmt"
	"io"
	"os"
)

func ReadInt32(r *os.File) int32 {
	var value int32
	if err := binary.Read(r, binary.LittleEndian, &value); err != nil {
		panic(fmt.Errorf("read int32: %w", err))
	}
	return value
}

var stringBuf []byte // 包级缓冲区，全局复用

// ReadString 从 io.Reader 中读取格式为 [int32长度][UTF-8内容] 的字符串
func ReadString(r *os.File) string {
	length := ReadInt32(r)

	if length < 0 {
		panic(fmt.Errorf("invalid string length: %d", length))
	}

	if cap(stringBuf) < int(length) {
		stringBuf = make([]byte, length)
	} else {
		stringBuf = stringBuf[:length]
	}

	if _, err := io.ReadFull(r, stringBuf); err != nil {
		panic(fmt.Errorf("read string content: %w", err))
	}

	return string(stringBuf)
}
