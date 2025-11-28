package configgen.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownReaderTest {

    @TempDir
    Path tempDir;

    @Test
    void testReadMarkdownWithFrontmatter() throws IOException {
        // 创建测试文件
        String content = """
                ---
                title: Test Document
                author: John Doe
                date: 2025-11-28
                ---
                # Hello World

                This is the content.
                """;

        Path testFile = tempDir.resolve("test.md");
        Files.writeString(testFile, content);

        // 读取并验证
        MarkdownReader.MarkdownDocument doc = MarkdownReader.read(testFile);

        Map<String, String> frontmatter = doc.frontmatter();
        assertEquals(3, frontmatter.size());
        assertEquals("Test Document", frontmatter.get("title"));
        assertEquals("John Doe", frontmatter.get("author"));
        assertEquals("2025-11-28", frontmatter.get("date"));

        String expectedContent = """
                # Hello World

                This is the content.""";
        assertEquals(expectedContent, doc.content());
    }

    @Test
    void testReadMarkdownWithoutFrontmatter() throws IOException {
        String content = """
                # Hello World

                This is a simple markdown file without frontmatter.
                """;

        Path testFile = tempDir.resolve("simple.md");
        Files.writeString(testFile, content);

        MarkdownReader.MarkdownDocument doc = MarkdownReader.read(testFile);

        assertTrue(doc.frontmatter().isEmpty());
        String expectedContent = """
                # Hello World

                This is a simple markdown file without frontmatter.""";
        assertEquals(expectedContent, doc.content());
    }

    @Test
    void testReadEmptyFile() throws IOException {
        Path testFile = tempDir.resolve("empty.md");
        Files.writeString(testFile, "");

        MarkdownReader.MarkdownDocument doc = MarkdownReader.read(testFile);

        assertTrue(doc.frontmatter().isEmpty());
        assertEquals("", doc.content());
    }

    @Test
    void testReadFrontmatterOnly() throws IOException {
        String content = """
                ---
                key1: value1
                key2: value2
                ---
                """;

        Path testFile = tempDir.resolve("frontmatter-only.md");
        Files.writeString(testFile, content);

        MarkdownReader.MarkdownDocument doc = MarkdownReader.read(testFile);

        assertEquals(2, doc.frontmatter().size());
        assertEquals("value1", doc.frontmatter().get("key1"));
        assertEquals("value2", doc.frontmatter().get("key2"));
        assertEquals("", doc.content());
    }

    @Test
    void testFrontmatterWithEmptyLines() throws IOException {
        String content = """
                ---
                title: Test

                description: A test document
                ---
                Content here.
                """;

        Path testFile = tempDir.resolve("with-empty-lines.md");
        Files.writeString(testFile, content);

        MarkdownReader.MarkdownDocument doc = MarkdownReader.read(testFile);

        assertEquals(2, doc.frontmatter().size());
        assertEquals("Test", doc.frontmatter().get("title"));
        assertEquals("A test document", doc.frontmatter().get("description"));
        assertEquals("Content here.", doc.content());
    }

    @Test
    void testFrontmatterWithColonInValue() throws IOException {
        String content = """
                ---
                url: https://example.com:8080/path
                time: 12:30:45
                ---
                Content
                """;

        Path testFile = tempDir.resolve("colon-in-value.md");
        Files.writeString(testFile, content);

        MarkdownReader.MarkdownDocument doc = MarkdownReader.read(testFile);

        assertEquals("https://example.com:8080/path", doc.frontmatter().get("url"));
        assertEquals("12:30:45", doc.frontmatter().get("time"));
    }
}
