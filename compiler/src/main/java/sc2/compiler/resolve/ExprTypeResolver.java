//
// Copyright (c) 2024, chunquedong
// Licensed under the Academic Free License version 3.0
//
package sc2.compiler.resolve;

import java.util.ArrayDeque;
import sc2.compiler.ast.Scope;
import java.util.ArrayList;
import java.util.HashMap;
import sc2.compiler.CompilePass;
import sc2.compiler.CompilerLog;
import sc2.compiler.ast.AstNode;
import sc2.compiler.ast.AstNode.*;
import sc2.compiler.ast.*;
import sc2.compiler.ast.Expr.*;
import sc2.compiler.ast.Stmt.LocalDefStmt;
import sc2.compiler.ast.Token.TokenKind;
import static sc2.compiler.ast.Token.TokenKind.*;
import sc2.compiler.ast.Type.ArrayType;
import sc2.compiler.ast.Type.FuncType;

/**
 *
 * @author yangjiandong
 */
public class ExprTypeResolver extends CompilePass {
    
    private ArrayList<Scope> scopes = new ArrayList<>();
    private SModule module;
    
    //for func param or for init
    private Scope preScope = null;
    
    private ArrayDeque<AstNode> funcs = new ArrayDeque<AstNode>();
    private ArrayDeque<AstNode> loops = new ArrayDeque<AstNode>();
    private StructDef curStruct = null;
    
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
    
