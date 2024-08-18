//
// Copyright (c) 2006, Brian Frank and Andy Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Sep 05  Brian Frank  Creation
//   29 Aug 06  Brian Frank  Ported from Java to Fan
//
package sc2.compiler.parser;

import java.util.ArrayList;
import sc2.compiler.ast.AstNode.*;
import sc2.compiler.CompilerLog;
import sc2.compiler.ast.Expr.*;
import sc2.compiler.ast.Stmt.*;
import sc2.compiler.ast.*;
import sc2.compiler.ast.Token.TokenKind;
import static sc2.compiler.ast.Token.TokenKind.*;

/**
 *
 * @author yangjiandong
 */
public class DeepParser extends Parser {

    public DeepParser(CompilerLog log, String code, FileUnit unit) {
        super(log, code, unit);
    }

//////////////////////////////////////////////////////////////////////////
// Block
//////////////////////////////////////////////////////////////////////////
    /**
     ** Top level for blocks which must be surrounded by braces
     * <block> =  <stmt> | ( "{" <stmts> "}" )
     */
    @Override
    Block block() {
        Block block = new Block();
        Loc loc = cur.loc;
        consume(TokenKind.lbrace);
        while (curt != TokenKind.rbrace) {
            block.stmts.add(stmt());
        }
        consume(TokenKind.rbrace);
        endLoc(block, loc);
        return block;
    }

    private Block stmtAsBlock() {
        Stmt st = stmt();
        
        if (st instanceof Block) {
            return (Block)st;
        }
        
        Block block = new Block();
        block.stmts.add(st);
        endLoc(block, st.loc);
        return block;
    }

//////////////////////////////////////////////////////////////////////////
// Statements
//////////////////////////////////////////////////////////////////////////
    /**
     ** Statement:
     **   <stmt> =  <break> | <continue> | <for> | <if> | <return> | <switch> |
     **               <throw> | <while> | <try> | <exprStmt> | <localDef> | <itAdd> | <block>
     */
    private Stmt stmt() {
        // check for statement keywords
        switch (curt) {
            case breakKeyword:
            case continueKeyword:
//            case fallthroughKeyword:
                return jumpStmt();
            case forKeyword:
                return forStmt();
            case ifKeyword:
                return ifStmt();
            case returnKeyword:
                return returnStmt();
//            case lretKeyword:
//                return returnStmt();
            case switchKeyword:
                return switchStmt();
//            case throwKeyword:
//                return throwStmt();
//            case tryKeyword:
//                return tryStmt();
            case whileKeyword:
                return whileStmt();
            case unsafeKeyword:
                return unsafeStmt();
            case lbrace:
                return block();
        }

        // at this point we either have an expr or local var declaration
        return exprOrLocalDefStmt();
    }

    /**
     ** Expression or local variable declaration:
     **   <exprStmt> =  <expr> <eos>
     **   <localDef> =  <id> ":" [<type>] ["=" <expr>] <eos>
     */
    private Stmt exprOrLocalDefStmt() {
        // see if this statement begins with a type literal
        Loc loc = curLoc();

        if (curt == TokenKind.varKeyword) {
            return localDefStmt(loc, null);
        }

        // otherwise assume it's a stand alone expression statement
        Expr e = expr();

        // return expression as statement
        ExprStmt stmt = new ExprStmt();
        stmt.expr = e;
        e.isStmt = true;
        
        endOfStmt();
        endLoc(stmt, loc);
        return stmt;
    }

    /**
     ** Parse local variable declaration, the current token must be * the
     * identifier of the local variable.
     */
    private LocalDefStmt localDefStmt(Loc loc, Type localType) {
//        boolean isConst = false;
        consume(TokenKind.varKeyword);
        
        // verify name doesn't conflict with an import type
        String name = consumeId();
        FieldDef stmt = new FieldDef(null, name);
//        if (isConst) {
//            stmt.flags = AstNode.Const;
//        }
        if (curt == TokenKind.colon) {
            consume();
            localType = typeRef();
            stmt.fieldType = localType;
            
            if (curt == TokenKind.assign) {
                consume();
                stmt.initExpr = expr();
            }
        }
        else {
            consume(TokenKind.assign);
            stmt.initExpr = expr();
        }

        endOfStmt();
        endLoc(stmt, loc);
        stmt.isLocalVar = true;
        
        LocalDefStmt s = new LocalDefStmt(stmt);
        endLoc(s, loc);
        return s;
    }
    
