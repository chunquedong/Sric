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
        
        public UnaryExpr(TokenKind tok, Expr operand) {
            this.opToken = tok;
            this.operand = operand;
        }
    }
    
    public static class BinaryExpr extends Expr {
        public Token.TokenKind opToken;      // operator token type (Token.and, etc)
        public Expr lhs;           // left hand side
        public Expr rhs;           // right hand side
        
        public BinaryExpr(Expr lhs, TokenKind tok, Expr rhs) {
            this.lhs = lhs;
            this.opToken = tok;
            this.rhs = rhs;
        }
        
        public BinaryExpr() {
            
        }
    }
    
    /**
     * wrap type for sizeof(t) or 'epxr is/as T'
     */
    public static class TypeExpr extends Expr {
        public Type type;           // right hand side
        
        public TypeExpr(Type type) {
            this.type = type;
        }
    }
    
    public static class IndexExpr extends Expr {
        public Expr target;
        public Expr index;
    }
    
    public static class CallExpr extends Expr {
        public Expr target;
        public ArrayList<ArgExpr> args = null;
    }
    
    public static class ArgExpr extends Expr {
        public String name;
        public Expr argExpr;
        
        public ArgExpr() {
        }
        
        public ArgExpr(Expr argExpr) {
            this.argExpr = argExpr;
            this.loc = argExpr.loc;
            this.len = argExpr.len;
        }
    }
    
    public static class IdExpr extends Expr {
        public String namespace;
        public String name;
        
        public IdExpr(String name) {
            this.name = name;
        }
    }
    
    public static class AccessExpr extends Expr {
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
        public ArrayList<ArgExpr> args = null;
    }
    
    public static class LiteralExpr extends Expr {
        public Object value;
        
        public LiteralExpr(ExprId id, Object value) {
            this.id = id;
            this.value = value;
        }
    }
}
