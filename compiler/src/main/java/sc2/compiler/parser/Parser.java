//
// Copyright (c) 2006, Brian Frank and Andy Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Sep 05  Brian Frank  Creation
//   29 Aug 06  Brian Frank  Ported from Java to Fan
//
package sc2.compiler.parser;

import sc2.compiler.CompilerLog;
import sc2.compiler.ast.Token;
import sc2.compiler.ast.AstNode;
import sc2.compiler.ast.Loc;
import sc2.compiler.ast.Expr;
import sc2.compiler.ast.Type;
import sc2.compiler.ast.AstNode.FileUnit;
import sc2.compiler.ast.AstNode.*;
import sc2.compiler.ast.Token.TokenKind;
import sc2.compiler.CompilerLog.CompilerErr;
import java.util.ArrayList;
import static sc2.compiler.ast.Token.TokenKind.abstractKeyword;
import static sc2.compiler.ast.Token.TokenKind.asyncKeyword;
import static sc2.compiler.ast.Token.TokenKind.constKeyword;
import static sc2.compiler.ast.Token.TokenKind.enumKeyword;
import static sc2.compiler.ast.Token.TokenKind.extensionKeyword;
import static sc2.compiler.ast.Token.TokenKind.externKeyword;
import static sc2.compiler.ast.Token.TokenKind.finalKeyword;
import static sc2.compiler.ast.Token.TokenKind.funKeyword;
import static sc2.compiler.ast.Token.TokenKind.internalKeyword;
import static sc2.compiler.ast.Token.TokenKind.overrideKeyword;
import static sc2.compiler.ast.Token.TokenKind.privateKeyword;
import static sc2.compiler.ast.Token.TokenKind.protectedKeyword;
import static sc2.compiler.ast.Token.TokenKind.publicKeyword;
import static sc2.compiler.ast.Token.TokenKind.readonlyKeyword;
import static sc2.compiler.ast.Token.TokenKind.staticKeyword;
import static sc2.compiler.ast.Token.TokenKind.structKeyword;
import static sc2.compiler.ast.Token.TokenKind.traitKeyword;
import static sc2.compiler.ast.Token.TokenKind.virtualKeyword;

/**
 *
 * @author yangjiandong
 */
public class Parser {

    FileUnit unit;    // compilation unit to generate
    ArrayList<Token> tokens;       // tokens all read in
    protected int numTokens;           // number of tokens
    protected int pos;                 // offset into tokens for cur
    protected Token cur;           // current token
    protected TokenKind curt;             // current token type
    protected Token peek;          // next token
    protected TokenKind peekt;            // next token type
//    protected boolean inFieldInit;        // are we currently in a field initializer
//    protected Type curType;        // current TypeDef scope

    CompilerLog log;
    
    public Parser(CompilerLog log, String code, FileUnit unit) {
        this.log = log;
        this.unit = unit;
        Tokenizer toker = new Tokenizer(log, unit.name, code);
        tokens = toker.tokenize();
        
        this.numTokens = tokens.size();
        reset(0);
    }
    
    Loc curLoc() {
        return cur.loc;
    }

    public void parse() {
        imports();
        while (curt != TokenKind.eof) {
            try {
                AstNode defNode = topLevelDef();
                if (defNode != null) {
                    unit.addDef(defNode);
                }
            } catch (CompilerErr e) {
                if (!recoverToDef()) {
                    break;
                }
            }
        }
    }

    private boolean recoverToDef() {
        int oldPos = pos;
        while (curt != TokenKind.eof) {
            if (curt == TokenKind.classKeyword ||
                    curt == TokenKind.structKeyword ||
                    curt == TokenKind.traitKeyword ||
                    curt == TokenKind.enumKeyword ||
                    curt == TokenKind.funKeyword) {
                int curPos = this.pos;
                while (isModifierFlags(tokens.get(curPos - 1)) && curPos > 0) {
                    --curPos;
                }

                if (curPos <= oldPos) {
                    return false;
                }
                reset(curPos);
                return true;
            }
            consume();
        }
        return false;
    }

//////////////////////////////////////////////////////////////////////////
// Usings
//////////////////////////////////////////////////////////////////////////
    /**
     ** <using> :=  <usingPod> |
     *  <usingType> | <usingAs>
     **   <usingPod> := "import" <podSpec> <eos>
     **   <usingType> := "import" <podSpec> "::" <id> <eos>
     **   <usingAs> := "import" <podSpec> "::" <id> "as" <id> <eos>
     **   <podSpec> :=  <id>
     */
    private void imports() {
        while (curt == TokenKind.importKeyword) {
            parseImports();
        }
    }

