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
        StructDef typeDef = new AstNode.StructDef(null, 0, name);
        typeDef.loc = loc;
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
            makeBuildinType(scope, "[]");//array
            makeBuildinType(scope, "*");//pointer
            makeBuildinType(scope, "Void");
            makeBuildinType(scope, "...");//varargs
            makeBuildinType(scope, "=>");//func
            
            sizeofFunc(scope);
            offsetofFunc(scope);
            
            buildinScope = scope;
        }
        return buildinScope;
    }
    
    public static HashMap<TokenKind, String> tokenOperator = initOperators();
    public static String getOperator = "get";
    public static String setOperator = "set";
    
    private static HashMap<TokenKind, String> initOperators() {
        HashMap<TokenKind, String> tokenOperator = new HashMap<>();
        
        tokenOperator.put(TokenKind.plus, "plus");
        tokenOperator.put(TokenKind.minus, "minus");
        tokenOperator.put(TokenKind.star, "mult");
        tokenOperator.put(TokenKind.slash, "div");
        tokenOperator.put(TokenKind.cmp, "compare");
        
        return tokenOperator;
    }
}