    private UnsafeBlock unsafeStmt() {
        Loc loc = cur.loc;
        consume(TokenKind.unsafeKeyword);

        UnsafeBlock stmt = new UnsafeBlock();
        stmt.block = block();

        endLoc(stmt, loc);
        return stmt;
    }

    /**
     ** If/else statement:
     **   <if> = "if" "(" <expr> ")" <block> [ "else" <block> ]
     */
    private IfStmt ifStmt() {
        Loc loc = cur.loc;
        consume(TokenKind.ifKeyword);
        consume(TokenKind.lparen);
        Expr cond = expr();
        consume(TokenKind.rparen);
        Block trueBlock = stmtAsBlock();
        IfStmt stmt = new IfStmt();
        stmt.loc = loc;
        stmt.condition = cond;
        stmt.block = trueBlock;
        if (curt == TokenKind.elseKeyword) {
            consume(TokenKind.elseKeyword);
            stmt.elseBlock = stmtAsBlock();
        }
        return stmt;
    }

    /**
     ** Return statement:
     **   <return> = "return" [<expr>] <eos>
     */
    private ReturnStmt returnStmt() {
        ReturnStmt stmt = new ReturnStmt();
        Loc loc = cur.loc;
        consume(TokenKind.returnKeyword);

        if (curt != TokenKind.semicolon) {
            stmt.expr = expr();
        }
        endOfStmt();
        endLoc(stmt, loc);
        return stmt;
    }

    /**
     ** Throw statement:
     **   <throw> = "throw" <expr> <eos>
     */
    private ThrowStmt throwStmt() {
        Loc loc = cur.loc;
        consume(TokenKind.throwKeyword);
        ThrowStmt stmt = new ThrowStmt();
        stmt.loc = loc;
        stmt.expr = expr();
        endOfStmt();
        endLoc(stmt, loc);
        return stmt;
    }

    /**
     ** While statement:
     **   <while> = "while" "(" <expr> ")" <block>
     */
    private WhileStmt whileStmt() {
        Loc loc = cur.loc;
        consume(TokenKind.whileKeyword);
        consume(TokenKind.lparen);
        Expr cond = expr();
        consume(TokenKind.rparen);
        WhileStmt stmt = new WhileStmt();
        stmt.loc = loc;
        stmt.condition = cond;
        stmt.block = stmtAsBlock();
        endLoc(stmt, loc);
        return stmt;
    }

    /**
     ** For statement:
     **   <for> = "for" "(" [<forInit>] ";" <expr> ";" <expr> ")" <block>
     **   <forInit> =  <expr> | <localDef>
     */
    private ForStmt forStmt() {
        ForStmt stmt = new ForStmt();
        Loc loc = cur.loc;
        consume(TokenKind.forKeyword);
        consume(TokenKind.lparen);

        if (curt != TokenKind.semicolon) {
            stmt.init = exprOrLocalDefStmt();
        }
        else {
            consume(TokenKind.semicolon);
        }
        
        if (curt != TokenKind.semicolon) {
            stmt.condition = expr();
        }
        consume(TokenKind.semicolon);

        if (curt != TokenKind.rparen) {
            stmt.update = expr();
        }
        consume(TokenKind.rparen);

        stmt.block = stmtAsBlock();
        endLoc(stmt, loc);
        return stmt;
    }

    /**
     ** Break statement:
     **   <jump> = ("break"|"continue"|"fallthrough") <eos>
     */
    private JumpStmt jumpStmt() {
        JumpStmt stmt = new JumpStmt();
        Loc loc = cur.loc;
        stmt.opToken = consume().kind;
        endOfStmt();
        endLoc(stmt, loc);
        return stmt;
    }

    /**
     ** Try-catch-finally statement:
     **   <try> = "try" "{" <stmt>* "}" <catch>* [<finally>]
     **   <catch> = "catch" [<catchDef>] "{" <stmt>* "}"
     **   <catchDef> = "(" <type> <id> ")"
     **   <finally> = "finally" "{" <stmt>* "}"
     */
    private TryStmt tryStmt() {
        TryStmt stmt = new TryStmt();
        Loc loc = cur.loc;
        consume(TokenKind.tryKeyword);
        stmt.block = stmtAsBlock();
//    if (curt != TokenKind.catchKeyword && curt != TokenKind.finallyKeyword);
//      throw err("Expecting catch or finally block");
        while (curt == TokenKind.catchKeyword) {
            stmt.catches.add(tryCatch());
        }
        if (curt == TokenKind.finallyKeyword) {
            consume();
            stmt.finallyBlock = stmtAsBlock();
        }
        endLoc(stmt, loc);
        return stmt;
    }