    private void parseImports() {
        Loc loc = curLoc();
        consume(TokenKind.importKeyword);
        Import u = new Import();

        // using podName
        u.podName = consumeId();
        if (curt == TokenKind.doubleColon) {
            consume();
            u.name = consumeId();

            // using pod::type as rename
            if (curt == TokenKind.asKeyword) {
                consume();
                u.asName = consumeId();
            }
        }

        endLoc(u, loc);
        unit.addDef(u);
    }

    private AstNode topLevelDef() {
        // [<doc>]
        Comments doc = doc();
        if (curt == TokenKind.importKeyword) {
            err("Cannot use ** doc comments before using statement");
            parseImports();
        }
        if (curt == TokenKind.eof) {
            return null;
        }

        // <flags>
        int flags = flags();

        switch (curt) {
            case traitKeyword:
            case enumKeyword:
            case structKeyword:
                return typeDef(doc, flags);
            case funKeyword:
            {
                consume();
                Loc loc = curLoc();
                String sname = consumeId();
                return methodDef(loc, doc, flags, null, sname);
            }
            default:
            {
                Loc loc = curLoc();
                String sname = consumeId();
                return fieldDef(loc, doc, flags, null, sname);
            }
        }
    }
    
    private AstNode slotDef(Comments doc) {
        // <flags>
        int flags = flags();

        switch (curt) {
            case funKeyword:
            {
                consume();
                Loc loc = curLoc();
                String sname = consumeId();
                return methodDef(loc, doc, flags, null, sname);
            }
            default:
            {
                Loc loc = curLoc();
                String sname = consumeId();
                return fieldDef(loc, doc, flags, null, sname);
            }
        }
    }

//////////////////////////////////////////////////////////////////////////
// TypeDef
//////////////////////////////////////////////////////////////////////////
    /**
     ** TypeDef:
     **   <typeDef> :=  <classDef> | <mixinDef> | <enumDef>
     **
     **   <classDef> :=  <classHeader> <classBody>
     **   <classHeader> := [<doc>] <facets> <typeFlags> "class" [<inheritance>]
     **   <classFlags> := [<protection>] ["abstract"] ["virtual"]
     **   <classBody> := "{" <slotDefs> "}"
     **   <enumDef> :=  <enumHeader> * <enumBody>
     **   <enumHeader> := [<doc>] <facets> <protection> "enum" [<inheritance>]
     **   <enumBody> := "{" <enumDefs> <slotDefs> "}" * * <facetDef :=
     **   <mixinHeader> := [<doc>] <facets> <protection> "mixin" [<inheritance>]
     **   <mixinBody> := "{" <slotDefs> "}" 
     **   <protection> := "public" | "protected" | "private" | "internal"
     **   <inheritance> := ":" <typeList>*
     */
    TypeDef typeDef(Comments doc, int flags) {
        // local working variables
        Loc loc = curLoc();
        boolean isMixin = false;
        boolean isEnum = false;

        TypeDef typeDef = null;
        StructDef structDef = null;
        TraitDef traitDef = null;
        // mixin
        if (curt == TokenKind.traitKeyword) {
            if ((flags & AstNode.Abstract) != 0) {
                err("The 'abstract' modifier is implied on trait");
            }
            flags = flags | AstNode.Mixin | AstNode.Abstract;
            isMixin = true;
            consume();
            
            // name
            String name = consumeId();
            // lookup TypeDef
            traitDef = new TraitDef(doc, flags, name);
            typeDef = traitDef;
        }
        else if (curt == TokenKind.enumKeyword) {
            if ((flags & AstNode.Const) != 0) {
                err("The 'const' modifier is implied on enum");
            }
            if ((flags & AstNode.Abstract) != 0) {
                err("Cannot use 'abstract' modifier on enum");
            }
            flags = AstNode.Enum;
            isEnum = true;
            consume();
            
            // name
            String name = consumeId();
            // lookup TypeDef
            EnumDef def = new EnumDef(doc, flags, name);
            typeDef = def;
        } // class
        else {
            consume(TokenKind.structKeyword);
            
            // name
            String name = consumeId();
            // lookup TypeDef
            structDef = new StructDef(doc, flags, name);
            typeDef = structDef;
        }

        //GenericType Param
        if (curt == TokenKind.lt) {
            consume();
            ArrayList<GeneriParamDef> gparams = new ArrayList<GeneriParamDef>();
            while (true) {
                Loc gloc = curLoc();
                String paramName = consumeId();
                GeneriParamDef param = new GeneriParamDef();
                param.loc = gloc;
                param.name = paramName;
                gparams.add(param);
                if (curt == TokenKind.comma) {
                    consume();
                    continue;
                } else if (curt == TokenKind.gt) {
                    consume();
                    break;
                } else {
                    throw err("Error token: " + curt);
                }
            }
            typeDef.generiParamDefs = gparams;
        }

        if (structDef != null) {
            // inheritance
            if (curt == TokenKind.colon) {
                // first inheritance type can be extends or mixin
                consume();
                Type first = inheritType();
                structDef.inheritances.add(first);

                // additional mixins
                while (curt == TokenKind.comma) {
                    consume();
                    structDef.inheritances.add(inheritType());
                }
            }
        }

        // start class body
        consume(TokenKind.lbrace);

        // if enum, parse values
        if (isEnum) {
            EnumDef enumDef = (EnumDef)typeDef;
            enumDefs(enumDef);
        }
        else {
            // slots
            while (true) {
                Comments sdoc = this.doc();
                if (curt == TokenKind.rbrace) {
                    break;
                }
                AstNode slot = slotDef(sdoc);
                if (isMixin) {
                    if (slot instanceof FuncDef) {
                        traitDef.addSlot((FuncDef)slot);
                    }
                    else {
                        err("Can't define field in trait");
                    }
                }
                else {
                    structDef.addSlot(slot);
                }
            }
        }

        // end of class body
        consume(TokenKind.rbrace);
        endLoc(typeDef, loc);

        return typeDef;
    }

