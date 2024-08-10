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

        public FuncType(Loc loc, String name) {
            super(loc, name);
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
    
    public static Type voidType(Loc loc) {
        Type type = new Type(loc, "Void");
        return type;
    }
    
    public static Type boolType(Loc loc) {
        Type type = new Type(loc, "Bool");
        return type;
    }
    
    public static FuncType funcType(Loc loc, FuncPrototype prototype) {
        FuncType type = new FuncType(loc, "=>");
        type.prototype = prototype;
        return type;
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
    
//    public static Type placeHolder(Loc loc) {
//        Type type = new Type(loc, "PlaceHolder");
//        type.loc = loc;
//        return type;
//    }

}
