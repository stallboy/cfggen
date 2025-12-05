package configgen.geni18n;

/**
 * 翻译模型，用于渲染提示词
 */
public record TodoTranslateModel(String sourceLang,
                                 String targetLang,
                                 String relatedTermsInCsv,
                                 String todoOriginalsInCsv) {
}
