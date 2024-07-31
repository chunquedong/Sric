//
// Copyright (c) 2006, Brian Frank and Andy Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   15 Sep 05  Brian Frank  Creation
//   29 Aug 06  Brian Frank  Ported from Java to Fan
//
package sc2.compiler.parser;

import sc2.compiler.ast.AstNode.*;
import sc2.compiler.CompilerLog;
import sc2.compiler.ast.Expr;
import sc2.compiler.ast.Expr.*;
import sc2.compiler.ast.Loc;
import sc2.compiler.ast.Stmt;
import sc2.compiler.ast.Stmt.*;
import sc2.compiler.ast.Token;
import sc2.compiler.ast.Token.TokenKind;
import sc2.compiler.ast.Type;
import sc2.compiler.ast.ClosureExpr;

/**
 *
 * @author yangjiandong
 */
public class DeepParser extends Parser {

    public DeepParser(CompilerLog log, String code, FileUnit unit) {

    }

    //////////////////////////////////////////////////////////////////////////
// Block
//////////////////////////////////////////////////////////////////////////
    /**
     ** Top level for blocks which must be surrounded by braces
  *
     */
    @Override
    Block block() {
        verify(TokenKind.lbrace);
        return stmtOrBlock();
    }

    /**
     ** <block> =  <stmt> | ( "{" <stmts> "}" )
     ** <stmts> =  <stmt>*
  *
     */
    private Block stmtOrBlock() {
        Block block = new Block();
        block.loc = cur.loc;

        if (curt != TokenKind.lbrace) {
            block.stmts.add(stmt());
        } else {
            consume(TokenKind.lbrace);
            while (curt != TokenKind.rbrace) {
                block.stmts.add(stmt());
            }
            consume(TokenKind.rbrace);
        }

        endLoc(block);
        return block;
    }

//////////////////////////////////////////////////////////////////////////
// Statements
//////////////////////////////////////////////////////////////////////////
    /**
     ** Statement:
     **   <stmt> =  <break> | <continue> | <for> | <if> | <return> | <switch> |
     **               <throw> | <while> | <try> | <exprStmt> | <localDef> | <itAdd>
  *
     */
    private Stmt stmt() {
        // check for statement keywords
        switch (curt) {
            case breakKeyword:
                return breakStmt();
            case continueKeyword:
                return continueStmt();
            case forKeyword:
                return forStmt();
            case ifKeyword:
                return ifStmt();
            case returnKeyword:
                return returnStmt();
            case lretKeyword:
                return returnStmt();
            case switchKeyword:
                return switchStmt();
            case throwKeyword:
                return throwStmt();
            case tryKeyword:
                return tryStmt();
            case whileKeyword:
                return whileStmt();
        }

        // at this point we either have an expr or local var declaration
        return exprOrLocalDefStmt(true);
    }

    /**
     ** Expression or local variable declaration:
     **   <exprStmt> =  <expr> <eos>
     **   <localDef> = [<type>] <id> ["=" <expr>] <eos>
     **   <itAdd> =  <expr> ("," <expr>)*
  *
     */
    private Stmt exprOrLocalDefStmt(boolean isEndOfStmt) {
        // see if this statement begins with a type literal
        Loc loc = cur.loc;
        int mark = pos;
        Type localType = typeRef();

        // type followed by identifier must be local variable declaration
        if (localType != null) {
            if (curt == TokenKind.identifier) {
                return localDefStmt(loc, localType, isEndOfStmt);
            }
            if (curt == TokenKind.defAssign) {
                throw err("Expected local variable identifier");
            }
        }
        reset(mark);

        // identifier followed by def assign is inferred typed local var declaration
        if (curt == TokenKind.identifier && peekt == TokenKind.defAssign) {
            return localDefStmt(loc, null, isEndOfStmt);
        }

        // if current is an identifer, save for special error handling
        String id = (curt == TokenKind.identifier) ? (String) cur.val : null;

        // otherwise assume it's a stand alone expression statement
        Expr e = expr();

        // if expression statement ends with comma then this
        // is syntax sugar for it.add(expr) ...
        //if (curt == TokenKind.comma) e = itAdd(e)
        // return expression as statement
        ExprStmt stmt = new ExprStmt();
        stmt.expr = e;
        endLoc(stmt);
        if (!isEndOfStmt) {
            return stmt;
        }
        if (endOfStmt(null)) {
            return stmt;
        }

        // report error
        if (id != null && curt == TokenKind.identifier && (peekt == TokenKind.defAssign || peekt == TokenKind.assign)) {
            throw err("Unknown type '$id' for local declaration");
        } else if (id == null && curt == TokenKind.defAssign) {
            throw err("Left hand side of '=' must be identifier");
        } else {
            throw err("Expected expression statement");
        }
    }