    private Type inheritType() {
        Type t = simpleType();
        return t;
    }

//////////////////////////////////////////////////////////////////////////
// Flags
//////////////////////////////////////////////////////////////////////////
    
    private boolean isModifierFlags(Token t) {
        switch (t.kind) {
            case publicKeyword:
            case internalKeyword:
            case privateKeyword:
            case protectedKeyword:

            case abstractKeyword:
            case constKeyword:
            case finalKeyword:
            case virtualKeyword:
            case externKeyword:
                return true;

            case readonlyKeyword:
            case extensionKeyword:
            case overrideKeyword:
            case staticKeyword:
            case asyncKeyword:
            case funKeyword:
            case mutKeyword:
                return true;
        }
        return false;
    }

    /**
     ** Parse any list of flags in any order, we will check invalid *
     * combinations in the CheckErrors step.
     */
    private int flags() {
//    Loc loc = cur.loc;
        int flags = 0;
        boolean protection = false;
        for (boolean done = false; !done;) {
            int oldFlags = flags;
            switch (curt) {
                case abstractKeyword:
                    flags = flags | (AstNode.Abstract);
                    break;
                case constKeyword:
                    flags = flags | (AstNode.Const);
                    break;
                case readonlyKeyword:
                    flags = flags | (AstNode.Readonly);
                    break;
                case finalKeyword:
                    flags = flags | (AstNode.Final);
                    break;
                case internalKeyword:
                    flags = flags | (AstNode.Internal);
                    protection = true;
                    break;
                case externKeyword:
                    flags = flags | (AstNode.Native);
                    break;
                case extensionKeyword:
                    flags = flags | (AstNode.Extension);
                    break;
                case overrideKeyword:
                    flags = flags | (AstNode.Override);
                    break;
                case privateKeyword:
                    flags = flags | (AstNode.Private);
                    protection = true;
                    break;
                case protectedKeyword:
                    flags = flags | (AstNode.Protected);
                    protection = true;
                    break;
                case publicKeyword:
                    flags = flags | (AstNode.Public);
                    protection = true;
                    break;
                case staticKeyword:
                    flags = flags | (AstNode.Static);
                    break;
                case virtualKeyword:
                    flags = flags | (AstNode.Virtual);
                    break;
                //case TokenKind.rtconstKeyword:   flags = flags.or(FConst.RuntimeConst)
                case asyncKeyword:
                    flags = flags | (AstNode.Async);
                    break;
                default:
                    done = true;
            }
            if (done) {
                break;
            }
            if (oldFlags == flags) {
                err("Repeated modifier");
            }
            oldFlags = flags;
            consume();
        }

        return flags;
    }

//////////////////////////////////////////////////////////////////////////
// Enum
//////////////////////////////////////////////////////////////////////////
    
