//
// Copyright (c) 2006, Brian Frank and Andy Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Sep 05  Brian Frank  Creation
//   29 Aug 06  Brian Frank  Ported from Java to Fan
//
package sric.compiler.parser;

import sric.compiler.CompilerLog;
import sric.compiler.ast.Token;
import sric.compiler.ast.Loc;
import sric.compiler.CompilerLog.CompilerErr;
import sric.compiler.ast.Token.TokenKind;
import java.util.ArrayList;

/**
 *
 * @author yangjiandong
 */
public class Tokenizer {

    private String buf;           // buffer
    private int pos;           // index into buf for cur
    private boolean parseComment;
    private String filename;      // source file name
    private int line = 1;     // pos line number
    private int col = 1;      // pos column number
    private int curLine;       // line number of current token
    private int cur;           // current char
    private int peek;          // next char
    private int lastLine;      // line number of last token returned from next()
    private int posOfLine;     // index into buf for start of current line
    private ArrayList<Token> tokens; // token accumulator
    private boolean inStrLiteral; // return if inside a string literal token
    private boolean whitespace;   // was there whitespace before current token
    private CompilerLog log;

    public Tokenizer(CompilerLog log, String filename, String buf) {
        this.log = log;
        this.buf = buf;
        this.filename = filename;
        this.parseComment = true;

        this.tokens = new ArrayList<Token>();
        this.inStrLiteral = false;
        this.posOfLine = 0;
        this.whitespace = false;

        // initialize cur and peek
        cur = peek = ' ';
        if (buf.length() > 0) {
            cur = buf.charAt(0);
        }
        if (buf.length() > 1) {
            peek = buf.charAt(1);
        }
        pos = 0;

        // if first line starts with #, then treat it like an end of
        // line, so that Unix guys can specify the executable to run
        if (cur == '#') {
            while (true) {
                if (cur == '\n') {
                    consume();
                    break;
                }
                if (cur == 0) {
                    break;
                }
                consume();
            }
        }
    }

//////////////////////////////////////////////////////////////////////////
// Access
//////////////////////////////////////////////////////////////////////////
    /**
     ** Tokenize the entire input into a list of tokens.
  *
     */
    public ArrayList<Token> tokenize() {
        while (true) {
            Token tok = next();
            tokens.add(tok);
            if (tok.kind.equals(TokenKind.eof)) {
                break;
            }
        }
        return tokens;
    }

    /**
     ** Return the next token in the buffer.
  *
     */
    public Token next() {
        while (true) {
            // save current line
            curLine = this.line;
            int col = this.col;
            int offset = pos;

            // find next token
            Token tok;
            try {
                tok = find();
                if (tok == null) {
                    continue;
                }
            } catch (CompilerErr e) {
                continue;
            }

            // fill in token's location
            tok.loc = new Loc(filename, curLine, col, offset);
            tok.len = this.pos - offset;
            tok.newline = lastLine < line;
            tok.whitespace = whitespace;

            // save last line, clear whitespace flag
            lastLine = line;
            whitespace = false;

            return tok;
        }
//    return null; // TODO - shouldn't need this
    }
    
    public static boolean isWhitespace(int ch) {
        switch (ch) {
            case ' ':
            case '\t':
            case '\n':
            case '\r':
                return true;
        }
        return false;
    }

