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
    public static final int Inline     = 0x10000000;
    public static final int Packed     = 0x20000000;
    public static final int ConstExpr  = 0x40000000;
    public static final int Operator   = 0x80000000;
  
    public Loc loc;
    public int len = 0;
    
    public void getChildren(ArrayList<AstNode> list, Object options) {}
    
    public interface Visitor {
        public void visit(AstNode node);
    }
    
    public void walkChildren(Visitor visitor) {
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
    
    public static abstract class TopLevelDef extends AstNode {
        public AstNode parent;
        public int flags;
        public Comments comment;
        public String name;
    }
    
    public static abstract class TypeDef extends TopLevelDef {
        protected Scope scope = null;
        public abstract Scope getScope();
    }
    
    public static class FieldDef extends TopLevelDef {
        public Type fieldType;        // field type
        public Expr initExpr;         // init expression or null
        public boolean isLocalVar = false;
        
        public FieldDef(Comments comment, String name) {
            this.comment = comment;
            this.name = name;
        }
        
        public FieldDef parameterize(ArrayList<Type> typeGenericArgs) {
            FieldDef nf = new FieldDef(this.comment, this.name);
            nf.loc = this.loc;
            nf.len = this.len;
            nf.flags = this.flags;
            nf.parent = this.parent;
            nf.initExpr = this.initExpr;
            nf.isLocalVar = this.isLocalVar;
            nf.fieldType = this.fieldType.parameterize(typeGenericArgs);
            return nf;
        }
    }
    
    public static class StructDef extends TypeDef {
        public ArrayList<Type> inheritances = null;
        public ArrayList<FieldDef> fieldDefs = new ArrayList<FieldDef>();
        public ArrayList<FuncDef> funcDefs = new ArrayList<FuncDef>();
        public ArrayList<GenericParamDef> generiParamDefs = null;
        
        public StructDef(Comments comment, int flags, String name) {
            this.comment = comment;
            this.flags = flags;
            this.name = name;
        }
        
        public void addSlot(AstNode node) {
            if (node instanceof FieldDef f) {
                fieldDefs.add(f);
                f.parent = this;
            }
            else if (node instanceof FuncDef f) {
                funcDefs.add(f);
                f.parent = this;
            }
        }
        
        @Override public void walkChildren(Visitor visitor) {
//            if (this.inheritances != null) {
//                for (Type ins : this.inheritances) {
//                    visitor.visit(ins);
//                }
//            }
            for (FieldDef field : fieldDefs) {
                visitor.visit(field);
            }
            for (FuncDef func : funcDefs) {
                visitor.visit(func);
            }
        }
        
        public Scope getScope() {
            if (scope == null) {
                scope = new Scope();
                if (this.generiParamDefs != null) {
                    for (GenericParamDef gp : this.generiParamDefs) {
                        scope.put(gp.name, gp);
                    }
                }
                for (FieldDef f : fieldDefs) {
                    scope.put(f.name, f);
                }
                for (FuncDef f : funcDefs) {
                    scope.put(f.name, f);
                }
            }
            return scope;
        }
        
        public Scope getScopeForInherite() {
            if (scope == null) {
                scope = new Scope();
                for (FieldDef f : fieldDefs) {
                    if ((f.flags & AstNode.Private) != 0) {
                        continue;
                    }
                    scope.put(f.name, f);
                }
                for (FuncDef f : funcDefs) {
                    if ((f.flags & AstNode.Private) != 0) {
                        continue;
                    }
                    scope.put(f.name, f);
                }
            }
            return scope;
        }
        
        public StructDef parameterize(ArrayList<Type> typeGenericArgs) {
            StructDef nt = new StructDef(this.comment, this.flags, this.name);
            for (FieldDef f : fieldDefs) {
                nt.addSlot(f.parameterize(typeGenericArgs));
            }
            for (FuncDef f : funcDefs) {
                nt.addSlot(f.parameterize(typeGenericArgs));
            }
            return nt;
        }
    }
    
    public static class EnumDef extends TypeDef {
        public ArrayList<FieldDef> enumDefs = new ArrayList<FieldDef>();
        
        public EnumDef(Comments comment, int flags, String name) {
            this.comment = comment;
            this.flags = flags;
            this.name = name;
        }
        
        public void addSlot(FieldDef node) {
            node.parent = this;
            enumDefs.add(node);
        }
        
        public Scope getScope() {
            if (scope == null) {
                scope = new Scope();
                
                for (FieldDef f : enumDefs) {
                    scope.put(f.name, f);
                }
            }
            return scope;
        }
    }
    
    public static class TraitDef extends TypeDef {
        public ArrayList<FuncDef> funcDefs = new ArrayList<FuncDef>();
        
        public TraitDef(Comments comment, int flags, String name) {
            this.comment = comment;
            this.flags = flags;
            this.name = name;
        }
        
        public void addSlot(FuncDef node) {
            node.parent = this;
            funcDefs.add(node);
        }
        
        public Scope getScope() {
            if (scope == null) {
                scope = new Scope();

                for (FuncDef f : funcDefs) {
                    scope.put(f.name, f);
                }
            }
            return scope;
        }
        
        @Override public void walkChildren(Visitor visitor) {
            for (FuncDef func : funcDefs) {
                visitor.visit(func);
            }
        }
    }
    
    public static class FuncPrototype {
        public Type returnType;       // return type
        public ArrayList<ParamDef> paramDefs = null;   // parameter definitions
        public int postFlags = 0;
        
        @java.lang.Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("(");
            if (paramDefs != null) {
                int i = 0;
                for (ParamDef p : paramDefs) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(p.name);
                    sb.append(" : ");
                    sb.append(p.paramType);
                    ++i;
                }
            }
            sb.append(")");

            if (returnType != null && returnType.isVoid()) {
                sb.append(":");
                sb.append(returnType);
            }
            return sb.toString();
        }
    }
    
    public static class FuncDef extends TopLevelDef {
        public FuncPrototype prototype = new FuncPrototype();       // return type
        public Block code;            // code block
        public ArrayList<GenericParamDef> generiParamDefs = null;
        
        public FuncDef parameterize(ArrayList<Type> typeGenericArgs) {
            FuncDef nf = new FuncDef();
            nf.comment = this.comment;
            nf.flags = this.flags;
            nf.loc = this.loc;
            nf.len = this.len;
            nf.name = this.name;
            nf.code = this.code;
            nf.parent = this.parent;
            nf.prototype = new FuncPrototype();
            nf.prototype.returnType = this.prototype.returnType.parameterize(typeGenericArgs);
            nf.prototype.postFlags = this.prototype.postFlags;
            nf.prototype.paramDefs = new ArrayList<ParamDef>();
            for (ParamDef p : this.prototype.paramDefs) {
                ParamDef np = new ParamDef();
                np.name = p.name;
                np.defualtValue = p.defualtValue;
                np.loc = p.loc;
                np.len = p.len;
                np.paramType = p.paramType.parameterize(typeGenericArgs);
                nf.prototype.paramDefs.add(np);
            }
            return nf;
        }
    }

    
    public static class FileUnit extends AstNode {
        public String name;
        public ArrayList<TypeDef> typeDefs = new ArrayList<TypeDef>();
        public ArrayList<FieldDef> fieldDefs = new ArrayList<FieldDef>();
        public ArrayList<FuncDef> funcDefs = new ArrayList<FuncDef>();
        public ArrayList<Import> imports = new ArrayList<Import>();
        public ArrayList<TypeAlias> typeAlias = new ArrayList<TypeAlias>();
        public SModule module;
        
        public Scope importScope = null;
        
        public FileUnit(String file) {
            name = file;
        }
        
        public void addDef(TopLevelDef node) {
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
            else if (node instanceof TypeAlias) {
                typeAlias.add((TypeAlias)node);
            }
        }
        
        @Override public void walkChildren(Visitor visitor) {
            for (TypeAlias typeAlias : typeAlias) {
                visitor.visit(typeAlias);
            }
            for (TypeDef typeDef : typeDefs) {
                visitor.visit(typeDef);
            }
            for (FieldDef field : fieldDefs) {
                visitor.visit(field);
            }
            for (FuncDef func : funcDefs) {
                visitor.visit(func);
            }
        }
    }
    
    
    public static class Import extends AstNode {
        public IdExpr id;
        public boolean star = false;
    }
    
    public static class TypeAlias extends TopLevelDef {
        public Type type;
    }
    
    public static class Block extends Stmt {
        public ArrayList<Stmt> stmts = new ArrayList<Stmt>();
        @Override public void walkChildren(Visitor visitor) {
            for (Stmt s : stmts) {
                visitor.visit(s);
            }
        }
    }
    
    public static class GenericParamDef extends TypeDef {
        public Type bound;
        public int index;

        @Override
        public Scope getScope() {
            return scope;
        }
    }
    
    public static class ParamDef extends AstNode {
        public Type paramType;
        public Expr defualtValue;
        public String name;
    }
}