    private Catch tryCatch() {
        Catch c = new Catch();
        Loc loc = cur.loc;
        consume(TokenKind.catchKeyword);

        if (curt == TokenKind.lparen) {
            consume(TokenKind.lparen);
            Loc loc2 = curLoc();
            FieldDef errVariable = new FieldDef(null, consumeId());
            errVariable.fieldType = typeRef();
            c.errVariable = errVariable;
            endLoc(errVariable, loc2);
            
            consume(TokenKind.rparen);
        }

        c.block = stmtAsBlock();

        endLoc(c, loc);
        return c;
    }

    /**
     ** Switch statement:
     **   <switch> = "switch" "(" <expr> ")" "{" <case>* [<default>] "}"
     **   <case> = "case" <expr> ":" <stmts>
     **   <default> = "default" ":" <stmts>
     */
    private SwitchStmt switchStmt() {
        Loc loc = cur.loc;
        consume(TokenKind.switchKeyword);
        consume(TokenKind.lparen);
        SwitchStmt stmt = new SwitchStmt();
        stmt.loc = loc;
        stmt.condition = expr();
        consume(TokenKind.rparen);
        consume(TokenKind.lbrace);
        while (curt != TokenKind.rbrace) {
            if (curt == TokenKind.caseKeyword) {
                consume();
                CaseBlock c = new CaseBlock();
                Loc loc2 = cur.loc;
                c.caseExpr = expr();
                consume(TokenKind.colon);
                if (curt == TokenKind.fallthroughKeyword) {
                    c.fallthrough = true;
                    consume();
                    consume(TokenKind.semicolon);
                }
                c.block = switchBlock();
                endLoc(c, loc2);
                stmt.cases.add(c);
            } else if (curt == TokenKind.defaultKeyword) {
                if (stmt.defaultBlock != null) {
                    err("Duplicate default blocks");
                }
                consume();
                consume(TokenKind.colon);
                stmt.defaultBlock = switchBlock();
            } else {
                throw err("Expected case or default statement");
            }
        }
        consume(TokenKind.rbrace);
        //endOfStmt();
        endLoc(stmt, loc);
        return stmt;
    }

    private Block switchBlock() {
        Block block = new Block();
        Loc loc = cur.loc;
        while (curt != TokenKind.caseKeyword && curt != TokenKind.defaultKeyword &&
                curt != TokenKind.rbrace /*end of switch*/ &&
                curt != TokenKind.fallthroughKeyword) {
            block.stmts.add(stmt());
        }

        endLoc(block, loc);
        return block;
    }

//////////////////////////////////////////////////////////////////////////
// Expr
//////////////////////////////////////////////////////////////////////////
    /**
     ** Expression:
     **   <expr> =  <assignExpr>
     */
    @Override
    Expr expr() {
        return assignExpr();
    }

    /**
     ** Assignment expression:
     **   <assignExpr> =  <ifExpr> [<assignOp> <assignExpr>]
     **   <assignOp> = "=" | "*=" | "/=" | "%=" | "+=" | "-="
     */
    private Expr assignExpr() {
        Loc loc = curLoc();
        // this is tree if built to the right (others to the left)
        Expr expr = ifExpr();
        if (cur.isAssign()) {
            TokenKind tok = consume().kind;
            expr.inLeftSide = true;
            BinaryExpr e = new BinaryExpr(expr, tok, assignExpr());
            endLoc(e, loc);
            return e;
        }
        return expr;
    }