    /**
     ** Comma operator is sugar for it.add(target):
     **   <itAdd> =  <expr> ("," <expr>)* <eos>
  *
     */
//  private Expr itAdd(Expr e)
//  {
//    e = CallExpr(e.loc, ItExpr(cur.loc), "add") { args.add(e); isItAdd = true }
//    while (true)
//    {
//      consume(TokenKind.comma)
//      if (curt == TokenKind.rbrace || curt == TokenKind.semicolon) break
//      e = CallExpr(cur.loc, e, "add") { args.add(expr()) }
//      if (curt == TokenKind.rbrace || curt == TokenKind.semicolon) break
//    }
//    endLoc(e)
//    return e
//  }
    /**
     ** Parse local variable declaration, the current token must be * the
     * identifier of the local variable.
  *
     */
    private FieldDef localDefStmt(Loc loc, Type localType, boolean isEndOfStmt) {
        // verify name doesn't conflict with an import type
        String name = consumeId();
        FieldDef stmt = new FieldDef(loc, null, name);
        stmt.fieldType = localType;

        if (curt == TokenKind.assign) {
            //if (curt == TokenKind.assign) err("Must use = for declaration assignments")
            consume();
            stmt.initExpr = expr();
        }

        if (isEndOfStmt) {
            endOfStmt();
        }
        endLoc(stmt);
        return stmt;
    }

    /**
     ** If/else statement:
     **   <if> = "if" "(" <expr> ")" <block> [ "else" <block> ]
  *
     */
    private IfStmt ifStmt() {
        Loc loc = cur.loc;
        consume(TokenKind.ifKeyword);
        consume(TokenKind.lparen);
        Expr cond = expr();
        consume(TokenKind.rparen);
        Block trueBlock = stmtOrBlock();
        IfStmt stmt = new IfStmt();
        stmt.loc = loc;
        stmt.condition = cond;
        stmt.block = trueBlock;
        if (curt == TokenKind.elseKeyword) {
            consume(TokenKind.elseKeyword);
            stmt.elseBlock = stmtOrBlock();
        }
        return stmt;
    }

    /**
     ** Return statement:
     **   <return> = "return" [<expr>] <eos>
  *
     */
    private ReturnStmt returnStmt() {
        ReturnStmt stmt = new ReturnStmt();
        stmt.loc = cur.loc;
        if (curt == TokenKind.lretKeyword) {
            consume(TokenKind.lretKeyword);
            stmt.isLocal = true;
        } else {
            consume(TokenKind.returnKeyword);
        }

        if (!endOfStmt(null)) {
            stmt.expr = expr();
            endOfStmt();
        }
        endLoc(stmt);
        return stmt;
    }

    /**
     ** Throw statement:
     **   <throw> = "throw" <expr> <eos>
  *
     */
    private ThrowStmt throwStmt() {
        Loc loc = cur.loc;
        consume(TokenKind.throwKeyword);
        ThrowStmt stmt = new ThrowStmt();
        stmt.loc = loc;
        stmt.expr = expr();
        endOfStmt();
        endLoc(stmt);
        return stmt;
    }

    /**
     ** While statement:
     **   <while> = "while" "(" <expr> ")" <block>
  *
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
        stmt.block = stmtOrBlock();
        endLoc(stmt);
        return stmt;
    }

    /**
     ** For statement:
     **   <for> = "for" "(" [<forInit>] ";" <expr> ";" <expr> ")" <block>
     **   <forInit> =  <expr> | <localDef>
  *
     */
    private ForStmt forStmt() {
        ForStmt stmt = new ForStmt();
        stmt.loc = cur.loc;
        consume(TokenKind.forKeyword);
        consume(TokenKind.lparen);

        if (curt != TokenKind.semicolon) {
            stmt.init = exprOrLocalDefStmt(false);
        }
        consume(TokenKind.semicolon);

        if (curt != TokenKind.semicolon) {
            stmt.condition = expr();
        }
        consume(TokenKind.semicolon);

        if (curt != TokenKind.rparen) {
            stmt.update = expr();
        }
        consume(TokenKind.rparen);

        stmt.block = stmtOrBlock();
        endLoc(stmt);
        return stmt;
    }

