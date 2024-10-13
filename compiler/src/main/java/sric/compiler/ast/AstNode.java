//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler.ast;

import java.util.ArrayList;
import java.util.HashMap;
import sric.compiler.ast.Expr.IdExpr;
import sric.compiler.ast.Token.TokenKind;

/**
 *
 * @author yangjiandong
 */
public class AstNode {

    public Loc loc;
    public int len = 0;
        
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
        
        public String getDoc() {
            for (var c : comments) {
                if (c.type == TokenKind.docComment) {
                    return c.content;
                }
            }
            return null;
        }
    }
    
    public static abstract class TopLevelDef extends AstNode {
        public AstNode parent;
        public int flags;
        public Comments comment;
        public String name;
        
        public boolean isDeprecated() {
            if (comment != null) {
                for (var c : comment.comments) {
                    if (c.type == TokenKind.cmdComment) {
                        if (c.content.startsWith("deprecated")) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
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
        
        private Scope inheritScopes = null;
        StructDef genericFrom = null;
        private HashMap<String, StructDef> parameterizeCache;
        
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
        
        public void getAllFields(HashMap<String,FieldDef> fields) {
            for (FieldDef field : fieldDefs) {
                fields.put(field.name, field);
            }
            
            if (this.inheritances != null) {
                for (Type inh : this.inheritances) {
                    if (inh.id.resolvedDef != null) {
                        if (inh.id.resolvedDef instanceof StructDef inhSd) {
                            inhSd.getAllFields(fields);
                        }
                    }
                }
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
        
        public Scope getInheriteScope() {
            if (inheritScopes == null) {
                if (this.inheritances == null) {
                    return null;
                }
                Scope s = new Scope();
                for (Type inh : this.inheritances) {
                    if (inh.id.resolvedDef != null) {
                        if (inh.id.resolvedDef instanceof StructDef inhSd) {
                            inhSd.getScopeNoPrivate(s);
                            Scope inhScope2 = inhSd.getInheriteScope();
                            if (inhScope2 != null) {
                                s.addAll(inhScope2);
                            }
                        }
                        else if (inh.id.resolvedDef instanceof TraitDef inhSd) {
                            inhSd.getScopeNoPrivate(s);
                        }
                    }
                }
                inheritScopes = s;
            }
            return inheritScopes;
        }
        
        private void getScopeNoPrivate(Scope scope) {
            for (FieldDef f : fieldDefs) {
                if ((f.flags & FConst.Private) != 0) {
                    continue;
                }
                scope.put(f.name, f);
            }
            for (FuncDef f : funcDefs) {
                if ((f.flags & FConst.Private) != 0) {
                    continue;
                }
                scope.put(f.name, f);
            }
        }
        
        public StructDef parameterize(ArrayList<Type> typeGenericArgs) {
            if (parameterizeCache == null) {
                parameterizeCache = new HashMap<String, StructDef>();
            }
            StringBuilder keySb = new StringBuilder();
            for (Type t : typeGenericArgs) {
                if (!keySb.isEmpty()) {
                    keySb.append(',');
                }
                keySb.append(t.toString());
            }
            String key = keySb.toString();
            StructDef gt = parameterizeCache.get(key);
            if (gt != null) {
                return gt;
            }
            
            StructDef nt = new StructDef(this.comment, this.flags, this.name);
            nt.parent = this.parent;
            nt.genericFrom = this;
            for (FieldDef f : fieldDefs) {
                nt.addSlot(f.parameterize(typeGenericArgs));
            }
            for (FuncDef f : funcDefs) {
                nt.addSlot(f.parameterize(typeGenericArgs));
            }
            parameterizeCache.put(key, nt);
            return nt;
        }
        
        public boolean isInheriteFrom(TypeDef parent) {
            if (this.inheritances == null) {
                return false;
            }
            for (Type t : this.inheritances) {
                if (t.id.resolvedDef == parent) {
                    return true;
                }
                if (t.id.resolvedDef instanceof StructDef sd) {
                    boolean res = sd.isInheriteFrom(parent);
                    if (res) {
                        return true;
                    }
                }
            }
            return false;
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
        
       @Override public void walkChildren(Visitor visitor) {
            for (FieldDef field : enumDefs) {
                visitor.visit(field);
            }
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
        
        public void getScopeNoPrivate(Scope scope) {
            for (FuncDef f : funcDefs) {
                if ((f.flags & FConst.Private) != 0) {
                    continue;
                }
                scope.put(f.name, f);
            }
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
            if (this.prototype.paramDefs != null) {
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
