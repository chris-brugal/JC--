package plc.project;

import java.util.ArrayList;
import java.util.List;

/**
 * The lexer works through three main functions:
 * <p>
 * - {@link #lex()}, which repeatedly calls lexToken() and skips whitespace
 * - {@link #lexToken()}, which lexes the next token
 * - {@link CharStream}, which manages the state of the lexer and literals
 * <p>
 * If the lexer fails to parse something (such as an unterminated string) you
 * should throw a {@link ParseException} with an index at the character which is
 * invalid.
 * <p>
 * The {@link #peek(String...)} and {@link #match(String...)} functions are * helpers you need to use, they will make the implementation a lot easier.
 */
public final class Lexer {

    private final CharStream chars;


    public Lexer(String input) {
        chars = new CharStream(input);
    }

    /**
     * Repeatedly lexes the input using {@link #lexToken()}, also skipping over
     * whitespace where appropriate.
     */
    public List<Token> lex() {
        //GOAL: Calling LexToken() and adding to create list of tokens
        //create the list<token>
        //while there are still characters
        //if chars not whitespace via peek,
        //list tokens add lextoken() (ex: array.add(lexToken()); )
        //move in the char stream?
        //keep moving in the char stream?

        List<Token> tokenList = new ArrayList<Token>();

        while (peek(".")) {
            if (peek("[ \b\n\r\t]")) {
                chars.advance();
                chars.skip();
            } else {
                tokenList.add(lexToken());
            }
        }
        return tokenList;
    }

    /**
     * This method determines the type of the next token, delegating to the
     * appropriate lex method. As such, it is best for this method to not change
     * the state of the char stream (thus, use peek not match).
     * <p>
     * The next character should start a valid token since whitespace is handled
     * by {@link #lex()}
     */
    public Token lexToken() {
        //TODO
        //GOAL: Deal with the rest of the lex's
        //find a way to grab the first character and then find out which of the types it is
        //if peek(regex for first character in number)
        //lexNumber() (which will make the token)
        //if peek(regex for character)
        //lexCharacter()
        //last is lexOperator since its everything else

        if (peek("[A-Za-z@]")) {
            return lexIdentifier();
        } else if (peek("[0-9-]")) {
            return lexNumber();
        } else if (peek("[\\']")) {
            return lexCharacter();
        } else if (peek("\\\"")) {
            return lexString();
        } else {
            return lexOperator();
        }
    }

    public Token lexIdentifier() {
        //While there is characters that match the regex, we match them
        //return w chars.emit as said in lecture

        if (peek("@")) {
            match("@");
        }
        String regex = "([A-Za-z0-9_-])";
        while (peek(regex)) {
            if (peek("@")) {
                return chars.emit(Token.Type.IDENTIFIER);
            }
            match(regex);
        }
        return chars.emit(Token.Type.IDENTIFIER);
    }

    public Token lexNumber() {
        //TODO make sure this works correctly
        if (peek("-")) {
            match("-");
            if (peek("[1-9]")) {
                match("[1-9]");
                while (peek("[0-9]")) {
                    match("[0-9]");
                }
                if (peek("[.]", "[0-9]")) {
                    match("[.]");
                    while (peek("[0-9]")) {
                        match("[0-9]");
                    }
                    return chars.emit(Token.Type.DECIMAL);
                } else {
                    return chars.emit(Token.Type.INTEGER);
                }
            }
            return chars.emit(Token.Type.OPERATOR);
        } else {
            if (peek("[0]")) {
                match("[0]");
                return chars.emit(Token.Type.INTEGER);
            }
            if (peek("[1-9]")) {
                match("[1-9]");
                while (peek("[0-9]")) {
                    match("[0-9]");
                }
                if (peek("[.]", "[0-9]")) {
                    match("[.]");
                    while (peek("[0-9]")) {
                        match("[0-9]");
                    }
                    return chars.emit(Token.Type.DECIMAL);
                } else {
                    return chars.emit(Token.Type.INTEGER);
                }
            }
        }
        return chars.emit(Token.Type.INTEGER);
    }

    public Token lexCharacter() {
        //Perfection

        if (!peek("[\\']", ".")) {
            match("[\\']");
            return chars.emit(Token.Type.OPERATOR);
        } else {
            match("[\\']");
        }
        if (peek("[\\\\]", "[bnrt\\'\\\"\\\\]", "[\\']")) {
            match("[\\\\]");
            match("[bnrt\\'\\\"\\\\]");
            match("[\\']");
            return chars.emit(Token.Type.CHARACTER);
        } else if (peek("[^\\'\\\\]", "[\\']")) {
            match("[^\\'\\\\]", "[\\']");
            return chars.emit(Token.Type.CHARACTER);
        } else if (peek("[\\']")) {
            throw new ParseException("NOT ALLOWED", chars.index);
        } else {

            throw new ParseException("NOT ALLOWED", chars.index+=1);
        }


    }

    public Token lexString() {

        int indexIncrement = 0;
        if (!peek("[\\\"]", ".")) {
            match("[\\\"]");
            return chars.emit(Token.Type.OPERATOR);
        } else {
            match("[\\\"]");
        }

        while (peek(".")) {

            if (peek("[\\\\]")) {
                match("[\\\\]");
                if (peek("[bnrt\\'\\\"\\\\]")) {
                    match("[bnrt\\'\\\"\\\\]");
                } else {
                    indexIncrement = 0;
                    break;
                }
            } else {
                if (peek("[^\\\"\\\\]")) {
                    match(".");
                } else if (peek("[\\\"]")) {
                    match(".");
                    return chars.emit(Token.Type.STRING);
                } else {
                    indexIncrement = 0;
                    break;
                }

            }
        }
        throw new ParseException("NOT ALLOWED", chars.index += indexIncrement);
    }

    public void lexEscape() {
        throw new UnsupportedOperationException(); //TODO
    }

    public Token lexOperator() {

        if (peek("[!=]", "=") || peek("&", "&") || peek("[|]", "[|]")) {
            match(".", ".");
        } else {
            match(".");
        }
        return chars.emit(Token.Type.OPERATOR);
    }

    /**
     * Returns true if the next sequence of characters match the given patterns,
     * which should be a regex. For example, {@code peek("a", "b", "c")} would
     * return true if the next characters are {@code 'a', 'b', 'c'}.
     */
    public boolean peek(String... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!(chars.has(i)) || !(String.valueOf(chars.get(i)).matches(patterns[i]))) {
                //if there is no character or if there is no match to the regex statement, return false
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
        if (peek == true) {
            for (int i = 0; i < patterns.length; i++) {
                //if the character is matched to the regex, we advance on, moving the index pointer
                chars.advance();
            }
        }
        return peek;
    }

    /**
     * A helper class maintaining the input string, current index of the char
     * stream, and the current length of the token being matched.
     * <p>
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