    /**
     ** Break statement:
     **   <break> = "break" <eos>
  *
     */
    private JumpStmt breakStmt() {
        JumpStmt stmt = new JumpStmt();
        stmt.loc = cur.loc;
        consume(TokenKind.breakKeyword);
        endOfStmt();
        endLoc(stmt);
        return stmt;
    }

    /**
     ** Continue statement:
     **   <continue> = "continue" <eos>
  *
     */
    private JumpStmt continueStmt() {
        JumpStmt stmt = new JumpStmt();
        stmt.loc = cur.loc;
        consume(TokenKind.continueKeyword);
        endOfStmt();
        endLoc(stmt);
        return stmt;
    }

    /**
     ** Try-catch-finally statement:
     **   <try> = "try" "{" <stmt>* "}" <catch>* [<finally>]
     **   <catch> = "catch" [<catchDef>] "{" <stmt>* "}"
     **   <catchDef> = "(" <type> <id> ")"
     **   <finally> = "finally" "{" <stmt>* "}"
  *
     */
    private TryStmt tryStmt() {
        TryStmt stmt = new TryStmt();
        stmt.loc = cur.loc;
        consume(TokenKind.tryKeyword);
        stmt.block = stmtOrBlock();
//    if (curt != TokenKind.catchKeyword && curt != TokenKind.finallyKeyword);
//      throw err("Expecting catch or finally block");
        while (curt == TokenKind.catchKeyword) {
            stmt.catches.add(tryCatch());
        }
        if (curt == TokenKind.finallyKeyword) {
            consume();
            stmt.finallyBlock = stmtOrBlock();
        }
        endLoc(stmt);
        return stmt;
    }

    private Catch tryCatch() {
        Catch c = new Catch();
        c.loc = cur.loc;
        consume(TokenKind.catchKeyword);

        if (curt == TokenKind.lparen) {
            consume(TokenKind.lparen);

            FieldDef errVariable = new FieldDef(curLoc(), null, consumeId());
            errVariable.fieldType = typeRef();
            c.errVariable = errVariable;
            consume(TokenKind.rparen);
        }

        c.block = stmtOrBlock();

        endLoc(c);
        return c;
    }

    /**
     ** Switch statement:
     **   <switch> = "switch" "(" <expr> ")" "{" <case>* [<default>] "}"
     **   <case> = "case" <expr> ":" <stmts>
     **   <default> = "default" ":" <stmts>
  *
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
                CaseBlock c = new CaseBlock();
                c.loc = cur.loc;
                while (curt == TokenKind.caseKeyword) {
                    consume();
                    c.cases.add(expr());
                    consume(TokenKind.colon);
                }
                if (curt != TokenKind.defaultKeyword) // optimize away case fall-thru to default
                {
                    c.block = switchBlock();
                    endLoc(c);
                    stmt.cases.add(c);
                }
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
        endOfStmt();
        endLoc(stmt);
        return stmt;
    }

    private Block switchBlock() {
        Block block = new Block();
        block.loc = cur.loc;
        while (curt != TokenKind.caseKeyword && curt != TokenKind.defaultKeyword && curt != TokenKind.rbrace) {
            block.stmts.add(stmt());
        }

        endLoc(block);
        return block;
    }

//////////////////////////////////////////////////////////////////////////
// Expr
//////////////////////////////////////////////////////////////////////////
    /**
     ** Expression:
     **   <expr> =  <assignExpr>
  *
     */
    @Override
    Expr expr() {
        return assignExpr();
    }

    /**
     ** Assignment expression:
     **   <assignExpr> =  <ifExpr> [<assignOp> <assignExpr>]
     **   <assignOp> = "=" | "*=" | "/=" | "%=" | "+=" | "-="
  *
     */
    private Expr assignExpr() {
        return assignExpr(null);
    }

    private Expr assignExpr(Expr expr) {
        // this is tree if built to the right (others to the left)
        if (expr == null) {
            expr = ifExpr();
        }
        if (cur.isAssign()) {
            TokenKind tok = consume().kind;
            BinaryExpr e = new BinaryExpr(cur.loc, expr, tok, assignExpr());
            endLoc(e);
            return e;
        }
        return expr();
    }