    /**
     ** Enum definition list:
     **   <enumDefs> :=  <enumDef> ("," <enumDef>)* <eos>
     */
    private void enumDefs(EnumDef def) {
        // parse each enum def
        int ordinal = 0;
        def.enumDefs.add(enumSlotDef(ordinal++));
        while (curt == TokenKind.comma) {
            consume();
            FieldDef enumDef = enumSlotDef(ordinal++);
            def.enumDefs.add(enumDef);
        }
        endOfStmt();
    }

    /**
     ** Enum definition:
     **   <enumDef> :=  <facets> <id> ["(" <args> ")"]*
     */
    private FieldDef enumSlotDef(int ordinal) {
        Comments doc = doc();
        Loc loc = curLoc();
        FieldDef def = new FieldDef(doc, consumeId());

        // optional ctor args
        if (curt == TokenKind.assign) {
            consume(TokenKind.assign);
            def.initExpr = expr();
        }

        endLoc(def, loc);
        return def;
    }

//////////////////////////////////////////////////////////////////////////
// Deep parser
//////////////////////////////////////////////////////////////////////////
    /**
     ** Top level for blocks which must be surrounded by braces
     */
    Block block() {
        consume(TokenKind.lbrace);
        int deep = 1;
        while (deep > 0 && curt != TokenKind.eof) {
            if (curt == TokenKind.rbrace) {
                --deep;
            } else if (curt == TokenKind.lbrace) {
                ++deep;
            }
            consume();
        }
        return null;
    }

    private boolean skipBracket() {
        return skipBracket(true);
    }

    private boolean skipBracket(boolean brace) {
        boolean success = false;
        if (curt == TokenKind.lparen) {
            consume();
            int deep = 1;
            while (deep > 0 && curt != TokenKind.eof) {
                if (curt == TokenKind.rparen) {
                    --deep;
                } else if (curt == TokenKind.lparen) {
                    ++deep;
                }
                consume();
            }
            success = true;
        }
        if (brace && curt == TokenKind.lbrace) {
            consume();
            int deep = 1;
            while (deep > 0 && curt != TokenKind.eof) {
                if (curt == TokenKind.rbrace) {
                    --deep;
                } else if (curt == TokenKind.lbrace) {
                    ++deep;
                }
                consume();
            }
            success = true;
        }
        return success;
    }

    private static boolean isExprValue(TokenKind t) {
        switch (t) {
            case identifier:
            case intLiteral:
            case strLiteral:
            case floatLiteral:
            case trueKeyword:
            case falseKeyword:
            case thisKeyword:
            case superKeyword:
            case itKeyword:
            case nullKeyword:
                return true;
        }
        return false;
    }

    private static boolean isJoinToken(TokenKind t) {
        switch (t) {
            case dot://        ("."),
            case colon://         (":"),
            case doubleColon://   ("::"),
            case plus://          ("+"),
            case minus://         ("-"),
            case star://          ("*"),
            case slash://         ("/"),
            case percent://       ("%"),
            case pound://         ("#"),
            case increment://     ("++"),
            case decrement://     ("--"),
            case isKeyword://,
//      case isnotKeyword://,
            case asKeyword://,
            case tilde://         ("~"),
            case pipe://          ("|"),
            case amp://           ("&"),
            case caret://         ("^"),
            case at://            ("@"),
            case doublePipe://    ("||"),
            case doubleAmp://     ("&&"),
            case same://          ("==="),
            case notSame://       ("!=="),
            case eq://            ("=="),
            case notEq://         ("!="),
            case cmp://           ("<=>"),
            case lt://            ("<"),
            case ltEq://          ("<="),
            case gt://            (">"),
            case gtEq://          (">="),
            case dotDot://        (".."),
            case dotDotLt://      ("..<"),
            case arrow://         ("->"),
            case tildeArrow://    ("~>"),
            case elvis://         ("?:"),
            case safeDot://       ("?."),
            case safeArrow://     ("?->"),
            case safeTildeArrow://("?~>"),
                return true;
        }
        return false;
    }

