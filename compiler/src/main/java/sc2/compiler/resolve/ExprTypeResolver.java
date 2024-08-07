//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sc2.compiler.resolve;

import sc2.compiler.ast.Scope;
import java.util.ArrayList;
import java.util.HashMap;
import sc2.compiler.CompilerLog;
import sc2.compiler.ast.AstNode;
import sc2.compiler.ast.AstNode.*;
import sc2.compiler.ast.*;

/**
 *
 * @author yangjiandong
 */
public class ExprTypeResolver implements Visitor {
    
    private ArrayList<Scope> scopes = new ArrayList<>();
    private SModule module;
    public CompilerLog log;
    
    public ExprTypeResolver(SModule module, CompilerLog log) {
        this.module = module;
        this.log = log;
    }
    
    public void run() {
        module.walk(this);
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
    
    private CompilerLog.CompilerErr err(String msg, Loc loc) {
        return log.err(msg, loc);
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
    public boolean deepLevel() {
        return true;
    }

    @Override
    public void enterUnit(FileUnit v) {
        scopes.add(v.importScope);
        scopes.add(module.getScope());
        this.scopes.add(Buildin.getBuildinScope());
    }

    @Override
    public void exitUnit(FileUnit v) {
        popScope();
        popScope();
        popScope();
    }

    @Override
    public void enterField(FieldDef v) {
    }

    @Override
    public void exitField(FieldDef v) {
    }

    @Override
    public void enterFunc(FuncDef v) {
    }

    @Override
    public void exitFunc(FuncDef v) {
    }

    @Override
    public void enterTypeDef(TypeDef v) {
    }

    @Override
    public void exitTypeDef(TypeDef v) {
    }

    @Override
    public void enterStmt(Stmt v) {
    }

    @Override
    public void exitStmt(Stmt v) {
    }

    @Override
    public void enterExpr(Stmt v) {
    }

    @Override
    public void exitExpr(Stmt v) {
    }
    
}