    private Scope lastScope() {
        if (preScope != null) {
            return preScope;
        }
        return scopes.get(scopes.size()-1);
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
    
    private void resolveId(Expr.IdExpr idExpr) {
        if (idExpr.namespace == null) {
            if (idExpr.name.equals("this")) {
                if (curStruct == null) {
                    err("Use this out of struct", idExpr.loc);
                    return;
                }
                Type self = new Type(curStruct.loc, curStruct.name);
                self.id.resolvedDef = curStruct;
                idExpr.resolvedDef = Type.pointerType(idExpr.loc, self, Type.PointerAttr.raw, false);
                return;
            }
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
        
    private void resolveType(Type type) {
        resolveId(type.id);
        if (type.id.resolvedDef != null) {
            if (type.id.resolvedDef instanceof TypeDef) {
                //ok
            }
            else if (type.id.resolvedDef instanceof TypeAlias ta) {
                type.id.resolvedDef = ta.type.id.resolvedDef;
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
        if (v.fieldType != null) {
            resolveType(v.fieldType);
        }
        if (v.initExpr != null) {
            this.visit(v.initExpr);
            if (v.initExpr.resolvedType != null) {
                if (v.fieldType == null) {
                    v.fieldType = v.initExpr.resolvedType;
                }
                else {
                    if (!v.initExpr.resolvedType.fit(v.fieldType)) {
                        err("Invalid assign", v.loc);
                    }
                }
            }
            if (v.fieldType == null) {
                v.fieldType = v.initExpr.resolvedType;
            }
        }
        if (v.fieldType == null) {
            err("Unkonw field type", v.loc);
        }
        if (v.isLocalVar) {
            lastScope().put(v.name, v);
        }
    }
    
    private void visitFuncPrototype(AstNode.FuncPrototype prototype, Scope scope) {
        if (prototype != null && prototype.paramDefs != null) {
            for (AstNode.ParamDef p : prototype.paramDefs) {
                this.resolveType(p.paramType);
                if (p.defualtValue != null) {
                    this.visit(p.defualtValue);
                }
                scope.put(p.name, p);
            }
        }
        
        if (prototype != null) {
            if (prototype.returnType != null && prototype.returnType.isVoid()) {
                this.resolveType(prototype.returnType);
            }
        }
    }

    @Override
    public void visitFunc(FuncDef v) {
        this.funcs.push(v);
        preScope = new Scope();
        visitFuncPrototype(v.prototype, preScope);
        if ((v.flags & AstNode.Operator) != 0) {
            verifyOperator(v);
        }
        if (v.code != null) {
            this.visit(v.code);
        }
        preScope = null;
        
        funcs.pop();
    }

    @Override
    public void visitTypeDef(TypeDef v) {
        int scopeCount = 1;
        if (v instanceof StructDef sd) {
            curStruct = sd;
            if (sd.inheritances != null) {
                Scope inhScopes = pushScope();
                int i = 0;
                for (Type inh : sd.inheritances) {
                    this.resolveType(inh);
                    if (i > 0) {
                        if (inh.id.isResolved()) {
                            if (!(inh.id.resolvedDef instanceof TraitDef)) {
                                err("Unsupport multi struct inheritance", inh.loc);
                            }
                        }
                    }
                    if (inh.id.resolvedDef != null && inh.id.resolvedDef instanceof StructDef inhSd) {
                        Scope inhScope = inhSd.getScopeForInherite();
                        inhScopes.addAll(inhScope);
                    }
                    ++i;
                }
                ++scopeCount;
                
                for (FieldDef f : sd.fieldDefs) {
                    if (inhScopes.contains(f.name)) {
                        err("Field name is already exsits"+f.name, f.loc);
                    }
                }
                
                for (FuncDef f : sd.funcDefs) {
                    if ((f.flags & AstNode.Static) != 0 || (f.flags | AstNode.Override) != 0) {
                        continue;
                    }
                    if (inhScopes.contains(f.name)) {
                        err("Field name is already exsits"+f.name, f.loc);
                    }
                }
            }
        }
        Scope scope = v.getScope();
        this.scopes.add(scope);
        v.walkChildren(this);
        
        for (int i=0; i<scopeCount; ++i) {
            popScope();
        }
        curStruct = null;
    }

    @Override
    public void visitStmt(Stmt v) {
        if (v instanceof Block bs) {
            if (preScope != null) {
                this.scopes.add(preScope);
                preScope = null;
            }
            else {
                pushScope();
            }
            bs.walkChildren(this);
            popScope();
        }
        else if (v instanceof Stmt.IfStmt ifs) {
            this.visit(ifs.condition);
            this.visit(ifs.block);
            if (ifs.elseBlock != null) {
                this.visit(ifs.elseBlock);
            }
            verifyBool(ifs.condition);
        }
        else if (v instanceof Stmt.LocalDefStmt e) {
            this.visit(e.fieldDef);
        }
        else if (v instanceof Stmt.WhileStmt whiles) {
            this.loops.push(v);
            this.visit(whiles.condition);
            this.visit(whiles.block);
            verifyBool(whiles.condition);
            this.loops.pop();
        }
        else if (v instanceof Stmt.ForStmt fors) {
            this.loops.push(v);
            if (fors.init != null) {
                pushScope();
                
                if (fors.init instanceof Stmt.LocalDefStmt varDef) {
                    this.visit(varDef.fieldDef);
                }
                else if (fors.init instanceof Stmt.ExprStmt s) {
                    this.visit(s.expr);
                }
                else {
                    err("Unsupport for init stmt", fors.init.loc);
                }
            }
            
            if (fors.condition != null) {
                this.visit(fors.condition);
                verifyBool(fors.condition);
            }
            
            if (fors.update != null) {
                this.visit(fors.update);
            }
            this.visit(fors.block);
            
            if (fors.init != null) {
                this.popScope();
            }
            this.loops.pop();
        }
        else if (v instanceof Stmt.SwitchStmt switchs) {
            this.visit(switchs.condition);
            verifyInt(switchs.condition);
            
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
            if (this.loops.size() == 0) {
                err("break, continue outside of loop", v.loc);
            }
        }
        else if (v instanceof Stmt.UnsafeBlock bs) {
            this.visit(bs.block);
        }
        else if (v instanceof Stmt.ReturnStmt rets) {
            if (rets.expr != null) {
                this.visit(rets.expr);
                if (rets.expr.resolvedType != null) {
                    AstNode func = this.funcs.getLast();
                    FuncPrototype prototype;
                    if (func instanceof FuncDef f) {
                        prototype = f.prototype;
                    }
                    else {
                        ClosureExpr f = (ClosureExpr)func;
                        prototype = f.prototype;
                    }
                    if (!rets.expr.resolvedType.fit(prototype.returnType)) {
                        err("Return type not fit function", rets.expr.loc);
                    }
                }
            }
        }
        else {
            err("Unkown stmt:"+v, v.loc);
        }
    }
    
    private Type getSlotType(AstNode resolvedDef) {
        if (resolvedDef instanceof FieldDef f) {
            return f.fieldType;
        }
        else if (resolvedDef instanceof FuncDef f) {
            return Type.funcType(f);
        }
        else if (resolvedDef instanceof TypeAlias f) {
            return Type.metaType(f.loc, f.type);
        }
        else if (resolvedDef instanceof TypeDef f) {
            //TODO
            return Type.metaType(f.loc, new Type(f.loc, f.name));
        }
        return null;
    }
    
    private void verifyBool(Expr condition) {
        if (condition.resolvedType != null && !condition.resolvedType.isBool()) {
            err("Must be Bool", condition.loc);
        }
    }
    
    private void verifyInt(Expr e) {
        if (e.resolvedType != null && !e.resolvedType.isInt()) {
            err("Must be Int type", e.loc);
        }
    }
    
    private void verifyMetType(Expr e) {
        if (e.resolvedType != null && !e.resolvedType.isMetaType()) {
            err("Type required", e.loc);
        }
    }
    
    private void verifyOperator(FuncDef f) {
        if (f.name.equals("plus") || f.name.equals("minus") || 
                f.name.equals("mult") || f.name.equals("div")) {
            if (f.prototype.paramDefs.size() != 1) {
                err("Must 1 params", f.loc);
            }
            if (f.prototype.returnType.isVoid()) {
                err("Must has return", f.loc);
            }
        }
        else if (f.name.equals("compare")) {
            if (f.prototype.paramDefs.size() != 1) {
                err("Must 1 params", f.loc);
            }
            if (!f.prototype.returnType.isInt()) {
                err("Must return Int", f.loc);
            }
        }
        else if (f.name.equals(Buildin.getOperator)) {
            if (f.prototype.paramDefs.size() != 1) {
                err("Must 1 params", f.loc);
            }
            if (f.prototype.returnType.isVoid()) {
                err("Must has return", f.loc);
            }
        }
        else if (f.name.equals(Buildin.setOperator)) {
            if (f.prototype.paramDefs.size() != 2) {
                err("Must 1 params", f.loc);
            }
        }
    }
    
    private AstNode resoveOnTarget(Expr target, String name, Loc loc) {
        if (!target.isResolved()) {
            return null;
        }
        
        AstNode resolvedDef = target.resolvedType.id.resolvedDef;
        if (resolvedDef != null) {
            if (resolvedDef instanceof TypeDef t) {
                Scope scope = t.getScope();
                AstNode def = scope.get(name, loc, log);
                if (def == null) {
                    err("Unkown name:"+name, loc);
                }
                return def;
            }
        }
        return null;
    }

    @Override
    public void visitExpr(Expr v) {
        if (v instanceof Expr.IdExpr e) {
            resolveId(e);
            if (e.resolvedDef != null) {
                e.resolvedType = getSlotType(e.resolvedDef);
            }
        }
        else if (v instanceof Expr.AccessExpr e) {
            this.visit(e.target);
            e.resolvedDef = resoveOnTarget(e.target, e.name, e.loc);
            if (e.resolvedDef != null) {
                e.resolvedType = getSlotType(e.resolvedDef);
            }
        }
        else if (v instanceof Expr.LiteralExpr e) {
            if (e.value instanceof Long) {
                v.resolvedType = Type.intType(e.loc);
            }
            else if (e.value instanceof Double) {
                v.resolvedType = Type.floatType(e.loc);
            }
            else if (e.value instanceof Boolean) {
                v.resolvedType = Type.boolType(e.loc);
            }
            else if (e.value instanceof String) {
                v.resolvedType = Type.strType(e.loc);
            }
        }
        else if (v instanceof Expr.BinaryExpr e) {
            resolveBinaryExpr(e);
        }
        else if (v instanceof Expr.CallExpr e) {
            resolveCallExpr(e);
        }
        else if (v instanceof Expr.UnaryExpr e) {
            this.visit(e.operand);
            if (e.operand.isResolved()) {
                TokenKind curt = e.opToken;
                switch (curt) {
                    //~
                    case tilde:
                        verifyInt(e.operand);
                        e.resolvedType = e.operand.resolvedType;
                        break;
                    //!
                    case bang:
                        verifyBool(e.operand);
                        e.resolvedType = e.operand.resolvedType;
                        break;
                    //+, -
                    case plus:
                    case minus:
                        e.resolvedType = e.operand.resolvedType;
                        break;
                    //*
                    case star:
                        if (!e.operand.resolvedType.isPointerType()) {
                            err("Invalid * for non pointer", e.loc);
                        }
                        else {
                            e.resolvedType = e.operand.resolvedType.genericArgs.get(0);
                        }
                        break;
                    //++, --
                    case increment:
                    case decrement:
                        verifyInt(e.operand);
                        e.resolvedType = e.operand.resolvedType;
                        break;
                    //&
                    case amp:
                        e.resolvedType = Type.pointerType(e.loc, e.operand.resolvedType, Type.PointerAttr.ref, true);
                        break;
                    case awaitKeyword:
                        e.resolvedType = e.operand.resolvedType;
                        break;
                    default:
                        break;
                }
            }
        }
        else if (v instanceof Expr.TypeExpr e) {
            e.resolvedType = Type.metaType(e.loc, e.type);
        }
        else if (v instanceof Expr.IndexExpr e) {
            this.visit(e.target);
            this.visit(e.index);
            verifyInt(e.index);
            
            if (e.target.isResolved()) {
                if (e.target.resolvedType.isArray() && e.target.resolvedType.genericArgs != null) {
                    e.resolvedType = e.target.resolvedType.genericArgs.get(0);
                }
                else {
                    String operatorName = e.inLeftSide ? Buildin.setOperator : Buildin.getOperator;
                    AstNode rdef = resoveOnTarget(e.target, operatorName, e.loc);
                    if (rdef == null) {
                        err("Unknow operator []", e.loc);
                    }
                    else if (rdef instanceof FuncDef f) {
                        int expectedParamSize = e.inLeftSide ? 2:1;
                        if (f.prototype.paramDefs == null || f.prototype.paramDefs.size() != expectedParamSize) {
                            err("Invalid operator []", e.loc);
                        }
                        e.resolvedType = f.prototype.returnType;
                    }
                    else {
                        err("Invalid operator []", e.loc);
                    }
                }
            }
        }
        else if (v instanceof Expr.GenericInstance e) {
            this.visit(e.target);
            for (Type t : e.genericArgs) {
                this.resolveType(t);
            }
        }
        else if (v instanceof Expr.IfExpr e) {
            this.visit(e.condition);
            this.visit(e.trueExpr);
            this.visit(e.falseExpr);
            verifyBool(e.condition);
        }
        else if (v instanceof Expr.InitBlockExpr e) {
            resolveInitBlockExpr(e);
        }
        else if (v instanceof ClosureExpr e) {
            this.funcs.push(v);

//            for (Expr t : e.captures) {
//                this.visit(t);
//            }
            
            preScope = new Scope();
            
            visitFuncPrototype(e.prototype, preScope);
            this.visit(e.code);
            
            preScope = null;
            this.funcs.pop();
            
            e.resolvedType = Type.voidType(e.loc);
        }
        else {
            err("Unkown expr:"+v, v.loc);
        }
    }

    private void resolveInitBlockExpr(Expr.InitBlockExpr e) {
        this.visit(e.target);
        if (e.args != null) {
            for (Expr.CallArg t : e.args) {
                this.visit(t.argExpr);
            }
        }
        if (!e.target.isResolved()) {
            return;
        }
        
        StructDef sd = null;
        if (e.target instanceof IdExpr id) {
            if (id.resolvedDef instanceof StructDef) {
                sd = (StructDef)id.resolvedDef;
            }
        }
        else if (e.target instanceof CallExpr call) {
            AstNode rdef = e.target.resolvedType.id.resolvedDef;
            if (rdef != null) {
                if (rdef instanceof StructDef) {
                    sd = (StructDef)rdef;
                }
            }
        }
        else if (e.target instanceof TypeExpr te) {
            if (te.type instanceof ArrayType at) {
                e.isArray = true;
                for (Expr.CallArg t : e.args) {
                    if (t.name != null) {
                        err("Invalid name for array", t.loc);
                    }
                }
                at.sizeExpr = new LiteralExpr(Long.valueOf(e.args.size()));
                at.sizeExpr.loc = e.loc;

                e.resolvedType = te.type;
            }
        }

        if (sd != null) {
            for (FieldDef f : sd.fieldDefs) {
                if (f.initExpr != null) {
                    continue;
                }
                boolean found = false;
                for (Expr.CallArg t : e.args) {
                    if (t.name.equals(f.name)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    err("Field not init:"+f.name, e.loc);
                }
            }

            for (Expr.CallArg t : e.args) {
                boolean found = false;
                for (FieldDef f : sd.fieldDefs) {
                    if (t.name.equals(f.name)) {
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    err("Field not found:"+t.name, t.loc);
                }
            }
            e.resolvedType = e.target.resolvedType;
        }
        else if (!e.isArray) {
            err("Invalid init block", e.loc);
        }
    }
    
    private void resolveGenericInstance(Expr.GenericInstance e) {
        this.visit(e.target);
        if (!e.target.isResolved()) {
            return;
        }
        
        IdExpr idExpr;
        if (e.target instanceof IdExpr) {
            idExpr = (IdExpr)e.target;
        }
        else {
            err("Unexpected generic args", e.loc);
            return;
        }
        
        if (e.genericArgs != null) {
            boolean genericOk = false;
            if (idExpr.resolvedDef instanceof StructDef sd) {
                if (sd.generiParamDefs != null) {
                    if (e.genericArgs.size() == sd.generiParamDefs.size()) {
                        idExpr.resolvedDef = sd.parameterize(e.genericArgs);
                        genericOk = true;
                    }
                }
            }
            else if (idExpr.resolvedDef instanceof FuncDef sd) {
                if (sd.generiParamDefs != null) {
                    if (e.genericArgs.size() == sd.generiParamDefs.size()) {
                        idExpr.resolvedDef = sd.parameterize(e.genericArgs);
                        genericOk = true;
                    }
                }
            }
            err("Generic args not match", e.loc);
        }
        else if (idExpr.resolvedDef instanceof StructDef sd) {
            if (sd.generiParamDefs != null) {
                err("Miss generic args", idExpr.loc);
            }
        }
        else if (idExpr.resolvedDef instanceof FuncDef sd) {
            if (sd.generiParamDefs != null) {
                err("Miss generic args", idExpr.loc);
            }
        }
    }

    private void resolveCallExpr(Expr.CallExpr e) {
        this.visit(e.target);
        if (e.args != null) {
            for (Expr.CallArg t : e.args) {
                this.visit(t.argExpr);
            }
        }
        if (e.target.isResolved()) {
            if (e.target.resolvedType instanceof FuncType f) {
                e.resolvedType = f.prototype.returnType;
            }
            else {
                err("Call a non-function type:"+e.target, e.loc);
            }
        }
        else {
            return;
        }
    }

    private void resolveBinaryExpr(Expr.BinaryExpr e) {

        this.visit(e.lhs);
        this.visit(e.rhs);
        
        if (e.lhs.isResolved() && e.rhs.isResolved()) {
            TokenKind curt = e.opToken;
            switch (curt) {
                case isKeyword:
                    verifyMetType(e.rhs);
                    e.resolvedType = Type.boolType(e.loc);
                    break;
                case asKeyword:
                    verifyMetType(e.rhs);
                    e.resolvedType = e.rhs.resolvedType;
                    break;
                case eq:
                case notEq:
                case same:
                case notSame:
                case lt:
                case gt:
                case ltEq:
                case gtEq:
                    if (e.lhs.resolvedType.isInt() && e.rhs.resolvedType.isInt()) {
                        //OK
                    }
                    else if (e.lhs.resolvedType.isFloat() && e.rhs.resolvedType.isFloat()) {
                        //OK
                    }
                    else if ((e.lhs.resolvedType.isFloat() && e.rhs.resolvedType.isInt()) ||
                            (e.lhs.resolvedType.isInt() && e.rhs.resolvedType.isFloat())) {
                        if (curt == TokenKind.eq || curt == TokenKind.notEq || curt == TokenKind.same || curt == TokenKind.notSame) {
                            err("Cant compare different type", e.loc);
                        }
                    }
                    else if (!e.lhs.resolvedType.equals(e.rhs.resolvedType)) {
                        err("Cant compare different type", e.loc);
                    }
                    else {
                        String operatorName = Buildin.operatorToName(TokenKind.cmp);
                        AstNode rdef = resoveOnTarget(e.lhs, operatorName, e.loc);
                        if (rdef != null) {
                            err("Unknow operator:"+curt, e.loc);
                        }
                        else if (rdef instanceof FuncDef f) {
                        }
                        else {
                            err("Invalid operator:"+curt, e.loc);
                        }
                    }
                    e.resolvedType = Type.boolType(e.loc);
                    break;
                case leftShift:
                case rightShift:
                case pipe:
                case caret:
                case amp:
                case percent:
                    verifyInt(e.lhs);
                    verifyInt(e.rhs);
                    e.resolvedType = Type.intType(e.loc);
                    break;
                case plus:
                case minus:
                case star:
                case slash:
                    if (e.lhs.resolvedType.isInt() && e.rhs.resolvedType.isInt()) {
                        e.resolvedType = e.lhs.resolvedType;
                    }
                    else if (e.lhs.resolvedType.isFloat() && e.rhs.resolvedType.isFloat()) {
                        e.resolvedType = e.lhs.resolvedType;
                    }
                    else if ((e.lhs.resolvedType.isFloat() && e.rhs.resolvedType.isInt()) ||
                            (e.lhs.resolvedType.isInt() && e.rhs.resolvedType.isFloat())) {
                        e.resolvedType = Type.floatType(e.loc);
                    }
                    else {
                        resolveMathOperator(curt, e);
                    }
                    
                    break;
                case assign:
                case assignPlus:
                case assignMinus:
                case assignStar:
                case assignSlash:
                case assignPercent:
                    boolean ok = false;
                    if (e.lhs instanceof IdExpr idExpr) {
                        if (idExpr.resolvedDef instanceof FieldDef f) {
                            ok = true;
                        }
                    }
                    else if (e.lhs instanceof AccessExpr accessExpr) {
                        if (accessExpr.resolvedDef instanceof FieldDef f) {
                            ok = true;
                        }
                    }
                    else if (e.lhs instanceof IndexExpr indexExpr) {
                        ok = true;
                        return;
                    }
                    
                    if (!ok) {
                        err("Not assignable", e.lhs.loc);
                    }
                    else {
                        if (e.lhs.resolvedType.isInt() && e.rhs.resolvedType.isInt()) {
                            e.resolvedType = e.lhs.resolvedType;
                        }
                        else if (e.lhs.resolvedType.isFloat() && e.rhs.resolvedType.isFloat()) {
                            e.resolvedType = e.lhs.resolvedType;
                        }
                        else if (e.lhs.resolvedType.isFloat() && e.rhs.resolvedType.isInt()) {
                            //OK
                        }
                        else if (!e.lhs.resolvedType.equals(e.rhs.resolvedType)) {
                            err("Cant compare different type", e.loc);
                        }
                        else {
                            TokenKind overrideToken = null;
                            if (curt == TokenKind.assignPlus) {
                                overrideToken = TokenKind.plus;
                            }
                            else if (curt == TokenKind.assignMinus) {
                                overrideToken = TokenKind.minus;
                            }
                            else if (curt == TokenKind.assignStar) {
                                overrideToken = TokenKind.star;
                            }
                            else if (curt == TokenKind.assignSlash) {
                                overrideToken = TokenKind.slash;
                            }

                            if (overrideToken != null) {
                                resolveMathOperator(overrideToken, e);
                            }
                            else {
                                err("Unsupport operator:"+curt, e.loc);
                            }
                        }
                        e.resolvedType = e.lhs.resolvedType;
                    }
                    
                    break;
                default:
                    break;
            }
        }
    }

    private void resolveMathOperator(TokenKind curt, Expr.BinaryExpr e) {
        String operatorName = Buildin.operatorToName(curt);
        if (operatorName == null) {
            err("Unknow operator:"+curt, e.loc);
        }
        AstNode rdef = resoveOnTarget(e.lhs, operatorName, e.loc);
        if (rdef == null) {
            err("Unknow operator:"+curt, e.loc);
        }
        else if (rdef instanceof FuncDef f) {
            if (!e.rhs.resolvedType.fit(f.prototype.paramDefs.get(0).paramType)) {
                err("Invalid arg type:"+e.rhs.resolvedType, e.rhs.loc);
            }
            e.resolvedType = f.prototype.returnType;
        }
        else {
            err("Invalid operator:"+curt, e.loc);
        }
    }

}
