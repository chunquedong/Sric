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
    public ArrayList<Type> genericArgs;
    
    //  ** for sized primitive type. the Int32's extName is 32
    public int size;
  
    //** Is this is a nullable type (marked with trailing ?)
    public boolean isNullable = false;
  
    public TypeDef resolvedTypeDef;
    
    public static Type voidType(Loc loc) {
        Type type = new Type();
        type.loc = loc;
        return type;
    }
    
    public static Type listType(Loc loc, Type elemType) {
        Type type = new Type();
        type.loc = loc;
        return type;
    }
}