    /**
     ** Find the next token or return null.
  *
     */
    private Token find() {
        // skip whitespace
        if (isWhitespace(cur)) {
            consume();
            whitespace = true;
            return null;
        }

        // alpha means keyword or identifier
        if (isIdentifierStart(cur)) {
            return word();
        }

        // number or .number (note that + and - are handled as unary operator)
        if (Character.isDigit(cur)) {
            return number();
        }
        if (cur == '.' && Character.isDigit(peek)) {
            return number();
        }

        // str literal
        if (cur == '"' && peek == '"' && peekPeek() == '"') {
            return quoted(true);
        }
        if (cur == '"') {
            return quoted(false);
        }
        if (cur == '\'') {
            return ch();
        }

        // comments
        if (cur == '/' && peek == '/') {
            return readCommentSL();
        }
        if (cur == '/' && peek == '*') {
            return readCommentML();
        }

        // symbols
        return symbol();
    }

//////////////////////////////////////////////////////////////////////////
// Word
//////////////////////////////////////////////////////////////////////////
    /**
     ** Parse a word token: alpha (alpha|number)* * Words are either keywords
     * or identifiers
  *
     */
    private Token word() {
        // store starting position of word
        int start = pos;

        // find end of word to compute length
        while (Character.isAlphabetic(cur) || Character.isDigit(cur) || cur == '_' || cur > 256) {
            consume();
        }

        // create Str (gc note this string might now reference buf)
        String word = buf.substring(start, pos);

        //replace alias
//    if (alias != null) {
//      if (alias.containsKey(word)) word = alias[word];
//    }
        // check keywords
        TokenKind keyword = Token.keywords.get(word);
        if (keyword != null) {
            return new Token(keyword);
        }

        // otherwise this is a normal identifier
        return new Token(TokenKind.identifier, word);
    }

    private static boolean isIdentifierStart(int c) {
        return Character.isAlphabetic(c) || c == '_' || c > 256;
    }

//////////////////////////////////////////////////////////////////////////
// Number
//////////////////////////////////////////////////////////////////////////
    /**
     ** Parse a number literal token: int, float, decimal, or duration.
  *
     */
    private Token number() {
        // check for hex value
        if (cur == '0' && peek == 'x') {
            return hex();
        }

        // find end of literal
        int start = pos;
        boolean dot = false;
        boolean exp = false;

        // whole part
        while (Character.isDigit(cur) || cur == '_') {
            consume();
        }

        // fraction part
        if (cur == '.' && Character.isDigit(peek)) {
            dot = true;
            consume();
            while (Character.isDigit(cur) || cur == '_') {
                consume();
            }
        }

        // exponent
        if (cur == 'e' || cur == 'E') {
            consume();
            exp = true;
            if (cur == '-' || cur == '+') {
                consume();
            }
            if (!Character.isDigit(cur)) {
                throw err("Expected exponent digits");
            }
            while (Character.isDigit(cur) || cur == '_') {
                consume();
            }
        }

        // string value of literal
        String str = buf.substring(start, pos).replace("_", "");

        // check for suffixes
        boolean floatSuffix = false;
        if (cur == 'f' || cur == 'F') {
            consume();
            floatSuffix = true;
        }
        try {
            // float literal
            if (floatSuffix || dot || exp) {
                double num = Double.parseDouble(str);
                return new Token(TokenKind.floatLiteral, num);
            }

            // int literal
            long num = Long.parseLong(str);
            return new Token(TokenKind.intLiteral, num);
        } catch (CompilerErr e) {
            throw err("Invalid numeric literal '$str'");
        }
    }

    static int fromDigit(int digit) {
        switch (digit) {
            case '0':
                return 0;
            case '1':
                return 1;
            case '2':
                return 2;
            case '3':
                return 3;
            case '4':
                return 4;
            case '5':
                return 5;
            case '6':
                return 6;
            case '7':
                return 7;
            case '8':
                return 8;
            case '9':
                return 9;
            case 'A':
            case 'a':
                return 10;
            case 'B':
            case 'b':
                return 11;
            case 'C':
            case 'c':
                return 12;
            case 'D':
            case 'd':
                return 13;
            case 'E':
            case 'e':
                return 14;
            case 'F':
            case 'f':
                return 15;
            default:
                return -1;
        }
    }

