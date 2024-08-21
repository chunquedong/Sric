//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sc2.compiler.resolve;

import java.util.ArrayList;
import java.util.HashMap;
import sc2.compiler.CompilePass;
import sc2.compiler.CompilerLog;
import sc2.compiler.Compiler;
import sc2.compiler.ast.AstNode;
import sc2.compiler.ast.AstNode.*;
import sc2.compiler.ast.Expr.IdExpr;
import sc2.compiler.ast.*;
import sc2.compiler.ast.SModule.Depend;

/**
 *
 * @author yangjiandong
 */
public class TopLevelTypeResolver  extends CompilePass {
    
    private ArrayList<Scope> scopes = new ArrayList<>();
    private SModule module;
    private Compiler compiler;
    
    public TopLevelTypeResolver(CompilerLog log, SModule module, Compiler compiler) {
        super(log);
        this.module = module;
        this.log = log;
        this.compiler = compiler;
    }
    
    public void run() {
        module.walkChildren(this);
    }
    
    private AstNode findSymbol(String name, Loc loc) {
        for (int i = scopes.size()-1; i >=0; --i) {
            Scope scope = scopes.get(i);
            AstNode node = scope.get(name, loc, log);
            if (node != null) {
                return node;
            }
        }
        err("Unknow symbol "+name, loc);
        return null;
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
        
    private void resolveType(Type type, Loc loc) {
        if (type == null) {
            err("Type inference not support for top level node", loc);
            return;
        }
        resolveId(type.id);
        if (type.id.resolvedDef != null) {
            if (type.id.resolvedDef instanceof TypeDef) {
                //ok
                type.id.resolvedType = Type.metaType(type.loc, type);
            }
            else if (type.id.resolvedDef instanceof TypeAlias ta) {
                type.id.resolvedDef = ta.type.id.resolvedDef;
                type.id.resolvedType = Type.metaType(type.loc, type);
            }
            else {
                type.id.resolvedDef = null;
                err("It's not a type: "+type.id.name, type.loc);
            }
        }
        else {
            return;
        }
        
        if (type.genericArgs != null) {
            boolean genericOk = false;
            if (type.id.resolvedDef instanceof StructDef sd) {
                if (sd.generiParamDefs != null) {
                    if (type.genericArgs.size() == sd.generiParamDefs.size()) {
                        type.id.resolvedDef = sd.parameterize(type.genericArgs);
                        genericOk = true;
                    }
                }
            }
            if (!genericOk) {
                err("Generic args not match", type.loc);
            }
        }
        else if (type.id.resolvedDef instanceof StructDef sd) {
            if (sd.generiParamDefs != null) {
                err("Miss generic args", type.loc);
            }
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
        resolveType(v.fieldType, v.loc);
    }

    @Override
    public void visitFunc(AstNode.FuncDef v) {
        resolveType(v.prototype.returnType, v.loc);
        if (v.prototype.paramDefs != null) {
            for (AstNode.ParamDef p : v.prototype.paramDefs) {
                if (p.paramType != null) {
                    p.paramType.setToImutable();
                }
                resolveType(p.paramType, p.loc);
            }
        }
        
        if (v.parent instanceof StructDef sd) {
            if ((v.flags & FConst.Virtual) != 0) {
                if ((sd.flags & FConst.Virtual) != 0 || (sd.flags & FConst.Abstract) != 0) {
                    //ok
                }
                else {
                    err("Struct must be virtual or abstract", v.loc);
                }
            }
            else if ((v.flags & FConst.Abstract) != 0) {
                if ((sd.flags & FConst.Abstract) != 0) {
                    //ok
                }
                else {
                    err("Struct must be abstract", v.loc);
                }
                if (v.code != null) {
                    err("abstract method must no code", v.loc);
                }
            }
        }
        else if (v.parent instanceof TraitDef tt) {
            if ((v.flags & FConst.Abstract) != 0) {
                if (v.code != null) {
                    err("abstract method must no code", v.loc);
                }
            }
        }
        else {
            if ((v.flags & FConst.Abstract) != 0 ||
                    (v.flags & FConst.Virtual) != 0 ||
                    (v.flags & FConst.Static) != 0) {
                err("Invalid flags", v.loc);
            }
        }
        
        if ((v.flags & FConst.Readonly) != 0) {
            err("Invalid flags", v.loc);
        }
    }

    @Override
    public void visitTypeDef(AstNode.TypeDef v) {
        Scope gpScope = null;
        if (v instanceof StructDef sd) {
            if (sd.generiParamDefs != null) {
                gpScope = new Scope();
                for (GenericParamDef gp : sd.generiParamDefs) {
                    gpScope.put(gp.name, gp);
                }
                this.scopes.add(gpScope);
            }
            if (sd.inheritances != null) {
                int i = 0;
                for (Type inh : sd.inheritances) {
                    this.resolveType(inh, inh.loc);
                    if (i > 0) {
                        if (inh.id.resolvedDef != null) {
                            if (!(inh.id.resolvedDef instanceof TraitDef)) {
                                err("Unsupport multi struct inheritance", inh.loc);
                            }
                        }
                    }
                    ++i;
                }
            }
        }
        v.walkChildren(this);
        
        if (gpScope != null) {
            this.scopes.remove(this.scopes.size()-1);
        }
    }
    
    @Override
    public void visitTypeAlias(TypeAlias v) {
        this.resolveType(v.type, v.loc);
    }

    @Override
    public void visitStmt(Stmt v) {
        
    }

    @Override
    public void visitExpr(Expr v) {
        
    }
}
