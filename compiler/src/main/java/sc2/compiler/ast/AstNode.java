//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sc2.compiler.ast;

import java.util.ArrayList;
import sc2.compiler.ast.Token.TokenKind;

/**
 *
 * @author yangjiandong
 */
public class AstNode {
    public static final int Abstract   = 0x00000001;
    public static final int Const      = 0x00000002;
    public static final int Ctor       = 0x00000004;
    public static final int Enum       = 0x00000008;
    public static final int Facet      = 0x00000010;
    public static final int Final      = 0x00000020;
    public static final int Getter     = 0x00000040;
    public static final int Internal   = 0x00000080;
    public static final int Mixin      = 0x00000100;
    public static final int Native     = 0x00000200;
    public static final int Override   = 0x00000400;
    public static final int Private    = 0x00000800;
    public static final int Protected  = 0x00001000;
    public static final int Public     = 0x00002000;
    public static final int Setter     = 0x00004000;
    public static final int Static     = 0x00008000;
    public static final int Storage    = 0x00010000;
    public static final int Synthetic  = 0x00020000;
    public static final int Virtual    = 0x00040000;
    public static final int Struct     = 0x00080000;
    public static final int Extension  = 0x00100000;
    public static final int RuntimeConst= 0x00200000;
    public static final int Readonly   = 0x00400000;
    public static final int Async      = 0x00800000;
    public static final int Overload   = 0x01000000;
    public static final int Closure    = 0x02000000;
    public static final int Once       = 0x04000000;
    public static final int FlagsMask  = 0x0fffffff;
  
    public Loc loc;
    public int len = 0;
    
    public AstNode parent;
    
    public int flags;
    
    public void getChildren(ArrayList<AstNode> list, Object options) {}
    
    
    public static class Comment extends AstNode {
        public String content;
        public TokenKind type;
        
        public Comment(Loc loc, String content, TokenKind type) {
            this.loc = loc;
            this.content = content;
            this.type = type;
        }
    }
    
    public static abstract class TypeDef extends AstNode {
        public String name;
        public Comment comment;
    }
    
    public static class FieldDef extends Stmt {
        public String name;
        public Comment comment;
        public Type fieldType;        // field type
        public Expr initExpr;         // init expression or null
    }
    
    public static class StructDef extends TypeDef {
        public String qname;
        public ArrayList<GeneriParamDef> generiParamDefs;
        public ArrayList<Type> inheritances;
        public ArrayList<FieldDef> fieldDefs;
        public ArrayList<FuncDef> funcDefs;
        
        public void addSlot(AstNode node) {
            node.parent = this;
        }
    }
    
    public static class EnumDef extends TypeDef {
        public ArrayList<FieldDef> enumDefs;
    }
    
    public static class TraitDef extends TypeDef {
        public ArrayList<FuncDef> slotDefList;
    }
    
    public static class FuncPrototype {
        public Type returnType;       // return type
        public ArrayList<ParamDef> paramDefs;   // parameter definitions
    }
    
    public static class FuncDef extends AstNode {
        public String name;
        public Comment comment;
        public FuncPrototype prototype = new FuncPrototype();       // return type
        public Block code;            // code block
        public ArrayList<GeneriParamDef> generiParams;
    }
    
    public static class FileUnit extends AstNode {
        public String name;
        public ArrayList<TypeDef> typeDefs;
        public ArrayList<FieldDef> fieldDefs;
        public ArrayList<FuncDef> funcDefs;
        public ArrayList<FuncDef> usings;
        
        public void addDef(AstNode node) {
            node.parent = this;
        }
    }
    
    public static class Import extends AstNode {
        public String podName;
        public String name;
        public String asName;
    }
    
    public static class Module extends AstNode {
        public String name;
        public ArrayList<FileUnit> fileUints;
    }
    
    public static class Block extends AstNode {
        public ArrayList<Stmt> stmts;
    }
    
    public static class GeneriParamDef extends TypeDef {
        public Type bound;
        public TypeDef parent;
    }
    
    public static class ParamDef extends AstNode {
        public Type paramType;
        public Expr defualtValue;
        public String name;
    }
}
