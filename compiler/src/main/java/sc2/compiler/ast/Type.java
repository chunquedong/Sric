//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sc2.compiler.ast;

import sc2.compiler.ast.AstNode.TypeDef;
import java.util.ArrayList;
import java.util.HashMap;
import sc2.compiler.ast.Expr.ClosureExpr;
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
    
    public boolean explicitImutable = false;
    public boolean isImutable = false;
    
    public static class FuncType extends Type {
        public FuncPrototype prototype;
        public FuncDef funcDef = null;

        public FuncType(Loc loc, FuncPrototype prototype) {
            super(loc, "=>");
            this.prototype = prototype;
            this.genericArgs = new ArrayList<>();
            this.genericArgs.add(prototype.returnType);
            if (prototype.paramDefs != null) {
                for (ParamDef p : prototype.paramDefs) {
                    this.genericArgs.add(p.paramType);
                }
            }
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
        
        public PointerType toNonNullable() {
            PointerType n = new PointerType(loc, this.genericArgs.get(0), this.pointerAttr, false);
            n.id = this.id;
            return n;
        }
        
        @java.lang.Override
        public String toString() {
            String t = super.toString();
            return pointerAttr + "*" + t;
        }
        
        @Override
        protected boolean checkEquals(Type target) {
            if (target instanceof PointerType a) {
                return (this.pointerAttr == a.pointerAttr) && (this.isNullable == a.isNullable);
            }
            else {
                return false;
            }
        }
        
        @Override
        public boolean fit(Type target) {
            if (this.isImutable && !target.isImutable) {
                return false;
            }
            if (equals(target)) {
                return true;
            }
            
            if (target instanceof PointerType a) {
                if ((this.pointerAttr == a.pointerAttr) && (this.isNullable == a.isNullable)) {
                    //ok
                }
                else {
                    if (this.isNullable && !a.isNullable) {
                        //error to nonnullable
                        return false;
                    }
                    
                    if (this.pointerAttr == PointerAttr.own && (a.pointerAttr == PointerAttr.ref || a.pointerAttr == PointerAttr.raw)) {
                        //ok
                    }
                    else if (this.pointerAttr == PointerAttr.ref && (a.pointerAttr == PointerAttr.raw)) {
                        //ok
                    }
                    else {
                        return false;
                    }
                }

                if (!genericArgsEquals(target)) {
                    return false;
                }

                if (this.id.resolvedDef == target.id.resolvedDef) {
                    return true;
                }

                if (this.id.resolvedDef != null && target.id.resolvedDef != null) {
                    if (this.id.resolvedDef instanceof StructDef sd && target.id.resolvedDef instanceof StructDef td) {
                        if (sd.genericFrom == td.genericFrom || sd == td.genericFrom || sd.genericFrom == td) {
                            return true;
                        }
                    }
                }

                if (this.id.resolvedDef != null && target.id.resolvedDef != null) {
                    if (this.id.resolvedDef instanceof StructDef sd && target.id.resolvedDef instanceof TypeDef td) {
                        if (sd.isInheriteFrom(td)) {
                            return true;
                        }
                    }
                }
            }

            return false;
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
     
        @Override
        protected boolean checkEquals(Type target) {
            if (target instanceof ArrayType a) {
                return this.sizeExpr == a.sizeExpr;
            }
            else {
                return false;
            }
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
        
        @Override
        protected boolean checkEquals(Type target) {
            if (target instanceof NumType a) {
                return this.isUnsigned == a.isUnsigned;
            }
            else {
                return false;
            }
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
        if (this.isImutable && !target.isImutable) {
            return false;
        }
        return equals(target);
    }
    
    public boolean equals(Type target) {
        if (this == target) {
            return true;
        }
        
        if (!genericArgsEquals(target)) {
            return false;
        }
        
        if (!checkEquals(target)) {
            return false;
        }

        if (this.id.resolvedDef == target.id.resolvedDef) {
            return true;
        }
        
        if (this.id.resolvedDef != null && target.id.resolvedDef != null) {
            if (this.id.resolvedDef instanceof StructDef sd && target.id.resolvedDef instanceof StructDef td) {
                if (sd.genericFrom == td.genericFrom) {
                    return true;
                }
            }
        }
        return false;
    }
    
    protected boolean checkEquals(Type target) {
        return true;
    }
    
    protected boolean genericArgsEquals(Type target) {
        if (this.genericArgs != null || target.genericArgs != null) {
            if (this.genericArgs == null || target.genericArgs == null) {
                return false;
            }
            if (this.genericArgs.size() != target.genericArgs.size()) {
                return false;
            }
            for (int i=0; i<this.genericArgs.size(); ++i) {
                if (!this.genericArgs.get(i).equals(target.genericArgs.get(i))) {
                    return false;
                }
            }
        }
        return true;
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
        type.isImutable = true;
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
        if (type.id.resolvedDef == null && type.id.namespace == null) {
            type.id.resolvedDef = Buildin.getBuildinScope().get(type.id.name, type.loc, null);
        }
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
    
    public void setToImutable() {
        if (this.explicitImutable) {
            return;
        }
        this.isImutable = true;
        if (this instanceof PointerType) {
            this.genericArgs.get(0).setToImutable();
        }
    }
}
