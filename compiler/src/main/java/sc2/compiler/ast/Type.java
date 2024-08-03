//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sc2.compiler.ast;

import sc2.compiler.ast.AstNode.TypeDef;
import java.util.ArrayList;

/**
 *
 * @author yangjiandong
 */
public class Type extends AstNode {
    public String namespace;
    public String name;
    public ArrayList<Type> genericArgs = null;
    
    //** array size or primitive type sized. the Int32 size is 32
    public int size;
  
    //** Is this is a nullable type (marked with trailing ?)
    public boolean isNullable = false;
    
    public static enum PointerAttr {
        own, ref, raw, weak
    };
    
    public static enum ImutableAttr {
        unknow, imu, mut
    };
    
    public ImutableAttr imutable = ImutableAttr.unknow;
  
    public TypeDef resolvedTypeDef;
    
    public boolean isPointer() {
        return false;
    }
    
    public static Type voidType(Loc loc) {
        Type type = new Type();
        type.loc = loc;
        return type;
    }
    
    public static Type funcType(Loc loc, FuncPrototype prototype) {
        Type type = new Type();
        type.loc = loc;
        return type;
    }
    
    public static Type listType(Loc loc, Type elemType) {
        Type type = new Type();
        type.loc = loc;
        return type;
    }
    
    public static Type arrayRefType(Loc loc, Type elemType) {
        Type type = new Type();
        type.loc = loc;
        return type;
    }
    
    public static Type arrayType(Loc loc, Type elemType, int size) {
        Type type = new Type();
        type.loc = loc;
        type.size = size;
        return type;
    }
    
    public static Type pointerType(Loc loc, Type elemType, PointerAttr pointerAttr) {
        Type type = new Type();
        type.loc = loc;
        return type;
    }
    
    public static Type placeHolder(Loc loc) {
        Type type = new Type();
        type.loc = loc;
        return type;
    }
}
