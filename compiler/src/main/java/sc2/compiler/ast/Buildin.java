/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sc2.compiler.ast;

import java.util.ArrayList;
import java.util.HashMap;
import sc2.compiler.ast.AstNode.*;
import sc2.compiler.ast.Token.TokenKind;

/**
 *
 * @author yangjiandong
 */
public class Buildin {
        
    private static Scope buildinScope;
    
    private static Loc loc = new Loc("buildin", 0, 0, 0);

    private static TypeDef makeBuildinType(Scope scope, String name) {
        return makeBuildinType(scope, name, null);
    }
    private static TypeDef makeBuildinType(Scope scope, String name, ArrayList<GenericParamDef> gps) {
        StructDef typeDef = new AstNode.StructDef(null, 0, name);
        typeDef.loc = loc;
        typeDef.generiParamDefs = gps;
        scope.put(name, typeDef);
        return typeDef;
    }
    
    private static FuncDef sizeofFunc(Scope scope) {
        FuncDef f = new FuncDef();
        f.loc = loc;
        f.name = "sizeof";
        f.prototype.returnType = Type.intType(loc);
        f.prototype.paramDefs = new ArrayList<ParamDef>();
        ParamDef param = new ParamDef();
        param.loc = loc;
        param.name = "type";
        param.paramType = Type.metaType(loc, Type.voidType(loc));
        f.prototype.paramDefs.add(param);
        
        scope.put(f.name, f);
        return f;
    }
    
    private static FuncDef offsetofFunc(Scope scope) {
        FuncDef f = new FuncDef();
        f.loc = loc;
        f.name = "offsetof";
        f.prototype.returnType = Type.intType(loc);
        f.prototype.paramDefs = new ArrayList<ParamDef>();
        ParamDef param = new ParamDef();
        param.loc = loc;
        param.name = "type";
        param.paramType = Type.metaType(loc, Type.voidType(loc));
        f.prototype.paramDefs.add(param);
        
        ParamDef param2 = new ParamDef();
        param2.loc = loc;
        param2.name = "field";
        param2.paramType = Type.metaType(loc, Type.voidType(loc));
        f.prototype.paramDefs.add(param2);
        
        scope.put(f.name, f);
        return f;
    }
    
    public static Scope getBuildinScope() {
        if (buildinScope == null) {
            Scope scope = new Scope();

            makeBuildinType(scope, "Int");
            makeBuildinType(scope, "Bool");
            makeBuildinType(scope, "Float");
            
            ArrayList<GenericParamDef> gps = new ArrayList<GenericParamDef>();
            GenericParamDef gp = new GenericParamDef();
            gp.name = "T";
            gps.add(gp);
            makeBuildinType(scope, "[]", gps);//array
            
            ArrayList<GenericParamDef> gps2 = new ArrayList<GenericParamDef>();
            GenericParamDef gp2 = new GenericParamDef();
            gp2.name = "T";
            gps2.add(gp2);
            makeBuildinType(scope, "*", gps2);//pointer
            
            makeBuildinType(scope, "Void");
            makeBuildinType(scope, "...");//varargs
            makeBuildinType(scope, "=>");//func
            
            buildinScope = scope;
            
            sizeofFunc(scope);
            offsetofFunc(scope);
            
        }
        return buildinScope;
    }
    
    public static final String getOperator = "get";
    public static final String setOperator = "set";
    
    public static String operatorToName(TokenKind tok) {
        switch (tok) {
            case plus:
                return "plus";
            case minus:
                return "minus";
            case star:
                return "mult";
            case slash:
                return "div";
            case cmp:
                return "compare";
        }
        return null;
    }
}
