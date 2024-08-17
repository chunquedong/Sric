//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sc2.compiler.ast;

import sc2.compiler.ast.AstNode.TypeDef;
import java.util.ArrayList;
import java.util.HashMap;
import sc2.compiler.ast.Expr.IdExpr;

/**
 *
 * @author yangjiandong
 */
public class Type extends AstNode {
    public IdExpr id;
    public ArrayList<Type> genericArgs = null;
    
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

        public FuncType(Loc loc, FuncPrototype prototype) {
            super(loc, "=>");
            this.prototype = prototype;
        }
        
        @java.lang.Override
        public String toString() {
            return prototype.toString();
        }
    }
    
    public static class PointerType extends Type {
        public PointerAttr pointerAttr = PointerAttr.ref;
        //** Is this is a nullable type (marked with trailing ?)
        public boolean isNullable = false;
    
        public PointerType(Loc loc, Type elemType, PointerAttr pointerAttr, boolean nullable) {
            super(loc, "*");
            this.genericArgs = new ArrayList<>();
            this.genericArgs.add(elemType);
            this.pointerAttr = pointerAttr;
            this.isNullable = nullable;
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
    
    public static class ArrayType extends Type {
        public Expr sizeExpr;
        
        public ArrayType(Loc loc, Type elemType, Expr sizeExpr) {
            super(loc, "[]");
            this.genericArgs = new ArrayList<>();
            this.genericArgs.add(elemType);
        }
        
        @java.lang.Override
        public String toString() {
            return "["+sizeExpr.toString()+"]"+this.genericArgs.get(0).toString();
        }
    }
    
    public static class NumType extends Type {
        //** primitive type sized. the Int32 size is 32
        public int size = 0;

        //unsigned int
        public boolean isUnsigned = false;
        
        public NumType(Loc loc, String name, int size) {
            super(loc, name);
            this.size = size;
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
            return sb.toString();
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
        FuncType type = new FuncType(loc, prototype);
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, type.loc, null);
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
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, loc, null);
        return type;
    }
    
    public static Type boolType(Loc loc) {
        Type type = new Type(loc, "Bool");
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, loc, null);
        return type;
    }
    
    public static NumType intType(Loc loc) {
        NumType type = new NumType(loc, "Int", 64);
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, loc, null);
        return type;
    }
    
    public static NumType floatType(Loc loc) {
        NumType type = new NumType(loc, "Float", 64);
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, loc, null);
        return type;
    }
    
    public static Type strType(Loc loc) {
        NumType type = new NumType(loc, "Int", 8);
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, type.loc, null);
        type.imutableAttr = ImutableAttr.imu;
        return pointerType(loc, type, PointerAttr.raw, false);
    }

    public static Type arrayType(Loc loc, Type elemType, Expr size) {
        Type type = new ArrayType(loc, elemType, size);
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, type.loc, null);
        return type;
    }
    
    public static PointerType pointerType(Loc loc, Type elemType, PointerAttr pointerAttr, boolean nullable) {
        PointerType type = new PointerType(loc, elemType, pointerAttr, nullable);
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, type.loc, null);
        return type;
    }
    
    public static Type varArgType(Loc loc) {
        Type type = new Type(loc, "...");
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, type.loc, null);
        return type;
    }
    
    public static Type metaType(Loc loc, Type type) {
        MetaType t = new MetaType(loc, type);
        type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, type.loc, null);
        return t;
    }
    
    @java.lang.Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        
        sb.append(id.toString());
        
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

    public Type parameterize(ArrayList<Type> typeGenericArgs) {
        if (!(this.id.resolvedDef instanceof GenericParamDef g) && this.genericArgs == null) {
            return null;
        }
        
        Type nt = new Type(this.id);
        if (this.genericArgs != null) {
            nt.genericArgs = new ArrayList<Type>();
            for (int i=0; i<this.genericArgs.size(); ++i) {
                Type t = this.genericArgs.get(i).parameterize(typeGenericArgs);
                nt.genericArgs.set(i, t);
            }
        }
        if (this.id.resolvedDef instanceof GenericParamDef g) {
            if (g.index < typeGenericArgs.size()) {
                Type at = typeGenericArgs.get(g.index);
                if (at != null) {
                    nt.id = at.id;
                }
            }
        }
        return nt;
    }
}
