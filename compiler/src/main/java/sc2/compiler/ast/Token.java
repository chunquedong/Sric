//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sc2.compiler.ast;

import java.util.HashMap;

/**
 *
 * @author yangjiandong
 */
public class Token {

    public static enum TokenKind {
        identifier      ("identifier", false),
        strLiteral      ("Str literal", false),
        intLiteral      ("Int literal", false),
        floatLiteral    ("Float literal", false),
        // operators
        dot(".", false),
        semicolon     (";", false),
        comma         (",", false),
        colon         (":", false),
        doubleColon   ("::", false),
        plus          ("+", false),
        minus         ("-", false),
        star          ("*", false),
        slash         ("/", false),
        percent       ("%", false),
        pound         ("#", false),
        increment     ("++", false),
        decrement     ("--", false),
        bang          ("!", false),
        question      ("?", false),
        tilde         ("~", false),
        pipe          ("|", false),
        amp           ("&", false),
        caret         ("^", false),
        at            ("@", false),
        doublePipe    ("||", false),
        doubleAmp     ("&&", false),
        same          ("===", false),
        notSame       ("!==", false),
        eq            ("==", false),
        notEq         ("!=", false),
        cmp           ("<=>", false),
        lt            ("<", false),
        ltEq          ("<=", false),
        gt            (">",  false),
        gtEq          (">=", false),
        lpipe         ("<|", false),
        rpipe         ("|>", false),
        lbrace        ("{", false),
        rbrace        ("}", false),
        lparen        ("(", false),
        rparen        (")", false),
        lbracket      ("[", false),
        rbracket      ("]", false),
        dotDot        ("..", false),
        dotDotDot     ("...", false),
        dotDotLt      ("..<", false),
        defAssign     (":=", false),
        assign        ("=", false),
        assignPlus    ("+=", false),
        assignMinus   ("-=", false),
        assignStar    ("*=", false),
        assignSlash   ("/=", false),
        assignPercent ("%=", false),
        arrow         ("->", false),
        tildeArrow    ("~>", false),
        elvis         ("?:", false),
        safeDot       ("?.", false),
        safeArrow     ("?->", false),
        safeTildeArrow("?~>", false),
        rightShift    ("rightShif", false),
        leftShift     ("<<", false),
        docComment    ("/**", false),
        cmdComment    ("//@", false),
        slComment     ("//", false),
        mlComment     ("/*", false),
        dollar        ("$", false),
        lparenSynthetic ("(", false),  // synthetic () grouping of interpolated string exprs

        // keywords
        abstractKeyword("abstract", true),
        asKeyword("as", true),
        //assertKeyword,
        breakKeyword("break", true),
        caseKeyword("case", true),
        catchKeyword("catch", true),
        classKeyword("class", true),
        structKeyword("struct", true),
        enumKeyword("enum", true),
        constKeyword("const", true),
        continueKeyword("continue", true),
        defaultKeyword("default", true),
        doKeyword("do", true),
        elseKeyword("else", true),
        falseKeyword("false", true),
        //finalKeyword("final", true),
        finallyKeyword("finally", true),
        forKeyword("for", true),
        foreachKeyword("foreach", true),
        ifKeyword("if", true),
        //internalKeyword("internal", true),
        isKeyword("is", true),
        itKeyword("it", true),
        traitKeyword("trait", true),
        externKeyword("extern", true),
        newKeyword("new", true),
        nullKeyword("null", true),
        overrideKeyword("override", true),
        privateKeyword("private", true),
        protectedKeyword("protected", true),
        publicKeyword("public", true),
        readonlyKeyword("readonly", true),
        returnKeyword("return", true),
        staticKeyword("static", true),
        superKeyword("super", true),
        switchKeyword("switch", true),
        thisKeyword("this", true),
        throwKeyword("throw", true),
        throwsKeyword("throws", true),
        trueKeyword("true", true),
        tryKeyword("try", true),
        importKeyword("import", true),
        virtualKeyword("virtual", true),
        volatileKeyword("volatile", true),
        voidKeyword("void", true),
        whileKeyword("while", true),
        extensionKeyword("extension", true),
        inlineKeyword("inline", true),
        mutKeyword("mut", true),
        asyncKeyword("async", true),
        yieldKeyword("yield", true),
        //lretKeyword("lret", true),
        awaitKeyword("await", true),
        sizeofKeyword("sizeof", true),
        offsetofKeyword("offsetof", true),
        uninitKeyword("uninit", true),
        funKeyword("fun", true),
        ownKeyword("own", true),
        refKeyword("ref", true),
        moveKeyword("move", true),
        weakKeyword("weak", true),
        rawKeyword("raw", true),
        unsafeKeyword("unsafe", true),
        typealiasKeyword("typealias", true),
        varKeyword("var", true),
        reflectKeyword("reflect", true),
        packedKeyword("packed", true),
        fallthroughKeyword("fallthrough", true),
        
        // misc
        eof("eof", false);
        
        public final String symbol;
        public final boolean keyword;
                
        TokenKind(String symbol, boolean isKeyword) {
            this.symbol = symbol;
            this.keyword = isKeyword;
        }
        
        @Override
        public String toString() {
            return super.toString() + "(" + symbol+")";
        }
    }
    
    public static HashMap<String, TokenKind> keywords = new HashMap<String, TokenKind>();
    static {
        for (TokenKind kind : TokenKind.class.getEnumConstants()) {
            if (kind.keyword) {
                keywords.put(kind.symbol, kind);
            }
        }
    }
    
    public final TokenKind kind;      // enum for Token type
    public final Object val;        // Str, Int, Float, Duration, or Str[]
    public Loc loc;         // position of token
    public int len;         // length of token
    public boolean whitespace;// was this token preceeded by whitespace
    public boolean newline;// have we processed one or more newlines since the last token
    
    
    public Token(TokenKind kind, Object val, Loc loc, int len) {
        this.kind = kind;
        this.val = val;
        this.loc = loc;
        this.len = len;
    }
    
    public Token(TokenKind kind, Object val) {
        this.kind = kind;
        this.val = val;
    }
    
    public Token(TokenKind kind) {
        this.kind = kind;
        this.val = null;
        this.len = -1;
    }
    
    public boolean isAssign() {
        switch (kind) {
            case assign:
            case assignPlus:
            case assignMinus:
            case assignStar:
            case assignSlash:
            case assignPercent:
                return true;
        }
        return false;
    }
    
    @Override
    public String toString() {
        if (val == null) {
            return this.kind.toString();
        }
        return this.kind + ":" + val;
    }
}
