package configgen.geni18n;

import configgen.gen.CliException;
import configgen.gen.ParameterParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class TodoTranslatorTest {
    @ParameterizedTest
    @ValueSource(strings = {"translate", "translate,batch=1", "translate,batch=2147483647"})
    void acceptsDefaultAndPositiveBatchSizes(String arg) {
        assertDoesNotThrow(() -> new TodoTranslator(new ParameterParser(arg)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "", "abc", "1.5", "2147483648"})
    void rejectsInvalidBatchSizes(String size) {
        assertThrows(CliException.class, () -> new TodoTranslator(new ParameterParser("translate,batch:" + size)));
    }
}
