//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sc2.compiler.ast;

import sc2.compiler.ast.AstNode.*;
import java.util.ArrayList;
import sc2.compiler.ast.Token.TokenKind;

/**
 *
 * @author yangjiandong
 */
public class ClosureExpr extends Expr {
    public FuncPrototype prototype = new FuncPrototype();// function signature
    public Block code;             // code block
    public ArrayList<Expr> captures;
    public TokenKind defaultCapture;
}
