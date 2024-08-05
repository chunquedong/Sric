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
        
        while (curt == TokenKind.typealiasKeyword) {
            parseTypeAlias();
        }
    }
    
    private void parseTypeAlias() {
        Loc loc = curLoc();
        consume(TokenKind.typealiasKeyword);
        TypeAlias u = new TypeAlias();
        u.asName = consumeId();
        
        consume(TokenKind.assign);
        u.type = typeRef();

        endOfStmt();
        endLoc(u, loc);
        unit.addDef(u);
    }

    private void parseImports() {
        Loc loc = curLoc();
        consume(TokenKind.importKeyword);
        Import u = new Import();
        u.name = idExpr();
        if (curt == TokenKind.doubleColon && peekt == TokenKind.star) {
            u.star = true;
        }

        endOfStmt();
        endLoc(u, loc);
        unit.addDef(u);
    }

    private AstNode topLevelDef() {
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
            case constKeyword:
            {
                if (curt == TokenKind.constKeyword) {
                    flags |= AstNode.Const;
                }
                consume();
                String sname = consumeId();
                return fieldDef(loc, doc, flags, null, sname);
            }
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
            case constKeyword:
            {
                if (curt == TokenKind.constKeyword) {
                    flags |= AstNode.Const;
                }
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
        while (curt == TokenKind.doubleColon) {
            //a::* for import
            if (peekt == TokenKind.star) {
                break;
            }
            if (e.namespace != null) {
                e.namespace += "::" + id;
            }
            else {
                e.namespace = id;
            }
            consume();
            id = consumeId();
        }
        e.name = id;
        
        endLoc(e, loc);
        return e;
    }

//////////////////////////////////////////////////////////////////////////
// TypeDef
//////////////////////////////////////////////////////////////////////////
    
    private ArrayList<GeneriParamDef> tryGenericParamDef() {
        if (curt == TokenKind.dollar && !cur.whitespace && peekt == TokenKind.lt) {
            consume();
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
            
            //GenericType Param
            structDef.generiParamDefs = tryGenericParamDef();
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
    
    private int funcPostFlags() {
        int flags = 0;
        for (boolean done = false; !done;) {
            int oldFlags = flags;
            switch (curt) {
                case constKeyword:
                    flags = flags | (AstNode.Const);
                    break;
                case mutKeyword:
                    flags = flags | (AstNode.Mutable);
                    break;
                case throwsKeyword:
                    flags = flags | (AstNode.Throws);
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
        method.generiParams = tryGenericParamDef();
        
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
    }

    private ParamDef paramDef() {
        Loc loc = curLoc();

        ParamDef param = new ParamDef();
        param.name = consumeId();
        
        consume(TokenKind.colon);
        
        if (curt == TokenKind.dotDotDot) {
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
     **   <type> :=  <simpleType> | <pointerType> | <funcType> | <arrayType> | <arrayRefType> | <constType>
     */
    protected Type typeRef() {
        return pointerType();
    }
    
    private Type pointerType() {
        Loc loc = curLoc();
        Type.PointerAttr pointerAttr = null;
        if (null != curt) switch (curt) {
            case ownKeyword:
                consume();
                pointerAttr = Type.PointerAttr.own;
                break;
            case refKeyword:
                consume();
                pointerAttr = Type.PointerAttr.ref;
                break;
            case weakKeyword:
                consume();
                pointerAttr = Type.PointerAttr.weak;
                break;
            case rawKeyword:
                consume();
                pointerAttr = Type.PointerAttr.weak;
                break;
            default:
                break;
        }
        
        if (pointerAttr != null) {
            Type type = arrayType();
            consume(TokenKind.star);
            type = Type.pointerType(loc, type, pointerAttr);

            // check for ? nullable
            if (curt == TokenKind.question) {
                consume(TokenKind.question);
                type.isNullable = true;
                if (curt == TokenKind.question) {
                    err("Type cannot have multiple '?'");
                }
            }
            return type;
        }
        else {
            Type type = arrayType();
            if (curt == TokenKind.star) {
                consume();
                type = Type.pointerType(loc, type, pointerAttr);

                // check for ? nullable
                if (curt == TokenKind.question) {
                    consume(TokenKind.question);
                    type.isNullable = true;
                    if (curt == TokenKind.question) {
                        err("Type cannot have multiple '?'");
                    }
                }
            }
            return type;
        }
    }
    
    private Type arrayType() {
        Loc loc = curLoc();
        Type type = imutableType();
        // trailing [] for array
        if (curt == TokenKind.lbracket) {
            consume(TokenKind.lbracket);
            if (curt == TokenKind.rbracket) {
                type = Type.arrayRefType(loc, type);
            }
            else if (curt == TokenKind.intLiteral) {
                type = Type.arrayType(loc, type, (Integer)cur.val);
            }
            else {
                err("Array size must int literal");
                expr();
            }
            consume(TokenKind.rbracket);
            endLoc(type, loc);
        }
        return type;
    }
    
    private Type imutableType() {
        Type.ImutableAttr imutable = Type.ImutableAttr.auto;
        if (curt == TokenKind.constKeyword) {
            consume();
            imutable = Type.ImutableAttr.imu;
        }
        else if (curt == TokenKind.mutKeyword) {
            consume();
            imutable = Type.ImutableAttr.mut;
        }
        
        Type type = funcOrSimpleType();
        type.imutable = imutable;
        return type;
    }

    private Type funcOrSimpleType() {
        if (curt == TokenKind.lbracket) {
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
            type.genericArgs = genericArgs();
        }

        // got it
        endLoc(type, loc);
        return type;
    }
    
    protected ArrayList<Type> genericArgs() {
        if (cur.whitespace) {
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
     **   <funcType> := "[]" "(" <args> ")" [<type>]
     */
    private Type funcType() {
        Loc loc = cur.loc;
        
        consume(TokenKind.lbracket);
        consume(TokenKind.rbracket);
        
        FuncPrototype prototype = new FuncPrototype();
        funcPrototype(prototype);
       
        Type t = Type.funcType(loc, prototype);

        endLoc(t, loc);
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
