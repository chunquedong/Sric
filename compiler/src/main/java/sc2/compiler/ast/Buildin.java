/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sc2.compiler.ast;

import sc2.compiler.ast.AstNode.*;

/**
 *
 * @author yangjiandong
 */
public class Buildin {
        
    private static Scope buildinScope;
    
    private static TypeDef makeBuindType(Scope scope, String name) {
        StructDef typeDef = new AstNode.StructDef(null, 0, name);
        typeDef.loc = new Loc("buildin", 0, 0, 0);
        scope.put(name, typeDef);
        return typeDef;
    }
    
    public static Scope getBuildinScope() {
        if (buildinScope == null) {
            Scope scope = new Scope();

            makeBuindType(scope, "Int");
            makeBuindType(scope, "Bool");
            makeBuindType(scope, "Float");
            makeBuindType(scope, "[]");//array
            makeBuindType(scope, "*");//pointer
            makeBuindType(scope, "Void");
            makeBuindType(scope, "...");//varargs
            makeBuindType(scope, "=>");//func
            buildinScope = scope;
        }
        return buildinScope;
    }
}
