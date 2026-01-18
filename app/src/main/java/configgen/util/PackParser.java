package configgen.util;

import java.util.ArrayList;
import java.util.List;

public class PackParser {

    private enum NestListState {
        START, NO_QUOTE, QUOTE, QUOTE2, IN_PARENTHESES, PARENTHESES_OK
    }

    // , ;都能用于分割，
    // excel列类型为list<int>，策划在列的格子里写123,456想表达的是两个数，但被excel自动记录成123456，此时就出错了
    // 所以这里增加; 也用于分割，策划可以写123;456
    private static final char separatorComma = ',';
    private static final char separatorSemicolon = ';';

    private static final char quote = '"';
    private static final char whitespace = ' ';
    private static final char leftParentheses = '(';
    private static final char rightParentheses = ')';


    /**
     * "b,c" 解析为两段：                  <1>b        <2>c
     * "(b,c)" 解析为一段                  <1>b,c
     * "a(b,c)" 解析为一段                 <1>a(b,c)  -> 这要用parseFunction来解析
     * "a,(b,c)" 解析为两段                <1>a        <2>b,c
     * "a,(b,(c1,c2)),d(e,f)" 解析为三段： <1>a        <2>b,(c1,c2)        <3>d(e,f)
     */
    public static List<String> parsePack(String str) {
        NestListState state = NestListState.START;
        ArrayList<String> list = new ArrayList<>(8);
        StringBuilder field = new StringBuilder(128);
        field.setLength(0);
        int quoteCount_InParentheses = 0;
        int leftNotMatchCount_InParentheses = 0;
        boolean outMostIsFunction = false;

        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            switch (state) {
                case START:
                    //noinspection StatementWithEmptyBody
                    if (c == whitespace) {
                        // ignore, stay at START state
                    } else if (c == separatorComma || c == separatorSemicolon) {
                        list.add("");
                    } else if (c == quote) {
                        field.setLength(0);
                        state = NestListState.QUOTE;
                    } else if (c == leftParentheses) {
                        field.setLength(0);
                        outMostIsFunction = false;
                        leftNotMatchCount_InParentheses = 1;
                        quoteCount_InParentheses = 0;
                        state = NestListState.IN_PARENTHESES;
                    } else {
                        field.setLength(0);
                        field.append(c);
                        state = NestListState.NO_QUOTE;
                    }
                    break;

                case NO_QUOTE:
                    if (c == separatorComma || c == separatorSemicolon) {
                        list.add(field.toString());
                        state = NestListState.START;
                    } else if (c == leftParentheses) {
                        field.append(c);
                        outMostIsFunction = true;
                        leftNotMatchCount_InParentheses = 1;
                        quoteCount_InParentheses = 0;
                        state = NestListState.IN_PARENTHESES;
                    } else {
                        field.append(c);
                    }
                    break;

                case QUOTE:
                    if (c == quote) {
                        state = NestListState.QUOTE2;
                    } else {
                        field.append(c);
                    }
                    break;

                case QUOTE2:
                    if (c == separatorComma || c == separatorSemicolon) {
                        list.add(field.toString());
                        state = NestListState.START;
                    } else if (c == quote) {
                        field.append(quote);
                        state = NestListState.QUOTE;
                    } else {
                        field.append(c);
                        state = NestListState.NO_QUOTE;
                    }
                    break;
                case IN_PARENTHESES:
                    if (c == quote) {
                        quoteCount_InParentheses++;
                    }

                    if (quoteCount_InParentheses % 2 == 1) { //在转义符号中
                        field.append(c);
                    } else if (c == leftParentheses) {
                        leftNotMatchCount_InParentheses++;
                        field.append(c);
                    } else if (c == rightParentheses) {
                        leftNotMatchCount_InParentheses--;
                        if (leftNotMatchCount_InParentheses > 0 || outMostIsFunction) {
                            field.append(c);
                        }
                        if (leftNotMatchCount_InParentheses == 0) {
                            state = NestListState.PARENTHESES_OK;
                        }
                    } else {
                        field.append(c);
                    }
                    break;
                case PARENTHESES_OK:
                    //noinspection StatementWithEmptyBody
                    if (c == whitespace) {
                        // ignore, stay at START state
                    } else if (c == separatorComma || c == separatorSemicolon) {
                        list.add(field.toString());
                        state = NestListState.START;
                    } else {
                        throw new RuntimeException(LocaleUtil.getLocaleString("PackParser.ExpectedSpaceAfterParentheses",
                            "Expected whitespace after outermost parentheses"));
                    }

                    break;
            }
        }

        switch (state) {
            case START: //注意这里和CSVParser的不同，这里最后一个分隔符后面如果没有符号，就不算
                break;
            default:
                list.add(field.toString());
                break;
        }
        list.trimToSize();
        return list;
    }


    private enum FunctionState {
        START, NAME, IN_PARENTHESES,
    }


    /**
     * "a(b,c)" 解析为两段：  <1>a   <2>b,c
     */
    public static List<String> parseFunction(String str) {
        FunctionState state = FunctionState.START;
        ArrayList<String> list = new ArrayList<>(2);
        StringBuilder field = new StringBuilder(128);
        field.setLength(0);

        boolean parameters_ok = false;
        int quoteCount_InParentheses = 0;
        int leftNotMatchCount_InParentheses = 0;


        for (char c : str.toCharArray()) {
            switch (state) {
                case START:
                    //noinspection StatementWithEmptyBody
                    if (c == whitespace) {
                        // ignore, stay at START state
                    } else if (parameters_ok) {
                        throw new RuntimeException(LocaleUtil.getLocaleString("PackParser.ExtraCharsAfterParams",
                            "Extra characters after parsed parameters"));
                    } else if (c == leftParentheses) {
                        throw new RuntimeException(LocaleUtil.getLocaleString("PackParser.MissingFunctionName",
                            "Missing function name"));
                    } else {
                        field.setLength(0);
                        field.append(c);
                        state = FunctionState.NAME;
                    }
                    break;
                case NAME:
                    if (c == leftParentheses) {
                        list.add(field.toString());

                        field.setLength(0);
                        //noinspection ConstantConditions
                        quoteCount_InParentheses = 0;
                        leftNotMatchCount_InParentheses = 1;
                        state = FunctionState.IN_PARENTHESES;
                    } else {
                        field.append(c);
                    }
                    break;
                case IN_PARENTHESES:
                    if (c == quote) {
                        quoteCount_InParentheses++;
                    }

                    if (quoteCount_InParentheses % 2 == 1) { //在转义符号中
                        field.append(c);
                    } else if (c == leftParentheses) {
                        leftNotMatchCount_InParentheses++;
                        field.append(c);
                    } else if (c == rightParentheses) {
                        leftNotMatchCount_InParentheses--;
                        if (leftNotMatchCount_InParentheses > 0) {
                            field.append(c);
                        }
                        if (leftNotMatchCount_InParentheses == 0) {
                            list.add(field.toString());
                            parameters_ok = true;
                            state = FunctionState.START;
                        }
                    } else {
                        field.append(c);
                    }
                    break;
            }
        }

        if (list.size() != 2) {
            throw new RuntimeException(LocaleUtil.getFormatedLocaleString("PackParser.ParameterCountMismatch",
                "Parameter count mismatch, got {0}, expected 2", list.size()));
        }
        return list;
    }
}
