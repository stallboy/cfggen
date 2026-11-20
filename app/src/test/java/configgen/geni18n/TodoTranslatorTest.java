package configgen.geni18n;

import configgen.gen.CliException;
import configgen.gen.ParameterParser;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertThrows;

class TodoTranslatorTest {
    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void rejectsNonPositiveBatchSizes(int size) {
        assertThrows(CliException.class, () -> new TodoTranslator(new ParameterParser("translate,batch:" + size)));
    }
}
