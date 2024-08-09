//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sc2.compiler.resolve;

import sc2.compiler.ast.Scope;
import java.util.ArrayList;
import java.util.HashMap;
import sc2.compiler.CompilePass;
import sc2.compiler.CompilerLog;
import sc2.compiler.ast.AstNode;
import sc2.compiler.ast.AstNode.*;
import sc2.compiler.ast.*;

/**
 *
 * @author yangjiandong
 */
public class ExprTypeResolver extends CompilePass {
    
    private ArrayList<Scope> scopes = new ArrayList<>();
    private SModule module;
    
    public ExprTypeResolver(CompilerLog log, SModule module) {
        super(log);
        this.module = module;
        this.log = log;
    }
    
    public void run() {
        module.walkChildren(this);
    }
    
    private Scope pushScope() {
        Scope s = new Scope();
        scopes.add(s);
        return s;
    }
    
    private Scope popScope() {
        return scopes.remove(scopes.size()-1);
    }
    
    private AstNode findSymbol(String name, Loc loc) {
        for (Scope scope : scopes) {
            AstNode node = scope.get(name, loc, log);
            if (node != null) {
                return node;
            }
        }
        err("Unknow symbol "+name, loc);
        return null;
    }
    
    private void resolveId(Expr.IdExpr idExpr) {
        if (idExpr.namespace == null) {
            idExpr.resolvedDef = findSymbol(idExpr.name, idExpr.loc);
            return;
        }
        resolveId(idExpr.namespace);
        if (idExpr.namespace.resolvedDef == null) {
            return;
        }
        if (idExpr.namespace.resolvedDef instanceof SModule m) {
            AstNode node = m.getScope().get(idExpr.name, idExpr.loc, log);
            if (node == null) {
                err("Unknow symbol "+idExpr.name, idExpr.loc);
            }
            idExpr.resolvedDef = node;
            return;
        }
        else if (idExpr.namespace.resolvedDef instanceof StructDef m) {
            AstNode node = m.getScope().get(idExpr.name, idExpr.loc, log);
            if (node == null) {
                err("Unknow symbol "+idExpr.name, idExpr.loc);
            }
            idExpr.resolvedDef = node;
            return;
        }
        else {
            err("Unsupport :: for "+idExpr.namespace.name, idExpr.loc);
        }
    }
        
    private void resolveType(Type type) {
        resolveId(type.id);
        if (type.id.resolvedDef != null) {
            if (!(type.id.resolvedDef instanceof TypeDef)) {
                type.id.resolvedDef = null;
                err("It's not a type "+type.id.name, type.loc);
            }
            return;
        }
    }

    @Override
    public void visitUnit(FileUnit v) {
        scopes.add(v.importScope);
        scopes.add(module.getScope());
        this.scopes.add(Buildin.getBuildinScope());
        
        v.walkChildren(this);
        
        popScope();
        popScope();
        popScope();
    }

    @Override
    public void visitField(FieldDef v) {
    }

    @Override
    public void visitFunc(FuncDef v) {
    }

    @Override
    public void visitTypeDef(TypeDef v) {
        v.walkChildren(this);
    }

    @Override
    public void visitStmt(Stmt v) {
    }

    @Override
    public void visitExpr(Expr v) {
    }

}
