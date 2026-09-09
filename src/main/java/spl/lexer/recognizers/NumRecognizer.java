package spl.lexer.recognizers;

import spl.lexer.Token;
import spl.lexer.TokenType;

/**
 * Owner: TODO(assign teammate)
 *
 * Regex from spec:
 *   0_ |
 *   (–|ε)0.(0|1|...|9)*(1|...|9)_ |
 *   (–|ε)(1|...|9)(0|...|9)*.(0|...|9)*(1|...|9)_ |
 *   (–|ε)(1|...|9)(0|...|9)*_
 * (where _ denotes the mandatory trailing blank_space)
 */
public class NumRecognizer implements TokenRecognizer {
    @Override
    public Token tryMatch(String source, int pos, int line, int column) {
        if(pos < 0 || pos > source.length()){
            return null;
        }

        int i = pos;
        int len = source.length();

        boolean negative = false;
        if(i < len && source.charAt(i) == '-'){
            negative = true;
            i++;
        }

        if(i >= len || !isDigit(source.charAt(i))){
            return null;
        }

        int intStart = i;
        if(source.charAt(i) == '0'){
            i++;
        }else{
            while(i < len && isDigit(source.charAt(i))){
                i++;
            }
        }

        boolean bareZeroInteger = (i - intStart == 1) && source.charAt(intStart) == '0';
        boolean hasFraction = false;

        if( i < len && source.charAt(i) =='.'){
            int fracDigitStart = i + 1;
            int j = fracDigitStart;
            while( j < len && isDigit(source.charAt(j))){
                j++;
            }

            boolean fractionNonEmpty = j > fracDigitStart;
            boolean fractionEndNonZero = fractionNonEmpty && source.charAt(j - 1) != '0';

            if(fractionNonEmpty && fractionEndNonZero){
                hasFraction = true;
                i = j;
            }
        }


        if(bareZeroInteger && !hasFraction && negative){
            return null;
        }

        if(i >= len || !isBlankSpace(source.charAt(i))){
            return null;
        }

        String lexeme = source.substring(pos, i);
        return new Token(TokenType.NUM, lexeme, line, column);
    }

    private static boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }
 
    private static boolean isBlankSpace(char c) {
        // Per spec: ASCII 32 (space) or ASCII 13 (CR) - NOT ASCII 10 (LF).
        return c == 32 || c == 13;
    }
}