    Expr expr() {
        while (curt != TokenKind.eof) {
            if (isExprValue(curt)) {
                consume();
                skipBracket();
                if (isExprValue(curt)) {
                    break;
                }
                continue;
            }

            if (isJoinToken(curt)) {
                consume();
                if (skipBracket()) {
                    if (isExprValue(curt)) {
                        break;
                    }
                }
                continue;
            }
            break;
        }
        return null;
    }

//////////////////////////////////////////////////////////////////////////
// FieldDef
//////////////////////////////////////////////////////////////////////////
    /**
     ** Field definition:
     **   <fieldDef> :=  <facets> <fieldFlags> [<type>] <id> [":=" <expr>] * [ "{"
     * [<fieldGetter>] [<fieldSetter>] "}" ] <eos>
     **   <fieldFlags> := [<protection>] ["readonly"] ["static"]
     **   <fieldGetter> := "get" (<eos> | <block>)
     **   <fieldSetter> :=  <protection> "set" (<eos> | <block>)*
     */
    private FieldDef fieldDef(Loc loc, Comments doc, int flags, Type type, String name) {
        return fieldDef(loc, doc, flags, type, name, false);
    }
    private FieldDef fieldDef(Loc loc, Comments doc, int flags, Type type, String name, boolean isLocalVar) {
        // define field itself
        FieldDef field = new FieldDef(doc, name);
        field.flags = flags;
        field.fieldType = type;
        
        if (curt == TokenKind.colon) {
            consume();
            field.fieldType = typeRef();
        }

        // field initializer
        if (curt == TokenKind.assign) {
            //if (curt === TokenKind.assign) err("Must use := for field initialization")
            consume();
            field.initExpr = expr();
        }

        // disable type inference for now - doing inference for literals is
        // pretty trivial, but other types is tricky;  I'm not sure it is such
        // a hot idea anyways so it may just stay disabled forever
        if (field.fieldType == null) {
            err("Type inference not supported for fields");
        }

        endOfStmt();
        endLoc(field, loc);
        return field;
    }

//////////////////////////////////////////////////////////////////////////
// MethodDef
//////////////////////////////////////////////////////////////////////////
    /**
     ** Method definition:
     **   <methodDef> :=  <facets> <methodFlags> <type> <id> "(" <params> ")"
     * <methodBody>
     **   <methodFlags> := [<protection>] ["virtual"] ["override"] ["abstract"]
     * ["static"]
     **   <params> := [<param> ("," <param>)*]
     **   <param> :=  <type> <id> [":=" <expr>]
     **   <methodBody> :=  <eos> | ( "{" <stmts> "}" )*
     */
    private FuncDef methodDef(Loc loc, Comments doc, int flags, Type ret, String name) {
        FuncDef method = new FuncDef();
        method.loc = loc;
        method.comment = doc;
        method.flags = flags;
        method.prototype.returnType = ret;
        method.name = name;

        funcPrototype(method.prototype);

        // if This is returned, then we configure inheritedRet
        // right off the bat (this is actual signature we will use)
        //if (ret.isThis) method.inheritedRet = parent.asRef
        // if no body expected
        //if (parent.isNative) flags = flags.or(FConst.Native)
        if (curt == TokenKind.lbrace) {
            block();  // keep parsing
        } else {
            endOfStmt();
        }

        endLoc(method, loc);
        return method;
    }
    
    protected void funcPrototype(FuncPrototype prototype) {
        consume(TokenKind.lparen);
        if (curt != TokenKind.rparen) {
            while (true) {
                ParamDef newParam = paramDef();
                prototype.paramDefs.add(newParam);
                if (curt == TokenKind.rparen) {
                    break;
                }
                consume(TokenKind.comma);
            }
        }
        consume(TokenKind.rparen);
        
        if (curt == TokenKind.colon) {
            consume();
            prototype.returnType = typeRef();
        }
    }

