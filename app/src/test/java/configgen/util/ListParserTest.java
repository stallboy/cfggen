package configgen.util;


import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;


public class ListParserTest {

    @Test
    public void parseList() {
        test("aa,bb", "aa", "bb");
    }

    @Test
    public void midfixSeparator_Counts() {
        test("aa,,bb", "aa", "", "bb");
    }

    @Test
    public void prefixSeparator_Counts() {
        test(",aa,bb", "", "aa", "bb");
    }

    @Test
    public void surfixSeparator_NotCounts() {
        test("aa,bb,", "aa", "bb");
    }

    @Test
    public void useQuoteToEscapeSeparatorInString() {
        test("\"a,a\", bb", "a,a", " bb");
        test("\"aa\", bb", "aa", " bb");
    }

    @Test
    public void useQuoteToEscapeQuoteInString() {
        test("\"a\"\"a,a\", bb", "a\"a,a", " bb");
    }

    @Test
    public void useQuoteNoClose_IgnoreQuote() {
        test("\"ab", "ab");
        test("\"a\"b", "ab");
        test("\"a\"bc,d", "abc", "d");
    }

    @Test
    public void whitespace_Counts() {
        test("aa, bb", "aa", " bb");
        test(" aa, bb ", " aa", " bb ");
    }

    private void test(String source, String... row) {
        List<String> a = ListParser.parseList(source, ',');
        assertEquals(row.length, a.size());
        int i = 0;
        for (String c : row) {
            assertEquals(c, a.get(i++));
        }
    }


    @Test
    public void separatorNoComma_Ok() {
        List<String> a = ListParser.parseList("12:24:30", ':');
        assertEquals(3, a.size());
        assertEquals("12", a.get(0));
        assertEquals("24", a.get(1));
        assertEquals("30", a.get(2));
    }
}