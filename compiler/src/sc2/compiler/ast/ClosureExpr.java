//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sc2.compiler.ast;

import sc2.compiler.ast.AstNode.FieldDef;
import sc2.compiler.ast.AstNode.FuncDef;
import java.util.ArrayList;

/**
 *
 * @author yangjiandong
 */
public class ClosureExpr extends Expr {
    public AstNode.FuncPrototype prototype;// function signature
    public AstNode.Block code;             // code block
    public ArrayList<FieldDef> captures;
}