    /**
     ** Process hex int/long literal starting with 0x
  *
     */
    Token hex() {
        consume(); // 0
        consume(); // x

        // read first hex
        int val = fromDigit(cur);
        if (val == -1) {
            throw err("Expecting hex number");
        }
        consume();
        int nibCount = 1;
        while (true) {
            int nib = fromDigit(cur);
            if (nib == -1) {
                if (cur == '_') {
                    consume();
                    continue;
                }
                break;
            }
            nibCount++;
            if (nibCount > 16) {
                throw err("Hex literal too big");
            }
            val = (val << 4) + nib;
            consume();
        }

        return new Token(TokenKind.intLiteral, val);
    }

//////////////////////////////////////////////////////////////////////////
// Quoted Literals
//////////////////////////////////////////////////////////////////////////
    /**
     ** Parse a quoted literal token: normal, triple, or uri * Opening quote
     * must already be consumed.
  *
     */
    private Token quoted(boolean triple) {
        inStrLiteral = true;
        try {
            // opening quote
//      int line = this.line;
//      int col = this.col;
//      int offset = this.pos;
            consume();
            if (triple) {
                consume();
                consume();
            }

            // init starting position
            int openLine = posOfLine;
            int openPos = pos;
            boolean multiLineOk = true;
            StringBuilder s = new StringBuilder();
//      boolean interpolated = false;

            // loop until we find end of string
            while (true) {
                if (cur == 0) {
                    throw err("Unexpected end of $q");
                }

                if (endOfQuoted(triple)) {
                    break;
                }

                if (cur == '\n') {
                    //if (!multiLine) throw err("Unexpected end of $q");
                    s.append((char) cur);
                    consume();
                    if (multiLineOk) {
                        multiLineOk = skipStrWs(openLine, openPos);
                    }
                    continue;
                }
                if (cur == '\\') {
                    s.append((char) escape());
                } else {
                    s.append((char) cur);
                    consume();
                }
            }

            return new Token(TokenKind.strLiteral, s.toString());
        } finally {
            inStrLiteral = false;
        }
    }

    /**
     ** Leading white space in a multi-line string is assumed * to be outside
     * of the string literal. If there is an * non-whitespace char, then it is
     * an compile time error. * Return true if ok, false on error.
  *
     */
    private boolean skipStrWs(int openLine, int openPos) {
        for (int i = openLine; i < openPos; ++i) {
            int a = buf.charAt(i);
            if ((a == '\t' && cur != '\t') || (a != '\t' && cur != ' ')) {
                if (cur == '\n') {
                    return true;
                }
                int numTabs = 0;
                int numSpaces = 0;
                for (int j = openLine; j < openPos; ++j) {
                    if (buf.charAt(j) == '\t') {
                        ++numTabs;
                    } else {
                        ++numSpaces;
                    }
                }
                if (numTabs == 0) {
                    err("Leading space in multi-line Str must be $numSpaces spaces");
                } else {
                    err("Leading space in multi-line Str must be $numTabs tabs and $numSpaces spaces");
                }
                return false;
            }
            consume();
        }
        return true;
    }

    /**
     ** If at end of quoted literal consume the * ending token(s) and return
     * true.
  *
     */
    private boolean endOfQuoted(boolean triple) {
        if (triple) {
            if (cur != '"' || peek != '"' || peekPeek() != '"') {
                return false;
            }
            consume();
            consume();
            consume();
            return true;
        } else {
            if (cur != '"') {
                return false;
            }
            consume();
            return true;
        }
    }

//////////////////////////////////////////////////////////////////////////
// Char
//////////////////////////////////////////////////////////////////////////
    /**
     ** Parse a char literal token.
  *
     */
    private Token ch() {
        // consume opening quote
        consume();

        // if \ then process as escape
        int c = -1;
        if (cur == '\\') {
            c = escape();
        } else {
            c = cur;
            consume();
        }

        // expecting ' quote
        if (cur != '\'') {
            throw err("Expecting ' close of char literal");
        }
        consume();

        return new Token(TokenKind.intLiteral, c);
    }

