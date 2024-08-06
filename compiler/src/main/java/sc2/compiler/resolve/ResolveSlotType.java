/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package sc2.compiler.resolve;

import java.util.ArrayList;
import java.util.HashMap;
import sc2.compiler.CompilerLog;
import sc2.compiler.ast.AstNode;
import sc2.compiler.ast.AstNode.*;
import sc2.compiler.ast.Expr.IdExpr;
import sc2.compiler.ast.*;
import sc2.compiler.ast.SModule.Depend;

/**
 *
 * @author yangjiandong
 */
public class ResolveSlotType implements Visitor {
    
    private ArrayList<Scope> scopes = new ArrayList<>();
    private SModule module;
    private CompilerLog log;
    
    public ResolveSlotType(SModule module, CompilerLog log) {
        this.module = module;
        this.log = log;
    }
    
    public void run() {
        module.walk(this);
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
    
    private SModule importModule(String namespace) {
        //TODO
        return null;
    }
    
    private CompilerLog.CompilerErr err(String msg, Loc loc) {
        return log.err(msg, loc);
    }
    
    private void resolveImportId(IdExpr idExpr) {
        if (idExpr.namespace == null) {
            for (Depend d : module.depends) {
                if (idExpr.namespace.equals(d.name)) {
                    idExpr.resolvedDef = importModule(d.name);
                    return;
                }
            }
            err("Unknow symbol "+idExpr.name, idExpr.loc);
            return;
        }
        resolveImportId(idExpr.namespace);
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
    
    private void resolveId(IdExpr idExpr) {
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
    
    private Scope getImportScope(AstNode.FileUnit v) {
        v.importScope = new Scope();
        for (AstNode.Import i : v.imports) {
            resolveImportId(i.id);
            if (i.id.resolvedDef != null) {
                if (!i.star) {
                    v.importScope.put(i.id.name, i.id.resolvedDef);
                }
                else {
                    if (i.id.resolvedDef instanceof SModule m) {
                        Scope mcope = m.getScope();
                        v.importScope.addAll(mcope);
                    }
                    else if (i.id.resolvedDef instanceof StructDef c) {
                        Scope mcope = c.getScope();
                        v.importScope.addAll(mcope);
                    }
                    else {
                        err("Unsupport ::* for "+i.id.name, i.loc);
                    }
                }
            }
        }
        return v.importScope;
    }
    
    @Override
    public boolean deepLevel() {
        return false;
    }

    @Override
    public void enterUnit(AstNode.FileUnit v) {
        Scope scope1 = getImportScope(v);
        this.scopes.add(scope1);
        
        Scope scope2 = module.getScope();
        this.scopes.add(scope2);
    }

    @Override
    public void exitUnit(AstNode.FileUnit v) {
        this.scopes.clear();
    }


    @Override
    public void enterField(AstNode.FieldDef v) {
        resolveType(v.fieldType);
    }

    @Override
    public void exitField(AstNode.FieldDef v) {

    }

    @Override
    public void enterFunc(AstNode.FuncDef v) {
        if (v.prototype.returnType != null) {
            resolveType(v.prototype.returnType);
        }
        if (v.prototype.paramDefs != null) {
            for (AstNode.ParamDef p : v.prototype.paramDefs) {
                resolveType(p.paramType);
            }
        }
    }

    @Override
    public void exitFunc(AstNode.FuncDef v) {

    }

    @Override
    public void enterTypeDef(AstNode.TypeDef v) {

    }

    @Override
    public void exitTypeDef(AstNode.TypeDef v) {

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