    private ParamDef paramDef() {
        Loc loc = curLoc();
        if (curt == TokenKind.dotDotDot) {
            consume();
            ParamDef param = new ParamDef();
            param.name = consumeId();
            endLoc(param, loc);
            return param;
        }

        ParamDef param = new ParamDef();
        if (curt == TokenKind.assign) {
            //if (curt === TokenKind.assign) err("Must use := for parameter default");
            consume();
            param.defualtValue = expr();
        }
        endLoc(param, loc);
        return param;
    }

//////////////////////////////////////////////////////////////////////////
// Types
//////////////////////////////////////////////////////////////////////////

    /**
     ** Type signature:
     **   <type> :=  <simpleType> | <listType> | <mapType> | <funcType>
     **   <listType> :=  <type> "[]"
  *
     */
    protected Type typeRef() {
        return typeRef(false);
    }

    protected Type typeRef(boolean isTypeRef) {
        Type t = null;

        boolean isConst = false;
        if (curt == TokenKind.constKeyword) {
            isConst = true;
            consume();
        }
        
        Type.PointerType pointerType = null;
        if (null != curt) switch (curt) {
            case ownKeyword:
                pointerType = Type.PointerType.own;
                break;
            case refKeyword:
                pointerType = Type.PointerType.ref;
                break;
            case weakKeyword:
                pointerType = Type.PointerType.weak;
                break;
            default:
                break;
        }
        
        Loc loc = curLoc();

        // Types can begin with:
        //   - id
        //   - [](a:T):T
        if (curt == TokenKind.identifier) {
            t = simpleType();
        }
        else if (curt == TokenKind.lbracket) {
            t = funcType(isTypeRef);
        }
        else {
            throw err("Expecting type name not $cur");
//            consume();
//            return Type.placeHolder(loc);
        }
        
        if (curt == TokenKind.star) {
            consume();
            t = Type.pointerType(loc, t, pointerType);
            
            // check for ? nullable
            if (curt == TokenKind.question) {
                consume(TokenKind.question);
                t.isNullable = true;
                if (curt == TokenKind.question) {
                    err("Type cannot have multiple '?'");
                }
            }
        }

        // trailing [] for array
        if (curt == TokenKind.lbracket) {
            consume(TokenKind.lbracket);            
            if (curt == TokenKind.rbracket) {
                t = Type.arrayRefType(loc, t);
            }
            else if (curt == TokenKind.intLiteral) {
                t = Type.arrayType(loc, t, (Integer)cur.val);
            }
        }

        endLoc(t, loc);
        return t;
    }

    /**
     ** Simple type signature:
     **   <simpleType> :=  <id> ["::" <id>]*
     */
    private Type simpleType() {
        Loc loc = cur.loc;
        String id = consumeId();

        Type type;
        // fully qualified
        if (curt == TokenKind.doubleColon) {
            consume();
            type = new Type();
            type.loc = loc;
            type.name = consumeId();
            type.namespace = id;
        } else {
            type = new Type();
            type.loc = loc;
            type.name = id;
        }

        //generic param
        if (curt == TokenKind.lt) {
            consume();
            ArrayList<Type> params = new ArrayList<Type>();
            while (true) {
                Type type1 = typeRef();
                params.add(type1);
                if (curt == TokenKind.comma) {
                    consume();
                    continue;
                } else if (curt == TokenKind.gt) {
                    consume();
                    break;
                }
                break;
            }
            type.genericArgs = params;
        }

        // got it
        endLoc(type, loc);
        return type;
    }