    /**
     ** Ternary/Elvis expressions:
     **   <ifExpr> =  <ternaryExpr> | <elvisExpr>
     **   <ternaryExpr> =  <condOrExpr> ["?" <ifExprBody> ":" <ifExprBody>]
     **   <elvisExpr> =  <condOrExpr> "?:" <ifExprBody>
  *
     */
    private Expr ifExpr() {
        Expr expr = condOrExpr();
        if (curt == TokenKind.question) {
            Expr condition = expr;
            consume(TokenKind.question);
            Expr trueExpr = condOrExpr();
            // nice error checking for Foo? x =
            if (curt == TokenKind.defAssign && expr.id == ExprId.unknownVar && trueExpr.id == ExprId.unknownVar) {
                throw err("Unknown type '$expr' for local declaration");
            }
            consume(TokenKind.colon);
            Expr falseExpr = condOrExpr();
            IfExpr ifExpr = new IfExpr();
            ifExpr.loc = cur.loc;
            ifExpr.condition = condition;
            ifExpr.trueExpr = trueExpr;
            ifExpr.falseExpr = falseExpr;
            expr = ifExpr;
        } else if (curt == TokenKind.elvis) {
            Expr lhs = expr();
            consume();
            Expr rhs = condOrExpr();
            BinaryExpr bexpr = new BinaryExpr(cur.loc, lhs, TokenKind.elvis, rhs);
            expr = bexpr;
        }

        endLoc(expr);
        return expr;
    }

    /**
     ** Conditional or expression:
     **   <condOrExpr> =  <condAndExpr> ("||" <condAndExpr>)*
  *
     */
    private Expr condOrExpr() {
        Expr expr = condAndExpr();
        if (curt == TokenKind.doublePipe) {
            Loc loc = cur.loc;
            Expr lhs = expr;
            TokenKind opToken = cur.kind;
            Expr rhs = null;
            while (curt == TokenKind.doublePipe) {
                consume();
                rhs = condAndExpr();
            }
            BinaryExpr cond = new BinaryExpr(cur.loc, lhs, opToken, rhs);
            endLoc(cond);
            expr = cond;
        }
        return expr;
    }

    /**
     ** Conditional and expression:
     **   <condAndExpr> =  <equalityExpr> ("&&" <equalityExpr>)*
  *
     */
    private Expr condAndExpr() {
        Expr expr = equalityExpr();
        if (curt == TokenKind.doubleAmp) {
            BinaryExpr cond = new BinaryExpr();
            cond.loc = cur.loc;
            cond.lhs = expr();
            cond.opToken = cur.kind;
            while (curt == TokenKind.doubleAmp) {
                consume();
                cond.rhs = equalityExpr();
            }
            endLoc(cond);
            expr = cond;
        }
        return expr;
    }

    /**
     ** Equality expression:
     **   <equalityExpr> =  <relationalExpr> [("==" | "!=" | "==" | "!=")
     * <relationalExpr>]
  *
     */
    private Expr equalityExpr() {
        Expr expr = relationalExpr();
        if (curt == TokenKind.eq || curt == TokenKind.notEq
                || curt == TokenKind.same || curt == TokenKind.notSame) {
            Expr lhs = expr;
            TokenKind tok = consume().kind;
            Expr rhs = relationalExpr();

            BinaryExpr bexpr = new BinaryExpr(cur.loc, lhs, tok, rhs);
            expr = bexpr;
            endLoc(expr);
        }
        return expr;
    }

    /**
     ** Relational expression:
     **   <relationalExpr> =  <typeCheckExpr> | <compareExpr>
     **   <typeCheckExpr> =  <rangeExpr> [("is" | "as" | "isnot") <type>]
     **   <compareExpr> =  <rangeExpr> [("<" | "<=" | ">" | ">=" | "<=>")
     * <rangeExpr>]
  *
     */
    private Expr relationalExpr() {
        Expr expr = addExpr();
        if (curt == TokenKind.isKeyword
                || curt == TokenKind.asKeyword
                || curt == TokenKind.lt || curt == TokenKind.ltEq
                || curt == TokenKind.gt || curt == TokenKind.gtEq
                || curt == TokenKind.cmp) {
            expr = new BinaryExpr(cur.loc, expr, consume().kind, addExpr());
            endLoc(expr);
        }
        return expr;
    }

