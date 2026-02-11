@import java.util.List
@param String pkg
@param List<String> languages
package ${pkg}

// TextPoolManager 客户端模式：全局文本管理器（应用层负责语言切换）
type TextPoolManager struct {
    globalTexts []string
}

var textPoolManagerInstance *TextPoolManager

func TextPoolManagerInstance() *TextPoolManager {
    if textPoolManagerInstance == nil {
        textPoolManagerInstance = &TextPoolManager{}
    }
    return textPoolManagerInstance
}

func (m *TextPoolManager) SetGlobalTexts(texts []string) {
    m.globalTexts = texts
}

func (m *TextPoolManager) GetText(index int) string {
    if index < 0 || index >= len(m.globalTexts) {
        return ""
    }
    return m.globalTexts[index]
}

type Text struct {
    index int
}

func createText(stream *Stream) *Text {
    self := &Text{}
    self.index = stream.ReadTextIndex()
    return self
}

// T 从全局文本数组获取文本
func (t *Text) T() string {
    return TextPoolManagerInstance().GetText(t.index)
}

func (t *Text) String() string {
    return t.T()
}
