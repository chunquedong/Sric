//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sric.compiler.resolve;

import java.util.ArrayList;
import java.util.HashMap;
import sric.compiler.CompilePass;
import sric.compiler.CompilerLog;
import sric.compiler.Compiler;
import sric.compiler.ast.AstNode;
import sric.compiler.ast.AstNode.*;
import sric.compiler.ast.Expr.IdExpr;
import sric.compiler.ast.SModule.Depend;
import sric.compiler.ast.*;
/**
 *
 * @author yangjiandong
 */
public class TopLevelTypeResolver extends TypeResolver {
    
    private Compiler compiler;
    
    public TopLevelTypeResolver(CompilerLog log, SModule module, Compiler compiler) {
        super(log, module);
        this.compiler = compiler;
    }
    
    public void run() {
        module.walkChildren(this);
    }

    private void resolveImportId(IdExpr idExpr) {
        if (idExpr.namespace == null) {
            for (Depend d : module.depends) {
                if (idExpr.name.equals(d.name)) {
                    if (d.cache == null) {
                        d.cache = compiler.importModule(d.name, d.version);
                    }
                    idExpr.resolvedDef = d.cache;
                    return;
                }
            }
            err("Unknow depends "+idExpr.name, idExpr.loc);
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
        else if (idExpr.namespace.resolvedDef instanceof TypeDef m) {
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

    private void importToScope(AstNode.Import i, Scope importScope) {

        resolveImportId(i.id);
        
        if (i.id.resolvedDef != null) {
            if (!i.star) {
                importScope.put(i.id.name, i.id.resolvedDef);
            }
            else {
                if (i.id.resolvedDef instanceof SModule m) {
                    Scope mcope = m.getScope();
                    importScope.addAll(mcope);
                }
                else if (i.id.resolvedDef instanceof StructDef c) {
                    Scope mcope = c.getScope();
                    importScope.addAll(mcope);
                }
                else {
                    err("Unsupport ::* for "+i.id.name, i.loc);
                }
            }
        }
    }
    

    @Override
    public void visitUnit(AstNode.FileUnit v) {
        v.importScope = new Scope();

        for (AstNode.Import i : v.imports) {
            importToScope(i, v.importScope);
        }
        this.scopes.add(v.importScope);
        
        Scope scope2 = module.getScope();
        this.scopes.add(scope2);
        
        this.scopes.add(Buildin.getBuildinScope());
        
        v.walkChildren(this);
        
        this.scopes.clear();
    }

    @Override
    public void visitField(AstNode.FieldDef v) {
        if (v.parent instanceof EnumDef d) {
            Type self = new Type(d.loc, d.name);
            self.id.resolvedDef = d;
            v.fieldType = self;
        }
        resolveTopLevelType(v.fieldType, v.loc);
    }

    @Override
    public void visitFunc(AstNode.FuncDef v) {
        Scope gpScope = null;
        if (v.generiParamDefs != null) {
            for (GenericParamDef gp : v.generiParamDefs) {
                resolveTopLevelType(gp.bound, gp.loc);
            }
            gpScope = pushScope();
            for (GenericParamDef gp : v.generiParamDefs) {
                gpScope.put(gp.name, gp);
            }
            this.scopes.add(gpScope);
        }
        
        resolveTopLevelType(v.prototype.returnType, v.loc);
        if (v.prototype.paramDefs != null) {
            for (AstNode.ParamDef p : v.prototype.paramDefs) {
                resolveTopLevelType(p.paramType, p.loc);
            }
        }
        
        if (gpScope != null) {
            this.popScope();
        }
    }

    @Override
    public void visitTypeDef(AstNode.TypeDef v) {
        Scope gpScope = null;
        if (v instanceof StructDef sd) {
            if (sd.generiParamDefs != null) {
                for (GenericParamDef gp : sd.generiParamDefs) {
                    resolveTopLevelType(gp.bound, gp.loc);
                }
                gpScope = new Scope();
                for (GenericParamDef gp : sd.generiParamDefs) {
                    gpScope.put(gp.name, gp);
                }
                this.scopes.add(gpScope);
            }
            if (sd.inheritances != null) {
                for (Type inh : sd.inheritances) {
                    this.resolveTopLevelType(inh, inh.loc);
                }
            }
        }
        v.walkChildren(this);
        
        if (gpScope != null) {
            this.popScope();
        }
    }
    
    @Override
    public void visitTypeAlias(TypeAlias v) {
        this.resolveTopLevelType(v.type, v.loc);
    }

    @Override
    public void visitStmt(Stmt v) {
        
    }

    @Override
    public void visitExpr(Expr v) {
        
    }
}
