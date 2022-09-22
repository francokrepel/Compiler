package plc.project;

import java.util.List;


/**
 * The lexer works through three main functions:
 *
 *  - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 *  - {@link #lexToken()}, which lexes the next token
 *  - {@link CharStream}, which manages the state of the lexer and literals
 *
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid.
 *
 * The {@link #peek(String...)} and {@link #match(String...)} functions are * helpers you need to use, they will make the implementation a lot easier. */
public final class Lexer {

    private final CharStream chars;

    List<Token> tokens = null;

    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() { //TODO
        while (chars.has(0)/*this might be wrong*/) {
            tokens.add(lexToken());
            //Skip over whitespace
        }
        return tokens;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     *
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() { //TODO

        if (peek("(@|[A-Za-z])[A-Za-z0-9_-]*")) {
            return lexIdentifier();
        } else if (peek("(-?[1-9][0-9]*)|0")) {
            return lexNumber(); // Integer
        } else if (peek("-?([1-9][0-9]*|0)\\.[0-9]+")) {
            return lexNumber(); // Decimal
        } else if (peek("'")) { //'([^'\n\r\\]|\\[bnrt'"\\])'
            return lexCharacter();
        } else if (peek("\"([^\"\\n\\r\\\\]|\\\\[bnrt'\"\\\\])*\"")) {
            return lexString();
        } else if (peek("([!=]=)|&&|\\|\\||[^\\n\\r\\t]")) {
            return lexOperator();
        } else {
            throw new ParseException("Not a valid token", chars.index);
        }
    }

    public Token lexIdentifier() {

        chars.advance();
        while(match("[A-Za-z0-9_-]"));
//        } else {
//            throw new ParseException("Not a valid token", chars.index);
//        }
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexCharacter() {
        boolean esc = false;
        boolean norm = false;

        chars.advance();
        if (peek("\\\\")) { //This if block checks to see if the next character after the initial ' is a normal character or an escape \
            chars.advance();
            esc = true;
        } else if (peek("[^'\\n\\r\\\\]")) {
            chars.advance();
            norm = true;
        } else {
            throw new ParseException("Not a valid token", chars.index);
        }

        if (esc) {  //if the previous character was an escape, this checks for another character
            if (peek("[bnrt'\"\\\\]")) {
                chars.advance();
            } else {
                throw new ParseException("Not a valid token", chars.index);
            }
            if (!peek("'")) {
                throw new ParseException("Not a valid token", chars.index);
            } else {
                chars.advance();
            }

        } else if (norm) {  //if the previous character was not an escape
            if (!peek("'")) {
                throw new ParseException("Not a valid token", chars.index);
            } else {
                chars.advance();
            }
        } else {
            throw new ParseException("Not a valid token", chars.index);
        }

        return chars.emit(Token.Type.CHARACTER);
    }

    public Token lexString() {
        throw new UnsupportedOperationException(); //TODO
    }

    public void lexEscape() {
        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexOperator() {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */

    /** patterns.length number of different regex you pass at once as strings.
     So like if you want to check two characters with the same peek call patterns.length
     would be 2

     so it first checks with has and the i as offset if you will have the next char
     then it checks if it actually matches the regex
     */
    public boolean peek(String... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!chars.has(i) || !String.valueOf(chars.get(i)).matches(patterns[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns true in the same way as {@link #peek(String...)}, but also
     * advances the character stream past all matched characters if peek returns
     * true. Hint - it's easiest to have this method simply call peek.
     */
    public boolean match(String... patterns) {
        boolean peek = peek(patterns);

        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                chars.advance();
            }
        }
        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     *
     * You should rely on peek/match for state management in nearly all cases.
     * The only field you need to access is {@link #index} for any {@link
     * ParseException} which is thrown.
     */
    public static final class CharStream {

        private final String input;
        private int index = 0;
        private int length = 0;

        public CharStream(String input) {
            this.input = input;
        }

        public boolean has(int offset) {
            return index + offset < input.length();
        }

        public char get(int offset) {
            return input.charAt(index + offset);
        }

        public void advance() {
            index++;
            length++;
        }

        public void skip() {
            length = 0;
        }

        public Token emit(Token.Type type) {
            int start = index - length;
            skip();
            return new Token(type, input.substring(start, index), start);
        }

    }

}
