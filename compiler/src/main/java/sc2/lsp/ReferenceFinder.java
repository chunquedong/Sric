/*
 * see license.txt
 */
package sc2.lsp;

import java.util.*;
import java.util.stream.Collectors;
import sc2.compiler.CompilePass;
import sc2.compiler.CompilerLog;
import sc2.compiler.ast.AstNode;
import sc2.compiler.ast.AstNode.FileUnit;
import sc2.compiler.ast.Expr;
import sc2.compiler.ast.Expr.IdExpr;
import sc2.compiler.ast.SModule;
import sc2.compiler.ast.Stmt;
import sc2.compiler.ast.Type;

import sc2.lsp.JsonRpc.*;

public class ReferenceFinder extends CompilePass {

    private ArrayList<AstNode> refs = new ArrayList<AstNode>();
    private AstNode symDef;

    public ReferenceFinder(CompilerLog log) {
        super(log);
    }
    
    public ArrayList<AstNode> findRefs(SModule module, AstNode symDef) {
        this.symDef = symDef;
        refs.clear();
        module.walkChildren(this);
        return refs;
    }

    @Override
    public void visitUnit(AstNode.FileUnit v) {
        v.walkChildren(this);
    }

    @Override
    public void visitField(AstNode.FieldDef v) {
        visitType(v.fieldType);
    }
    
    void visitId(IdExpr e) {
        if (e.resolvedDef == symDef) {
            refs.add(e);
        }
    }
    
    void visitType(Type type) {
        if (type == null) 
            return;
        
        visitId(type.id);
        
        if (type.genericArgs != null) {
            for (var p : type.genericArgs) {
                visitType(p);
            }
        }
    }
    
    void visitFuncPrototype(AstNode v, AstNode.FuncPrototype prototype) {
        if (prototype.paramDefs != null) {
            for (var p : prototype.paramDefs) {
                visitType(p.paramType);
            }
        }
    }

    @Override
    public void visitFunc(AstNode.FuncDef v) {

        visitFuncPrototype(v, v.prototype);
        
        if (v.code != null) {
            this.visit(v.code);
        }
    }

    @Override
    public void visitTypeDef(AstNode.TypeDef v) {

        if (v instanceof AstNode.StructDef sd) {
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
        if (v instanceof Expr.IdExpr e) {
            if (e.namespace != null) {
                this.visit(e.namespace);
            }
            this.visitId(e);
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
        else if (v instanceof Expr.OptionalExpr e) {
            this.visit(e.operand);
        }
        else {
            //err("Unkown expr:"+v, v.loc);
        }

    }
}