    /**
     ** Parse an escapse sequence which starts with a \
  *
     */
    int escape() {
        // consume slash
        if (cur != '\\') {
            throw err("Internal error");
        }
        consume();

        // check basics
        switch (cur) {
            case 'b':
                consume();
                return '\b';
            case 'f':
                consume();
                return '\f';
            case 'n':
                consume();
                return '\n';
            case 'r':
                consume();
                return '\r';
            case 't':
                consume();
                return '\t';
            case '"':
                consume();
                return '"';
            case '$':
                consume();
                return '$';
            case '\'':
                consume();
                return '\'';
            case '`':
                consume();
                return '`';
            case '\\':
                consume();
                return '\\';
        }

//     check for uxxxx
//    if (cur == 'u')
//    {
//      consume();
//      int n3 = cur.fromDigit(16); consume();
//      int n2 = cur.fromDigit(16); consume();
//      int n1 = cur.fromDigit(16); consume();
//      int n0 = cur.fromDigit(16); consume();
//      if (n3 == null || n2 == null || n1 == null || n0 == null) throw err("Invalid hex value for \\uxxxx");
//      return n3.shiftl(12).or(n2.shiftl(8)).or(n1.shiftl(4)).or(n0);
//    }
        throw err("Invalid escape sequence");
    }

//////////////////////////////////////////////////////////////////////////
// Comments
//////////////////////////////////////////////////////////////////////////
    /**
     ** Skip a single line // comment
  *
     */
    private Token readCommentSL() {
        int start = pos;
        int end = start;
        int line = this.line;
        consume();   // first slash
        consume();   // next slash
        boolean isDoc = peek == '@';
        StringBuilder s = new StringBuilder();
        while (true) {
            if (cur == '\n') {
                end = pos - 1;
                consume();
                break;
            }
            if (cur == 0) {
                end = pos;
                break;
            }
            s.append((char) cur);
            consume();
        }
        if (isDoc) {
            return new Token(TokenKind.cmdComment, s.toString());
        }
        if (parseComment) {
            return new Token(TokenKind.mlComment, s.toString());
        }
        return null;
    }

