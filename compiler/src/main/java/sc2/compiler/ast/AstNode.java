//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sc2.compiler.ast;

import java.util.ArrayList;
import java.util.HashMap;
import sc2.compiler.ast.Expr.IdExpr;
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
    public static final int Unsafe     = 0x00000020;
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
    public static final int Mutable    = 0x00200000;
    public static final int Readonly   = 0x00400000;
    public static final int Async      = 0x00800000;
    public static final int Overload   = 0x01000000;
    public static final int Closure    = 0x02000000;
    public static final int Throws     = 0x04000000;
    public static final int Reflect    = 0x08000000;
  
    public Loc loc;
    public int len = 0;
    
    public AstNode parent;
    
    public int flags;
    
    public void getChildren(ArrayList<AstNode> list, Object options) {}
    
    
    public void walk(Visitor visitor) {
    }
        
    
    public static class Comment extends AstNode {
        public String content;
        public TokenKind type;
        
        public Comment(String content, TokenKind type) {
            this.content = content;
            this.type = type;
        }
    }
    
    public static class Comments extends AstNode {
        public ArrayList<Comment> comments = new ArrayList<Comment>();
    }
    
    public static abstract class TypeDef extends AstNode {
        public String name;
        public Comments comment;
        
        @Override public void walk(Visitor visitor) {
            visitor.enterTypeDef(this);
            walkChildren(visitor);
            visitor.exitTypeDef(this);
        }
        
        protected void walkChildren(Visitor visitor) {
            
        }
    }
    
    public static class FieldDef extends Stmt {
        public String name;
        public Comments comment;
        public Type fieldType;        // field type
        public Expr initExpr;         // init expression or null
        
        public FieldDef(Comments comment, String name) {
            this.comment = comment;
            this.name = name;
        }
        
        @Override public void walk(Visitor visitor) {
            visitor.enterField(this);
            //walkChildren(visitor);
            visitor.exitField(this);
        }
    }
    
    public static class StructDef extends TypeDef {
        public ArrayList<Type> inheritances;
        public ArrayList<FieldDef> fieldDefs = new ArrayList<FieldDef>();
        public ArrayList<FuncDef> funcDefs = new ArrayList<FuncDef>();
        public ArrayList<GeneriParamDef> generiParamDefs = null;
        public Scope scope = null;
        
        public StructDef(Comments comment, int flags, String name) {
            this.comment = comment;
            this.flags = flags;
            this.name = name;
        }
        
        public void addSlot(AstNode node) {
            node.parent = this;
            if (node instanceof FieldDef) {
                fieldDefs.add((FieldDef)node);
            }
            else if (node instanceof FuncDef) {
                funcDefs.add((FuncDef)node);
            }
        }
        
        @Override protected void walkChildren(Visitor visitor) {
            for (FieldDef field : fieldDefs) {
                field.walk(visitor);
            }
            for (FuncDef func : funcDefs) {
                func.walk(visitor);
            }
        }
        
        public Scope getScope() {
            if (scope == null) {
                scope = new Scope();
                
                for (FieldDef f : fieldDefs) {
                    scope.put(f.name, f);
                }
                for (FuncDef f : funcDefs) {
                    scope.put(f.name, f);
                }
            }
            return scope;
        }
    }
    
    public static class EnumDef extends TypeDef {
        public ArrayList<FieldDef> enumDefs;
        
        public EnumDef(Comments comment, int flags, String name) {
            this.comment = comment;
            this.flags = flags;
            this.name = name;
        }
        
        public void addSlot(FieldDef node) {
            node.parent = this;
            enumDefs.add(node);
        }
    }
    
    public static class TraitDef extends TypeDef {
        public ArrayList<FuncDef> funcDefs;
        
        public TraitDef(Comments comment, int flags, String name) {
            this.comment = comment;
            this.flags = flags;
            this.name = name;
        }
        
        public void addSlot(FuncDef node) {
            node.parent = this;
            funcDefs.add(node);
        }
    }
    
    public static class FuncPrototype {
        public Type returnType;       // return type
        public ArrayList<ParamDef> paramDefs = null;   // parameter definitions
        public int postFlags = 0;
    }
    
    public static class FuncDef extends AstNode {
        public String name;
        public Comments comment;
        public FuncPrototype prototype = new FuncPrototype();       // return type
        public Block code;            // code block
        public ArrayList<GeneriParamDef> generiParams = null;
        
        @Override public void walk(Visitor visitor) {
            visitor.enterFunc(this);
            //walkChildren(visitor);
            visitor.exitFunc(this);
        }
    }

    
    public static class FileUnit extends AstNode {
        public String name;
        public ArrayList<TypeDef> typeDefs = new ArrayList<TypeDef>();
        public ArrayList<FieldDef> fieldDefs = new ArrayList<FieldDef>();
        public ArrayList<FuncDef> funcDefs = new ArrayList<FuncDef>();
        public ArrayList<Import> imports = new ArrayList<Import>();
        public ArrayList<TypeAlias> typeAlias = new ArrayList<TypeAlias>();
        
        public Scope importScope = null;
        
        public FileUnit(String file) {
            name = file;
        }
        
        public void addDef(AstNode node) {
            node.parent = this;
            if (node instanceof TypeDef) {
                typeDefs.add((TypeDef)node);
            }
            else if (node instanceof FieldDef) {
                fieldDefs.add((FieldDef)node);
            }
            else if (node instanceof FuncDef) {
                funcDefs.add((FuncDef)node);
            }
            else if (node instanceof Import) {
                imports.add((Import)node);
            }
            else if (node instanceof TypeAlias) {
                typeAlias.add((TypeAlias)node);
            }
        }
        
        @Override public void walk(Visitor visitor) {
            visitor.enterUnit(this);
            for (TypeDef typeDef : typeDefs) {
                typeDef.walk(visitor);
            }
            for (FieldDef field : fieldDefs) {
                field.walk(visitor);
            }
            for (FuncDef func : funcDefs) {
                func.walk(visitor);
            }
            visitor.exitUnit(this);
        }
    }
    
    
    public static class Import extends AstNode {
        public IdExpr id;
        public boolean star = false;
    }
    
    public static class TypeAlias extends AstNode {
        public Type type;
        public String asName;
        public Comments comment;
    }
    
    public static class Block extends AstNode {
        public ArrayList<Stmt> stmts = new ArrayList<Stmt>();
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
