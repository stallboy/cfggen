package configgen.geni18n;

import java.util.List;

/**
 * 翻译模型，用于渲染提示词
 */
public record TodoTranslateModel(String sourceLang,
                                 String targetLang,
                                 List<String> todoOriginals,
                                 String relatedTermsInCsv,
                                 String relatedTranslationsInCsv) {
}