    /**
     ** Ternary/Elvis expressions:
     **   <ifExpr> =  <ternaryExpr> | <elvisExpr>
     **   <ternaryExpr> =  <condOrExpr> ["?" <ifExprBody> ":" <ifExprBody>]
     **   <elvisExpr> =  <condOrExpr> "?:" <ifExprBody>
     */
    private Expr ifExpr() {
        Loc loc = curLoc();
        Expr expr = condOrExpr();
        if (curt == TokenKind.question) {
            Expr condition = expr;
            consume(TokenKind.question);
            Expr trueExpr = condOrExpr();

            consume(TokenKind.colon);
            Expr falseExpr = condOrExpr();
            IfExpr ifExpr = new IfExpr();
            ifExpr.loc = loc;
            ifExpr.condition = condition;
            ifExpr.trueExpr = trueExpr;
            ifExpr.falseExpr = falseExpr;
            expr = ifExpr;
        }
//        else if (curt == TokenKind.elvis) {
//            Expr lhs = expr;
//            consume();
//            Expr rhs = condOrExpr();
//            BinaryExpr bexpr = new BinaryExpr(cur.loc, lhs, TokenKind.elvis, rhs);
//            expr = bexpr;
//        }

        endLoc(expr, loc);
        return expr;
    }

    /**
     ** Conditional or expression:
     **   <condOrExpr> =  <condAndExpr> ("||" <condAndExpr>)*
     */
    private Expr condOrExpr() {
        Loc loc = curLoc();
        Expr expr = condAndExpr();
        while (curt == TokenKind.doublePipe) {
            consume();
            Expr lhs = expr;
            Expr rhs = condAndExpr();

            BinaryExpr cond = new BinaryExpr(lhs, TokenKind.doublePipe, rhs);
            endLoc(cond, loc);
            expr = cond;
        }
        return expr;
    }

    /**
     ** Conditional and expression:
     **   <condAndExpr> =  <equalityExpr> ("&&" <equalityExpr>)*
     */
    private Expr condAndExpr() {
        Loc loc = curLoc();
        Expr expr = bitOrExpr();
        while (curt == TokenKind.doubleAmp) {
            consume();
            Expr lhs = expr;
            Expr rhs = bitOrExpr();

            BinaryExpr cond = new BinaryExpr(lhs, TokenKind.doubleAmp, rhs);
            endLoc(cond, loc);
            expr = cond;
        }
        return expr;
    }
    
    private Expr bitOrExpr() {
        Loc loc = curLoc();
        Expr expr = bitXorExpr();
        while (curt == TokenKind.pipe) {
            consume();
            Expr lhs = expr;
            Expr rhs = bitXorExpr();

            BinaryExpr cond = new BinaryExpr(lhs, TokenKind.pipe, rhs);
            endLoc(cond, loc);
            expr = cond;
        }
        return expr;
    }
    
    private Expr bitXorExpr() {
        Loc loc = curLoc();
        Expr expr = bitAndExpr();
        while (curt == TokenKind.caret) {
            consume();
            Expr lhs = expr;
            Expr rhs = bitAndExpr();

            BinaryExpr cond = new BinaryExpr(lhs, TokenKind.caret, rhs);
            endLoc(cond, loc);
            expr = cond;
        }
        return expr;
    }
    
    private Expr bitAndExpr() {
        Loc loc = curLoc();
        Expr expr = equalityExpr();
        while (curt == TokenKind.amp) {
            consume();
            Expr lhs = expr;
            Expr rhs = equalityExpr();

            BinaryExpr cond = new BinaryExpr(lhs, TokenKind.amp, rhs);
            endLoc(cond, loc);
            expr = cond;
        }
        return expr;
    }

    /**
     ** Equality expression:
     **   <equalityExpr> =  <relationalExpr> [("==" | "!=" | "==" | "!=") <relationalExpr>]
     */
    private Expr equalityExpr() {
        Loc loc = curLoc();
        Expr expr = relationalExpr();
        if (curt == TokenKind.eq || curt == TokenKind.notEq
                || curt == TokenKind.same || curt == TokenKind.notSame) {
            Expr lhs = expr;
            TokenKind tok = consume().kind;
            Expr rhs = relationalExpr();

            BinaryExpr bexpr = new BinaryExpr(lhs, tok, rhs);
            expr = bexpr;
            endLoc(expr, loc);
        }
        return expr;
    }

    /**
     ** Relational expression:
     **   <relationalExpr> =  <typeCheckExpr> | <compareExpr>
     **   <typeCheckExpr> =  [("is" | "as") <type>]
     **   <compareExpr> =  [("<" | "<=" | ">" | ">=" | "<=>")
     */
    private Expr relationalExpr() {
        Loc loc = curLoc();
        Expr expr = bitShiftExpr();
        
        if (curt == TokenKind.isKeyword
                || curt == TokenKind.asKeyword) {
            expr = new BinaryExpr(expr, consume().kind, typeExpr());
            endLoc(expr, loc);
        }
        else if (curt == TokenKind.lt || curt == TokenKind.ltEq
                || curt == TokenKind.gt || curt == TokenKind.gtEq
                //|| curt == TokenKind.cmp
                ) {
            
            //not >> or <<
            if (peekt != curt) {
                expr = new BinaryExpr(expr, consume().kind, bitShiftExpr());
                endLoc(expr, loc);
            }
        }
        return expr;
    }
    
