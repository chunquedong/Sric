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

    void parse() {
        usings();
        while (curt.equals(TokenKind.eof)) {
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
            if (curt == TokenKind.classKeyword && curt == TokenKind.mixinKeyword) {
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
     ** Parse <using>* - note that we are just skipping them because * they are
     * already parsed by ScanForUsingsAndTypes. * *   <using> :=  <usingPod> |
     * <usingType> | <usingAs>
     **   <usingPod> := "using" <podSpec> <eos>
     **   <usingType> := "using" <podSpec> "::" <id> <eos>
     **   <usingAs> := "using" <podSpec> "::" <id> "as" <id> <eos>
     **   <podSpec> :=  <id> | <str> | <ffiPodSpec>
     **   <ffiPodSpec> := "[" <id> "]" <id> ("." <id>)*
  *
     */
    private void usings() {
        while (curt == TokenKind.usingKeyword) {
            parseUsing();
        }
    }

    private void parseUsing() {
        consume(TokenKind.usingKeyword);
        Import u = new Import();
        u.loc = this.cur.loc;

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

        endLoc(u);
        unit.addDef(u);
    }

    private AstNode topLevelDef() {
        // [<doc>]
        String doc = doc();
        //if (curt == TokenKind.usingKeyword) throw err("Cannot use ** doc comments before using statement");
        if (curt == TokenKind.eof) {
            return null;
        }

        // <facets>
//    facets = facets();
        // <flags>
        int flags = flags();

//    loc := cur.loc;
        if (curt == TokenKind.mixinKeyword || curt == TokenKind.enumKeyword || curt == TokenKind.structKeyword) {
            return typeDef(doc, flags);
        }

        // otherwise must be field or method
//    type := typeRef;
//    name := consumeId;
//    if (curt === TokenKind.lparen)
//    {
//      return methodDef(loc, curType, doc, facets, flags, type, name);
//    }
//    else
//    {
//      return fieldDef(loc, curType, doc, facets, flags, type, name);
//    }
        String sdoc = this.doc();
        int flag = flags();
        String sname = consumeId();
        Type type = ctype();
        AstNode slot = methodDef(cur.loc, sdoc, flag, type, sname);
        return slot;
    }

//////////////////////////////////////////////////////////////////////////
// TypeDef
//////////////////////////////////////////////////////////////////////////
    /**
     ** TypeDef:
     **   <typeDef> :=  <classDef> | <mixinDef> | <enumDef> | <facetDef>
     **
     **   <classDef> :=  <classHeader> <classBody>
     **   <classHeader> := [<doc>] <facets> <typeFlags> "class" [<inheritance>]
     **   <classFlags> := [<protection>] ["abstract"] ["final"]
     **   <classBody> := "{" <slotDefs> "}" * *   <enumDef> :=  <enumHeader>
     * <enumBody>
     **   <enumHeader> := [<doc>] <facets> <protection> "enum" [<inheritance>]
     **   <enumBody> := "{" <enumDefs> <slotDefs> "}" * * <facetDef :=
     *  <facetHeader> <enumBody>
     **   <facetHeader> := [<doc>] <facets> [<protection>] "facet" "class" <id>
     * [<inheritance>]
     **   <facetBody> := "{" <slotDefs> "}" * *   <mixinDef> :=  <enumHeader>
     * <enumBody>
     **   <mixinHeader> := [<doc>] <facets> <protection> "mixin" [<inheritance>]
     **   <mixinBody> := "{" <slotDefs> "}" * *   <protection> := "public" |
     * "protected" | "private" | "internal"
     **   <inheritance> := ":" <typeList>
  *
     */
    TypeDef typeDef(String doc, int flags) {
        // local working variables
        Loc loc = cur.loc;
        boolean isMixin = false;
        boolean isEnum = false;

        // mixin
        if (curt == TokenKind.mixinKeyword) {
            if ((flags & AstNode.Abstract) != 0) {
                err("The 'abstract' modifier is implied on mixin");
            }
            if ((flags & AstNode.Final) != 0) {
                err("Cannot use 'final' modifier on mixin");
            }
            flags = flags | AstNode.Mixin | AstNode.Abstract;
            isMixin = true;
            consume();
        }
        if (curt == TokenKind.enumKeyword) {
            if ((flags & AstNode.Const) != 0) {
                err("The 'const' modifier is implied on enum");
            }
            if ((flags & AstNode.Final) != 0) {
                err("The 'final' modifier is implied on enum");
            }
            if ((flags & AstNode.Abstract) != 0) {
                err("Cannot use 'abstract' modifier on enum");
            }
            flags = flags | AstNode.Enum | AstNode.Const | AstNode.Final;
            isEnum = true;
            consume();
        } // class
        else {
            consume(TokenKind.structKeyword);
        }

        // name
        String name = consumeId();
        // lookup TypeDef
        StructDef def = new StructDef();
        def.name = name;
        def.loc = loc;

        def.doc = doc();
        def.flags = flags();

        //GenericType Param
        if (curt == TokenKind.lt) {
            consume();
            ArrayList<GeneriParamDef> gparams = new ArrayList<GeneriParamDef>();
            while (true) {
                Loc gloc = cur.loc;
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
                    err("Error token: " + curt);
                }
            }
            def.generiParamDefs = gparams;
        }

        // open current type
//    curType = def;
//    closureCount = 0
        // inheritance
        if (curt == TokenKind.colon) {
            // first inheritance type can be extends or mixin
            consume();
            Type first = inheritType();
//      if (!first.isMixin)
//        def.base = first
//      else
//        def.mixins.add(first)
            def.inheritances.add(first);

            // additional mixins
            while (curt == TokenKind.comma) {
                consume();
                def.inheritances.add(inheritType());
            }
        }

        // start class body
        consume(TokenKind.lbrace);

        // if enum, parse values
        if (isEnum) {
            EnumDef enumDef = new EnumDef();
            enumDefs(enumDef);
            //TODO
        }

        // slots
        while (true) {
            String sdoc = this.doc();
            if (curt == TokenKind.rbrace) {
                break;
            }

            int flag = flags();
            String sname = consumeId();
            Type type = ctype();
            AstNode slot = fieldDef(cur.loc, sdoc, flag, type, sname);
            def.addSlot(slot);
        }

        // close cur type
//    closureCount = null
//    curType = null;
        // end of class body
        consume(TokenKind.rbrace);
        endLoc(def);

        return def;
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
            case nativeKeyword:
                return true;

            case readonlyKeyword:
//      case onceKeyword:
            case extensionKeyword:
            case overrideKeyword:
            case staticKeyword:
            case asyncKeyword:
            case funKeyword:
            case varKeyword:
            case letKeyword:
                return true;

            case rtconstKeyword:
                return true;
        }
        return false;
    }

    /**
     ** Parse any list of flags in any order, we will check invalid *
     * combinations in the CheckErrors step.
  *
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
                case constKeyword:
                    flags = flags | (AstNode.Const);
                case readonlyKeyword:
                    flags = flags | (AstNode.Readonly);
                case finalKeyword:
                    flags = flags | (AstNode.Final);
                case internalKeyword:
                    flags = flags | (AstNode.Internal);
                    protection = true;
                case nativeKeyword:
                    flags = flags | (AstNode.Native);
//        case onceKeyword:      flags = flags.or(FConst.Once); // Parser only flag
                case extensionKeyword:
                    flags = flags | (AstNode.Extension);
                case overrideKeyword:
                    flags = flags | (AstNode.Override);
                case privateKeyword:
                    flags = flags | (AstNode.Private);
                    protection = true;
                case protectedKeyword:
                    flags = flags | (AstNode.Protected);
                    protection = true;
                case publicKeyword:
                    flags = flags | (AstNode.Public);
                    protection = true;
                case staticKeyword:
                    flags = flags | (AstNode.Static);
                case virtualKeyword:
                    flags = flags | (AstNode.Virtual);
                //case TokenKind.rtconstKeyword:   flags = flags.or(FConst.RuntimeConst)
                case asyncKeyword:
                    flags = flags | (AstNode.Async);
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
  *
     */
    private void enumDefs(EnumDef def) {
        // create static$init to wrap enums in case
        // they have closures
//    sInit := MethodDef.makeStaticInit(def.loc, def, null)
//    sInit.code = Block(def.loc)
//    def.addSlot(sInit)
//    curSlot = sInit

        // parse each enum def
        int ordinal = 0;
        def.enumDefs.add(enumDef(ordinal++));
        while (curt == TokenKind.comma) {
            consume();
            FieldDef enumDef = enumDef(ordinal++);
            def.enumDefs.add(enumDef);
        }
        endOfStmt();

        // clear static$init scope
//    curSlot = null;
    }

    /**
     ** Enum definition:
     **   <enumDef> :=  <facets> <id> ["(" <args> ")"]
  *
     */
    private FieldDef enumDef(int ordinal) {
        String doc = doc();
//    facets := facets();

        FieldDef def = new FieldDef();
        def.loc = cur.loc;
        def.doc = doc;
        def.name = consumeId();

        // optional ctor args
        if (curt == TokenKind.assign) {
            consume(TokenKind.assign);
            def.initExpr = expr();
        }

        return def;
    }

//////////////////////////////////////////////////////////////////////////
// Deep parser
//////////////////////////////////////////////////////////////////////////
    /**
     ** Top level for blocks which must be surrounded by braces
  *
     */
    Block block() {
//    verify(TokenKind.lbrace)
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
//      case TokenKind.durationLiteral:
            case floatLiteral:
            case trueKeyword:
            case falseKeyword:
            case thisKeyword:
            case superKeyword:
            case itKeyword:
//      case TokenKind.dsl:
//      case TokenKind.uriLiteral:
//      case TokenKind.decimalLiteral:
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
     **   <fieldSetter> :=  <protection> "set" (<eos> | <block>)
  *
     */
    private FieldDef fieldDef(Loc loc, String doc, int flags, Type type, String name) {
        // define field itself
        FieldDef field = new FieldDef();
        field.loc = loc;
        field.doc = doc;
        field.flags = flags;
        field.name = name;
        if (type != null) {
            field.fieldType = type;
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
        if (type == null) {
            err("Type inference not supported for fields");
        }

        // explicit getter or setter
//    if (curt === TokenKind.lbrace)
//    {
//      consume(TokenKind.lbrace)
//      getOrSet(field)
//      getOrSet(field)
//      consume(TokenKind.rbrace)
//    }
        endOfStmt();
        endLoc(field);
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
     **   <methodBody> :=  <eos> | ( "{" <stmts> "}" )
  *
     */
    private FuncDef methodDef(Loc loc, String doc, int flags, Type ret, String name) {
        FuncDef method = new FuncDef();
        method.loc = loc;
        method.doc = doc;
//    method.facets = facets;
        method.flags = flags;
        if (ret != null) {
            method.prototype.returnType = ret;
        }

        // parameters
        consume(TokenKind.lparen);
        if (curt != TokenKind.rparen) {
            while (true) {
                ParamDef newParam = paramDef();
                method.prototype.paramDefs.add(newParam);
                if (curt == TokenKind.rparen) {
                    break;
                }
                consume(TokenKind.comma);
            }
        }
        consume(TokenKind.rparen);

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

        endLoc(method);
        return method;
    }

    private ParamDef paramDef() {
        if (curt == TokenKind.dotDotDot) {
            consume();
            ParamDef param = new ParamDef();
            param.loc = cur.loc;
            param.name = consumeId();
            endLoc(param);
            return param;
        }

        ParamDef param = new ParamDef();
        if (curt == TokenKind.assign) {
            //if (curt === TokenKind.assign) err("Must use := for parameter default");
            consume();
            param.defualtValue = expr();
        }
        endLoc(param);
        return param;
    }

//////////////////////////////////////////////////////////////////////////
// Types
//////////////////////////////////////////////////////////////////////////
    /**
     ** Parse a type production into a TypeRef and wrap it as AST TypeRef.
  *
     */
    protected Type typeRef() {
//    Loc loc := cur
//    return TypeRef(loc, ctype(true))
        return ctype(true);
    }

    /**
     ** Type signature:
     **   <type> :=  <simpleType> | <listType> | <mapType> | <funcType>
     **   <listType> :=  <type> "[]"
  *
     */
    protected Type ctype() {
        return ctype(false);
    }

    protected Type ctype(boolean isTypeRef) {
        Type t = null;
        Loc loc = cur.loc;

        // Types can begin with:
        //   - id
        //   - [k:v]
        //   - |a, b -> r|
        if (curt == TokenKind.autoKeyword) {
//      t = TypeRef.objType(loc);
            consume();
        } else if (curt == TokenKind.identifier) {
            t = simpleType();
        } else if (curt == TokenKind.lbracket) {
            loc = consume(TokenKind.lbracket).loc;
            t = ctype(isTypeRef);
            // check for type?:type map (illegal);
            if (curt == TokenKind.elvis && !cur.whitespace) {
                err("Map type cannot have nullable key type");
            }

            // check for ":" for map type
//        if (curt === TokenKind.colon)
//        {
//          if (t.isNullable) err("Map type cannot have nullable key type")
//          consume(TokenKind.colon)
//          key := t
//          val := ctype(isTypeRef)
//          //throw err("temp test")
//        //      t = MapType(key, val)
//          t = TypeRef.mapType(loc, key, val)
//        }
            consume(TokenKind.rbracket);
            //if (!(t is MapType)) err("Invalid map type", loc)
        } else if (curt == TokenKind.pipe) {
            t = funcType(isTypeRef);
        } else {
            err("Expecting type name not $cur");
            consume();
        }

        // check for ? nullable
        if (curt == TokenKind.question && !cur.whitespace) {
            consume(TokenKind.question);
            t.isNullable = true;
            if (curt == TokenKind.question && !cur.whitespace) {
                throw err("Type cannot have multiple '?'");
            }
        }

        // trailing [] for lists
        while (curt == TokenKind.lbracket && peekt == TokenKind.rbracket) {
            consume(TokenKind.lbracket);
            consume(TokenKind.rbracket);
            Type valT = t;
            t = new Type();
            t.loc = cur.loc;
            t.genericArgs.add(valT);
            if (curt == TokenKind.question && !cur.whitespace) {
                consume(TokenKind.question);
                t.isNullable = true;
            }
        }

        if (curt == TokenKind.star || curt == TokenKind.amp) {
//        if (curt == TokenKind.star) {
//            t = TypeRef.ownerptrType(loc, t);
//        }
//        else {
//            t = TypeRef.instantptrType(loc, t);
//        }
//TODO
            consume();
            if (curt == TokenKind.question && !cur.whitespace) {
                consume(TokenKind.question);
                t.isNullable = true;
            }
        }

        endLoc(t);
        return t;
    }

    /**
     ** Simple type signature:
     **   <simpleType> :=  <id> ["::" <id>]
  *
     */
    private Type simpleType() {
        Loc loc = cur.loc;
        String id = consumeId();

        Type type;
        // fully qualified
        if (curt == TokenKind.doubleColon) {
            consume();
//      return ResolveImports.resolveQualified(this, id, consumeId, loc) ?: ns.voidType
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
        endLoc(type);
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
    private String doc() {
        String doc = null;
        while (curt == TokenKind.mlComment) {
            //Loc loc = cur.loc;
            String lines = (String) consume(TokenKind.mlComment).val;
            doc = lines;
        }
        return doc;
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
            CompilerErr e = err("Expected identifier, not '$cur'");
            if (curt == TokenKind.eof) {
                throw e;
            }
            return "";
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
    protected void endLoc(AstNode node) {
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
        return endOfStmt("Expected end of statement: semicolon, newline, or end of block; not '" + cur + "'");
    }

    protected boolean endOfStmt(String errMsg) {
        //if (cur.newline) return true;
        if (curt == TokenKind.semicolon) {
            consume();
            return true;
        }
        if (curt == TokenKind.rbrace) {
            return true;
        }
        if (curt == TokenKind.eof) {
            return true;
        }
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
