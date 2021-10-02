package plc.project;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The parser takes the sequence of tokens emitted by the lexer and turns that
 * into a structured representation of the program, called the Abstract Syntax
 * Tree (AST).
 * <p>
 * The parser has a similar architecture to the lexer, just with {@link Token}s
 * instead of characters. As before, {@link #peek(Object...)} and {@link
 * #match(Object...)} are helpers to make the implementation easier.
 * <p>
 * This type of parser is called <em>recursive descent</em>. Each rule in our
 * grammar will have it's own function, and reference to other rules correspond
 * to calling that functions.
 */
public final class Parser {

    private final TokenStream tokens;

    public Parser(List<Token> tokens) {
        this.tokens = new TokenStream(tokens);
    }

    String tokenToString(Token.Type tokenType) {
        String s = tokens.get(0).getLiteral();
        match(tokenType);
        return s;
    }

    int errorIndex(boolean there) {
        if (!there) {
            //grab before
            return tokens.get(-1).getLiteral().length() + tokens.get(-1).getIndex();
        } else if (there) {
            //grab there
            return tokens.get(0).getIndex();
        }
        return -1;
    }


    /**
     * Parses the {@code source} rule.
     */
    public Ast.Source parseSource() throws ParseException {
        List<Ast.Global> globs = new ArrayList<>();
        List<Ast.Function> funcs = new ArrayList<>();

        if(peek("LIST") || peek("VAR") || peek("VAL")) {
            while (peek("LIST") || peek("VAR") || peek("VAL")) {
                globs.add(parseGlobal());
//             if(tokens.has(0) && !peek("DEF") && !peek("LIST") && !peek("VAR") && !peek("VAL")){
//                 //This token is not a global or a function so error
//                 throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
//             }
            }
        }
        if (peek("FUN")){
            while(peek("FUN")){
                funcs.add(parseFunction());
                if(tokens.has(0) && !peek("FUN")){
                    //This token is not a function and theres nothing else
                    throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
                }
            }
        }

        if(tokens.has(0)){
            //lef tover tokens that dont belong
            throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
        }else{
            return new Ast.Source(globs, funcs);
        }
    }

    /**
     * Parses the {@code field} rule. This method should only be called if the
     * next tokens start a field, aka {@code LET}.
     */
    public Ast.Global parseGlobal() throws ParseException {
        Ast.Global glob;
        if(peek("LIST")){
            glob = parseList();
        }else if (peek("VAR")){
            glob = parseMutable();
        } else if (peek("VAL")){
            glob = parseImmutable();
        }else{
            // missing start of global
            throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
        }

        if(peek(";")){
            match(";");
            return glob;
        }else{
            // missing
            throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
        }
    }

    /**
     * Parses the {@code list} rule. This method should only be called if the
     * next token declares a list, aka {@code LIST}.
     */
    public Ast.Global parseList() throws ParseException {
        match("LIST");
        String first;
        List<Ast.Expression> list = new ArrayList<>();

        if (peek(Token.Type.IDENTIFIER)) {
            first = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
            if (peek("=")) {
                match("=");
                if (peek("[")) {
                    match("[");
                    while (!peek("]")) {
                        list.add(parseExpression());
                        if (peek(",")) {
                            match(",");
                            if (peek("]")) {
                                //bad comma then ]
                                throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
                            }
                        }
                    }
                    if (peek("]")) {
                        match("]");
                        Ast.Expression.PlcList PLClist = new Ast.Expression.PlcList(list);
                        return new Ast.Global(first, true, Optional.of(PLClist));
                    } else {
                        // missing ]
                        throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
                    }
                }
                else{
                    // missing [
                    throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
                }
            } else {
                // missing =
                throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
            }
        } else {
            // missing identifier
            throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
        }
    }

    /**
     * Parses the {@code mutable} rule. This method should only be called if the
     * next token declares a mutable global variable, aka {@code VAR}.
     */
    public Ast.Global parseMutable() throws ParseException {
        match("VAR");
        String first;

        if (peek(Token.Type.IDENTIFIER)) {
            first = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
            if (peek("=")) {
                match("=");
                Ast.Expression exp = parseExpression();
                return new Ast.Global(first, true, Optional.of(exp));
            } else {
                //make sure this doesnt cause problems since there
                // may be something after that isnt a = so prev if statement fails
                return new Ast.Global(first, true, Optional.empty());
            }
        } else {
            // missing identifier
            throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
        }
    }

    /**
     * Parses the {@code immutable} rule. This method should only be called if the
     * next token declares an immutable global variable, aka {@code VAL}.
     */
    public Ast.Global parseImmutable() throws ParseException {
        match("VAL");
        String first;

        if (peek(Token.Type.IDENTIFIER)) {
            first = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
            if (peek("=")) {
                match("=");
                Ast.Expression exp = parseExpression();
                return new Ast.Global(first, false, Optional.of(exp));
            } else {
                // Missing equals sign
                throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
            }
        } else {
            // missing identifier
            throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
        }
    }

    /**
     * Parses the {@code method} rule. This method should only be called if the
     * next tokens start a method, aka {@code DEF}.
     */
    public Ast.Function parseFunction() throws ParseException {
        match("FUN");
        String first;
        List<String> params = new ArrayList<>();
        List<Ast.Statement> statements = new ArrayList<>();

        if (peek(Token.Type.IDENTIFIER)) {
            first = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);
            if (peek("(")) {
                match("(");
                while (peek(Token.Type.IDENTIFIER)) {
                    params.add(tokens.get(0).getLiteral());
                    match(Token.Type.IDENTIFIER);
                    if (peek(",")) {
                        match(",");
                        if (!peek(Token.Type.IDENTIFIER)) {
                            // missing identifier after comma
                            throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
                        }
                    } else if (peek(Token.Type.IDENTIFIER)) {
                        // two identifiers back to back
                        throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
                    }
                }
                if (peek(")")) {
                    match(")");
                    if (peek("DO")) {
                        match("DO");
                        //TODO check block
                        statements = parseBlock();
                        if (peek("END")) {
                            match("END");
                            return new Ast.Function(first, params, statements);
                        } else {
                            // missing END
                            throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
                        }

                    } else {
                        // missing DO
                        throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
                    }
                } else {
                    // missing ) with no params
                    throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
                }
            } else {
                //missing (
                throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
            }
        } else {
            //missing IDENTIFIER
            throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
        }

    }

    /**
     * Parses the {@code block} rule. This method should only be called if the
     * preceding token indicates the opening a block.
     */
    public List<Ast.Statement> parseBlock() throws ParseException {
        ArrayList<Ast.Statement> statements = new ArrayList<>();
        //TODO make sure this covers all cases for statement building
        while (!peek("ELSE") || !peek("END") || !peek("DEFAULT") || !peek("CASE")) {
            statements.add(parseStatement());
        }
        return statements;
    }

    /**
     * Parses the {@code statement} rule and delegates to the necessary method.
     * If the next tokens do not start a declaration, if, while, or return
     * statement, then it is an expression/assignment statement.
     */
    public Ast.Statement parseStatement() throws ParseException {
        if (peek("LET")) {
            return parseDeclarationStatement();
        } else if (peek("SWITCH")) {
            return parseSwitchStatement();
        } else if (peek("IF")) {
            return parseIfStatement();
        } else if (peek("WHILE")) {
            return parseWhileStatement();
        } else if (peek("RETURN")) {
            return parseReturnStatement();
        } else {
            Ast.Expression first = parseExpression();
            if (peek("=")) {
                match("=");
                Ast.Expression second = parseExpression();
                if (peek(";")) {
                    match(";");
                    return new Ast.Statement.Assignment(first, second);
                } else {
                    //no closing semicolon
                    throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
                }
            } else {
                if (peek(";")) {
                    match(";");
                    return new Ast.Statement.Expression(first);
                } else {
                    //no closing semicolon
                    throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
                }
            }
        }
    }

    /**
     * Parses a declaration statement from the {@code statement} rule. This
     * method should only be called if the next tokens start a declaration
     * statement, aka {@code LET}.
     */
    public Ast.Statement.Declaration parseDeclarationStatement() throws ParseException {
        match("LET");
        String first;

        if (peek(Token.Type.IDENTIFIER)) {
            first = tokens.get(0).getLiteral();
            match(Token.Type.IDENTIFIER);

            if (peek("=")) {
                match("=");
                //TODO may throw error? will have to check
                Ast.Expression value = parseExpression();
                if (peek(";")) {
                    match(";");
                    return new Ast.Statement.Declaration(first, Optional.of(value));
                } else {
                    //missing ;
                    throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
                }
            } else {
                if (peek(";")) {
                    match(";");
                    return new Ast.Statement.Declaration(first, Optional.empty());
                } else {
                    //missing ; and no equals
                    throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
                }
            }
        } else {
            //missing IDENTIFIER
            throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
        }
    }

    /**
     * Parses an if statement from the {@code statement} rule. This method
     * should only be called if the next tokens start an if statement, aka
     * {@code IF}.
     */
    public Ast.Statement.If parseIfStatement() throws ParseException {
        match("IF");
        Ast.Expression first = parseExpression();
        List<Ast.Statement> then = new ArrayList<>();
        List<Ast.Statement> elses = new ArrayList<>();

        if (peek("DO")) {
            //TODO check this block
            then = parseBlock();
            if (peek("ELSE")) {
                match("ELSE");
                //TODO check block
                elses = parseBlock();
            }
            if (peek("END")) {
                match("MATCH");
                return new Ast.Statement.If(first, then, elses);
            }
            //no ELSE or END
            throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
        } else {
            //missing DO
            throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
        }
    }

    /**
     * Parses a switch statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a switch statement, aka
     * {@code SWITCH}.
     */
    public Ast.Statement.Switch parseSwitchStatement() throws ParseException {
        match("SWITCH");
        Ast.Expression first = parseExpression();
        List<Ast.Statement.Case> cases = new ArrayList<>();

        if (peek("CASE") || peek("DEFAULT")) {
            while (peek("CASE") || peek("DEFAULT")) {
                cases.add(parseCaseStatement());
            }
            return new Ast.Statement.Switch(first, cases);
        } else {
            //SWITCH with no case or default
            throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
        }

    }

    /**
     * Parses a case or default statement block from the {@code switch} rule.
     * This method should only be called if the next tokens start the case or
     * default block of a switch statement, aka {@code CASE} or {@code DEFAULT}.
     */
    public Ast.Statement.Case parseCaseStatement() throws ParseException {
        List<Ast.Statement> statements = new ArrayList<>();

        if (peek("CASE")) {
            match("CASE");
            Ast.Expression first = parseExpression();
            if (peek(":")) {
                match(":");
                //TODO check block
                statements = parseBlock();
                return new Ast.Statement.Case(Optional.of(first), statements);
            } else {
                //missing :
                throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
            }
        } else if (peek("DEFAULT")) {
            match("DEFAULT");
            //TODO check block
            statements = parseBlock();
            if (peek("END")) {
                match("END");
                return new Ast.Statement.Case(Optional.empty(), statements);
            } else {
                //no END
                throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
            }

        } else {
            //nothing in the switch statement, cant parse case
            throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
        }
    }

    /**
     * Parses a while statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a while statement, aka
     * {@code WHILE}.
     */
    public Ast.Statement.While parseWhileStatement() throws ParseException {
        match("WHILE");
        Ast.Expression first = parseExpression();
        List<Ast.Statement> statements = new ArrayList<>();

        if (peek("DO")) {
            match("DO");
            //TODO check block
            statements = parseBlock();
            if (peek("END")) {
                match("END");
                return new Ast.Statement.While(first, statements);
            } else {
                //missing END
                throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
            }
        } else {
            //missing DO
            throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
        }
    }

    /**
     * Parses a return statement from the {@code statement} rule. This method
     * should only be called if the next tokens start a return statement, aka
     * {@code RETURN}.
     */
    public Ast.Statement.Return parseReturnStatement() throws ParseException {
        match("RETURN");

        Ast.Expression first = parseExpression();

        if (peek(";")) {
            match(";");
            return new Ast.Statement.Return(first);
        } else {
            //missing semicolon
            throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
        }
    }

    /**
     * Parses the {@code expression} rule.
     */
    public Ast.Expression parseExpression() throws ParseException {
        return parseLogicalExpression();
    }

    /**
     * Parses the {@code logical-expression} rule.
     */
    public Ast.Expression parseLogicalExpression() throws ParseException {
        Ast.Expression first = parseComparisonExpression();

        while (peek("&&") || peek("||")) {
            String op = tokenToString(Token.Type.OPERATOR);
            Ast.Expression second = parseComparisonExpression();

            if (peek("&&") || peek("||")) {
                first = new Ast.Expression.Binary(op, first, second);
            } else {
                return new Ast.Expression.Binary(op, first, second);
            }
        }
        return first;
    }

    /**
     * Parses the {@code equality-expression} rule.
     */
    public Ast.Expression parseComparisonExpression() throws ParseException {
        Ast.Expression first = parseAdditiveExpression();

        while (peek(">") || peek("<") || peek("==") || peek("!=")) {
            String op = tokenToString(Token.Type.OPERATOR);
            Ast.Expression second = parseAdditiveExpression();

            if (peek(">") || peek("<") || peek("==") || peek("!=")) {
                first = new Ast.Expression.Binary(op, first, second);
            } else {
                return new Ast.Expression.Binary(op, first, second);
            }
        }
        return first;
    }

    /**
     * Parses the {@code additive-expression} rule.
     */
    public Ast.Expression parseAdditiveExpression() throws ParseException {
        Ast.Expression first = parseMultiplicativeExpression();

        while (peek("+") || peek("-")) {
            String op = tokenToString(Token.Type.OPERATOR);
            Ast.Expression second = parseMultiplicativeExpression();

            if (peek("+") || peek("-")) {
                first = new Ast.Expression.Binary(op, first, second);
            } else {
                return new Ast.Expression.Binary(op, first, second);
            }
        }
        return first;
    }

    /**
     * Parses the {@code multiplicative-expression} rule.
     */
    public Ast.Expression parseMultiplicativeExpression() throws ParseException {
        Ast.Expression first = parsePrimaryExpression();

        while (peek("*") || peek("/") || peek("^")) {
            String op = tokenToString(Token.Type.OPERATOR);
            Ast.Expression second = parsePrimaryExpression();

            if (peek("[*]") || peek("/") || peek("[^]")) {
                first = new Ast.Expression.Binary(op, first, second);
            } else {
                return new Ast.Expression.Binary(op, first, second);
            }
        }
        return first;
    }

    /**
     * Parses the {@code primary-expression} rule. This is the top-level rule
     * for expressions and includes literal values, grouping, variables, and
     * functions. It may be helpful to break these up into other methods but is
     * not strictly necessary.
     */
    public Ast.Expression parsePrimaryExpression() throws ParseException {
        if (peek("NIL")) {
            match("NIL");
            return new Ast.Expression.Literal(null);
        } else if (peek("TRUE")) {
            match("TRUE");
            return new Ast.Expression.Literal(Boolean.TRUE);
        } else if (peek("FALSE")) {
            match("FALSE");
            return new Ast.Expression.Literal(Boolean.FALSE);
        } else if (peek(Token.Type.INTEGER)) {
            return new Ast.Expression.Literal(new BigInteger(tokenToString(Token.Type.INTEGER)));
        } else if (peek(Token.Type.DECIMAL)) {
            return new Ast.Expression.Literal(new BigDecimal(tokenToString(Token.Type.DECIMAL)));
        } else if (peek(Token.Type.CHARACTER)) {
            String c = tokenToString(Token.Type.CHARACTER);
            c = c.substring(1, c.length() - 1);
            c = c.replace("\\b", "\b");
            c = c.replace("\\n", "\n");
            c = c.replace("\\r", "\r");
            c = c.replace("\\t", "\t");
            c = c.replace("\\\"", "\"");
            c = c.replace("\\\\", "\\");
            c = c.replace("\\\'", "\'");
            Character first = c.charAt(0);
            return new Ast.Expression.Literal(first);
        } else if (peek(Token.Type.STRING)) {
            String s = tokenToString(Token.Type.STRING);
            s = s.substring(1, s.length() - 1);
            s = s.replace("\\b", "\b");
            s = s.replace("\\n", "\n");
            s = s.replace("\\r", "\r");
            s = s.replace("\\t", "\t");
            s = s.replace("\\\"", "\"");
            s = s.replace("\\\\", "\\");
            s = s.replace("\\\'", "\'");
            return new Ast.Expression.Literal(s);
        } else if (peek("(")) {
            match("(");
            Ast.Expression first = parseExpression();
            if (peek(")")) {
                match(")");
                return new Ast.Expression.Group(first);
            } else {
                //error that there is something else other than closing quote on group
                throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
            }
        } else if (peek(Token.Type.IDENTIFIER)) {
            String first = tokenToString(Token.Type.IDENTIFIER);
            if (peek("(")) {
                match("(");
                List<Ast.Expression> arguments = new ArrayList<>();
                while (!peek(")")) {
                    arguments.add(parseExpression());
                    if (peek(",")) {
                        match(",");

                        if (peek(")")) {
                            //error that there is a closing bracket after a comma
                            throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
                        }
                    }
                }
                if (peek(")")) {
                    match(")");
                    return new Ast.Expression.Function(first, arguments);
                }
                //error that there is no closing bracket on function
                throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
            } else if (peek("[")) {
                match("[");
                Ast.Expression second = parseExpression();
                if (peek("]")) {
                    match("]");
                    return new Ast.Expression.Access(Optional.of(second), first);
                } else {
                    //error that it doesn't end in a ']'
                    if (peek(".")) {
                        throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
                    } else {
                        throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
                    }
                }
            } else {
                //just return an identifier
                return new Ast.Expression.Access(Optional.empty(), first);
            }
        } else {
            //error that there is nothing to peek/match as a primary expression
            throw new ParseException("PARSE ERRORRRR!", errorIndex(tokens.has(0)));
        }
    }

    /**
     * As in the lexer, returns {@code true} if the current sequence of tokens
     * matches the given patterns. Unlike the lexer, the pattern is not a regex;
     * instead it is either a {@link Token.Type}, which matches if the token's
     * type is the same, or a {@link String}, which matches if the token's
     * literal is the same.
     * <p>
     * In other words, {@code Token(IDENTIFIER, "literal")} is matched by both
     * {@code peek(Token.Type.IDENTIFIER)} and {@code peek("literal")}.
     */
    private boolean peek(Object... patterns) {
        for (int i = 0; i < patterns.length; i++) {
            if (!tokens.has(i)) {
                return false;
            } else if (patterns[i] instanceof Token.Type) {
                if (patterns[i] != tokens.get(i).getType()) {
                    return false;
                }
            } else if (patterns[i] instanceof String) {
                if (!patterns[i].equals(tokens.get(i).getLiteral())) {
                    return false;
                }
            } else {
                throw new AssertionError("Invalid pattern object" + patterns.getClass());
            }
        }
        return true;
    }

    /**
     * As in the lexer, returns {@code true} if {@link #peek(Object...)} is true
     * and advances the token stream.
     */
    private boolean match(Object... patterns) {
        boolean peek = peek(patterns);
        if (peek) {
            for (int i = 0; i < patterns.length; i++) {
                tokens.advance();
            }
        }
        return peek;
    }

    private static final class TokenStream {

        private final List<Token> tokens;
        private int index = 0;

        private TokenStream(List<Token> tokens) {
            this.tokens = tokens;
        }

        /**
         * Returns true if there is a token at index + offset.
         */
        public boolean has(int offset) {
            return index + offset < tokens.size();
        }

        /**
         * Gets the token at index + offset.
         */
        public Token get(int offset) {
            return tokens.get(index + offset);
        }

        /**
         * Advances to the next token, incrementing the index.
         */
        public void advance() {
            index++;
        }

    }

}