    /**
     ** Additive expression:
     **   <addExpr> =  <multExpr> (("+" | "-") <multExpr>)*
  *
     */
    private Expr addExpr() {
        Expr expr = multExpr();
        while (curt == TokenKind.plus || curt == TokenKind.minus) {
            expr = new BinaryExpr(cur.loc, expr, consume().kind, multExpr());
            endLoc(expr);
        }
        return expr;
    }

    /**
     ** Multiplicative expression:
     **   <multExpr> =  <parenExpr> (("*" | "/" | "%") <parenExpr>)*
  *
     */
    private Expr multExpr() {
        Expr expr = parenExpr();
        while (curt == TokenKind.star || curt == TokenKind.slash || curt == TokenKind.percent) {
            expr = new BinaryExpr(cur.loc, expr, consume().kind, parenExpr());
            endLoc(expr);
        }
        return expr;
    }

    /**
     ** Paren grouped expression:
     **   <parenExpr> =  <unaryExpr> | <castExpr> | <groupedExpr>
     **   <castExpr> = "(" <type> ")" <parenExpr>
     **   <groupedExpr> = "(" <expr> ")" <termChain>*
  *
     */
    private Expr parenExpr() {
        if (curt != TokenKind.lparen && curt != TokenKind.lparenSynthetic) {
            return unaryExpr();
        }

        // consume() opening paren (or synthetic paren)
        Loc loc = cur.loc;
        consume();

        // this is just a normal parenthesized expression
        Expr expr = expr();
        consume(TokenKind.rparen);
        while (true) {
            Expr chained = termChainExpr(expr);
            if (chained == null) {
                break;
            }
            expr = chained;
        }
        return expr;
    }

    /**
     ** Unary expression:
     **   <unaryExpr> =  <prefixExpr> | <termExpr> | <postfixExpr>
     **   <prefixExpr> = ("!" | "+" | "-" | "~" | "++" | "--") <parenExpr>
     **   <postfixExpr> =  <termExpr> ("++" | "--")
  *
     */
    private Expr unaryExpr() {
        Loc loc = cur.loc;
        Token tok = cur;
        TokenKind tokt = curt;

        if (tokt == TokenKind.bang) {
            consume();
            UnaryExpr e = new UnaryExpr(loc, tokt, parenExpr());
            endLoc(e);
            return e;
        }

        if (tokt == TokenKind.plus) {
            consume();
            return parenExpr(); // optimize +expr to just expr
        }

        if (tokt == TokenKind.minus) {
            consume();
            UnaryExpr e = new UnaryExpr(loc, tokt, parenExpr());
            endLoc(e);
            return e;
        }

        if (tokt == TokenKind.increment || tokt == TokenKind.decrement) {
            consume();
            UnaryExpr e = new UnaryExpr(loc, tokt, parenExpr());
            endLoc(e);
            return e;
        }

        Expr expr = termExpr();

        // postfix ++/-- must be on the same line
//    tokt = curt;
//    tok = cur;
//    if (tokt.isIncrementOrDecrement && !tok.newline)
//    {
//      consume();
//      shortcut = ShortcutExpr.makeUnary(loc, tokt, expr);
//      shortcut.isPostfixLeave = true;
//      endLoc(shortcut);
//      return shortcut;
//    }
        return expr;
    }

//////////////////////////////////////////////////////////////////////////
// Term Expr
//////////////////////////////////////////////////////////////////////////
    /**
     ** A term is a base terminal such as a variable, call, or literal, *
     * optionally followed by a chain of accessor expressions - such * as
     * "x.y[z](a, b)". * *   <termExpr> =  <termBase> <termChain>*
  *
     */
    private Expr termExpr() {
        return termExpr(null);
    }

    private Expr termExpr(Expr target) {
        if (target == null) {
            target = termBaseExpr();
        }
        while (true) {
            Expr chained = termChainExpr(target);
            if (chained == null) {
                break;
            }
            target = chained;
        }
        return target;
    }