    private Expr bitShiftExpr() {
        Loc loc = curLoc();
        Expr expr = addExpr();
        if (curt == TokenKind.lt && !peek.whitespace && peekt == TokenKind.lt) {
            consume();
            consume();
            expr = new BinaryExpr(expr, TokenKind.leftShift, addExpr());
            endLoc(expr, loc);
        }
        if (curt == TokenKind.gt && !peek.whitespace && peekt == TokenKind.gt) {
            consume();
            consume();
            expr = new BinaryExpr(expr, TokenKind.rightShift, addExpr());
            endLoc(expr, loc);
        }
        return expr;
    }

    /**
     ** Additive expression:
     **   <addExpr> =  <multExpr> (("+" | "-") <multExpr>)*
     */
    private Expr addExpr() {
        Loc loc = curLoc();
        Expr expr = multExpr();
        while (curt == TokenKind.plus || curt == TokenKind.minus) {
            expr = new BinaryExpr(expr, consume().kind, multExpr());
            endLoc(expr, loc);
        }
        return expr;
    }

    /**
     ** Multiplicative expression:
     **   <multExpr> =  <unaryExpr> (("*" | "/" | "%") <unaryExpr>)*
     */
    private Expr multExpr() {
        Loc loc = curLoc();
        Expr expr = unaryExpr();
        while (curt == TokenKind.star || curt == TokenKind.slash || curt == TokenKind.percent) {
            expr = new BinaryExpr(expr, consume().kind, unaryExpr());
            endLoc(expr, loc);
        }
        return expr;
    }


    /**
     ** Unary expression:
     **   <unaryExpr> =  <prefixExpr> | <termExpr>
     **   <prefixExpr> = ("!" | "+" | "-" | "~" | "++" | "--" | "~" | "*" | "&" ) <termExpr>
     */
    private Expr unaryExpr() {
        Loc loc = cur.loc;
        switch (curt) {
            case bang:
            case minus:
            case increment:
            case decrement:
            case tilde:
            case star:
            case amp:
            {
                TokenKind tokt = curt;
                consume();
                UnaryExpr e = new UnaryExpr(tokt, termExpr());
                endLoc(e, loc);
                return e;
            }
            case plus: {
                consume();
                return termExpr(); // optimize +expr to just expr
            }
        }

        Expr expr = termExpr();

        return expr;
    }

//////////////////////////////////////////////////////////////////////////
// Term Expr
//////////////////////////////////////////////////////////////////////////

    /**
     ** A term is a base terminal such as a variable, call, or literal,
     ** optionally followed by a chain of accessor expressions - such
     ** as "x.y[z](a, b)". 
     **   <termExpr> =  <optinalPrimaryExpr> <optinalTermChain>*
     *    <optinalPrimaryExpr> = <primaryExpr> ["!"]
     *    <optinalTermChain> = <termChain> ["!"]
     **/
    private Expr termExpr() {
        Expr target = primaryExpr();
        if (curt == TokenKind.bang) {
            consume();
            Loc loc = target.loc;
            target = new OptionalExpr(target);
            endLoc(target, loc);
        }
        while (curt != TokenKind.semicolon) {
            Expr chained = termChainExpr(target);
            if (chained == null) {
                break;
            }
            target = chained;
            if (curt == TokenKind.bang) {
                consume();
                Loc loc = target.loc;
                target = new OptionalExpr(target);
                endLoc(target, loc);
            }
        }
        return target;
    }

