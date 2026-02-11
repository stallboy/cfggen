package config

import "fmt"

// LoadErrorType 加载错误类型
type LoadErrorType int

const (
	LoadErrorTypeDuplicateKey LoadErrorType = iota
	LoadErrorTypeRefNotFound
	LoadErrorTypeListRefEmpty
)

// LoadError 加载错误
type LoadError struct {
	ErrorType LoadErrorType
	TableName string
	Key       string
	RefTable  string // 仅用于 RefNotFound 类型
}

func (e *LoadError) Error() string {
	switch e.ErrorType {
	case LoadErrorTypeDuplicateKey:
		return fmt.Sprintf("duplicate key '%s' in table '%s'", e.Key, e.TableName)
	case LoadErrorTypeRefNotFound:
		return fmt.Sprintf("reference not found: table '%s' key '%s' -> refTable '%s'", e.TableName, e.Key, e.RefTable)
	case LoadErrorTypeListRefEmpty:
		return fmt.Sprintf("list ref empty: table '%s' key '%s'", e.TableName, e.Key)
	default:
		return fmt.Sprintf("unknown error type %d in table '%s' key '%s'", e.ErrorType, e.TableName, e.Key)
	}
}

// LoadErrors 加载错误集合
type LoadErrors struct {
	errors []*LoadError
}

// NewLoadErrors 创建新的错误集合
func NewLoadErrors() *LoadErrors {
	return &LoadErrors{
		errors: make([]*LoadError, 0),
	}
}

// AddDuplicateKey 添加重复键错误
func (le *LoadErrors) AddDuplicateKey(tableName, key string) {
	le.errors = append(le.errors, &LoadError{
		ErrorType: LoadErrorTypeDuplicateKey,
		TableName: tableName,
		Key:       key,
	})
}

// AddRefNotFound 添加引用未找到错误
func (le *LoadErrors) AddRefNotFound(tableName, key, refTable string) {
	le.errors = append(le.errors, &LoadError{
		ErrorType: LoadErrorTypeRefNotFound,
		TableName: tableName,
		Key:       key,
		RefTable:  refTable,
	})
}

// AddListRefEmpty 添加列表引用为空错误
func (le *LoadErrors) AddListRefEmpty(tableName, key string) {
	le.errors = append(le.errors, &LoadError{
		ErrorType: LoadErrorTypeListRefEmpty,
		TableName: tableName,
		Key:       key,
	})
}

// HasErrors 检查是否有错误
func (le *LoadErrors) HasErrors() bool {
	return len(le.errors) > 0
}

// Errors 返回所有错误
func (le *LoadErrors) Errors() []*LoadError {
	return le.errors
}

// Print 打印所有错误
func (le *LoadErrors) Print() {
	for _, e := range le.errors {
		fmt.Println(e.Error())
	}
}