    /**
     ** Atomic base of a termExpr * *   <termBase> =  <literal> | <idExpr> |
     * <closure> | <dsl>
     **   <literal> = "null" | "this" | "super" | <bool> | <int> |
     **                     <float> | <str> | <duration> | <list> | <map> | <uri> |
     **                     <typeLiteral> | <slotLiteral>
     **   <typeLiteral> =  <type> "#"
     **   <slotLiteral> = [<type>] "#" <id>
  *
     */
    private Expr termBaseExpr() {
        Loc loc = cur.loc;

        switch (curt) {
            case amp:
                return addressExpr();
            case identifier:
                return idExpr(null, false, false);
            case intLiteral:
                return new LiteralExpr(loc, ExprId.intLiteral, consume().val);
            case floatLiteral:
                return new LiteralExpr(loc, ExprId.floatLiteral, consume().val);
            case strLiteral:
                return new LiteralExpr(loc, ExprId.strLiteral, consume().val);
            case lbracket:
                return collectionLiteralExpr(loc, null);
            case falseKeyword:
                consume();
                return new LiteralExpr(loc, ExprId.falseLiteral, false);
            case nullKeyword:
                consume();
                return new LiteralExpr(loc, ExprId.nullLiteral, null);
            case superKeyword: {
                consume();
                IdExpr ie = new IdExpr();
                ie.loc = loc;
                ie.name = "super";
                ie.id = Expr.ExprId.superExpr;
                return ie;
            }
            case thisKeyword: {
                consume();
                IdExpr ie = new IdExpr();
                ie.loc = loc;
                ie.name = "this";
                ie.id = Expr.ExprId.thisExpr;
                return ie;
            }
            case itKeyword: {
                consume();
                IdExpr ie = new IdExpr();
                ie.loc = loc;
                ie.name = "it";
                ie.id = Expr.ExprId.itExpr;
                return ie;
            }
            case trueKeyword:
                consume();
                return new LiteralExpr(loc, ExprId.trueLiteral, true);
//      case pound:           
//        consume();
//        expr = SlotLiteralExpr(loc, curType.asRef(), consumeId());
//        endLoc(expr);
//        return expr;
            case awaitKeyword:
                consume();
                UnaryExpr aexpr = new UnaryExpr(loc, TokenKind.awaitKeyword, expr());
                endLoc(aexpr);
                return aexpr;
            case sizeofKeyword:
                consume();
                consume(TokenKind.lparen);
                SizeOfExpr sexpr = new SizeOfExpr();
                sexpr.loc = loc;
                sexpr.type = this.typeRef();
                consume(TokenKind.rparen);
                endLoc(sexpr);
                return sexpr;
//      case addressofKeyword:
//        consume();
//        consume(TokenKind.lparen);
//        expr = AddressOfExpr(loc, this.expr);
//        consume(TokenKind.rparen);
//        endLoc(expr);
//        return expr;
            case pipe:
                Expr c = tryClosure();
                if (c != null) {
                    return c;
                }
        }

        if (curt == TokenKind.pipe) {
            throw err("Invalid closure expression (check types)");
        } else {
            if (cur.kind.keyword) {
                throw err("Expected expression, not keyword '" + cur + "'");
            } else {
                throw err("Expected expression, not '" + cur + "'");
            }
        }
    }

    /**
     ** A chain expression is a piece of a term expression that may * be
     * chained together such as "call.var[x]". If the specified * target
     * expression contains a chained access, then return the new * expression,
     * otherwise return null. * *   <termChain> =  <compiledCall> | <dynamicCall> |
     * <indexExpr>
     **   <compiledCall> = "." <idExpr>
     **   <dynamicCall> = "->" <idExpr>
  *
     */
    private Expr termChainExpr(Expr target) {
        Loc loc = cur.loc;

        // handle various call operators: . -> ?. ?->
        switch (curt) {
            // if ".id" field access or ".id" call
            case dot:
                consume();
                return idExpr(target, false, false);

            // if "->id" dynamic call
            //case TokenKind.arrow: consume(); return idExpr(target, true, false, false)
            // if "~>" checked dynamic call
            //case TokenKind.tildeArrow:
            //  consume(); return idExpr(target, true, false, true)
            // if "?.id" safe call
            case safeDot:
                consume();
                return idExpr(target, false, true);

            // if "?->id" safe dynamic call
            //case TokenKind.safeArrow: consume(); return idExpr(target, true, true, false)
            // if "?~>id" safe checked dynamic call
            //case TokenKind.safeTildeArrow:
            //  consume(); return idExpr(target, true, true, true)
        }

        // if target[...]
        if (cur.kind == TokenKind.lbracket && !cur.newline) {
            return indexExpr(target);
        }

        // if target(...)
        if (cur.kind == TokenKind.lparen && !cur.newline) {
            return callExpr(target);
        }

        // otherwise the expression should be finished
        return null;
    }

//////////////////////////////////////////////////////////////////////////
// Term Expr Utils
//////////////////////////////////////////////////////////////////////////
    private Expr addressExpr() {
        Loc loc = cur.loc;
        consume(TokenKind.amp);
        Expr expr = new UnaryExpr(loc, TokenKind.amp, expr());
        endLoc(expr);
        return expr;
    }

