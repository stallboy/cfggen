package configgen.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class StringUtilTest {

    @Test
    public void nullInput() {
        assertNull(StringUtil.underscoreToPascalCase(null));
    }

    @Test
    public void emptyInput() {
        assertEquals("", StringUtil.underscoreToPascalCase(""));
    }

    @Test
    public void singleWord() {
        assertEquals("Hello", StringUtil.underscoreToPascalCase("hello"));
        assertEquals("HELLO", StringUtil.underscoreToPascalCase("HELLO"));
        assertEquals("HELLO", StringUtil.underscoreToPascalCase("hELLO"));
    }

    @Test
    public void simpleSnakeCase() {
        assertEquals("HelloWorld", StringUtil.underscoreToPascalCase("hello_world"));
        assertEquals("FooBarBaz", StringUtil.underscoreToPascalCase("foo_bar_baz"));
    }

    @Test
    public void allCapsSnakeCase() {
        assertEquals("HELLOWORLD", StringUtil.underscoreToPascalCase("HELLO_WORLD"));
    }

    @Test
    public void mixedCaseSnakeCase() {
        assertEquals("HelloWorld", StringUtil.underscoreToPascalCase("Hello_World"));
        assertEquals("FOOBar", StringUtil.underscoreToPascalCase("FOO_bar"));
    }

    @Test
    public void noUnderscore() {
        assertEquals("Hello", StringUtil.underscoreToPascalCase("hello"));
        assertEquals("Hello", StringUtil.underscoreToPascalCase("Hello"));
    }

    @Test
    public void consecutiveUnderscores() {
        assertEquals("HelloWorld", StringUtil.underscoreToPascalCase("hello__world"));
        assertEquals("AB", StringUtil.underscoreToPascalCase("a___b"));
    }

    @Test
    public void leadingUnderscore() {
        assertEquals("Hello", StringUtil.underscoreToPascalCase("_hello"));
    }

    @Test
    public void trailingUnderscore() {
        assertEquals("Hello", StringUtil.underscoreToPascalCase("hello_"));
    }

    @Test
    public void singleChar() {
        assertEquals("A", StringUtil.underscoreToPascalCase("a"));
        assertEquals("A", StringUtil.underscoreToPascalCase("A"));
    }

    @Test
    public void singleUnderscore() {
        assertEquals("", StringUtil.underscoreToPascalCase("_"));
    }

    @Test
    public void preserveOriginalCase() {
        assertEquals("FooBARBaz", StringUtil.underscoreToPascalCase("foo_BAR_Baz"));
        assertEquals("MyXMLParser", StringUtil.underscoreToPascalCase("My_XML_Parser"));
    }

    // ---- toScreamingSnakeCase ----

    @Test
    public void snake_nullInput() {
        assertNull(StringUtil.toScreamingSnakeCase(null));
    }

    @Test
    public void snake_emptyInput() {
        assertEquals("", StringUtil.toScreamingSnakeCase(""));
    }

    @Test
    public void snake_camelCase() {
        assertEquals("RESET_DURATION", StringUtil.toScreamingSnakeCase("ResetDuration"));
        assertEquals("MAX_HP", StringUtil.toScreamingSnakeCase("MaxHp"));
        assertEquals("FIREBALL", StringUtil.toScreamingSnakeCase("Fireball"));
    }

    @Test
    public void snake_alreadyUnderscored() {
        // 已有下划线：只做大写归一，不再插入额外下划线
        assertEquals("RESET_DURATION", StringUtil.toScreamingSnakeCase("Reset_Duration"));
        assertEquals("RESET_DURATION", StringUtil.toScreamingSnakeCase("reset_duration"));
        assertEquals("RESET_DURATION", StringUtil.toScreamingSnakeCase("RESET_DURATION"));
    }

    @Test
    public void snake_acronym() {
        // 连续大写按缩写词处理，不在中间拆分
        assertEquals("HTTP_SERVER", StringUtil.toScreamingSnakeCase("HTTPServer"));
        assertEquals("XML_PARSER", StringUtil.toScreamingSnakeCase("XMLParser"));
        assertEquals("PARSE_XML", StringUtil.toScreamingSnakeCase("parseXML"));
    }

    @Test
    public void snake_consecutiveAndLeadingUnderscores() {
        assertEquals("FOO_BAR", StringUtil.toScreamingSnakeCase("foo__bar"));
        assertEquals("FOO", StringUtil.toScreamingSnakeCase("_foo"));
        assertEquals("FOO", StringUtil.toScreamingSnakeCase("foo_"));
    }

    @Test
    public void snake_singleChar() {
        assertEquals("A", StringUtil.toScreamingSnakeCase("a"));
        assertEquals("A", StringUtil.toScreamingSnakeCase("A"));
    }
}