    /**
    **
    ** A chain expression is a piece of a term expression that may
    ** be chained together such as "call.var[x]".  If the specified
    ** target expression contains a chained access, then return the new
    ** expression, otherwise return null.
    **
    **   <termChain>      :=  <accessExpr> | <indexExpr> | <callOp>
    **
    **/
    private Expr termChainExpr(Expr target) {

        // handle various call operators: . -> ~>
        switch (curt) {
            case dot:
//            case arrow:
//            case tildeArrow:
                return accessExpr(target);
        }
        
        // target$<...>
        if (cur.kind == TokenKind.dollar && peekt == TokenKind.lt) {
            GenericInstance gi = new GenericInstance();
            gi.target = target;
            gi.genericArgs = genericArgs();
            endLoc(gi, target.loc);
            return gi;
        }

        // target[...]
        if (cur.kind == TokenKind.lbracket && !cur.newline) {
            return indexExpr(target);
        }

        // target(...)
        if (cur.kind == TokenKind.lparen && !cur.newline) {
            return callExpr(target);
        }
        
        // target{...}
        if (cur.kind == TokenKind.lbrace) {
            return initBlockExpr(target);
        }

        // otherwise the expression should be finished
        return null;
    }
    
    /**
     ** Identifier expression:
     **   <accessExpr> = ("." | "->" | "~>") <id>
     */
    private Expr accessExpr(Expr target) {
        Loc loc = target.loc;
        TokenKind token = consume().kind;
        String name = consumeId();

        // at this point we are parsing a single identifier, but
        // if it looks like it was expected to be a type we can
        // provide a more meaningful error
        if (curt == TokenKind.pound) {
            throw err("Unknown type '"+name+"' for type literal" + loc);
        }

        AccessExpr expr = new AccessExpr();
        expr.opToken = token;
        expr.name = name;
        expr.target = target;
        endLoc(expr, loc);
        return expr;
    }

    /**
     ** Call expression:
     **   <callOp> =  "(" <args> ")"
     */
    private CallExpr callExpr(Expr target) {
        Loc loc = target.loc;
        
        consume(TokenKind.lparen);
        
        CallExpr call = new CallExpr();
        call.loc = loc;
        call.target = target;
        call.args = callArgs(TokenKind.rparen);
        
        consume(TokenKind.rparen);
        endLoc(call, loc);
        return call;
    }

    /**
     ** Parse args with known parens:
     **   <args> = <arg> ("," <arg>)*
     **   <arg> = [<id> "="] <expr>
     */
    private ArrayList<CallArg> callArgs(TokenKind right) {
        if (curt != right) {
            ArrayList<CallArg> args = new ArrayList<CallArg>();
            while (true) {
                Loc loc = curLoc();
                CallArg arg = new CallArg();

                //named param
                if (curt == TokenKind.identifier && peekt == TokenKind.assign) {
                    String name = consumeId();
                    consume();
                    arg.name = name;
                }
                arg.argExpr = expr();
                endLoc(arg, loc);
                args.add(arg);
                if (curt == right) {
                    break;
                }
                consume(TokenKind.comma);
            }
            return args;
        }
        return null;
//        if (closureOk) {
//            Expr closure = closureExpr();
//            if (closure != null) {
//                ArgExpr arg = new ArgExpr();
//                arg.argExpr = closure;
//                call.args.add(arg);
//            }
//        }
    }

    /**
     ** Index expression:
     **   <indexExpr> = "[" <expr> "]"
     */
    private Expr indexExpr(Expr target) {
        Loc loc = target.loc;
        consume(TokenKind.lbracket);

        // otherwise this must be a standard single key index
        Expr expr = expr();
        consume(TokenKind.rbracket);
        
        IndexExpr e = new IndexExpr();
        e.loc = loc;
        e.target = target;
        e.index = expr;
        endLoc(e, loc);
        return e;
    }

//////////////////////////////////////////////////////////////////////////
// primary Expr
//////////////////////////////////////////////////////////////////////////
    
    /**
     ** Paren grouped expression:
     **   <typeExpr> =  <type>
     */
    private TypeExpr typeExpr() {
        Loc loc = curLoc();
        //consume(TokenKind.colon);
        Type type = typeRef();
        TypeExpr expr = new TypeExpr(type);
        endLoc(expr, loc);
        return expr;
    }
    
    /**
     ** Paren grouped expression:
     **   <parenExpr> =  "(" <expr> ")"
     */
    private Expr parenExpr() {
        // consume() opening paren (or synthetic paren)
        consume(TokenKind.lparen);

        // this is just a normal parenthesized expression
        Expr expr = expr();
        consume(TokenKind.rparen);

        return expr;
    }
    
    private Expr sizeofExpr() {
        Loc loc = cur.loc;
        //TokenKind tokt = curt;
        consume(TokenKind.sizeofKeyword);
        consume(TokenKind.lparen);
        
        CallExpr call = new CallExpr();
        call.target = new IdExpr(TokenKind.sizeofKeyword.symbol);
        call.args.add(new CallArg(typeExpr()));
        
        consume(TokenKind.rparen);
        endLoc(call, loc);
        return call;
    }
    