    /**
     ** Skip a multi line /* comment. Note unlike C/Java, * slash/star comments
     * can be nested.
  *
     */
    private Token readCommentML() {
        int start = pos;
        int end = start;
        int line = this.line;
        consume();   // first slash
        consume();   // next slash
        boolean isDoc = peek == '*';
        int depth = 1;
        StringBuilder s = new StringBuilder();
        while (true) {
            if (cur == '*' && peek == '/') {
                consume();
                consume();
                depth--;
                if (depth <= 0) {
                    end = pos - 1;
                    break;
                }
            }
            if (cur == '/' && peek == '*') {
                consume();
                consume();
                depth++;
                continue;
            }
            if (cur == 0) {
                break;
            }
            s.append((char) cur);
            consume();
        }
        if (isDoc) {
            return new Token(TokenKind.docComment, s.toString());
        }
        if (parseComment) {
            return new Token(TokenKind.mlComment, s.toString());
        }
        return null;
    }

//////////////////////////////////////////////////////////////////////////
// Symbol
//////////////////////////////////////////////////////////////////////////
    /**
     ** Parse a symbol token (typically into an operator).
  *
     */
    private Token symbol() {
        int c = cur;
        consume();
        switch (c) {
            case '\r':
                throw err("Carriage return \\r not allowed in source");
            case '!':
                if (cur == '=') {
                    consume();
                    if (cur == '=') {
                        consume();
                        return new Token(TokenKind.notSame);
                    }
                    return new Token(TokenKind.notEq);
                }
                return new Token(TokenKind.bang);
            case '#':
                return new Token(TokenKind.pound);
            case '%':
                if (cur == '=') {
                    consume();
                    return new Token(TokenKind.assignPercent);
                }
                return new Token(TokenKind.percent);
            case '&':
                if (cur == '&') {
                    consume();
                    return new Token(TokenKind.doubleAmp);
                }
                return new Token(TokenKind.amp);
            case '(':
                return new Token(TokenKind.lparen);
            case ')':
                return new Token(TokenKind.rparen);
            case '*':
                if (cur == '=') {
                    consume();
                    return new Token(TokenKind.assignStar);
                }
                return new Token(TokenKind.star);
            case '+':
                if (cur == '=') {
                    consume();
                    return new Token(TokenKind.assignPlus);
                }
                if (cur == '+') {
                    consume();
                    return new Token(TokenKind.increment);
                }
                return new Token(TokenKind.plus);
            case ',':
                return new Token(TokenKind.comma);
            case '-':
                if (cur == '>') {
                    consume();
                    return new Token(TokenKind.arrow);
                }
                if (cur == '-') {
                    consume();
                    return new Token(TokenKind.decrement);
                }
                if (cur == '=') {
                    consume();
                    return new Token(TokenKind.assignMinus);
                }
                return new Token(TokenKind.minus);
            case '.':
                if (cur == '.') {
                    consume();
                    if (cur == '.') {
                        consume();
                        return new Token(TokenKind.dotDotDot);
                    }
                    if (cur == '<') {
                        consume();
                        return new Token(TokenKind.dotDotLt);
                    }
                    return new Token(TokenKind.dotDot);
                }
                return new Token(TokenKind.dot);
            case '/':
                if (cur == '=') {
                    consume();
                    return new Token(TokenKind.assignSlash);
                }
                return new Token(TokenKind.slash);
            case ':':
                if (cur == ':') {
                    consume();
                    return new Token(TokenKind.doubleColon);
                }
                if (cur == '=') {
                    consume();
                    return new Token(TokenKind.defAssign);
                }
                return new Token(TokenKind.colon);
            case ';':
                return new Token(TokenKind.semicolon);
            case '<':
                if (cur == '=') {
                    consume();
                    if (cur == '>') {
                        consume();
                        return new Token(TokenKind.cmp);
                    }
                    return new Token(TokenKind.ltEq);
                }
                return new Token(TokenKind.lt);
            case '=':
                if (cur == '=') {
                    consume();
                    if (cur == '=') {
                        consume();
                        return new Token(TokenKind.same);
                    }
                    return new Token(TokenKind.eq);
                }
                return new Token(TokenKind.assign);
            case '>':
                if (cur == '=') {
                    consume();
                    return new Token(TokenKind.gtEq);
                }
                return new Token(TokenKind.gt);
            case '?':
                if (cur == ':') {
                    consume();
                    return new Token(TokenKind.elvis);
                }
                if (cur == '.') {
                    consume();
                    return new Token(TokenKind.safeDot);
                }
                if (cur == '-' && peek == '>') {
                    consume();
                    consume();
                    return new Token(TokenKind.safeArrow);
                }
                if (cur == '~' && peek == '>') {
                    consume();
                    consume();
                    return new Token(TokenKind.safeTildeArrow);
                }
                return new Token(TokenKind.question);
            case '@':
                return new Token(TokenKind.at);
            case '[':
                return new Token(TokenKind.lbracket);
            case ']':
                return new Token(TokenKind.rbracket);
            case '^':
                return new Token(TokenKind.caret);
            case '{':
                return new Token(TokenKind.lbrace);
            case '|':
                if (cur == '|') {
                    consume();
                    return new Token(TokenKind.doublePipe);
                }
                return new Token(TokenKind.pipe);
            case '}':
                return new Token(TokenKind.rbrace);
            case '~':
                if (cur == '>') {
                    consume();
                    return new Token(TokenKind.tildeArrow);
                }
                return new Token(TokenKind.tilde);
            case '$':
                return new Token(TokenKind.dollar);
        }

        if (c == 0) {
            return new Token(TokenKind.eof);
        }

        throw err("Unexpected symbol: " + String.valueOf((char) c));
    }

//////////////////////////////////////////////////////////////////////////
// Utils
//////////////////////////////////////////////////////////////////////////
    /**
     ** Return a CompilerException for current location in source.
  *
     */
    CompilerErr err(String msg) {
        Loc loc = new Loc(filename, line, col, pos);
        return log.err(msg, loc);
    }

////////////////////////////////////////////////////////////////
// Consume
////////////////////////////////////////////////////////////////
    /**
     ** Peek at the character after peek
  *
     */
    private int peekPeek() {
        return pos + 2 < buf.length() ? buf.charAt(pos + 2) : 0;
    }

    /**
     ** Consume the cur char and advance to next char in buffer: * - updates
     * cur and peek fields * - updates the line and col count * - end of file,
     * sets fields to 0
  *
     */
    private void consume() {
        // if cur is a line break, then advance line number,
        // because the char we are getting ready to make cur
        // is the first char on the next line
        if (cur == '\n') {
            line++;
            col = 1;
            posOfLine = pos + 1;
        } else {
            col++;
        }

        // get the next character from the buffer, any
        // problems mean that we have read past the end
        cur = peek;
        pos++;
        if (pos + 1 < buf.length()) {
            peek = buf.charAt(pos + 1); // next peek is cur+1
        } else {
            peek = 0;
        }
    }
}
