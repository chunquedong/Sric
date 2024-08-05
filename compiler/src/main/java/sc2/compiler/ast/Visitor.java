//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sc2.compiler.ast;

import sc2.compiler.ast.AstNode.*;

/**
 *
 * @author yangjiandong
 */
public interface Visitor {
    
    boolean deepLevel();

    void enterUnit(FileUnit v);
    void exitUnit(FileUnit v);
    
    void enterField(FieldDef v);
    void exitField(FieldDef v);
    
    void enterFunc(FuncDef v);
    void exitFunc(FuncDef v);
    
    void enterTypeDef(TypeDef v);
    void exitTypeDef(TypeDef v);
    
    void enterStmt(Stmt v);
    void exitStmt(Stmt v);
    
    void enterExpr(Stmt v);
    void exitExpr(Stmt v);
}