    /**
     ** Identifier expression:
     **   <idExpr> =  <local> | <field> | <call>
     **   <local> =  <id>
     **   <field> = ["*"] <id>
  *
     */
    private Expr idExpr(Expr target, boolean dynamicCall, boolean safeCall) {
        return idExpr(target, dynamicCall, safeCall, true);
    }

    private Expr idExpr(Expr target, boolean dynamicCall, boolean safeCall, boolean checkedCall) {
        Loc loc = cur.loc;

//    if (curt == TokenKind.amp)
//    {
//      consume()
//      expr = UnknownVarExpr(loc, target, consumeId, ExprId.storage)
//      endLoc(expr)
//      return expr
//    }
        if (peekt == TokenKind.lparen && !peek.newline) {
            CallExpr call = callExpr(target);
            //call.isDynamic = dynamicCall
            //call.isCheckedCall = checkedCall
            //call.isSafe = safeCall;
            return call;
        }

        String name = consumeId();

        // if we have a closure then this is a call with one arg of a closure
//    ClosureExpr closure = tryClosure();
//    if (closure != null)
//    {
//      call = CallExpr(loc);
//      call.target    = target;
//      call.name      = name;
//      //call.isDynamic = dynamicCall
//      //call.isCheckedCall = checkedCall
//      call.isSafe    = safeCall;
//      call.noParens  = true;
//      call.args.add(closure);
//      endLoc(call);
//      return call;
//    }
        // if dynamic call then we know this is a call not a field
//    if (dynamicCall)
//    {
//      call = CallExpr(loc)
//      call.target    = target
//      call.name      = name
//      //call.isDynamic = true
//      //call.isCheckedCall = checkedCall
//      call.isSafe    = safeCall
//      call.noParens  = true
//      endLoc(call)
//      return call
//    }
        // at this point we are parsing a single identifier, but
        // if it looks like it was expected to be a type we can
        // provide a more meaningful error
        if (curt == TokenKind.pound) {
            throw err("Unknown type '$name' for type literal" + loc);
        }

        IdExpr expr = new IdExpr();
        expr.loc = loc;
        expr.name = name;
        expr.target = target;
        endLoc(expr);
        return expr;
    }

    /**
     ** Call expression:
     **   <call> =  <id> ["(" <args> ")"] [<closure>]
  *
     */
    private CallExpr callExpr(Expr target) {
        CallExpr call = new CallExpr();
        call.loc = cur.loc;
        call.target = target;
        call.name = consumeId();
        callArgs(call);
        endLoc(call);
        return call;
    }

    /**
     ** Parse args with known parens:
     **   <args> = [<expr> ("," <expr>)*] [<closure>]
  *
     */
    private void callArgs(CallExpr call) {
        callArgs(call, true);
    }

    private void callArgs(CallExpr call, boolean closureOk) {
        consume(TokenKind.lparen);
        if (curt != TokenKind.rparen) {
            while (true) {
                ArgExpr arg = new ArgExpr();

                //named param
                if (curt == TokenKind.identifier && peekt == TokenKind.colon) {
                    String name = consumeId();
                    consume(TokenKind.colon);
                    arg.name = name;
                }
                arg.argExpr = expr();
                call.args.add(arg);
                if (curt == TokenKind.rparen) {
                    break;
                }
                consume(TokenKind.comma);
            }
        }
        consume(TokenKind.rparen);

        if (closureOk) {
            Expr closure = tryClosure();
            if (closure != null) {
                ArgExpr arg = new ArgExpr();
                arg.argExpr = closure;
                call.args.add(arg);
            }
        }
    }

