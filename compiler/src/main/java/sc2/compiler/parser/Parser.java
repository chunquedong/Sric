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
import sc2.compiler.ast.*;
import sc2.compiler.ast.AstNode.FileUnit;
import sc2.compiler.ast.AstNode.*;
import sc2.compiler.ast.Token.TokenKind;
import sc2.compiler.CompilerLog.CompilerErr;
import java.util.ArrayList;
import sc2.compiler.ast.Expr.IdExpr;
import static sc2.compiler.ast.Token.TokenKind.*;
import sc2.compiler.ast.Type.NumType;


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
        usings();
        while (curt != TokenKind.eof) {
            try {
                TopLevelDef defNode = topLevelDef();
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
        while (curt != TokenKind.eof) {
            if (curt == TokenKind.colon) {
                consume();
                return true;
            }
            else if (curt == TokenKind.rbrace) {
                consume();
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
     ** <using> :=  <usingPod> | <usingType>
     **   <usingPod> := "import" <id> <eos>
     **   <usingType> := "import" <type>  ["as" <id>] <eos>
     */
    private void usings() {
        while (curt == TokenKind.importKeyword) {
            parseImports();
        }
    }
    
    private TypeAlias parseTypeAlias(Comments doc, int flags) {
        Loc loc = curLoc();
        consume(TokenKind.typealiasKeyword);
        TypeAlias u = new TypeAlias();
        u.name = consumeId();
        u.flags = flags;
        u.comment = doc;
        
        consume(TokenKind.assign);
        u.type = typeRef();

        endOfStmt();
        endLoc(u, loc);
        return u;
    }

    private void parseImports() {
        Loc loc = curLoc();
        consume(TokenKind.importKeyword);
        Import u = new Import();
        u.id = idExpr();
        if (curt == TokenKind.doubleColon && peekt == TokenKind.star) {
            consume();
            consume();
            u.star = true;
        }

        endOfStmt();
        endLoc(u, loc);
        unit.imports.add(u);
    }

    private TopLevelDef topLevelDef() {
        Loc loc = curLoc();
        // [<doc>]
        Comments doc = doc();

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
                String sname = consumeId();
                return methodDef(loc, doc, flags, null, sname);
            }
            case varKeyword:
            {
//                if (curt == TokenKind.constKeyword) {
//                    flags |= AstNode.Const;
//                }
                consume();
                String sname = consumeId();
                return fieldDef(loc, doc, flags, null, sname);
            }
            case typealiasKeyword:
                return parseTypeAlias(doc, flags);
        }
        throw err("Expected var,const,fun keyword");
    }
    
    private AstNode slotDef(Comments doc) {
        Loc loc = curLoc();
        // <flags>
        int flags = flags();

        switch (curt) {
            case funKeyword:
            {
                consume();
                String sname = consumeId();
                return methodDef(loc, doc, flags, null, sname);
            }
            case varKeyword:
            {
//                if (curt == TokenKind.constKeyword) {
//                    flags |= AstNode.Const;
//                }
                consume();
                String sname = consumeId();
                return fieldDef(loc, doc, flags, null, sname);
            }
        }
        throw err("Expected var,const,fun keyword");
    }
            
    /**
     ** Identifier expression:
     **   <idExpr> =  [(<id>"::")*] <id>
     */
    protected IdExpr idExpr() {
        Loc loc = curLoc();
        Expr.IdExpr e = new Expr.IdExpr(null);
        String id = consumeId();
        e.name = id;
        endLoc(e, loc);
        
        while (curt == TokenKind.doubleColon) {
            //a::* for import
            if (peekt == TokenKind.star) {
                break;
            }
            
            consume();
            IdExpr idexpr = idExpr();
            idexpr.namespace = e;
            e = idexpr;
        }
        
        return e;
    }

//////////////////////////////////////////////////////////////////////////
// TypeDef
//////////////////////////////////////////////////////////////////////////
    
    private ArrayList<GenericParamDef> tryGenericParamDef(AstNode parent) {
        if (curt == TokenKind.dollar && !peek.whitespace && peekt == TokenKind.lt) {
            consume();
            consume();
            ArrayList<GenericParamDef> gparams = new ArrayList<GenericParamDef>();
            while (true) {
                Loc gloc = curLoc();
                String paramName = consumeId();
                GenericParamDef param = new GenericParamDef();
                param.loc = gloc;
                param.name = paramName;
                param.parent = parent;
                param.index = gparams.size();
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
            return gparams;
        }
        return null;
    }
    
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
            if ((flags & FConst.Abstract) != 0) {
                err("The 'abstract' modifier is implied on trait");
            }
            flags = flags | FConst.Mixin | FConst.Abstract;
            isMixin = true;
            consume();
            
            // name
            String name = consumeId();
            // lookup TypeDef
            traitDef = new TraitDef(doc, flags, name);
            typeDef = traitDef;
        }
        else if (curt == TokenKind.enumKeyword) {
            if ((flags & FConst.Const) != 0) {
                err("The 'const' modifier is implied on enum");
            }
            if ((flags & FConst.Abstract) != 0) {
                err("Cannot use 'abstract' modifier on enum");
            }
            flags = FConst.Enum;
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
            
            //GenericType Param
            structDef.generiParamDefs = tryGenericParamDef(structDef);
        }


        if (structDef != null) {
            // inheritance
            if (curt == TokenKind.colon) {
                // first inheritance type can be extends or mixin
                consume();
                Type first = inheritType();
                structDef.inheritances = new ArrayList<Type>();
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
                    if (slot instanceof FieldDef) {
                        if (structDef.funcDefs.size() > 0) {
                            err("Field should come before methods");
                        }
                    }
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
    
    private int flags() {
//    Loc loc = cur.loc;
        int flags = 0;
        boolean protection = false;
        for (boolean done = false; !done;) {
            int oldFlags = flags;
            switch (curt) {
                case abstractKeyword:
                    flags = flags | (FConst.Abstract);
                    break;
                case constKeyword:
                    flags = flags | (FConst.Const);
                    break;
                case readonlyKeyword:
                    flags = flags | (FConst.Readonly);
                    break;
//                case finalKeyword:
//                    flags = flags | (AstNode.Final);
//                    break;
//                case internalKeyword:
//                    flags = flags | (AstNode.Internal);
//                    protection = true;
//                    break;
                case externKeyword:
                    flags = flags | (FConst.Native);
                    break;
                case extensionKeyword:
                    flags = flags | (FConst.Extension);
                    break;
                case overrideKeyword:
                    flags = flags | (FConst.Override);
                    break;
                case privateKeyword:
                    flags = flags | (FConst.Private);
                    protection = true;
                    break;
                case protectedKeyword:
                    flags = flags | (FConst.Protected);
                    protection = true;
                    break;
                case publicKeyword:
                    flags = flags | (FConst.Public);
                    protection = true;
                    break;
                case staticKeyword:
                    flags = flags | (FConst.Static);
                    break;
                case virtualKeyword:
                    flags = flags | (FConst.Virtual);
                    break;
                //case TokenKind.rtconstKeyword:   flags = flags.or(FConst.RuntimeConst)
                case asyncKeyword:
                    flags = flags | (FConst.Async);
                    break;
                case reflectKeyword:
                    flags = flags | (FConst.Reflect);
                    break;
                case unsafeKeyword:
                    flags = flags | (FConst.Unsafe);
                    break;
                case throwKeyword:
                    flags = flags | (FConst.Throws);
                    break;
                case inlineKeyword:
                    flags = flags | (FConst.Inline);
                    break;
                case packedKeyword:
                    flags = flags | (FConst.Packed);
                    break;
                case constexprKeyword:
                    flags = flags | (FConst.ConstExpr);
                    break;
                case operatorKeyword:
                    flags = flags | (FConst.Operator);
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
    
    private int funcPostFlags() {
        int flags = 0;
        for (boolean done = false; !done;) {
            int oldFlags = flags;
            switch (curt) {
                case constKeyword:
                    flags = flags | (FConst.Const);
                    break;
                case mutKeyword:
                    flags = flags | (FConst.Mutable);
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
        //endOfStmt();
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
     **   <fieldDef> :=  <facets> <fieldFlags> <id> ":" [<type>] ["=" <expr>] eos
     **   <fieldFlags> := [<protection>] ["readonly"] ["static"]
     */
    private FieldDef fieldDef(Loc loc, Comments doc, int flags, Type type, String name) {
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
        method.generiParamDefs = tryGenericParamDef(method);
        
        funcPrototype(method.prototype);

        // if This is returned, then we configure inheritedRet
        // right off the bat (this is actual signature we will use)
        //if (ret.isThis) method.inheritedRet = parent.asRef
        // if no body expected
        //if (parent.isNative) flags = flags.or(FConst.Native)
        if (curt == TokenKind.lbrace) {
            method.code = block();  // keep parsing
        } else {
            endOfStmt();
        }

        endLoc(method, loc);
        return method;
    }
    
    protected void funcPrototype(FuncPrototype prototype) {
        consume(TokenKind.lparen);
        if (curt != TokenKind.rparen) {
            prototype.paramDefs = new ArrayList<ParamDef>();
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
        
        if (curt == TokenKind.mutKeyword) {
            prototype.postFlags = funcPostFlags();
        }
        
        if (curt == TokenKind.colon) {
            consume();
            prototype.returnType = typeRef();
        }
        else {
            prototype.returnType = Type.voidType(cur.loc);
        }
    }

    private ParamDef paramDef() {
        Loc loc = curLoc();

        ParamDef param = new ParamDef();
        param.name = consumeId();
        
        consume(TokenKind.colon);
        
        if (curt == TokenKind.dotDotDot) {
            param.paramType = Type.varArgType(cur.loc);
            consume();
        }
        else {
            param.paramType = typeRef();
        }
        
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
     **   <type> :=  <simpleType> | <pointerType> | <funcType> | <arrayType> | <constType>
     */
    protected Type typeRef() {
        Loc loc = curLoc();
        Type type;
        switch (curt) {
            case ownKeyword:
                consume();
                return pointerType(Type.PointerAttr.own);
            case rawKeyword:
                consume();
                return pointerType(Type.PointerAttr.raw);
            case refKeyword:
                consume();
                return pointerType(Type.PointerAttr.ref);
            case weakKeyword:
                consume();
                return pointerType(Type.PointerAttr.weak);
//            case star:
//                return pointerType(Type.PointerAttr.ref);
            case constKeyword:
                return imutableType();
            case mutKeyword:
                return imutableType();
            case lbracket:
                return arrayType();
            default:
                break;
        }
        
        return funcOrSimpleType();
    }
    
    private Type pointerType(Type.PointerAttr pointerAttr) {
        Loc loc = curLoc();
        consume(TokenKind.star);
        boolean isNullable = false;
        if (curt == TokenKind.question) {
            consume(TokenKind.question);
            isNullable = true;
        }
        
        Type type = typeRef();
        type = Type.pointerType(loc, type, pointerAttr, isNullable);
        endLoc(type, loc);
        return type;
    }
    
    private Type arrayType() {
        Loc loc = curLoc();
        consume(TokenKind.lbracket);
        Expr size = null;
        if (curt != TokenKind.rbracket) {
            size = expr();
        }
        consume(TokenKind.rbracket);
        
        Type type = typeRef();
        type = Type.arrayType(loc, type, size);
        endLoc(type, loc);
        return type;
    }
    
    private Type imutableType() {
        boolean imutable = false;
        boolean explicitImutable = false;
        if (curt == TokenKind.constKeyword) {
            consume();
            imutable = true;
            explicitImutable = true;
        }
        else if (curt == TokenKind.mutKeyword) {
            consume();
            imutable = false;
            explicitImutable = true;
        }
        
        Type type = typeRef();
        type.explicitImutable = explicitImutable;
        type.isImutable = imutable;
        return type;
    }

    private Type funcOrSimpleType() {
        if (curt == TokenKind.funKeyword) {
            return funcType();
        }
        return simpleType();
    }

    /**
     ** Simple type signature:
     **   <simpleType> :=  <id> ["::" <id>]*
     */
    private Type simpleType() {
        Loc loc = cur.loc;
        IdExpr id = idExpr();

        Type type = null;
        
        //type name rewrite
        if (id.namespace == null) {
            NumType ntype = null;
            switch (id.name) {
                case "Int8":
                    ntype = Type.intType(loc);
                    ntype.size = 8;
                    break;
                case "Int16":
                    ntype = Type.intType(loc);
                    ntype.size = 16;
                    break;
                case "Int32":
                    ntype = Type.intType(loc);
                    ntype.size = 32;
                    break;
                case "Int64":
                    ntype = Type.intType(loc);
                    ntype.size = 64;
                    break;
                case "UInt8":
                    ntype = Type.intType(loc);
                    ntype.size = 8;
                    ntype.isUnsigned = true;
                    break;
                case "UInt16":
                    ntype = Type.intType(loc);
                    ntype.size = 16;
                    ntype.isUnsigned = true;
                    break;
                case "UInt32":
                    ntype = Type.intType(loc);
                    ntype.size = 32;
                    ntype.isUnsigned = true;
                    break;
                case "UInt64":
                    ntype = Type.intType(loc);
                    ntype.size = 64;
                    ntype.isUnsigned = true;
                    break;
                case "Float32":
                    ntype = Type.floatType(loc);
                    ntype.size = 32;
                    break;
                case "Float64":
                    ntype = Type.floatType(loc);
                    ntype.size = 64;
                    break;
                case "Int":
                    ntype = Type.intType(loc);
                    break;
                case "Float":
                    ntype = Type.floatType(loc);
                    break;
            }
            if (ntype != null) {
                type = ntype;
            }
        }
        
        if (type == null) {
            type = new Type(id);
        }

        //generic param
        if (curt == TokenKind.lt) {
            type.genericArgs = genericArgs();
        }

        // got it
        endLoc(type, loc);
        return type;
    }
    
    protected ArrayList<Type> genericArgs() {
        if (peek.whitespace) {
            err("Expected $<");
        }
        consume(TokenKind.dollar);
        consume(TokenKind.lt);
        ArrayList<Type> params = new ArrayList<Type>();
        while (true) {
            Type type1 = typeRef();
            params.add(type1);
            if (curt == TokenKind.gt) {
                break;
            }
            consume(TokenKind.comma);
        }
        consume(TokenKind.gt);
        return params;
    }

    /**
     ** Method type signature:
     **   <funcType> := "fun" "(" <args> ")" [<type>]
     */
    private Type funcType() {
        Loc loc = cur.loc;
        
        consume(TokenKind.funKeyword);
        
        FuncPrototype prototype = new FuncPrototype();
        funcPrototype(prototype);
       
        Type t = Type.funcType(loc, prototype);

        endLoc(t, loc);
        return t;
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
            throw err("Expected identifier, not '"+cur+"'");
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
            throw err("Expected '"+kind.symbol+"', not '"+cur+"'");
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
        node.loc = loc;
        
        Token preToken = (pos > 0) ? tokens.get(pos - 1) : cur;
        int end = preToken.loc.offset + preToken.len;
        int begin = loc.offset;
        int len = end - begin;
        
        if (len <= 0) {
            return;
        }
        node.len = len;
    }

    /**
     ** Statements can be terminated with a semicolon
     */
    protected void endOfStmt() {
        //if (cur.newline) return true;
        if (curt == TokenKind.semicolon) {
            consume();
            return;
        }
        
        String errMsg = "Expected end of statement with ';' not '" + cur + "'";
        err(errMsg);
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
