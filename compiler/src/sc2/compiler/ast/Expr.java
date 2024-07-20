//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sc2.compiler.ast;

import sc2.compiler.ast.AstNode.Block;
import sc2.compiler.ast.Token.TokenKind;
import java.util.ArrayList;

/**
 *
 * @author yangjiandong
 */
public abstract class Expr extends AstNode {
    public static enum ExprId {
      nullLiteral,      // LiteralExpr
      trueLiteral,
      falseLiteral,
      intLiteral,
      floatLiteral,
      strLiteral,
      typeLiteral,
      slotLiteral,      // SlotLiteralExpr
      listLiteral,      // ListLiteralExpr
      boolNot,          // UnaryExpr
      cmpNull,
      cmpNotNull,
      elvis,
      assign,           // BinaryExpr
      same,
      notSame,
      boolOr,           // CondExpr
      boolAnd,
      isExpr,           // TypeCheckExpr
      asExpr,
      coerce,
      call,             // CallExpr
      construction,
      shortcut,         // ShortcutExpr (has ShortcutOp)
      field,            // FieldExpr
      localVar,         // LocalVarExpr
      thisExpr,         // ThisExpr
      superExpr,        // SuperExpr
      itExpr,           // ItExpr
      staticTarget,     // StaticTargetExpr
      unknownVar,       // UnknownVarExpr
      ternary,          // TernaryExpr
      complexLiteral,   // ComplexLiteral
      closure,          // ClosureExpr
      throwExpr,        // ThrowExpr
      awaitExpr,
      sizeOfExpr,
      addressOfExpr,
      initBlockExpr
    }
    
    public ExprId id;
    public Type resolvedType;
    
    public ExprId getId() {
        return id;
    }
    
    public static class UnaryExpr extends Expr {
        public Token.TokenKind opToken;   // operator token type (Token.bang, etc)
        public Expr operand;    // operand expression
        
        public UnaryExpr(Loc loc, TokenKind tok, Expr operand) {
            this.loc = loc;
            this.opToken = tok;
            this.operand = operand;
        }
    }
    
    public static class BinaryExpr extends Expr {
        public Token.TokenKind opToken;      // operator token type (Token.and, etc)
        public Expr lhs;           // left hand side
        public Expr rhs;           // right hand side
        
        public BinaryExpr(Loc loc, Expr lhs, TokenKind tok, Expr rhs) {
            this.loc = loc;
            this.lhs = lhs;
            this.opToken = tok;
            this.rhs = rhs;
        }
        
        public BinaryExpr() {
            
        }
    }
    
    public static class IndexExpr extends Expr {
        public Expr target;
        public Expr index;
    }
    
    public static class CallExpr extends Expr {
        public String name;
        public Expr target;
        public ArrayList<ArgExpr> args = new ArrayList<ArgExpr>();
    }
    
    public static class ArgExpr extends Expr {
        public String name;
        public Expr argExpr;
    }
    
    public static class IdExpr extends Expr {
        public Expr target;
        public String name;
    }
    
    public static class IfExpr extends Expr {
        public Expr condition;     // boolean test
        public Expr trueExpr;      // result of expression if condition is true
        public Expr falseExpr;     // result of expression if condition is false
    }
    
    public static class InitBlockExpr extends Expr {
        public Expr target;
        public Block block;
    }
    
    public static class LiteralExpr extends Expr {
        public Object value;
        
        public LiteralExpr(Loc loc, ExprId id, Object value) {
            this.loc = loc;
            this.id = id;
            this.value = value;
        }
    }
    
    public static class ListLiteralExpr extends Expr {
        public Type explicitType;
        public ArrayList<Expr> vals = new ArrayList<Expr>();
    }
    
    public static class SizeOfExpr extends Expr {
        public Type type;
    }
}
