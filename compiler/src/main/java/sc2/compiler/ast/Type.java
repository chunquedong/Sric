//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sc2.compiler.ast;

import sc2.compiler.ast.AstNode.TypeDef;
import java.util.ArrayList;
import sc2.compiler.ast.Expr.IdExpr;

/**
 *
 * @author yangjiandong
 */
public class Type extends AstNode {
    public IdExpr id;
    public ArrayList<Type> genericArgs = null;
    
    //** array size or primitive type sized. the Int32 size is 32
    public int size = 0;
    
    //unsigned int
    public boolean isUnsigned = false;
    
    public static enum PointerAttr {
        own, ref, raw, weak
    };
    
    public static enum ImutableAttr {
        auto, imu, mut
    };
        
    public ImutableAttr imutableAttr = ImutableAttr.auto;
    
    public static class FuncType extends Type {
        public FuncPrototype prototype;
        public FuncDef funcDef = null;

        public FuncType(Loc loc, String name) {
            super(loc, name);
        }
        
        @java.lang.Override
        public String toString() {
            return prototype.toString();
        }
    }
    
    public static class PointerType extends Type {
        public FuncPrototype prototype;
        public PointerAttr pointerAttr = PointerAttr.ref;
        //** Is this is a nullable type (marked with trailing ?)
        public boolean isNullable = false;
    
        public PointerType(Loc loc, String name) {
            super(loc, name);
        }
        
        @java.lang.Override
        public String toString() {
            String t = super.toString();
            return pointerAttr + "*" + t;
        }
    }
    
    public static class MetaType extends Type {
        public Type type;
        public MetaType(Loc loc, Type type) {
            super(loc, "Type");
            this.type = type;
        }
        
        @java.lang.Override
        public String toString() {
            return type.toString();
        }
    }
    
    public Type(IdExpr id) {
        this.id = id;
        this.loc = id.loc;
    }
    
    public Type(Loc loc, String name) {
        this.loc = loc;
        this.id = new IdExpr(name);
        this.id.loc = loc;
    }
    
    public boolean isVoid() {
        if (id.namespace != null) {
            return false;
        }
        return id.name.equals("Void");
    }
    
    public boolean isBool() {
        if (id.namespace != null) {
            return false;
        }
        return id.name.equals("Bool");
    }
    
    public boolean isInt() {
        if (id.namespace != null) {
            return false;
        }
        return id.name.equals("Int");
    }
    
    public boolean isFloat() {
        if (id.namespace != null) {
            return false;
        }
        return id.name.equals("Float");
    }
    
    public boolean isArray() {
        if (id.namespace != null) {
            return false;
        }
        return id.name.equals("[]");
    }
    
    public boolean isMetaType() {
        return this instanceof MetaType;
    }
    
    public boolean isPointerType() {
        return this instanceof PointerType;
    }
    
    public boolean isFuncType() {
        return this instanceof FuncType;
    }
    
    public boolean fit(Type target) {
        if (this == target) {
            return true;
        }
        if (this.id.namespace == target.id.namespace && this.id.name.equals(target.id.name)) {
            return true;
        }
        if (this.id.resolvedDef == target.id.resolvedDef) {
            return true;
        }
        return false;
    }
    
    public boolean equals(Type target) {
        if (this == target) {
            return true;
        }
        if (this.id.namespace == target.id.namespace && this.id.name.equals(target.id.name)) {
            return true;
        }
        if (this.id.resolvedDef == target.id.resolvedDef) {
            return true;
        }
        return false;
    }
        
    public static FuncType funcType(Loc loc, FuncPrototype prototype) {
        FuncType type = new FuncType(loc, "=>");
        type.prototype = prototype;
        return type;
    }
    
    public static Type funcType(FuncDef f) {
        FuncType type = funcType(f.loc, f.prototype);
        type.funcDef = f;
        return type;
    }
    
    public static Type funcType(ClosureExpr f) {
        FuncType type = funcType(f.loc, f.prototype);
        return type;
    }
    
    public static Type voidType(Loc loc) {
        Type type = new Type(loc, "Void");
        return type;
    }
    
    public static Type boolType(Loc loc) {
        Type type = new Type(loc, "Bool");
        return type;
    }
    
    public static Type intType(Loc loc) {
        Type type = new Type(loc, "Int");
        type.size = 64;
        return type;
    }
    
    public static Type floatType(Loc loc) {
        Type type = new Type(loc, "Float");
        type.size = 64;
        return type;
    }
    
    public static Type strType(Loc loc) {
        Type type = new Type(loc, "Int");
        type.size = 8;
        type.imutableAttr = ImutableAttr.imu;
        return pointerType(loc, type, PointerAttr.raw, false);
    }

    public static Type arrayType(Loc loc, Type elemType, int size) {
        Type type = new Type(loc, "[]");
        type.size = size;
        type.genericArgs = new ArrayList<>();
        type.genericArgs.add(elemType);
        return type;
    }
    
    public static PointerType pointerType(Loc loc, Type elemType, PointerAttr pointerAttr, boolean nullable) {
        PointerType type = new PointerType(loc, "*");
        type.isNullable = nullable;
        type.pointerAttr = pointerAttr;
        type.genericArgs = new ArrayList<>();
        type.genericArgs.add(elemType);
        return type;
    }
    
    public static Type varArgType(Loc loc) {
        Type type = new Type(loc, "...");
        return type;
    }
    
    public static Type metaType(Loc loc, Type type) {
        MetaType t = new MetaType(loc, type);
        return t;
    }
    
    @java.lang.Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        if (this.isUnsigned) {
            sb.append("U");
        }
        
        sb.append(id.toString());
        
        if (size != 0) {
            sb.append(size);
        }
        
        if (this.genericArgs != null) {
            sb.append("$<");
            int i = 0;
            for (Type t : this.genericArgs) {
                if (i > 0) {
                    sb.append(", ");
                }
                sb.append(t.toString());
                ++i;
            }
            sb.append(">");
        }
        
        return sb.toString();
    }

}
