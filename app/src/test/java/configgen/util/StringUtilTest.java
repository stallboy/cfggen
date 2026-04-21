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
}