    /**
     ** Method type signature:
     **   <funcType> := "|" ("->" | <funcTypeSig>) "|"
     **   <funcTypeSig> :=  <formals> ["->" <type>]
     **   <formals> := [<formal> ("," <formal>)*]
     **   <formal> :=  <formFull> | <formalInferred> | <formalTypeOnly>
     **   <formalFull> :=  <type> <id>
     **   <formalInferred> :=  <id>
     **   <formalTypeOnly> :=  <type>
     **
     ** If isTypeRef is true (slot signatures), then we requrie explicit *
     * parameter types.
  *
     */
    protected Type funcType(boolean isTypeRef) {
        Loc loc = cur.loc;
        Type ret = Type.voidType(loc);

        // opening pipe
        consume(TokenKind.pipe);

        //TODO
        // closing pipe
        consume(TokenKind.pipe);

        FuncPrototype ft = new FuncPrototype();
        //endLoc(ft);
        return null;
    }

//////////////////////////////////////////////////////////////////////////
// Misc
//////////////////////////////////////////////////////////////////////////
    /**
     ** Parse fandoc or return null
     *
     */
    private Comments doc() {
        Loc loc0 = cur.loc;
        Comments comments = null;
        while (curt == TokenKind.docComment || curt == TokenKind.cmdComment) {
            Loc loc = cur.loc;
            TokenKind kind = curt;
            String lines = (String) consume().val;
            Comment doc = new Comment(lines, kind);
            if (comments == null) {
                comments = new Comments();
                comments.loc = loc;
            }
            comments.comments.add(doc);
            endLoc(doc, loc);
        }
        if (comments != null) {
            endLoc(comments, loc0);
        }
        return comments;
    }

//////////////////////////////////////////////////////////////////////////
// Errors
//////////////////////////////////////////////////////////////////////////
    CompilerErr err(String msg) {
        return log.err(msg, cur.loc);
    }

//////////////////////////////////////////////////////////////////////////
// Tokens
//////////////////////////////////////////////////////////////////////////
    /**
     ** Verify current is an identifier, consume it, and return it.
     *
     */
    protected String consumeId() {
        if (curt != TokenKind.identifier) {
            throw err("Expected identifier, not '$cur'");
            //consume();
            //return "";
        }
        return (String) consume().val;
    }

    /**
     ** Check that the current token matches the specified * type, but do not
     * consume it.
     *
     */
    protected void verify(TokenKind kind) {
        if (!curt.equals(kind)) {
            throw err("Expected '$kind.symbol', not '$cur'");
        }
    }

    /**
     ** Consume the current token and return consumed token. * If kind is
     * non-null then verify first
  *
     */
    protected Token consume() {
        return consume(null);
    }

    protected Token consume(TokenKind kind) {
        // verify if not null
        if (kind != null) {
            verify(kind);
        }

        // save the current we are about to consume for return
        Token result = cur;

        // get the next token from the buffer, if pos is past numTokens,
        // then always use the last token which will be eof
        Token next;
        pos++;
        if (pos + 1 < numTokens) {
            next = tokens.get(pos + 1);  // next peek is cur+1
        } else {
            next = tokens.get(numTokens - 1);
        }

        this.cur = peek;
        this.peek = next;
        this.curt = cur.kind;
        this.peekt = peek.kind;

        return result;
    }

    //** next next token
    protected Token peekpeek() {
        if (pos + 2 < numTokens) {
            return tokens.get(pos + 2);
        }
        return tokens.get(numTokens - 1);
    }

    /**
     ** update loc.len field
     *
     */
    protected void endLoc(AstNode node, Loc loc) {
        Token preToken = (pos > 0) ? tokens.get(pos - 1) : cur;
        int lastEnd = preToken.loc.offset + preToken.len;
        Loc self = node.loc;
        int selfEnd = self.offset + node.len;
        if (lastEnd == selfEnd) {
            return;
        }
        if (lastEnd < selfEnd) {
            return;
        }

        int len = lastEnd - self.offset;
        //loc := Loc.make(self.file, self.line, self.col, self.offset, lastEnd-self.offset)
        node.len = len;
    }

    /**
     ** Statements can be terminated with a semicolon, end of line * or } end
     * of block. Return true on success. On failure * return false if errMsg is
     * null or log/throw an exception.
  *
     */
    protected boolean endOfStmt() {
        return endOfStmt("Expected end of statement with ';' not '" + cur + "'");
    }

    protected boolean endOfStmt(String errMsg) {
        //if (cur.newline) return true;
        if (curt == TokenKind.semicolon) {
            consume();
            return true;
        }
//        if (curt == TokenKind.rbrace) {
//            return true;
//        }
//        if (curt == TokenKind.eof) {
//            return true;
//        }
        if (errMsg == null) {
            return false;
        }
        err(errMsg);
        return false;
    }

    /**
     ** Reset the current position to the specified tokens index.
  *
     */
    protected void reset(int pos) {
        this.pos = pos;
        this.cur = tokens.get(pos);
        if (pos + 1 < numTokens) {
            this.peek = tokens.get(pos + 1);
        } else {
            this.peek = tokens.get(pos);
        }
        this.curt = cur.kind;
        this.peekt = peek.kind;
    }
}