    /**
     ** Index expression:
     **   <indexExpr> = "[" <expr> "]"
  *
     */
    private Expr indexExpr(Expr target) {
        Loc loc = cur.loc;
        consume(TokenKind.lbracket);

        // nice error for BadType[,]
        if (curt == TokenKind.comma && target.id == ExprId.unknownVar) {
            throw err("Unknown type '$target' for list literal" + target.loc);
        }

        // otherwise this must be a standard single key index
        Expr expr = expr();
        consume(TokenKind.rbracket);
        IndexExpr e = new IndexExpr();
        e.loc = loc;
        e.target = target;
        e.index = expr;
        endLoc(e);
        return e;
    }

//////////////////////////////////////////////////////////////////////////
// Collection "Literals"
//////////////////////////////////////////////////////////////////////////
    /**
     ** Collection literal:
     **   <list> = [<type>] "[" <listItems> "]"
     **   <listItems> = "," | (<expr> ("," <expr>)*)
     **   <map> = [<mapType>] "[" <mapItems> "]"
     **   <mapItems> = ":" | (<mapPair> ("," <mapPair>)*)
     **   <mapPair> =  <expr> ":" <expr>
  *
     */
    private Expr collectionLiteralExpr(Loc loc, Type explicitType) {
        // empty list [,]
        if (peekt == TokenKind.comma) {
            return listLiteralExpr(loc, explicitType, null);
        }

        // empty map [:]
//    if (peekt == TokenKind.colon)
//      return mapLiteralExpr(loc, explicitType, null)
        // opening bracket
        consume(TokenKind.lbracket);

        // [] is error
        if (curt == TokenKind.rbracket) {
            err("Invalid list literal; use '[,]' for empty Obj[] list" + loc);
            consume();
            ListLiteralExpr expr = new ListLiteralExpr();
            expr.loc = loc;
            endLoc(expr);
            return expr;
        }

        // read first expression
        Expr first = expr();

        // at this point we can determine if it is a list or a map
//    if (curt == TokenKind.colon)
//      return mapLiteralExpr(loc, explicitType, first)
//    else
        return listLiteralExpr(loc, explicitType, first);
    }

    /**
     ** Parse List literal; if first is null then * cur must be on lbracket *
     * else * cur must be on comma after first item
  *
     */
    private ListLiteralExpr listLiteralExpr(Loc loc, Type explicitType, Expr first) {
        // explicitType is type of List:  String[,]
        if (explicitType != null) {
            Type elemType = explicitType;
            explicitType = Type.listType(loc, elemType);
        }
        ListLiteralExpr list = new ListLiteralExpr();
        list.loc = loc;
        list.explicitType = explicitType;

        // if first is null, must be on lbracket
        if (first == null) {
            consume(TokenKind.lbracket);

            // if [,] empty list
            if (curt == TokenKind.comma) {
                consume();
                consume(TokenKind.rbracket);
                return list;
            }

            first = expr();
        }

        list.vals.add(first);
        while (curt == TokenKind.comma) {
            consume();
            if (curt == TokenKind.rbracket) {
                break; // allow extra trailing comma
            }
            list.vals.add(expr());
        }
        consume(TokenKind.rbracket);
        endLoc(list);
        return list;
    }

//////////////////////////////////////////////////////////////////////////
// Closure
//////////////////////////////////////////////////////////////////////////
    /**
     ** Attempt to parse a closure expression or return null if we * aren't
     * positioned at the start of a closure expression.
  *
     */
    private ClosureExpr tryClosure() {
        Loc loc = cur.loc;

        // if not pipe then not closure
        if (curt != TokenKind.pipe) {
            return null;
        }

        // otherwise this can only be a FuncType declaration,
        // so give it a whirl, and bail if that fails
        int mark = pos;
//    funcType = tryType as FuncType
        FuncPrototype funcType = null; //TODO:= this.funcType(false);
//    if (funcType == null) { reset(mark); return null }

        // if we don't see opening brace for body - no go
        if (curt != TokenKind.lbrace) {
            reset(mark);
            return null;
        }

        return closure(loc, funcType);
    }

    /**
     ** Parse body of closure expression and return ClosureExpr.
  *
     */
    private ClosureExpr closure(Loc loc, FuncPrototype funcType) {
        // verify func types has named parameters
        // create closure
        ClosureExpr closure = new ClosureExpr();
        closure.loc = loc;
        closure.prototype = funcType;
        closure.code = block();

        endLoc(closure);
        return closure;
    }

}
