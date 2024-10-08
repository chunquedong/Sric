/*
 * see license.txt
 */
package sric.lsp;

import sric.compiler.CompilePass;
import sric.compiler.CompilerLog;
import sric.compiler.ast.AstNode;
import sric.compiler.ast.AstNode.FileUnit;
import sric.compiler.ast.Expr;
import sric.compiler.ast.FConst;
import sric.compiler.ast.Stmt;
import sric.compiler.ast.Type;
import sric.lsp.JsonRpc.*;

/**
 * Finds the Ast node
 */
public class AstFinder extends CompilePass {

    private int pos;
    private AstNode found;

    public AstFinder(CompilerLog log) {
        super(log);
    }
    
    public AstNode findSourceNode(FileUnit file, int pos) {
        this.pos = pos;
        found = null;
        visitUnit(file);
        return found;
    }
    
    boolean isContains(AstNode v) {
        if (v.len == 0) {
            return pos == v.loc.offset;
        }
        if (pos >= v.loc.offset && pos < v.loc.offset + v.len) {
            return true;
        }
        return false;
    }

    @Override
    public void visitUnit(AstNode.FileUnit v) {
        v.walkChildren(this);
    }

    @Override
    public void visitField(AstNode.FieldDef v) {
        if (isContains(v)) {
            found = v;
            visitType(v.fieldType);
        }
    }
    
    boolean visitType(Type type) {
        if (type == null) 
            return false;
        if (isContains(type)) {
            found = type;
            if (type.genericArgs != null) {
                for (var p : type.genericArgs) {
                    if (visitType(p)) {
                        return true;
                    }
                }
            }
            return true;
        }
        return false;
    }
    
    void visitFuncPrototype(AstNode v, AstNode.FuncPrototype prototype) {
        if (prototype.paramDefs != null) {
            for (var p : prototype.paramDefs) {
                if (isContains(p)) {
                    found = v;
                    visitType(p.paramType);
                }
            }
        }
    }

    @Override
    public void visitFunc(AstNode.FuncDef v) {
        if (!isContains(v)) {
            return;
        }
        found = v;
        
        visitFuncPrototype(v, v.prototype);
        
        if (v.code != null) {
            this.visit(v.code);
        }
    }

    @Override
    public void visitTypeDef(AstNode.TypeDef v) {
        if (!isContains(v)) {
            return;
        }
        found = v;
        
        if (v instanceof AstNode.StructDef sd) {
            if (sd.generiParamDefs != null) {
                for (var p: sd.generiParamDefs) {
                    if (isContains(p)) {
                        found = v;
                    }
                }
            }
            
            if (sd.inheritances != null) {
                for (Type inh : sd.inheritances) {
                    visitType(inh);
                }
            }
        }
        
        v.walkChildren(this);
    }

    @Override
    public void visitStmt(Stmt v) {
        if (!isContains(v)) {
            return;
        }
        found = v;
        
        if (v instanceof AstNode.Block bs) {
            bs.walkChildren(this);
        }
        else if (v instanceof Stmt.IfStmt ifs) {
            this.visit(ifs.condition);
            this.visit(ifs.block);
            if (ifs.elseBlock != null) {
                this.visit(ifs.elseBlock);
            }
        }
        else if (v instanceof Stmt.LocalDefStmt e) {
            this.visit(e.fieldDef);
        }
        else if (v instanceof Stmt.WhileStmt whiles) {
            this.visit(whiles.condition);
            this.visit(whiles.block);
        }
        else if (v instanceof Stmt.ForStmt fors) {
            if (fors.init != null) {
                if (fors.init instanceof Stmt.LocalDefStmt varDef) {
                    this.visit(varDef.fieldDef);
                }
                else if (fors.init instanceof Stmt.ExprStmt s) {
                    this.visit(s.expr);
                }
            }
            
            if (fors.condition != null) {
                this.visit(fors.condition);
            }
            
            if (fors.update != null) {
                this.visit(fors.update);
            }
            this.visit(fors.block);
        }
        else if (v instanceof Stmt.SwitchStmt switchs) {
            this.visit(switchs.condition);
            
            for (Stmt.CaseBlock cb : switchs.cases) {
                this.visit(cb.caseExpr);
                this.visit(cb.block);
            }
            
            if (switchs.defaultBlock != null) {
                this.visit(switchs.defaultBlock);
            }
        }
        else if (v instanceof Stmt.ExprStmt exprs) {
            this.visit(exprs.expr);
        }
        else if (v instanceof Stmt.JumpStmt jumps) {
            
        }
        else if (v instanceof Stmt.UnsafeBlock bs) {

            this.visit(bs.block);
        }
        else if (v instanceof Stmt.ReturnStmt rets) {
            if (rets.expr != null) {
                this.visit(rets.expr);
            }
        }
        else {
            //err("Unkown stmt:"+v, v.loc);
        }
    }

    @Override
    public void visitExpr(Expr v) {
        if (!isContains(v)) {
            return;
        }
        found = v;

        if (v instanceof Expr.IdExpr e) {
            if (e.namespace != null) {
                this.visit(e.namespace);
            }
        }
        else if (v instanceof Expr.AccessExpr e) {
            this.visit(e.target);
        }
        else if (v instanceof Expr.LiteralExpr e) {
        }
        else if (v instanceof Expr.BinaryExpr e) {
            this.visit(e.lhs);
            this.visit(e.rhs);
        }
        else if (v instanceof Expr.CallExpr e) {
            this.visit(e.target);
            if (e.args != null) {
                int i = 0;
                for (Expr.CallArg t : e.args) {
                    this.visit(t.argExpr);
                    ++i;
                }
            }
        }
        else if (v instanceof Expr.UnaryExpr e) {
            this.visit(e.operand);
        }
        else if (v instanceof Expr.TypeExpr e) {
            visitType(e.type);
        }
        else if (v instanceof Expr.IndexExpr e) {
            this.visit(e.target);
            this.visit(e.index);
        }
        else if (v instanceof Expr.GenericInstance e) {
            this.visit(e.target);
            for (Type t : e.genericArgs) {
                this.visit(t);
            }
        }
        else if (v instanceof Expr.IfExpr e) {
            this.visit(e.condition);
            this.visit(e.trueExpr);
            this.visit(e.falseExpr);
        }
        else if (v instanceof Expr.InitBlockExpr e) {
            for (Expr.CallArg t : e.args) {
                this.visit(t.argExpr);
            }
        }
        else if (v instanceof Expr.ClosureExpr e) {
            visitFuncPrototype(e, e.prototype);
            this.visit(e.code);
        }
        else if (v instanceof Expr.NonNullableExpr e) {
            this.visit(e.operand);
        }
        else {
            //err("Unkown expr:"+v, v.loc);
        }

    }
}