    private Expr offsetofExpr() {
        Loc loc = curLoc();
        consume(TokenKind.offsetofKeyword);
        
        CallExpr call = new CallExpr();
        call.loc = loc;
        call.target = new IdExpr(TokenKind.offsetofKeyword.symbol);
        
        consume(TokenKind.lparen);
        call.args.add(new CallArg(typeExpr()));
        call.args.add(new CallArg(idExpr()));
        consume(TokenKind.rparen);
        
        endLoc(call, loc);
        return call;
    }

    /**
     ** Atomic base of a termExpr 
     **   <primaryExpr> =  <literal> | <idExpr> | <closure> | <typeExpr> | <slotExpr>
     **   <literal> = "null" | "this" | "super" | <bool> | <int> | <float> | <str> | <array>
     */
    private Expr primaryExpr() {
        Loc loc = cur.loc;
        Expr expr = null;
        switch (curt) {
            case lparen:
                expr = parenExpr();
                break;
            case identifier:
                expr = idExpr();
                break;
            case intLiteral:
                expr = new LiteralExpr(consume().val);
                break;
            case floatLiteral:
                expr = new LiteralExpr(consume().val);
                break;
            case strLiteral:
                expr = new LiteralExpr(consume().val);
                break;
            case trueKeyword:
                consume();
                expr = new LiteralExpr(true);
                break;
            case falseKeyword:
                consume();
                expr = new LiteralExpr(false);
                break;
            case nullKeyword:
                consume();
                expr = new LiteralExpr(null);
                break;
            case superKeyword:
            case thisKeyword:
            case itKeyword: 
            {
                TokenKind tok = consume().kind;
                expr = new IdExpr(tok.symbol);
                break;
            }
            case sizeofKeyword:
                expr = sizeofExpr();
                break;
            case offsetofKeyword:
                expr = offsetofExpr();
                break;
            case awaitKeyword: {
                consume();
                expr = new UnaryExpr(TokenKind.awaitKeyword, expr());
                break;
            }
            case lbracket:
                if (peekt == rbracket) {
                    expr = typeExpr();
                }
                break;
            case funKeyword:
                expr = closureExpr();
                break;
        }
        
        if (expr != null) {
            endLoc(expr, loc);
            return expr;
        }

        if (cur.kind.keyword) {
            throw err("Expected expression, not keyword '" + cur + "'");
        } else {
            throw err("Expected expression, not '" + cur + "'");
        }
    }

//////////////////////////////////////////////////////////////////////////
// Init Block Expr
//////////////////////////////////////////////////////////////////////////

    /**
     ** Init Block Expr:
     **   <initBlockExpr> = "{" <args> "}"
     */
    private Expr initBlockExpr(Expr target) {
        consume(TokenKind.lbrace);
        
        InitBlockExpr expr = new InitBlockExpr();
        expr.target = target;
        expr.args = callArgs(TokenKind.rbrace);

        consume(TokenKind.rbrace);

        endLoc(expr, target.loc);
        return expr;
    }

//////////////////////////////////////////////////////////////////////////
// Closure
//////////////////////////////////////////////////////////////////////////
    /**
     ** Parse body of closure expression and return ClosureExpr.
     */
    private ClosureExpr closureExpr() {
        ClosureExpr closure = new ClosureExpr();
        Loc loc = curLoc();
        
        //consume(TokenKind.lbracket);
//        if (curt != TokenKind.rbracket) {
//            while (true) {
//                if (closure.captures == null) {
//                    closure.captures = new ArrayList<Expr>();
//                }
//                if (curt == TokenKind.assign || curt == TokenKind.amp) {
//                    if (peekt == TokenKind.comma || peekt == TokenKind.rbracket) {
//                        closure.defaultCapture = curt;
//                        consume();
//                        continue;
//                    }
//                }
//                Expr id = expr();
//                closure.captures.add(id);
//                if (curt != TokenKind.comma) {
//                    break;
//                }
//                consume();
//            }
//        }
        //consume(TokenKind.rbracket);
        
        consume(TokenKind.funKeyword);
        
        funcPrototype(closure.prototype);

        closure.code = block();
        
        endLoc(closure, loc);
        return closure;
    }

}
